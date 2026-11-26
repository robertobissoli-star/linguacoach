package com.linguacoach.app.ai

import com.linguacoach.app.data.ChatMessage
import com.linguacoach.app.data.Correction
import com.linguacoach.app.data.Level
import com.linguacoach.app.data.Role
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class TutorReply(
    val reply: String,
    val corrections: List<Correction>,
    val encouragement: String?,
)

/**
 * Talks to the Anthropic Messages API (POST /v1/messages) over raw HTTPS — the
 * idiomatic choice on Android, where the user supplies their own key. The tutor
 * is prompted to return a small JSON object so we can show a natural reply plus
 * instant corrections.
 */
class ClaudeClient {

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    private val json = "application/json".toMediaType()

    fun systemPrompt(level: Level): String = """
        You are a warm, encouraging English conversation tutor. The learner's CEFR level is ${level.code} (${level.label}).
        Goals: hold a natural spoken-style conversation AND correct the learner's English.

        Rules:
        - Keep your reply short (1–3 sentences) and always end with a question to keep the conversation going.
        - Match the learner's level: simpler words for A1/A2, richer language for B2/C1.
        - Look at the learner's LAST message and list only its real mistakes (grammar, word choice, word order, spelling).
          Do not invent mistakes. If it is already correct, return an empty "corrections" array.
        - For each mistake give the exact wrong fragment, the fixed version, and a one-line explanation in Italian.
        - "encouragement": one short motivating line in Italian (may be empty).

        Respond with ONLY a JSON object, no markdown, no extra text:
        {"reply": "<your English reply>", "corrections": [{"original": "<wrong>", "fixed": "<correct>", "note": "<spiegazione in italiano>"}], "encouragement": "<breve incoraggiamento in italiano>"}
    """.trimIndent()

    suspend fun converse(
        apiKey: String,
        model: String,
        level: Level,
        history: List<ChatMessage>,
    ): TutorReply = withContext(Dispatchers.IO) {
        val messages = JSONArray()
        history.filter { !it.pending }.forEach { m ->
            messages.put(
                JSONObject()
                    .put("role", if (m.role == Role.USER) "user" else "assistant")
                    .put("content", m.text)
            )
        }

        val body = JSONObject()
            .put("model", model)
            .put("max_tokens", 1024)
            .put("system", systemPrompt(level))
            .put("output_config", JSONObject().put("effort", "low"))
            .put("messages", messages)
            .toString()

        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .post(body.toRequestBody(json))
            .build()

        http.newCall(request).execute().use { resp ->
            val raw = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                val msg = runCatching { JSONObject(raw).getJSONObject("error").getString("message") }
                    .getOrDefault("HTTP ${resp.code}")
                throw ClaudeException(mapError(resp.code, msg))
            }
            parseReply(raw)
        }
    }

    private fun parseReply(raw: String): TutorReply {
        val text = extractText(raw)
        val obj = extractJson(text)
        if (obj == null) {
            // Model didn't return JSON — treat the whole thing as the reply.
            return TutorReply(text.ifBlank { "…" }, emptyList(), null)
        }
        val corrections = ArrayList<Correction>()
        obj.optJSONArray("corrections")?.let { arr ->
            for (i in 0 until arr.length()) {
                val c = arr.optJSONObject(i) ?: continue
                val orig = c.optString("original").trim()
                val fixed = c.optString("fixed").trim()
                if (orig.isEmpty() && fixed.isEmpty()) continue
                corrections.add(Correction(orig, fixed, c.optString("note").trim()))
            }
        }
        return TutorReply(
            reply = obj.optString("reply").ifBlank { text },
            corrections = corrections,
            encouragement = obj.optString("encouragement").trim().ifBlank { null },
        )
    }

    /** First text content block (skips thinking blocks). */
    private fun extractText(raw: String): String {
        val content = JSONObject(raw).optJSONArray("content") ?: return ""
        for (i in 0 until content.length()) {
            val block = content.optJSONObject(i) ?: continue
            if (block.optString("type") == "text") return block.optString("text")
        }
        return ""
    }

    /** Pull a JSON object out of a possibly fenced/wrapped string. */
    private fun extractJson(s: String): JSONObject? {
        val start = s.indexOf('{')
        val end = s.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return runCatching { JSONObject(s.substring(start, end + 1)) }.getOrNull()
    }

    private fun mapError(code: Int, msg: String): String = when (code) {
        401 -> "Chiave API non valida. Controllala nelle impostazioni."
        429 -> "Troppe richieste o credito esaurito. Riprova tra poco."
        in 500..599 -> "Servizio Claude momentaneamente non disponibile. Riprova."
        else -> msg
    }
}

class ClaudeException(message: String) : Exception(message)

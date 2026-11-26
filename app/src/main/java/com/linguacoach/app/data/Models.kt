package com.linguacoach.app.data

import kotlinx.serialization.Serializable

/** CEFR level the tutor adapts to. */
enum class Level(val code: String, val label: String) {
    A1("A1", "Principiante"),
    A2("A2", "Elementare"),
    B1("B1", "Intermedio"),
    B2("B2", "Intermedio-alto"),
    C1("C1", "Avanzato");
}

/** A correction of one mistake in the learner's message. */
@Serializable
data class Correction(
    val original: String,
    val fixed: String,
    val note: String,
)

enum class Role { USER, TUTOR }

@Serializable
data class ChatMessage(
    val id: String,
    val role: Role,
    val text: String,
    val corrections: List<Correction> = emptyList(),
    val encouragement: String? = null,
    val at: Long = System.currentTimeMillis(),
    /** true while the tutor reply is being generated. */
    val pending: Boolean = false,
)

/** Conversation starter scenarios. */
data class Scenario(val emoji: String, val title: String, val opener: String)

val SCENARIOS = listOf(
    Scenario("☕", "Small talk", "Start a friendly casual chat with me and ask how my day is going."),
    Scenario("🍝", "Al ristorante", "Role-play a waiter at a restaurant and take my order."),
    Scenario("💼", "Colloquio di lavoro", "Act as an interviewer for a job I'm applying to and start the interview."),
    Scenario("✈️", "In viaggio", "Role-play a hotel receptionist helping me check in."),
    Scenario("🛒", "Fare la spesa", "Role-play a shop assistant and help me find what I need."),
    Scenario("🩺", "Dal medico", "Role-play a doctor asking me about my symptoms."),
)

/** App settings persisted on device. */
@Serializable
data class Settings(
    val apiKey: String = "",
    val level: String = Level.B1.name,
    val model: String = "claude-opus-5-5",
    val speakReplies: Boolean = true,
    val onboarded: Boolean = false,
    val tutorName: String = "Alex",
    /** Avatar photo as a small base64 JPEG; null = use a preset. */
    val avatar: String? = null,
    /** Preset avatar index used when [avatar] is null. */
    val avatarPreset: Int = 0,
)

/** Built-in avatar presets (emoji + background color) when no photo is set. */
val AVATAR_PRESETS = listOf(
    "🧑‍🏫" to 0xFF5B8CFFL,
    "👩‍🏫" to 0xFFF7788AL,
    "🦉" to 0xFF3FB98AL,
    "🤖" to 0xFF8A5BFFL,
    "🐨" to 0xFFE3A341L,
)

/** Models the user can pick (id to human label). */
val MODELS = listOf(
    "claude-haiku-5-5" to "Haiku 5.5 — veloce ed economico",
    "claude-sonnet-5-5" to "Sonnet 5.5 — equilibrato",
    "claude-opus-5-5" to "Opus 5.5 — massima qualità",
)

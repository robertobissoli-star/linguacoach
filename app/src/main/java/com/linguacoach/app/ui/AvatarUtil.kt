package com.linguacoach.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import kotlin.math.max

/** Encode/decode the tutor avatar photo as a small base64 JPEG. */
object AvatarUtil {
    private const val TARGET = 256
    private const val QUALITY = 80

    fun encodeFromUri(context: Context, uri: Uri): String? = runCatching {
        val bounds = context.contentResolver.openInputStream(uri).use { input ->
            BitmapFactory.Options().apply { inJustDecodeBounds = true }
                .also { BitmapFactory.decodeStream(input, null, it) }
        }
        val sample = max(1, max(bounds.outWidth, bounds.outHeight) / TARGET)
        val bmp = context.contentResolver.openInputStream(uri).use { input ->
            BitmapFactory.decodeStream(input, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null
        val edge = minOf(bmp.width, bmp.height)
        val square = Bitmap.createBitmap(bmp, (bmp.width - edge) / 2, (bmp.height - edge) / 2, edge, edge)
        val scaled = Bitmap.createScaledBitmap(square, TARGET, TARGET, true)
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, QUALITY, out)
        Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }.getOrNull()

    fun decode(base64: String?): Bitmap? {
        if (base64.isNullOrBlank()) return null
        return runCatching {
            val bytes = Base64.decode(base64, Base64.NO_WRAP)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }
}

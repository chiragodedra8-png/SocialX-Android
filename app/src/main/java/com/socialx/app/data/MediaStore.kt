package com.socialx.app.data

import android.content.Context
import android.net.Uri
import java.io.File

/** Copies picked media into app-private storage so posts survive source deletion. */
object MediaStoreHelper {
    fun copyToApp(context: Context, uri: Uri, kind: String): String? = try {
        val dir = File(context.filesDir, "media").apply { mkdirs() }
        val ext = when (kind) {
            "video" -> ".mp4"
            else -> ".jpg"
        }
        val target = File(dir, "${System.currentTimeMillis()}_${(1000..9999).random()}$ext")
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        }
        target.absolutePath
    } catch (e: Exception) {
        null
    }

    fun mediaKind(context: Context, uri: Uri): String {
        val type = context.contentResolver.getType(uri) ?: return "image"
        return if (type.startsWith("video")) "video" else "image"
    }
}

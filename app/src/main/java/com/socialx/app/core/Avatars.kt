package com.socialx.app.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.widget.ImageView
import coil.load
import java.io.File

private val palette = intArrayOf(
    0xFF1877F2.toInt(), 0xFF7B61FF.toInt(), 0xFF12B76A.toInt(),
    0xFFF79009.toInt(), 0xFFEE46BC.toInt(), 0xFF06AED4.toInt(),
    0xFFE0245E.toInt(), 0xFF6172F3.toInt()
)

fun initialsDrawable(context: Context, name: String): Drawable {
    val size = 160
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = palette[(name.hashCode().let { if (it < 0) -it else it }) % palette.size]
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
    paint.color = Color.WHITE
    paint.textSize = size * 0.42f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.textAlign = Paint.Align.CENTER
    val label = Utils.initials(name.ifBlank { "?" })
    val y = size / 2f - (paint.descent() + paint.ascent()) / 2f
    canvas.drawText(label, size / 2f, y, paint)
    return BitmapDrawable(context.resources, bitmap)
}

fun ImageView.loadAvatar(path: String, name: String) {
    val hasFile = path.isNotBlank() && (path.startsWith("http") || File(path).exists())
    if (hasFile) {
        load(path) {
            crossfade(true)
            placeholder(initialsDrawable(context, name))
            error(initialsDrawable(context, name))
        }
    } else {
        setImageDrawable(initialsDrawable(context, name))
    }
}

fun ImageView.loadMedia(path: String) {
    if (path.isBlank()) return
    if (path.startsWith("http") || File(path).exists()) {
        load(path) { crossfade(true) }
    }
}

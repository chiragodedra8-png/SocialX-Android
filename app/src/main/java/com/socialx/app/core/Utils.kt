package com.socialx.app.core

import android.content.Context
import android.text.format.DateUtils
import android.widget.Toast
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Base64
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object Utils {
    fun newId(): String = UUID.randomUUID().toString()

    fun now(): Long = System.currentTimeMillis()

    fun timeAgo(millis: Long): String {
        if (millis <= 0) return ""
        val diff = System.currentTimeMillis() - millis
        return when {
            diff < 60_000 -> "Just now"
            diff < 3_600_000 -> "${diff / 60_000}m"
            diff < 86_400_000 -> "${diff / 3_600_000}h"
            diff < 604_800_000 -> "${diff / 86_400_000}d"
            else -> SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(millis))
        }
    }

    fun fullDate(millis: Long): String =
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(millis))

    fun clock(millis: Long): String =
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(millis))

    fun dayLabel(millis: Long): String {
        val now = System.currentTimeMillis()
        return when {
            DateUtils.isToday(millis) -> "Today"
            millis > now - 172_800_000 -> "Yesterday"
            else -> SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(millis))
        }
    }

    fun initials(name: String): String {
        val parts = name.trim().split(" ").filter { it.isNotBlank() }
        return when {
            parts.isEmpty() -> "?"
            parts.size == 1 -> parts[0].take(1).uppercase(Locale.getDefault())
            else -> (parts[0].take(1) + parts[parts.size - 1].take(1)).uppercase(Locale.getDefault())
        }
    }

    fun hashPassword(password: String, salt: String): String {
        val spec = PBEKeySpec(password.toCharArray(), salt.toByteArray(), 12000, 256)
        val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
        val hash = skf.generateSecret(spec).encoded
        return Base64.getEncoder().encodeToString(hash)
    }

    fun newSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.getEncoder().encodeToString(bytes)
    }

    fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return Base64.getEncoder().encodeToString(digest)
    }

    fun isValidEmail(value: String): Boolean =
        android.util.Patterns.EMAIL_ADDRESS.matcher(value).matches()

    fun isValidPhone(value: String): Boolean =
        value.replace(" ", "").replace("-", "").length in 8..15 &&
            value.replace(" ", "").replace("-", "").all { it.isDigit() || it == '+' }

    fun toast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun compactCount(count: Int): String = when {
        count < 1000 -> count.toString()
        count < 1_000_000 -> String.format(Locale.US, "%.1fK", count / 1000.0)
        else -> String.format(Locale.US, "%.1fM", count / 1_000_000.0)
    }

    fun tagify(text: String): String = text
}

package com.socialx.app.core

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

class Prefs(context: Context) {
    private val sp: SharedPreferences =
        context.getSharedPreferences("socialx_prefs", Context.MODE_PRIVATE)

    var userId: String?
        get() = sp.getString("user_id", null)
        set(value) = sp.edit().putString("user_id", value).apply()

    var darkMode: Int
        get() = sp.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        set(value) {
            sp.edit().putInt("theme_mode", value).apply()
            AppCompatDelegate.setDefaultNightMode(value)
        }

    var privateAccount: Boolean
        get() = sp.getBoolean("private_account", false)
        set(value) = sp.edit().putBoolean("private_account", value).apply()

    var showOnlineStatus: Boolean
        get() = sp.getBoolean("show_online", true)
        set(value) = sp.edit().putBoolean("show_online", value).apply()

    var messageRequests: Boolean
        get() = sp.getBoolean("msg_requests", true)
        set(value) = sp.edit().putBoolean("msg_requests", value).apply()

    var twoStep: Boolean
        get() = sp.getBoolean("two_step", false)
        set(value) = sp.edit().putBoolean("two_step", value).apply()

    fun clearSession() = sp.edit().remove("user_id").apply()
}

package com.socialx.app

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.socialx.app.core.Prefs
import com.socialx.app.core.ServiceLocator

class SocialXApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
        AppCompatDelegate.setDefaultNightMode(Prefs(this).darkMode)
    }
}

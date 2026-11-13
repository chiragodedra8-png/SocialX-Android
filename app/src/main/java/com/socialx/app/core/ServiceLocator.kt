package com.socialx.app.core

import android.content.Context
import com.socialx.app.data.DbHelper
import com.socialx.app.data.LocalRepository
import com.socialx.app.data.Notifier
import com.socialx.app.data.SocialRepository

object ServiceLocator {
    lateinit var prefs: Prefs
        private set
    lateinit var db: DbHelper
        private set
    lateinit var repo: SocialRepository
        private set
    lateinit var notifier: Notifier
        private set

    fun init(context: Context) {
        if (::repo.isInitialized) return
        val app = context.applicationContext
        prefs = Prefs(app)
        db = DbHelper(app)
        notifier = Notifier(app)
        repo = LocalRepository(db, prefs, app, notifier)
    }
}

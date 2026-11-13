package com.socialx.app.core

/**
 * Central switch for the data backend.
 *
 *  false -> LocalRepository : fully working on-device SQLite backend.
 *  true  -> FirebaseRepository : Firestore / Firebase Auth / Firebase Storage.
 *
 * To go live on Firebase:
 *  1. Create a Firebase project and add an Android app with package com.socialx.app
 *  2. Replace app/google-services.json with the file Firebase gives you
 *  3. Set USE_FIREBASE = true and rebuild
 */
object AppConfig {
    const val USE_FIREBASE = false
    const val DB_NAME = "socialx.db"
    const val DB_VERSION = 1
    const val STORY_TTL_MS = 24L * 60 * 60 * 1000
    const val PAGE_SIZE = 15
}

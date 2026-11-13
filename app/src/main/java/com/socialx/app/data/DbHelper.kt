package com.socialx.app.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.socialx.app.core.AppConfig

class DbHelper(context: Context) :
    SQLiteOpenHelper(context, AppConfig.DB_NAME, null, AppConfig.DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE users(
                id TEXT PRIMARY KEY, name TEXT NOT NULL, username TEXT NOT NULL,
                email TEXT, phone TEXT, password_hash TEXT, salt TEXT,
                bio TEXT, avatar TEXT, cover TEXT, created_at INTEGER,
                is_private INTEGER DEFAULT 0, last_seen INTEGER DEFAULT 0,
                security_q TEXT, security_a TEXT)"""
        )
        db.execSQL(
            """CREATE TABLE posts(
                id TEXT PRIMARY KEY, user_id TEXT NOT NULL, text TEXT,
                media_uri TEXT, media_type TEXT, created_at INTEGER,
                shared_from TEXT)"""
        )
        db.execSQL("CREATE TABLE likes(post_id TEXT, user_id TEXT, created_at INTEGER, PRIMARY KEY(post_id,user_id))")
        db.execSQL(
            """CREATE TABLE comments(
                id TEXT PRIMARY KEY, post_id TEXT, user_id TEXT, text TEXT,
                created_at INTEGER, parent_id TEXT)"""
        )
        db.execSQL("CREATE TABLE saves(post_id TEXT, user_id TEXT, PRIMARY KEY(post_id,user_id))")
        db.execSQL(
            """CREATE TABLE friendships(
                id TEXT PRIMARY KEY, requester TEXT, addressee TEXT,
                status TEXT, created_at INTEGER)"""
        )
        db.execSQL("CREATE TABLE follows(follower TEXT, following TEXT, created_at INTEGER, PRIMARY KEY(follower,following))")
        db.execSQL(
            """CREATE TABLE stories(
                id TEXT PRIMARY KEY, user_id TEXT, media_uri TEXT, media_type TEXT,
                caption TEXT, created_at INTEGER, expires_at INTEGER)"""
        )
        db.execSQL("CREATE TABLE story_views(story_id TEXT, viewer TEXT, PRIMARY KEY(story_id,viewer))")
        db.execSQL(
            """CREATE TABLE conversations(
                id TEXT PRIMARY KEY, user_a TEXT, user_b TEXT, last_at INTEGER)"""
        )
        db.execSQL(
            """CREATE TABLE messages(
                id TEXT PRIMARY KEY, conversation_id TEXT, sender TEXT, text TEXT,
                media_uri TEXT, media_type TEXT, created_at INTEGER,
                read_at INTEGER DEFAULT 0, deleted INTEGER DEFAULT 0)"""
        )
        db.execSQL(
            """CREATE TABLE notifications(
                id TEXT PRIMARY KEY, user_id TEXT, actor TEXT, type TEXT, text TEXT,
                post_id TEXT, created_at INTEGER, is_read INTEGER DEFAULT 0)"""
        )
        db.execSQL("CREATE TABLE blocks(blocker TEXT, blocked TEXT, PRIMARY KEY(blocker,blocked))")
        db.execSQL(
            """CREATE TABLE reports(
                id TEXT PRIMARY KEY, reporter TEXT, target_type TEXT, target_id TEXT,
                reason TEXT, created_at INTEGER)"""
        )
        db.execSQL("CREATE INDEX idx_posts_created ON posts(created_at)")
        db.execSQL("CREATE INDEX idx_msg_conv ON messages(conversation_id, created_at)")
        db.execSQL("CREATE INDEX idx_notif_user ON notifications(user_id, created_at)")
        db.execSQL("CREATE UNIQUE INDEX idx_users_email ON users(email)")
        db.execSQL("CREATE UNIQUE INDEX idx_users_username ON users(username)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Simple migration policy for v1: recreate.
        listOf(
            "users", "posts", "likes", "comments", "saves", "friendships", "follows",
            "stories", "story_views", "conversations", "messages", "notifications",
            "blocks", "reports"
        ).forEach { db.execSQL("DROP TABLE IF EXISTS $it") }
        onCreate(db)
    }
}

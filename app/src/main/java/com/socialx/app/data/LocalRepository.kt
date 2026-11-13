package com.socialx.app.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.socialx.app.core.AppConfig
import com.socialx.app.core.Prefs
import com.socialx.app.core.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Fully functional on-device backend: real SQLite persistence, real password
 * hashing, real 24h story expiry, real message/read receipts.
 */
class LocalRepository(
    private val helper: DbHelper,
    private val prefs: Prefs,
    private val context: Context,
    private val notifier: Notifier
) : SocialRepository {

    private val db: SQLiteDatabase get() = helper.writableDatabase
    private val typingMap = mutableMapOf<String, Long>()

    // ---------------------------------------------------------------- auth

    override suspend fun signUp(
        name: String, username: String, email: String, phone: String,
        password: String, securityQuestion: String, securityAnswer: String
    ): Result<User> = withContext(Dispatchers.IO) {
        try {
            val cleanUsername = username.trim().lowercase().replace(" ", "")
            if (name.isBlank()) return@withContext Result.failure(Exception("Enter your name"))
            if (cleanUsername.length < 3) return@withContext Result.failure(Exception("Username must be at least 3 characters"))
            if (email.isNotBlank() && !Utils.isValidEmail(email)) return@withContext Result.failure(Exception("Enter a valid email"))
            if (phone.isNotBlank() && !Utils.isValidPhone(phone)) return@withContext Result.failure(Exception("Enter a valid phone number"))
            if (email.isBlank() && phone.isBlank()) return@withContext Result.failure(Exception("Add an email or a phone number"))
            if (password.length < 6) return@withContext Result.failure(Exception("Password must be at least 6 characters"))

            if (email.isNotBlank() && exists("email", email.trim())) {
                return@withContext Result.failure(Exception("That email is already registered"))
            }
            if (phone.isNotBlank() && exists("phone", phone.trim())) {
                return@withContext Result.failure(Exception("That phone number is already registered"))
            }
            if (exists("username", cleanUsername)) {
                return@withContext Result.failure(Exception("That username is taken"))
            }

            val id = Utils.newId()
            val salt = Utils.newSalt()
            val values = ContentValues().apply {
                put("id", id)
                put("name", name.trim())
                put("username", cleanUsername)
                put("email", email.trim())
                put("phone", phone.trim())
                put("password_hash", Utils.hashPassword(password, salt))
                put("salt", salt)
                put("bio", "")
                put("avatar", "")
                put("cover", "")
                put("created_at", Utils.now())
                put("is_private", 0)
                put("last_seen", Utils.now())
                put("security_q", securityQuestion)
                put("security_a", Utils.sha256(securityAnswer.trim().lowercase()))
            }
            db.insert("users", null, values)
            prefs.userId = id
            Result.success(getUser(id)!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun exists(column: String, value: String): Boolean {
        val c = db.rawQuery("SELECT 1 FROM users WHERE $column = ? LIMIT 1", arrayOf(value))
        val found = c.moveToFirst()
        c.close()
        return found
    }

    override suspend fun login(identifier: String, password: String): Result<User> =
        withContext(Dispatchers.IO) {
            try {
                val key = identifier.trim()
                val c = db.rawQuery(
                    "SELECT id, password_hash, salt FROM users WHERE email = ? OR phone = ? OR username = ? LIMIT 1",
                    arrayOf(key, key, key.lowercase())
                )
                var id: String? = null
                var hash = ""
                var salt = ""
                if (c.moveToFirst()) {
                    id = c.getString(0)
                    hash = c.getString(1) ?: ""
                    salt = c.getString(2) ?: ""
                }
                c.close()
                if (id == null) return@withContext Result.failure(Exception("No account found for that email, phone or username"))
                if (Utils.hashPassword(password, salt) != hash) {
                    return@withContext Result.failure(Exception("Incorrect password"))
                }
                touchUser(id)
                prefs.userId = id
                Result.success(getUser(id)!!)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun resetPassword(
        identifier: String, answer: String, newPassword: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (newPassword.length < 6) return@withContext Result.failure(Exception("Password must be at least 6 characters"))
            val key = identifier.trim()
            val c = db.rawQuery(
                "SELECT id, security_a FROM users WHERE email = ? OR phone = ? OR username = ? LIMIT 1",
                arrayOf(key, key, key.lowercase())
            )
            var id: String? = null
            var stored = ""
            if (c.moveToFirst()) {
                id = c.getString(0)
                stored = c.getString(1) ?: ""
            }
            c.close()
            if (id == null) return@withContext Result.failure(Exception("No account found"))
            if (Utils.sha256(answer.trim().lowercase()) != stored) {
                return@withContext Result.failure(Exception("Security answer does not match"))
            }
            val salt = Utils.newSalt()
            val values = ContentValues().apply {
                put("password_hash", Utils.hashPassword(newPassword, salt))
                put("salt", salt)
            }
            db.update("users", values, "id = ?", arrayOf(id))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun changePassword(
        userId: String, oldPassword: String, newPassword: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (newPassword.length < 6) return@withContext Result.failure(Exception("Password must be at least 6 characters"))
            val c = db.rawQuery("SELECT password_hash, salt FROM users WHERE id = ?", arrayOf(userId))
            var hash = ""
            var salt = ""
            if (c.moveToFirst()) {
                hash = c.getString(0) ?: ""
                salt = c.getString(1) ?: ""
            }
            c.close()
            if (Utils.hashPassword(oldPassword, salt) != hash) {
                return@withContext Result.failure(Exception("Current password is incorrect"))
            }
            val newSalt = Utils.newSalt()
            db.update(
                "users",
                ContentValues().apply {
                    put("password_hash", Utils.hashPassword(newPassword, newSalt))
                    put("salt", newSalt)
                },
                "id = ?", arrayOf(userId)
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteAccount(userId: String, password: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val c = db.rawQuery("SELECT password_hash, salt FROM users WHERE id = ?", arrayOf(userId))
                var hash = ""
                var salt = ""
                if (c.moveToFirst()) {
                    hash = c.getString(0) ?: ""
                    salt = c.getString(1) ?: ""
                }
                c.close()
                if (Utils.hashPassword(password, salt) != hash) {
                    return@withContext Result.failure(Exception("Password is incorrect"))
                }
                listOf("likes", "saves").forEach { db.delete(it, "user_id = ?", arrayOf(userId)) }
                db.delete("comments", "user_id = ?", arrayOf(userId))
                db.delete("posts", "user_id = ?", arrayOf(userId))
                db.delete("friendships", "requester = ? OR addressee = ?", arrayOf(userId, userId))
                db.delete("follows", "follower = ? OR following = ?", arrayOf(userId, userId))
                db.delete("stories", "user_id = ?", arrayOf(userId))
                db.delete("messages", "sender = ?", arrayOf(userId))
                db.delete("notifications", "user_id = ? OR actor = ?", arrayOf(userId, userId))
                db.delete("blocks", "blocker = ? OR blocked = ?", arrayOf(userId, userId))
                db.delete("users", "id = ?", arrayOf(userId))
                prefs.clearSession()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun currentUser(): User? = withContext(Dispatchers.IO) {
        prefs.userId?.let { getUser(it) }
    }

    override fun logout() {
        prefs.userId?.let { id ->
            db.update("users", ContentValues().apply { put("last_seen", Utils.now() - 600_000) }, "id = ?", arrayOf(id))
        }
        prefs.clearSession()
    }

    override fun securityQuestionFor(identifier: String): String? {
        val key = identifier.trim()
        val c = db.rawQuery(
            "SELECT security_q FROM users WHERE email = ? OR phone = ? OR username = ? LIMIT 1",
            arrayOf(key, key, key.lowercase())
        )
        var q: String? = null
        if (c.moveToFirst()) q = c.getString(0)
        c.close()
        return q
    }

    private fun touchUser(id: String) {
        db.update("users", ContentValues().apply { put("last_seen", Utils.now()) }, "id = ?", arrayOf(id))
    }

    // ------------------------------------------------------------- profile

    override suspend fun getUser(id: String): User? = withContext(Dispatchers.IO) {
        val c = db.rawQuery("SELECT * FROM users WHERE id = ?", arrayOf(id))
        val user = if (c.moveToFirst()) readUser(c) else null
        c.close()
        user
    }

    private fun readUser(c: Cursor) = User(
        id = c.getString(c.getColumnIndexOrThrow("id")),
        name = c.getString(c.getColumnIndexOrThrow("name")),
        username = c.getString(c.getColumnIndexOrThrow("username")),
        email = c.getString(c.getColumnIndexOrThrow("email")) ?: "",
        phone = c.getString(c.getColumnIndexOrThrow("phone")) ?: "",
        bio = c.getString(c.getColumnIndexOrThrow("bio")) ?: "",
        avatar = c.getString(c.getColumnIndexOrThrow("avatar")) ?: "",
        cover = c.getString(c.getColumnIndexOrThrow("cover")) ?: "",
        createdAt = c.getLong(c.getColumnIndexOrThrow("created_at")),
        isPrivate = c.getInt(c.getColumnIndexOrThrow("is_private")) == 1,
        lastSeen = c.getLong(c.getColumnIndexOrThrow("last_seen")),
        securityQuestion = c.getString(c.getColumnIndexOrThrow("security_q")) ?: ""
    )

    override suspend fun updateProfile(
        userId: String, name: String, bio: String, avatar: String,
        cover: String, isPrivate: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val values = ContentValues().apply {
                if (name.isNotBlank()) put("name", name.trim())
                put("bio", bio)
                if (avatar.isNotEmpty()) put("avatar", avatar)
                if (cover.isNotEmpty()) put("cover", cover)
                put("is_private", if (isPrivate) 1 else 0)
            }
            db.update("users", values, "id = ?", arrayOf(userId))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchUsers(query: String, viewerId: String): List<PersonCard> =
        withContext(Dispatchers.IO) {
            val like = "%${query.trim()}%"
            val c = db.rawQuery(
                "SELECT * FROM users WHERE (name LIKE ? OR username LIKE ?) AND id != ? ORDER BY name LIMIT 30",
                arrayOf(like, like, viewerId)
            )
            val list = mutableListOf<PersonCard>()
            while (c.moveToNext()) {
                val user = readUser(c)
                list.add(PersonCard(user, relation(viewerId, user.id), mutualFriends(viewerId, user.id)))
            }
            c.close()
            list
        }

    override suspend fun suggestedPeople(viewerId: String, limit: Int): List<PersonCard> =
        withContext(Dispatchers.IO) {
            val c = db.rawQuery(
                """SELECT * FROM users WHERE id != ? AND id NOT IN (
                     SELECT CASE WHEN requester = ? THEN addressee ELSE requester END
                     FROM friendships WHERE requester = ? OR addressee = ?
                   ) ORDER BY RANDOM() LIMIT ?""",
                arrayOf(viewerId, viewerId, viewerId, viewerId, limit.toString())
            )
            val list = mutableListOf<PersonCard>()
            while (c.moveToNext()) {
                val user = readUser(c)
                list.add(PersonCard(user, relation(viewerId, user.id), mutualFriends(viewerId, user.id)))
            }
            c.close()
            list
        }

    private fun relation(viewerId: String, otherId: String): String {
        if (viewerId == otherId) return PersonCard.SELF
        val c = db.rawQuery(
            "SELECT requester, status FROM friendships WHERE (requester = ? AND addressee = ?) OR (requester = ? AND addressee = ?)",
            arrayOf(viewerId, otherId, otherId, viewerId)
        )
        var result = PersonCard.NONE
        if (c.moveToFirst()) {
            val requester = c.getString(0)
            val status = c.getString(1)
            result = when {
                status == "ACCEPTED" -> PersonCard.FRIENDS
                requester == viewerId -> PersonCard.SENT
                else -> PersonCard.RECEIVED
            }
        }
        c.close()
        return result
    }

    private fun mutualFriends(a: String, b: String): Int {
        val c = db.rawQuery(
            """SELECT COUNT(*) FROM (
                 SELECT CASE WHEN requester = ? THEN addressee ELSE requester END AS f
                 FROM friendships WHERE status = 'ACCEPTED' AND (requester = ? OR addressee = ?)
               ) x WHERE f IN (
                 SELECT CASE WHEN requester = ? THEN addressee ELSE requester END
                 FROM friendships WHERE status = 'ACCEPTED' AND (requester = ? OR addressee = ?)
               )""",
            arrayOf(a, a, a, b, b, b)
        )
        var count = 0
        if (c.moveToFirst()) count = c.getInt(0)
        c.close()
        return count
    }

    // --------------------------------------------------------------- posts

    override suspend fun createPost(
        userId: String, text: String, mediaUri: String, mediaType: String,
        sharedFromId: String?
    ): Result<Post> = withContext(Dispatchers.IO) {
        try {
            if (text.isBlank() && mediaUri.isEmpty() && sharedFromId == null) {
                return@withContext Result.failure(Exception("Write something or add media"))
            }
            val id = Utils.newId()
            val values = ContentValues().apply {
                put("id", id)
                put("user_id", userId)
                put("text", text)
                put("media_uri", mediaUri)
                put("media_type", mediaType)
                put("created_at", Utils.now())
                if (sharedFromId != null) put("shared_from", sharedFromId)
            }
            db.insert("posts", null, values)
            if (sharedFromId != null) {
                val owner = db.rawQuery("SELECT user_id FROM posts WHERE id = ?", arrayOf(sharedFromId))
                if (owner.moveToFirst() && owner.getString(0) != userId) {
                    notifyUser(owner.getString(0), userId, "share", "shared your post", sharedFromId)
                }
                owner.close()
            }
            Result.success(postById(id, userId)!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private val postSelect = """
        SELECT p.id AS pid, p.user_id AS uid, p.text AS ptext, p.media_uri AS media,
               p.media_type AS mtype, p.created_at AS created, p.shared_from AS shared,
               u.name AS uname, u.avatar AS uavatar,
               (SELECT COUNT(*) FROM likes l WHERE l.post_id = p.id) AS likes,
               (SELECT COUNT(*) FROM comments c WHERE c.post_id = p.id) AS comments,
               (SELECT COUNT(*) FROM posts p2 WHERE p2.shared_from = p.id) AS shares,
               (SELECT COUNT(*) FROM likes l WHERE l.post_id = p.id AND l.user_id = ?) AS liked,
               (SELECT COUNT(*) FROM saves s WHERE s.post_id = p.id AND s.user_id = ?) AS saved,
               sp.text AS sptext, su.name AS spname
        FROM posts p
        JOIN users u ON u.id = p.user_id
        LEFT JOIN posts sp ON sp.id = p.shared_from
        LEFT JOIN users su ON su.id = sp.user_id
    """

    private fun readPost(c: Cursor) = Post(
        id = c.getString(c.getColumnIndexOrThrow("pid")),
        userId = c.getString(c.getColumnIndexOrThrow("uid")),
        authorName = c.getString(c.getColumnIndexOrThrow("uname")) ?: "Unknown",
        authorAvatar = c.getString(c.getColumnIndexOrThrow("uavatar")) ?: "",
        text = c.getString(c.getColumnIndexOrThrow("ptext")) ?: "",
        mediaUri = c.getString(c.getColumnIndexOrThrow("media")) ?: "",
        mediaType = c.getString(c.getColumnIndexOrThrow("mtype")) ?: "",
        createdAt = c.getLong(c.getColumnIndexOrThrow("created")),
        likeCount = c.getInt(c.getColumnIndexOrThrow("likes")),
        commentCount = c.getInt(c.getColumnIndexOrThrow("comments")),
        shareCount = c.getInt(c.getColumnIndexOrThrow("shares")),
        likedByMe = c.getInt(c.getColumnIndexOrThrow("liked")) > 0,
        savedByMe = c.getInt(c.getColumnIndexOrThrow("saved")) > 0,
        sharedFromName = c.getString(c.getColumnIndexOrThrow("spname")),
        sharedFromText = c.getString(c.getColumnIndexOrThrow("sptext"))
    )

    override suspend fun feed(userId: String, offset: Int, limit: Int): List<Post> =
        withContext(Dispatchers.IO) {
            val sql = postSelect + """
                WHERE p.user_id NOT IN (SELECT blocked FROM blocks WHERE blocker = ?)
                  AND p.user_id NOT IN (SELECT blocker FROM blocks WHERE blocked = ?)
                  AND (p.user_id = ?
                       OR p.user_id IN (SELECT following FROM follows WHERE follower = ?)
                       OR p.user_id IN (
                            SELECT CASE WHEN requester = ? THEN addressee ELSE requester END
                            FROM friendships WHERE status = 'ACCEPTED' AND (requester = ? OR addressee = ?)))
                ORDER BY p.created_at DESC LIMIT ? OFFSET ?
            """
            val c = db.rawQuery(
                sql,
                arrayOf(userId, userId, userId, userId, userId, userId, userId, userId, userId,
                    limit.toString(), offset.toString())
            )
            val list = mutableListOf<Post>()
            while (c.moveToNext()) list.add(readPost(c))
            c.close()
            list
        }

    override suspend fun userPosts(
        targetId: String, viewerId: String, offset: Int, limit: Int
    ): List<Post> = withContext(Dispatchers.IO) {
        val sql = postSelect + """
            WHERE p.user_id = ? AND p.user_id NOT IN (SELECT blocked FROM blocks WHERE blocker = ?)
            ORDER BY p.created_at DESC LIMIT ? OFFSET ?
        """
        val c = db.rawQuery(sql, arrayOf(viewerId, viewerId, targetId, viewerId, limit.toString(), offset.toString()))
        val list = mutableListOf<Post>()
        while (c.moveToNext()) list.add(readPost(c))
        c.close()
        list
    }

    override suspend fun savedPosts(viewerId: String): List<Post> = withContext(Dispatchers.IO) {
        val sql = postSelect + """
            JOIN saves sv ON sv.post_id = p.id AND sv.user_id = ?
            ORDER BY p.created_at DESC LIMIT 100
        """
        val c = db.rawQuery(sql, arrayOf(viewerId, viewerId, viewerId))
        val list = mutableListOf<Post>()
        while (c.moveToNext()) list.add(readPost(c))
        c.close()
        list
    }

    override suspend fun postById(postId: String, viewerId: String): Post? =
        withContext(Dispatchers.IO) {
            val c = db.rawQuery(postSelect + " WHERE p.id = ?", arrayOf(viewerId, viewerId, postId))
            val post = if (c.moveToFirst()) readPost(c) else null
            c.close()
            post
        }

    override suspend fun toggleLike(postId: String, userId: String): Boolean =
        withContext(Dispatchers.IO) {
            val c = db.rawQuery("SELECT 1 FROM likes WHERE post_id = ? AND user_id = ?", arrayOf(postId, userId))
            val liked = c.moveToFirst()
            c.close()
            if (liked) {
                db.delete("likes", "post_id = ? AND user_id = ?", arrayOf(postId, userId))
            } else {
                db.insert("likes", null, ContentValues().apply {
                    put("post_id", postId)
                    put("user_id", userId)
                    put("created_at", Utils.now())
                })
                val owner = postOwner(postId)
                if (owner != null && owner != userId) notifyUser(owner, userId, "like", "liked your post", postId)
            }
            !liked
        }

    private fun postOwner(postId: String): String? {
        val c = db.rawQuery("SELECT user_id FROM posts WHERE id = ?", arrayOf(postId))
        var owner: String? = null
        if (c.moveToFirst()) owner = c.getString(0)
        c.close()
        return owner
    }

    override suspend fun addComment(
        postId: String, userId: String, text: String, parentId: String?
    ): Result<Comment> = withContext(Dispatchers.IO) {
        try {
            if (text.isBlank()) return@withContext Result.failure(Exception("Comment is empty"))
            val id = Utils.newId()
            val values = ContentValues().apply {
                put("id", id)
                put("post_id", postId)
                put("user_id", userId)
                put("text", text.trim())
                put("created_at", Utils.now())
                if (parentId != null) put("parent_id", parentId)
            }
            db.insert("comments", null, values)
            val owner = postOwner(postId)
            if (owner != null && owner != userId) notifyUser(owner, userId, "comment", "commented on your post", postId)
            if (parentId != null) {
                val c = db.rawQuery("SELECT user_id FROM comments WHERE id = ?", arrayOf(parentId))
                if (c.moveToFirst()) {
                    val replyTo = c.getString(0)
                    if (replyTo != userId && replyTo != owner) {
                        notifyUser(replyTo, userId, "reply", "replied to your comment", postId)
                    }
                }
                c.close()
            }
            val user = getUser(userId)!!
            Result.success(
                Comment(id, postId, userId, user.name, user.avatar, text.trim(), Utils.now(), parentId, null)
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun comments(postId: String): List<Comment> = withContext(Dispatchers.IO) {
        val c = db.rawQuery(
            """SELECT c.id, c.post_id, c.user_id, c.text, c.created_at, c.parent_id,
                      u.name, u.avatar, pu.name AS pname
               FROM comments c
               JOIN users u ON u.id = c.user_id
               LEFT JOIN comments pc ON pc.id = c.parent_id
               LEFT JOIN users pu ON pu.id = pc.user_id
               WHERE c.post_id = ? ORDER BY c.created_at ASC""",
            arrayOf(postId)
        )
        val list = mutableListOf<Comment>()
        while (c.moveToNext()) {
            list.add(
                Comment(
                    id = c.getString(0),
                    postId = c.getString(1),
                    userId = c.getString(2),
                    authorName = c.getString(6) ?: "Unknown",
                    authorAvatar = c.getString(7) ?: "",
                    text = c.getString(3) ?: "",
                    createdAt = c.getLong(4),
                    parentId = c.getString(5),
                    replyToName = c.getString(8)
                )
            )
        }
        c.close()
        list
    }

    override suspend fun deletePost(postId: String, userId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            val owner = postOwner(postId)
            if (owner != userId) return@withContext Result.failure(Exception("You can only delete your own posts"))
            db.delete("likes", "post_id = ?", arrayOf(postId))
            db.delete("comments", "post_id = ?", arrayOf(postId))
            db.delete("saves", "post_id = ?", arrayOf(postId))
            db.delete("posts", "id = ?", arrayOf(postId))
            Result.success(Unit)
        }

    override suspend fun toggleSave(postId: String, userId: String): Boolean =
        withContext(Dispatchers.IO) {
            val c = db.rawQuery("SELECT 1 FROM saves WHERE post_id = ? AND user_id = ?", arrayOf(postId, userId))
            val saved = c.moveToFirst()
            c.close()
            if (saved) {
                db.delete("saves", "post_id = ? AND user_id = ?", arrayOf(postId, userId))
            } else {
                db.insert("saves", null, ContentValues().apply {
                    put("post_id", postId)
                    put("user_id", userId)
                })
            }
            !saved
        }

    override suspend fun sharePost(postId: String, userId: String, text: String): Result<Unit> =
        createPost(userId, text, "", "", postId).map { }

    // ------------------------------------------------------ friends/follows

    override suspend fun sendFriendRequest(fromId: String, toId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (fromId == toId) return@withContext Result.failure(Exception("That is you"))
            val c = db.rawQuery(
                "SELECT id, status FROM friendships WHERE (requester = ? AND addressee = ?) OR (requester = ? AND addressee = ?)",
                arrayOf(fromId, toId, toId, fromId)
            )
            var existingId: String? = null
            var status = ""
            if (c.moveToFirst()) {
                existingId = c.getString(0)
                status = c.getString(1)
            }
            c.close()
            when {
                existingId != null && status == "ACCEPTED" ->
                    return@withContext Result.failure(Exception("You are already friends"))
                existingId != null && status == "PENDING" ->
                    return@withContext Result.failure(Exception("A friend request already exists"))
                existingId != null -> db.delete("friendships", "id = ?", arrayOf(existingId))
            }
            db.insert("friendships", null, ContentValues().apply {
                put("id", Utils.newId())
                put("requester", fromId)
                put("addressee", toId)
                put("status", "PENDING")
                put("created_at", Utils.now())
            })
            notifyUser(toId, fromId, "friend_request", "sent you a friend request", null)
            Result.success(Unit)
        }

    override suspend fun respondRequest(requestId: String, accept: Boolean): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (accept) {
                db.update("friendships", ContentValues().apply { put("status", "ACCEPTED") }, "id = ?", arrayOf(requestId))
                val c = db.rawQuery("SELECT requester, addressee FROM friendships WHERE id = ?", arrayOf(requestId))
                if (c.moveToFirst()) {
                    notifyUser(c.getString(1), c.getString(0), "friend_accept", "accepted your friend request", null)
                }
                c.close()
            } else {
                db.delete("friendships", "id = ?", arrayOf(requestId))
            }
            Result.success(Unit)
        }

    override suspend fun removeFriend(a: String, b: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            db.delete(
                "friendships",
                "(requester = ? AND addressee = ?) OR (requester = ? AND addressee = ?)",
                arrayOf(a, b, b, a)
            )
            Result.success(Unit)
        }

    override suspend fun friendRequests(userId: String): List<PersonCard> =
        withContext(Dispatchers.IO) {
            val c = db.rawQuery(
                """SELECT u.*, f.id AS fid FROM friendships f JOIN users u ON u.id = f.requester
                   WHERE f.addressee = ? AND f.status = 'PENDING' ORDER BY f.created_at DESC""",
                arrayOf(userId)
            )
            val list = mutableListOf<PersonCard>()
            while (c.moveToNext()) {
                val user = readUser(c)
                list.add(PersonCard(user, c.getString(c.getColumnIndexOrThrow("fid")), mutualFriends(userId, user.id)))
            }
            c.close()
            list
        }

    override suspend fun friends(userId: String): List<PersonCard> = withContext(Dispatchers.IO) {
        val c = db.rawQuery(
            """SELECT * FROM users WHERE id IN (
                 SELECT CASE WHEN requester = ? THEN addressee ELSE requester END
                 FROM friendships WHERE status = 'ACCEPTED' AND (requester = ? OR addressee = ?)
               ) ORDER BY name""",
            arrayOf(userId, userId, userId)
        )
        val list = mutableListOf<PersonCard>()
        while (c.moveToNext()) {
            val user = readUser(c)
            list.add(PersonCard(user, PersonCard.FRIENDS, mutualFriends(userId, user.id)))
        }
        c.close()
        list
    }

    override suspend fun followers(userId: String): List<PersonCard> = withContext(Dispatchers.IO) {
        val c = db.rawQuery(
            "SELECT * FROM users WHERE id IN (SELECT follower FROM follows WHERE following = ?) ORDER BY name",
            arrayOf(userId)
        )
        val list = mutableListOf<PersonCard>()
        while (c.moveToNext()) {
            val user = readUser(c)
            list.add(PersonCard(user, relation(userId, user.id), mutualFriends(userId, user.id)))
        }
        c.close()
        list
    }

    override suspend fun following(userId: String): List<PersonCard> = withContext(Dispatchers.IO) {
        val c = db.rawQuery(
            "SELECT * FROM users WHERE id IN (SELECT following FROM follows WHERE follower = ?) ORDER BY name",
            arrayOf(userId)
        )
        val list = mutableListOf<PersonCard>()
        while (c.moveToNext()) {
            val user = readUser(c)
            list.add(PersonCard(user, relation(userId, user.id), mutualFriends(userId, user.id)))
        }
        c.close()
        list
    }

    override suspend fun toggleFollow(followerId: String, targetId: String): Boolean =
        withContext(Dispatchers.IO) {
            val c = db.rawQuery(
                "SELECT 1 FROM follows WHERE follower = ? AND following = ?", arrayOf(followerId, targetId)
            )
            val follows = c.moveToFirst()
            c.close()
            if (follows) {
                db.delete("follows", "follower = ? AND following = ?", arrayOf(followerId, targetId))
            } else {
                db.insert("follows", null, ContentValues().apply {
                    put("follower", followerId)
                    put("following", targetId)
                    put("created_at", Utils.now())
                })
                notifyUser(targetId, followerId, "follow", "started following you", null)
            }
            !follows
        }

    // ------------------------------------------------------------- stories

    override suspend fun createStory(
        userId: String, mediaUri: String, mediaType: String, caption: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (mediaUri.isEmpty()) return@withContext Result.failure(Exception("Pick a photo or video"))
            db.insert("stories", null, ContentValues().apply {
                put("id", Utils.newId())
                put("user_id", userId)
                put("media_uri", mediaUri)
                put("media_type", mediaType)
                put("caption", caption)
                put("created_at", Utils.now())
                put("expires_at", Utils.now() + AppConfig.STORY_TTL_MS)
            })
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun activeStories(viewerId: String): List<Story> =
        withContext(Dispatchers.IO) {
            val c = db.rawQuery(
                """SELECT s.id, s.user_id, s.media_uri, s.media_type, s.caption, s.created_at,
                          u.name, u.avatar,
                          (SELECT COUNT(*) FROM story_views v WHERE v.story_id = s.id AND v.viewer = ?) AS seen
                   FROM stories s JOIN users u ON u.id = s.user_id
                   WHERE s.expires_at > ?
                     AND (s.user_id = ?
                          OR s.user_id IN (SELECT following FROM follows WHERE follower = ?)
                          OR s.user_id IN (
                               SELECT CASE WHEN requester = ? THEN addressee ELSE requester END
                               FROM friendships WHERE status = 'ACCEPTED' AND (requester = ? OR addressee = ?)))
                   ORDER BY s.created_at ASC""",
                arrayOf(viewerId, Utils.now().toString(), viewerId, viewerId, viewerId, viewerId, viewerId)
            )
            val list = mutableListOf<Story>()
            while (c.moveToNext()) {
                list.add(
                    Story(
                        id = c.getString(0),
                        userId = c.getString(1),
                        userName = c.getString(6) ?: "",
                        userAvatar = c.getString(7) ?: "",
                        mediaUri = c.getString(2) ?: "",
                        mediaType = c.getString(3) ?: "image",
                        caption = c.getString(4) ?: "",
                        createdAt = c.getLong(5),
                        viewed = c.getInt(8) > 0,
                        mine = c.getString(1) == viewerId
                    )
                )
            }
            c.close()
            list
        }

    override suspend fun myStories(userId: String): List<Story> = withContext(Dispatchers.IO) {
        activeStories(userId).filter { it.mine }
    }

    override suspend fun markStoryViewed(storyId: String, viewerId: String) {
        withContext(Dispatchers.IO) {
            db.insertWithOnConflict(
                "story_views", null,
                ContentValues().apply {
                    put("story_id", storyId)
                    put("viewer", viewerId)
                },
                SQLiteDatabase.CONFLICT_IGNORE
            )
        }
    }

    override suspend fun storyViewers(storyId: String): List<User> = withContext(Dispatchers.IO) {
        val c = db.rawQuery(
            "SELECT * FROM users WHERE id IN (SELECT viewer FROM story_views WHERE story_id = ?) ORDER BY name",
            arrayOf(storyId)
        )
        val list = mutableListOf<User>()
        while (c.moveToNext()) list.add(readUser(c))
        c.close()
        list
    }

    // -------------------------------------------------------------- chat

    override suspend fun conversations(userId: String): List<Chat> = withContext(Dispatchers.IO) {
        val c = db.rawQuery(
            "SELECT * FROM conversations WHERE user_a = ? OR user_b = ? ORDER BY last_at DESC",
            arrayOf(userId, userId)
        )
        val list = mutableListOf<Chat>()
        while (c.moveToNext()) {
            val id = c.getString(c.getColumnIndexOrThrow("id"))
            val a = c.getString(c.getColumnIndexOrThrow("user_a"))
            val b = c.getString(c.getColumnIndexOrThrow("user_b"))
            val otherId = if (a == userId) b else a
            val other = getUser(otherId) ?: continue
            val last = db.rawQuery(
                "SELECT text, media_uri, created_at FROM messages WHERE conversation_id = ? AND deleted = 0 ORDER BY created_at DESC LIMIT 1",
                arrayOf(id)
            )
            var lastText = "Say hi"
            var lastAt = c.getLong(c.getColumnIndexOrThrow("last_at"))
            if (last.moveToFirst()) {
                lastText = last.getString(0)?.takeIf { it.isNotBlank() }
                    ?: if (!last.getString(1).isNullOrEmpty()) "Photo" else "Message"
                lastAt = last.getLong(2)
            }
            last.close()
            val unread = db.rawQuery(
                "SELECT COUNT(*) FROM messages WHERE conversation_id = ? AND sender != ? AND read_at = 0 AND deleted = 0",
                arrayOf(id, userId)
            )
            var unreadCount = 0
            if (unread.moveToFirst()) unreadCount = unread.getInt(0)
            unread.close()
            list.add(Chat(id, other, lastText, lastAt, unreadCount))
        }
        c.close()
        list
    }

    override suspend fun openConversation(userId: String, otherId: String): String =
        withContext(Dispatchers.IO) {
            val c = db.rawQuery(
                """SELECT id FROM conversations WHERE
                   (user_a = ? AND user_b = ?) OR (user_a = ? AND user_b = ?) LIMIT 1""",
                arrayOf(userId, otherId, otherId, userId)
            )
            var id: String? = null
            if (c.moveToFirst()) id = c.getString(0)
            c.close()
            if (id == null) {
                id = Utils.newId()
                db.insert("conversations", null, ContentValues().apply {
                    put("id", id)
                    put("user_a", userId)
                    put("user_b", otherId)
                    put("last_at", Utils.now())
                })
            }
            id
        }

    override suspend fun otherParticipant(conversationId: String, viewerId: String): User? =
        withContext(Dispatchers.IO) {
            val c = db.rawQuery(
                "SELECT user_a, user_b FROM conversations WHERE id = ?", arrayOf(conversationId)
            )
            var other: String? = null
            if (c.moveToFirst()) {
                other = if (c.getString(0) == viewerId) c.getString(1) else c.getString(0)
            }
            c.close()
            other?.let { getUser(it) }
        }

    override suspend fun messages(conversationId: String, viewerId: String): List<Message> =
        withContext(Dispatchers.IO) {
            val c = db.rawQuery(
                """SELECT * FROM messages WHERE conversation_id = ?
                   ORDER BY created_at ASC LIMIT 500""",
                arrayOf(conversationId)
            )
            val list = mutableListOf<Message>()
            while (c.moveToNext()) {
                list.add(
                    Message(
                        id = c.getString(c.getColumnIndexOrThrow("id")),
                        conversationId = conversationId,
                        senderId = c.getString(c.getColumnIndexOrThrow("sender")),
                        text = c.getString(c.getColumnIndexOrThrow("text")) ?: "",
                        mediaUri = c.getString(c.getColumnIndexOrThrow("media_uri")) ?: "",
                        mediaType = c.getString(c.getColumnIndexOrThrow("media_type")) ?: "",
                        createdAt = c.getLong(c.getColumnIndexOrThrow("created_at")),
                        readAt = c.getLong(c.getColumnIndexOrThrow("read_at")),
                        deleted = c.getInt(c.getColumnIndexOrThrow("deleted")) == 1,
                        mine = c.getString(c.getColumnIndexOrThrow("sender")) == viewerId
                    )
                )
            }
            c.close()
            list
        }

    override suspend fun sendMessage(
        conversationId: String, senderId: String, text: String,
        mediaUri: String, mediaType: String
    ): Result<Message> = withContext(Dispatchers.IO) {
        try {
            if (text.isBlank() && mediaUri.isEmpty()) {
                return@withContext Result.failure(Exception("Message is empty"))
            }
            val id = Utils.newId()
            val now = Utils.now()
            db.insert("messages", null, ContentValues().apply {
                put("id", id)
                put("conversation_id", conversationId)
                put("sender", senderId)
                put("text", text)
                put("media_uri", mediaUri)
                put("media_type", mediaType)
                put("created_at", now)
                put("read_at", 0)
                put("deleted", 0)
            })
            db.update(
                "conversations", ContentValues().apply { put("last_at", now) }, "id = ?", arrayOf(conversationId)
            )
            val other = otherParticipant(conversationId, senderId)
            if (other != null) {
                notifyUser(other.id, senderId, "message", text.ifBlank { "sent you a photo" }, null, false)
            }
            Result.success(
                Message(id, conversationId, senderId, text, mediaUri, mediaType, now, 0, false, true)
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markConversationRead(conversationId: String, viewerId: String) {
        withContext(Dispatchers.IO) {
            db.update(
                "messages",
                ContentValues().apply { put("read_at", Utils.now()) },
                "conversation_id = ? AND sender != ? AND read_at = 0",
                arrayOf(conversationId, viewerId)
            )
            db.update(
                "notifications",
                ContentValues().apply { put("is_read", 1) },
                "user_id = ? AND type = 'message'", arrayOf(viewerId)
            )
        }
    }

    override suspend fun deleteMessage(messageId: String, userId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            val c = db.rawQuery("SELECT sender FROM messages WHERE id = ?", arrayOf(messageId))
            var sender: String? = null
            if (c.moveToFirst()) sender = c.getString(0)
            c.close()
            if (sender != userId) return@withContext Result.failure(Exception("You can only delete your own messages"))
            db.update(
                "messages",
                ContentValues().apply {
                    put("deleted", 1)
                    put("text", "")
                    put("media_uri", "")
                },
                "id = ?", arrayOf(messageId)
            )
            Result.success(Unit)
        }

    override suspend fun setTyping(userId: String, conversationId: String, typing: Boolean) {
        typingMap["$conversationId:$userId"] = if (typing) Utils.now() else 0L
    }

    override suspend fun isTyping(conversationId: String, otherId: String): Boolean {
        val stamp = typingMap["$conversationId:$otherId"] ?: 0L
        return stamp > 0 && Utils.now() - stamp < 4000
    }

    // ------------------------------------------------------ notifications

    override suspend fun notifications(userId: String): List<AppNotification> =
        withContext(Dispatchers.IO) {
            val c = db.rawQuery(
                """SELECT n.id, n.type, n.actor, n.text, n.post_id, n.created_at, n.is_read,
                          u.name, u.avatar
                   FROM notifications n LEFT JOIN users u ON u.id = n.actor
                   WHERE n.user_id = ? ORDER BY n.created_at DESC LIMIT 100""",
                arrayOf(userId)
            )
            val list = mutableListOf<AppNotification>()
            while (c.moveToNext()) {
                list.add(
                    AppNotification(
                        id = c.getString(0),
                        type = c.getString(1) ?: "",
                        actorId = c.getString(2) ?: "",
                        actorName = c.getString(7) ?: "Someone",
                        actorAvatar = c.getString(8) ?: "",
                        text = c.getString(3) ?: "",
                        postId = c.getString(4),
                        createdAt = c.getLong(5),
                        isRead = c.getInt(6) == 1
                    )
                )
            }
            c.close()
            list
        }

    override suspend fun markNotificationsRead(userId: String) {
        withContext(Dispatchers.IO) {
            db.update("notifications", ContentValues().apply { put("is_read", 1) }, "user_id = ?", arrayOf(userId))
        }
    }

    override suspend fun unreadNotificationCount(userId: String): Int = withContext(Dispatchers.IO) {
        val c = db.rawQuery(
            "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = 0", arrayOf(userId)
        )
        var count = 0
        if (c.moveToFirst()) count = c.getInt(0)
        c.close()
        count
    }

    override suspend fun unreadMessageCount(userId: String): Int = withContext(Dispatchers.IO) {
        val c = db.rawQuery(
            """SELECT COUNT(*) FROM messages m JOIN conversations c ON c.id = m.conversation_id
               WHERE (c.user_a = ? OR c.user_b = ?) AND m.sender != ? AND m.read_at = 0 AND m.deleted = 0""",
            arrayOf(userId, userId, userId)
        )
        var count = 0
        if (c.moveToFirst()) count = c.getInt(0)
        c.close()
        count
    }

    private fun userNameOf(id: String): String {
        val c = db.rawQuery("SELECT name FROM users WHERE id = ?", arrayOf(id))
        var name = "SocialX"
        if (c.moveToFirst()) name = c.getString(0) ?: name
        c.close()
        return name
    }

    private fun notifyUser(
        userId: String, actorId: String, type: String, text: String,
        postId: String?, push: Boolean = true
    ) {
        if (userId == actorId) return
        db.insert("notifications", null, ContentValues().apply {
            put("id", Utils.newId())
            put("user_id", userId)
            put("actor", actorId)
            put("type", type)
            put("text", text)
            if (postId != null) put("post_id", postId)
            put("created_at", Utils.now())
            put("is_read", 0)
        })
        if (push && type != "message") {
            notifier.show(
                (System.currentTimeMillis() % 100000).toInt(),
                userNameOf(actorId),
                text
            )
        }
    }

    // --------------------------------------------------------- moderation

    override suspend fun blockUser(blockerId: String, blockedId: String) {
        withContext(Dispatchers.IO) {
            db.insertWithOnConflict(
                "blocks", null,
                ContentValues().apply {
                    put("blocker", blockerId)
                    put("blocked", blockedId)
                },
                SQLiteDatabase.CONFLICT_IGNORE
            )
            db.delete(
                "friendships",
                "(requester = ? AND addressee = ?) OR (requester = ? AND addressee = ?)",
                arrayOf(blockerId, blockedId, blockedId, blockerId)
            )
            db.delete("follows", "(follower = ? AND following = ?) OR (follower = ? AND following = ?)",
                arrayOf(blockerId, blockedId, blockedId, blockerId))
        }
    }

    override suspend fun unblockUser(blockerId: String, blockedId: String) {
        withContext(Dispatchers.IO) {
            db.delete("blocks", "blocker = ? AND blocked = ?", arrayOf(blockerId, blockedId))
        }
    }

    override suspend fun blockedUsers(userId: String): List<User> = withContext(Dispatchers.IO) {
        val c = db.rawQuery(
            "SELECT * FROM users WHERE id IN (SELECT blocked FROM blocks WHERE blocker = ?) ORDER BY name",
            arrayOf(userId)
        )
        val list = mutableListOf<User>()
        while (c.moveToNext()) list.add(readUser(c))
        c.close()
        list
    }

    override suspend fun report(
        reporterId: String, targetType: String, targetId: String, reason: String
    ) {
        withContext(Dispatchers.IO) {
            db.insert("reports", null, ContentValues().apply {
                put("id", Utils.newId())
                put("reporter", reporterId)
                put("target_type", targetType)
                put("target_id", targetId)
                put("reason", reason)
                put("created_at", Utils.now())
            })
        }
    }

    // ---------------------------------------------------------- discovery

    override suspend fun searchPosts(query: String, viewerId: String): List<Post> =
        withContext(Dispatchers.IO) {
            val like = "%${query.trim()}%"
            val c = db.rawQuery(
                postSelect + " WHERE (p.text LIKE ? OR u.name LIKE ?) ORDER BY p.created_at DESC LIMIT 50",
                arrayOf(viewerId, viewerId, like, like)
            )
            val list = mutableListOf<Post>()
            while (c.moveToNext()) list.add(readPost(c))
            c.close()
            list
        }

    override suspend fun trendingTags(): List<TrendingTag> = withContext(Dispatchers.IO) {
        val c = db.rawQuery(
            "SELECT text FROM posts WHERE text LIKE '%#%' AND created_at > ? LIMIT 300",
            arrayOf((Utils.now() - 7L * 24 * 60 * 60 * 1000).toString())
        )
        val counts = mutableMapOf<String, Int>()
        val regex = Regex("#(\\w+)")
        while (c.moveToNext()) {
            regex.findAll(c.getString(0) ?: "").forEach { match ->
                val tag = match.groupValues[1].lowercase()
                counts[tag] = (counts[tag] ?: 0) + 1
            }
        }
        c.close()
        counts.entries.sortedByDescending { it.value }.take(10).map { TrendingTag(it.key, it.value) }
    }

    override suspend fun seedDemoContent(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val demos = listOf(
                Triple("Aarav Mehta", "aarav", "aarav@socialx.app"),
                Triple("Diya Sharma", "diya", "diya@socialx.app"),
                Triple("Kabir Nair", "kabir", "kabir@socialx.app")
            )
            val ids = mutableListOf<String>()
            for ((name, handle, email) in demos) {
                val existing = db.rawQuery("SELECT id FROM users WHERE email = ?", arrayOf(email))
                if (existing.moveToFirst()) {
                    ids.add(existing.getString(0))
                } else {
                    val id = Utils.newId()
                    val salt = Utils.newSalt()
                    db.insert("users", null, ContentValues().apply {
                        put("id", id)
                        put("name", name)
                        put("username", handle)
                        put("email", email)
                        put("phone", "")
                        put("password_hash", Utils.hashPassword("demo1234", salt))
                        put("salt", salt)
                        put("bio", "SocialX demo account")
                        put("created_at", Utils.now())
                        put("last_seen", Utils.now())
                        put("security_q", "What is your favourite colour?")
                        put("security_a", Utils.sha256("blue"))
                    })
                    ids.add(id)
                }
                existing.close()
            }
            val texts = listOf(
                "Just shipped a new design system for our app. Feels good to see it live! #design #socialx",
                "Morning walk by the lake. Best way to start the day. #nature",
                "Anyone else building with Compose this weekend? #android #kotlin",
                "Coffee, code and calm. #developerlife"
            )
            ids.forEachIndexed { index, id ->
                createPost(id, texts[index % texts.size], "", "", null)
                db.insert("friendships", null, ContentValues().apply {
                    put("id", Utils.newId())
                    put("requester", userId)
                    put("addressee", id)
                    put("status", "ACCEPTED")
                    put("created_at", Utils.now())
                })
                db.insert("follows", null, ContentValues().apply {
                    put("follower", userId)
                    put("following", id)
                    put("created_at", Utils.now())
                })
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

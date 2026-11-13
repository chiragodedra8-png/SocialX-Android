package com.socialx.app.data

data class User(
    val id: String,
    val name: String,
    val username: String,
    val email: String = "",
    val phone: String = "",
    val bio: String = "",
    val avatar: String = "",
    val cover: String = "",
    val createdAt: Long = 0L,
    val isPrivate: Boolean = false,
    val lastSeen: Long = 0L,
    val securityQuestion: String = ""
) {
    val online: Boolean
        get() = System.currentTimeMillis() - lastSeen < 120_000
}

data class Post(
    val id: String,
    val userId: String,
    val authorName: String,
    val authorAvatar: String,
    val text: String,
    val mediaUri: String,
    val mediaType: String,
    val createdAt: Long,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val shareCount: Int = 0,
    val likedByMe: Boolean = false,
    val savedByMe: Boolean = false,
    val sharedFromName: String? = null,
    val sharedFromText: String? = null
)

data class Comment(
    val id: String,
    val postId: String,
    val userId: String,
    val authorName: String,
    val authorAvatar: String,
    val text: String,
    val createdAt: Long,
    val parentId: String? = null,
    val replyToName: String? = null
)

data class Message(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val text: String,
    val mediaUri: String,
    val mediaType: String,
    val createdAt: Long,
    val readAt: Long = 0L,
    val deleted: Boolean = false,
    val mine: Boolean = false
)

data class Chat(
    val conversationId: String,
    val other: User,
    val lastText: String,
    val lastAt: Long,
    val unread: Int
)

data class Story(
    val id: String,
    val userId: String,
    val userName: String,
    val userAvatar: String,
    val mediaUri: String,
    val mediaType: String,
    val caption: String,
    val createdAt: Long,
    val viewed: Boolean = false,
    val mine: Boolean = false
)

data class AppNotification(
    val id: String,
    val type: String,
    val actorId: String,
    val actorName: String,
    val actorAvatar: String,
    val text: String,
    val postId: String?,
    val createdAt: Long,
    val isRead: Boolean
)

data class PersonCard(
    val user: User,
    val relation: String,
    val mutual: Int
) {
    companion object {
        const val NONE = "none"
        const val FRIENDS = "friends"
        const val SENT = "sent"
        const val RECEIVED = "received"
        const val SELF = "self"
        const val FOLLOWING = "following"
    }
}

data class TrendingTag(val tag: String, val posts: Int)

package com.socialx.app.data

/** Single abstraction used by every screen. Two implementations ship with the app. */
interface SocialRepository {

    // ---- auth ----
    suspend fun signUp(
        name: String, username: String, email: String, phone: String,
        password: String, securityQuestion: String, securityAnswer: String
    ): Result<User>

    suspend fun login(identifier: String, password: String): Result<User>
    suspend fun resetPassword(identifier: String, answer: String, newPassword: String): Result<Unit>
    suspend fun changePassword(userId: String, oldPassword: String, newPassword: String): Result<Unit>
    suspend fun deleteAccount(userId: String, password: String): Result<Unit>
    suspend fun currentUser(): User?
    fun logout()
    fun securityQuestionFor(identifier: String): String?

    // ---- profile ----
    suspend fun getUser(id: String): User?
    suspend fun updateProfile(
        userId: String, name: String, bio: String, avatar: String,
        cover: String, isPrivate: Boolean
    ): Result<Unit>

    suspend fun searchUsers(query: String, viewerId: String): List<PersonCard>
    suspend fun suggestedPeople(viewerId: String, limit: Int): List<PersonCard>

    // ---- posts ----
    suspend fun createPost(
        userId: String, text: String, mediaUri: String, mediaType: String,
        sharedFromId: String? = null
    ): Result<Post>

    suspend fun feed(userId: String, offset: Int, limit: Int): List<Post>
    suspend fun userPosts(targetId: String, viewerId: String, offset: Int, limit: Int): List<Post>
    suspend fun savedPosts(viewerId: String): List<Post>
    suspend fun postById(postId: String, viewerId: String): Post?
    suspend fun toggleLike(postId: String, userId: String): Boolean
    suspend fun addComment(postId: String, userId: String, text: String, parentId: String? = null): Result<Comment>
    suspend fun comments(postId: String): List<Comment>
    suspend fun deletePost(postId: String, userId: String): Result<Unit>
    suspend fun toggleSave(postId: String, userId: String): Boolean
    suspend fun sharePost(postId: String, userId: String, text: String): Result<Unit>

    // ---- friends & follows ----
    suspend fun sendFriendRequest(fromId: String, toId: String): Result<Unit>
    suspend fun respondRequest(requestId: String, accept: Boolean): Result<Unit>
    suspend fun removeFriend(a: String, b: String): Result<Unit>
    suspend fun friendRequests(userId: String): List<PersonCard>
    suspend fun friends(userId: String): List<PersonCard>
    suspend fun followers(userId: String): List<PersonCard>
    suspend fun following(userId: String): List<PersonCard>
    suspend fun toggleFollow(followerId: String, targetId: String): Boolean

    // ---- stories ----
    suspend fun createStory(userId: String, mediaUri: String, mediaType: String, caption: String): Result<Unit>
    suspend fun activeStories(viewerId: String): List<Story>
    suspend fun markStoryViewed(storyId: String, viewerId: String)
    suspend fun storyViewers(storyId: String): List<User>
    suspend fun myStories(userId: String): List<Story>

    // ---- messenger ----
    suspend fun conversations(userId: String): List<Chat>
    suspend fun openConversation(userId: String, otherId: String): String
    suspend fun messages(conversationId: String, viewerId: String): List<Message>
    suspend fun sendMessage(
        conversationId: String, senderId: String, text: String,
        mediaUri: String = "", mediaType: String = ""
    ): Result<Message>

    suspend fun markConversationRead(conversationId: String, viewerId: String)
    suspend fun deleteMessage(messageId: String, userId: String): Result<Unit>
    suspend fun setTyping(userId: String, conversationId: String, typing: Boolean)
    suspend fun isTyping(conversationId: String, otherId: String): Boolean
    suspend fun otherParticipant(conversationId: String, viewerId: String): User?

    // ---- notifications ----
    suspend fun notifications(userId: String): List<AppNotification>
    suspend fun markNotificationsRead(userId: String)
    suspend fun unreadNotificationCount(userId: String): Int
    suspend fun unreadMessageCount(userId: String): Int

    // ---- moderation ----
    suspend fun blockUser(blockerId: String, blockedId: String)
    suspend fun unblockUser(blockerId: String, blockedId: String)
    suspend fun blockedUsers(userId: String): List<User>
    suspend fun report(reporterId: String, targetType: String, targetId: String, reason: String)

    // ---- discovery ----
    suspend fun searchPosts(query: String, viewerId: String): List<Post>
    suspend fun trendingTags(): List<TrendingTag>
    suspend fun seedDemoContent(userId: String): Result<Unit>
}

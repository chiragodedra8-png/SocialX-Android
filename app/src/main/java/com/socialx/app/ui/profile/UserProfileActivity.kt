package com.socialx.app.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.socialx.app.core.ServiceLocator
import com.socialx.app.core.Utils
import com.socialx.app.core.loadAvatar
import com.socialx.app.core.loadMedia
import com.socialx.app.data.PersonCard
import com.socialx.app.data.Post
import com.socialx.app.data.User
import com.socialx.app.databinding.ActivityUserProfileBinding
import com.socialx.app.ui.chat.ChatActivity
import com.socialx.app.ui.feed.PostAdapter
import kotlinx.coroutines.launch

class UserProfileActivity : AppCompatActivity() {

    private lateinit var b: ActivityUserProfileBinding
    private lateinit var adapter: PostAdapter
    private var targetId = ""
    private var target: User? = null
    private var following = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityUserProfileBinding.inflate(layoutInflater)
        setContentView(b.root)
        targetId = intent.getStringExtra(EXTRA_USER_ID).orEmpty()
        b.toolbar.setNavigationOnClickListener { finish() }
        adapter = PostAdapter(this, ServiceLocator.prefs.userId.orEmpty()) { userId ->
            if (userId != targetId) {
                startActivity(Intent(this, UserProfileActivity::class.java).putExtra(EXTRA_USER_ID, userId))
            }
        }
        b.postsList.layoutManager = LinearLayoutManager(this)
        b.postsList.adapter = adapter
        load()
    }

    private fun load() {
        val me = ServiceLocator.prefs.userId ?: return
        lifecycleScope.launch {
            val user = ServiceLocator.repo.getUser(targetId) ?: return@launch
            target = user
            b.tvName.text = user.name
            b.tvHandle.text = "@${user.username}"
            b.tvBio.text = user.bio.ifBlank { "No bio yet." }
            b.profileAvatar.loadAvatar(user.avatar, user.name)
            b.toolbar.title = user.name
            if (user.cover.isNotBlank()) {
                b.coverImage.loadMedia(user.cover)
                b.coverImage.visibility = View.VISIBLE
            } else {
                b.coverImage.visibility = View.GONE
            }

            val mutual = ServiceLocator.repo.friends(me).map { it.user.id }
                .intersect(ServiceLocator.repo.friends(targetId).map { it.user.id }.toSet())
            b.tvMutual.text = if (mutual.isNotEmpty()) "${mutual.size} mutual friends" else ""

            val relation = ServiceLocator.repo.searchUsers(user.username, me)
                .firstOrNull { it.user.id == targetId }?.relation ?: PersonCard.NONE

            if (targetId == me) {
                b.btnFriend.text = "Edit profile"
                b.btnFriend.setOnClickListener {
                    startActivity(Intent(this@UserProfileActivity, EditProfileActivity::class.java))
                }
                b.btnMessage.visibility = View.GONE
                b.btnFollow.visibility = View.GONE
            } else {
                b.btnFriend.text = when (relation) {
                    PersonCard.FRIENDS -> "Friends"
                    PersonCard.SENT -> "Requested"
                    PersonCard.RECEIVED -> "Accept request"
                    else -> "Add friend"
                }
                b.btnFriend.setOnClickListener { friendAction(relation) }
                b.btnMessage.setOnClickListener { openChat(user.id) }
                following = ServiceLocator.repo.following(me).any { it.user.id == targetId }
                b.btnFollow.text = if (following) "Following" else "Follow"
                b.btnFollow.setOnClickListener {
                    lifecycleScope.launch {
                        following = ServiceLocator.repo.toggleFollow(me, targetId)
                        b.btnFollow.text = if (following) "Following" else "Follow"
                    }
                }
            }

            val posts: List<Post> = if (intent.getBooleanExtra(EXTRA_SHOW_SAVED, false)) {
                ServiceLocator.repo.savedPosts(me)
            } else {
                ServiceLocator.repo.userPosts(targetId, me, 0, 50)
            }
            adapter.submit(posts)
        }
    }

    private fun friendAction(relation: String) {
        val me = ServiceLocator.prefs.userId ?: return
        lifecycleScope.launch {
            when (relation) {
                PersonCard.FRIENDS -> ServiceLocator.repo.removeFriend(me, targetId)
                PersonCard.RECEIVED -> {
                    val requests = ServiceLocator.repo.friendRequests(me)
                    requests.firstOrNull { it.user.id == targetId }?.let {
                        ServiceLocator.repo.respondRequest(it.relation, true)
                    }
                }
                else -> ServiceLocator.repo.sendFriendRequest(me, targetId)
                    .onFailure { Utils.toast(this@UserProfileActivity, it.message ?: "Failed") }
            }
            load()
        }
    }

    private fun openChat(otherId: String) {
        val me = ServiceLocator.prefs.userId ?: return
        lifecycleScope.launch {
            val conversationId = ServiceLocator.repo.openConversation(me, otherId)
            startActivity(
                Intent(this@UserProfileActivity, ChatActivity::class.java)
                    .putExtra(ChatActivity.EXTRA_CONVERSATION_ID, conversationId)
                    .putExtra(ChatActivity.EXTRA_OTHER_ID, otherId)
            )
        }
    }

    companion object {
        const val EXTRA_USER_ID = "user_id"
        const val EXTRA_SHOW_SAVED = "show_saved"
    }
}

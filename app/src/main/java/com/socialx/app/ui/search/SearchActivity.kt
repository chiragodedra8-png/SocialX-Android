package com.socialx.app.ui.search

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.socialx.app.R
import com.socialx.app.core.ServiceLocator
import com.socialx.app.core.Utils
import com.socialx.app.data.PersonCard
import com.socialx.app.data.TrendingTag
import com.socialx.app.databinding.ActivitySearchBinding
import com.socialx.app.databinding.ItemTagBinding
import com.socialx.app.ui.feed.PostAdapter
import com.socialx.app.ui.friends.UserAdapter
import com.socialx.app.ui.profile.UserProfileActivity
import kotlinx.coroutines.launch

class SearchActivity : AppCompatActivity() {

    private lateinit var b: ActivitySearchBinding
    private lateinit var userAdapter: UserAdapter
    private lateinit var postAdapter: PostAdapter
    private lateinit var tagAdapter: TagAdapter
    private var mode = MODE_PEOPLE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.toolbar.setNavigationOnClickListener { finish() }
        b.resultsList.layoutManager = LinearLayoutManager(this)

        userAdapter = UserAdapter(onAction = { card -> sendRequest(card) }, onOpen = { card ->
            startActivity(
                Intent(this, UserProfileActivity::class.java)
                    .putExtra(UserProfileActivity.EXTRA_USER_ID, card.user.id)
            )
        })
        postAdapter = PostAdapter(this, ServiceLocator.prefs.userId.orEmpty()) { userId ->
            startActivity(
                Intent(this, UserProfileActivity::class.java)
                    .putExtra(UserProfileActivity.EXTRA_USER_ID, userId)
            )
        }
        tagAdapter = TagAdapter { tag -> b.etSearch.setText("#$tag") }

        b.chipPeople.setOnClickListener { setMode(MODE_PEOPLE) }
        b.chipPosts.setOnClickListener { setMode(MODE_POSTS) }
        b.chipTags.setOnClickListener { setMode(MODE_TAGS) }
        b.etSearch.setOnEditorActionListener { _, _, _ ->
            search()
            true
        }
        b.etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) = search()
            override fun beforeTextChanged(s: CharSequence?, a: Int, b2: Int, c: Int) = Unit
            override fun onTextChanged(s: CharSequence?, a: Int, b2: Int, c: Int) = Unit
        })
        setMode(MODE_PEOPLE)
    }

    private fun setMode(newMode: Int) {
        mode = newMode
        listOf(b.chipPeople, b.chipPosts, b.chipTags).forEachIndexed { index, button ->
            button.setTextColor(
                ContextCompat.getColor(this, if (index == newMode) R.color.brand_blue else R.color.text_secondary)
            )
        }
        search()
    }

    private fun search() {
        val query = b.etSearch.text?.toString()?.trim().orEmpty()
        val me = ServiceLocator.prefs.userId.orEmpty()
        lifecycleScope.launch {
            when (mode) {
                MODE_PEOPLE -> {
                    b.resultsList.adapter = userAdapter
                    val people = if (query.isBlank()) {
                        ServiceLocator.repo.suggestedPeople(me, 25)
                    } else {
                        ServiceLocator.repo.searchUsers(query, me)
                    }
                    userAdapter.submit(people)
                    b.tvSearchEmpty.visibility = if (people.isEmpty()) View.VISIBLE else View.GONE
                }
                MODE_POSTS -> {
                    b.resultsList.adapter = postAdapter
                    val posts = if (query.isBlank()) emptyList() else ServiceLocator.repo.searchPosts(query, me)
                    postAdapter.submit(posts)
                    b.tvSearchEmpty.visibility = if (posts.isEmpty()) View.VISIBLE else View.GONE
                    b.tvSearchEmpty.text = "No posts found for \"$query\"."
                }
                else -> {
                    b.resultsList.adapter = tagAdapter
                    val tags = ServiceLocator.repo.trendingTags()
                    tagAdapter.submit(tags)
                    b.tvSearchEmpty.visibility = if (tags.isEmpty()) View.VISIBLE else View.GONE
                    b.tvSearchEmpty.text = "No trending hashtags yet. Post with #tags to start a trend."
                }
            }
        }
    }

    private fun sendRequest(card: PersonCard) {
        val me = ServiceLocator.prefs.userId ?: return
        lifecycleScope.launch {
            when (card.relation) {
                PersonCard.RECEIVED -> ServiceLocator.repo.friendRequests(me)
                    .firstOrNull { it.user.id == card.user.id }
                    ?.let { ServiceLocator.repo.respondRequest(it.relation, true) }
                PersonCard.NONE -> ServiceLocator.repo.sendFriendRequest(me, card.user.id)
                    .onSuccess { Utils.toast(this@SearchActivity, "Friend request sent") }
                    .onFailure { Utils.toast(this@SearchActivity, it.message ?: "Failed") }
                else -> Unit
            }
            search()
        }
    }

    private class TagAdapter(
        private val onClick: (String) -> Unit
    ) : RecyclerView.Adapter<TagAdapter.VH>() {
        private val items = mutableListOf<TrendingTag>()

        fun submit(list: List<TrendingTag>) {
            items.clear()
            items.addAll(list)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(ItemTagBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

        inner class VH(private val b: ItemTagBinding) : RecyclerView.ViewHolder(b.root) {
            fun bind(tag: TrendingTag) {
                b.tagName.text = "#${tag.tag}"
                b.tagCount.text = "${tag.posts} posts this week"
                b.root.setOnClickListener { onClick(tag.tag) }
            }
        }
    }

    companion object {
        private const val MODE_PEOPLE = 0
        private const val MODE_POSTS = 1
        private const val MODE_TAGS = 2
    }
}

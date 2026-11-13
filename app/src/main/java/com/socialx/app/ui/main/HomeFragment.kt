package com.socialx.app.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.socialx.app.core.AppConfig
import com.socialx.app.core.ServiceLocator
import com.socialx.app.core.Utils
import com.socialx.app.core.loadAvatar
import com.socialx.app.data.Post
import com.socialx.app.data.Story
import com.socialx.app.data.User
import com.socialx.app.databinding.FragmentHomeBinding
import com.socialx.app.ui.feed.CreatePostActivity
import com.socialx.app.ui.feed.PostAdapter
import com.socialx.app.ui.feed.StoryAdapter
import com.socialx.app.ui.feed.StoryGroup
import com.socialx.app.ui.profile.UserProfileActivity
import com.socialx.app.ui.story.StoryCreateActivity
import com.socialx.app.ui.story.StoryViewerActivity
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _b: FragmentHomeBinding? = null
    private val b get() = _b!!
    private var me: User? = null
    private lateinit var postAdapter: PostAdapter
    private lateinit var storyAdapter: StoryAdapter
    private val posts = mutableListOf<Post>()
    private var loading = false
    private var exhausted = false
    private var storyGroups: List<StoryGroup> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?): View {
        _b = FragmentHomeBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        postAdapter = PostAdapter(viewLifecycleOwner, me?.id ?: "") { userId ->
            startActivity(
                Intent(requireContext(), UserProfileActivity::class.java)
                    .putExtra(UserProfileActivity.EXTRA_USER_ID, userId)
            )
        }
        b.feedList.layoutManager = LinearLayoutManager(requireContext())
        b.feedList.adapter = postAdapter

        storyAdapter = StoryAdapter { group -> openStory(group) }
        b.storiesList.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        b.storiesList.adapter = storyAdapter

        b.refresh.setOnRefreshListener { reload() }
        b.tvComposer.setOnClickListener { startActivity(Intent(requireContext(), CreatePostActivity::class.java)) }
        b.btnComposerPhoto.setOnClickListener {
            startActivity(
                Intent(requireContext(), CreatePostActivity::class.java)
                    .putExtra(CreatePostActivity.EXTRA_PICK_IMAGE, true)
            )
        }

        b.feedList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0 || loading || exhausted) return
                val lm = rv.layoutManager as LinearLayoutManager
                if (lm.findLastVisibleItemPosition() >= postAdapter.itemCount - 3) loadMore()
            }
        })

        lifecycleScope.launch {
            me = ServiceLocator.repo.currentUser()
            me?.let {
                b.myAvatar.loadAvatar(it.avatar, it.name)
                postAdapter = PostAdapter(viewLifecycleOwner, it.id) { userId ->
                    startActivity(
                        Intent(requireContext(), UserProfileActivity::class.java)
                            .putExtra(UserProfileActivity.EXTRA_USER_ID, userId)
                    )
                }
                b.feedList.adapter = postAdapter
                reload()
            }
        }
    }

    private fun reload() {
        val user = me ?: return
        loading = true
        exhausted = false
        lifecycleScope.launch {
            posts.clear()
            val page = ServiceLocator.repo.feed(user.id, 0, AppConfig.PAGE_SIZE)
            posts.addAll(page)
            exhausted = page.size < AppConfig.PAGE_SIZE
            postAdapter.submit(posts)
            b.tvEmptyFeed.visibility = if (posts.isEmpty()) View.VISIBLE else View.GONE
            loadStories(user)
            b.refresh.isRefreshing = false
            loading = false
        }
    }

    private fun loadMore() {
        val user = me ?: return
        loading = true
        lifecycleScope.launch {
            val page = ServiceLocator.repo.feed(user.id, posts.size, AppConfig.PAGE_SIZE)
            if (page.isEmpty()) exhausted = true
            posts.addAll(page)
            postAdapter.append(page)
            loading = false
        }
    }

    private suspend fun loadStories(user: User) {
        val stories: List<Story> = ServiceLocator.repo.activeStories(user.id)
        val groups = mutableListOf<StoryGroup>()
        groups.add(StoryGroup(user.id, user.name, user.avatar, false, true))
        stories.filter { !it.mine }.groupBy { it.userId }.forEach { (_, list) ->
            val first = list.first()
            groups.add(StoryGroup(first.userId, first.userName, first.userAvatar, list.any { !it.viewed }, false))
        }
        storyGroups = groups
        storyAdapter.submit(groups)
    }

    private fun openStory(group: StoryGroup) {
        if (group.isMine) {
            val mine = storyGroups.firstOrNull { it.isMine }
            if (mine != null && ServiceLocator.prefs.userId == group.userId) {
                lifecycleScope.launch {
                    val myStories = ServiceLocator.repo.myStories(group.userId)
                    if (myStories.isEmpty()) {
                        startActivity(Intent(requireContext(), StoryCreateActivity::class.java))
                    } else {
                        startActivity(
                            Intent(requireContext(), StoryViewerActivity::class.java)
                                .putExtra(StoryViewerActivity.EXTRA_USER_ID, group.userId)
                        )
                    }
                }
            }
        } else {
            startActivity(
                Intent(requireContext(), StoryViewerActivity::class.java)
                    .putExtra(StoryViewerActivity.EXTRA_USER_ID, group.userId)
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (me != null) reload()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}

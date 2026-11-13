package com.socialx.app.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.socialx.app.core.ServiceLocator
import com.socialx.app.core.loadAvatar
import com.socialx.app.core.loadMedia
import com.socialx.app.data.Post
import com.socialx.app.databinding.FragmentProfileBinding
import com.socialx.app.ui.feed.PostAdapter
import com.socialx.app.ui.profile.EditProfileActivity
import com.socialx.app.ui.profile.UserProfileActivity
import com.socialx.app.ui.settings.SettingsActivity
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _b: FragmentProfileBinding? = null
    private val b get() = _b!!
    private lateinit var adapter: PostAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?): View {
        _b = FragmentProfileBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = PostAdapter(viewLifecycleOwner, ServiceLocator.prefs.userId.orEmpty()) { }
        b.myPostsList.layoutManager = LinearLayoutManager(requireContext())
        b.myPostsList.adapter = adapter

        b.btnEditProfile.setOnClickListener {
            startActivity(Intent(requireContext(), EditProfileActivity::class.java))
        }
        b.btnSettings.setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }
        b.btnSaved.setOnClickListener {
            startActivity(
                Intent(requireContext(), UserProfileActivity::class.java)
                    .putExtra(UserProfileActivity.EXTRA_USER_ID, ServiceLocator.prefs.userId)
                    .putExtra(UserProfileActivity.EXTRA_SHOW_SAVED, true)
            )
        }
        b.statFriends.setOnClickListener { openFriends() }
        b.profileRefresh.setOnRefreshListener { load() }
    }

    private fun openFriends() {
        (activity as? MainActivity)?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(
            com.socialx.app.R.id.bottomNav
        )?.selectedItemId = com.socialx.app.R.id.nav_friends
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    private fun load() {
        val meId = ServiceLocator.prefs.userId ?: return
        lifecycleScope.launch {
            val user = ServiceLocator.repo.getUser(meId) ?: return@launch
            b.tvProfileName.text = user.name
            b.tvProfileHandle.text = "@${user.username}"
            b.tvProfileBio.text = user.bio.ifBlank { "Add a short bio to tell people about yourself." }
            b.profileAvatar.loadAvatar(user.avatar, user.name)
            if (user.cover.isNotBlank()) {
                b.coverImage.loadMedia(user.cover)
                b.coverImage.visibility = View.VISIBLE
            } else {
                b.coverImage.visibility = View.GONE
            }
            val friends = ServiceLocator.repo.friends(meId)
            val followers = ServiceLocator.repo.followers(meId)
            val following = ServiceLocator.repo.following(meId)
            b.tvFriendCount.text = friends.size.toString()
            b.tvFollowerCount.text = followers.size.toString()
            b.tvFollowingCount.text = following.size.toString()

            val posts: List<Post> = ServiceLocator.repo.userPosts(meId, meId, 0, 50)
            adapter.submit(posts)
            b.profileRefresh.isRefreshing = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}

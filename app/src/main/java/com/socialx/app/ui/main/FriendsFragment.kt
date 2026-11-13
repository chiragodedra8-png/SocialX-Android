package com.socialx.app.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.socialx.app.R
import com.socialx.app.core.ServiceLocator
import com.socialx.app.core.Utils
import com.socialx.app.data.PersonCard
import com.socialx.app.databinding.FragmentFriendsBinding
import com.socialx.app.ui.friends.UserAdapter
import com.socialx.app.ui.profile.UserProfileActivity
import kotlinx.coroutines.launch

class FriendsFragment : Fragment() {

    private var _b: FragmentFriendsBinding? = null
    private val b get() = _b!!
    private lateinit var adapter: UserAdapter
    private var mode = MODE_SUGGESTIONS
    private val requestIds = mutableMapOf<String, String>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?): View {
        _b = FragmentFriendsBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = UserAdapter(onAction = { card -> handleAction(card) }, onOpen = { card ->
            startActivity(
                Intent(requireContext(), UserProfileActivity::class.java)
                    .putExtra(UserProfileActivity.EXTRA_USER_ID, card.user.id)
            )
        })
        b.friendsList.layoutManager = LinearLayoutManager(requireContext())
        b.friendsList.adapter = adapter

        b.tabRequests.setOnClickListener { select(MODE_REQUESTS) }
        b.tabFriends.setOnClickListener { select(MODE_FRIENDS) }
        b.tabSuggestions.setOnClickListener { select(MODE_SUGGESTIONS) }
        select(MODE_SUGGESTIONS)
    }

    private fun select(newMode: Int) {
        mode = newMode
        val tabs = listOf(b.tabRequests, b.tabFriends, b.tabSuggestions)
        tabs.forEachIndexed { index, button ->
            button.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (index == newMode) R.color.brand_blue else R.color.text_secondary
                )
            )
        }
        b.tvFriendsTitle.text = when (mode) {
            MODE_REQUESTS -> "Friend requests"
            MODE_FRIENDS -> "Your friends"
            else -> "People you may know"
        }
        load()
    }

    private fun load() {
        val me = ServiceLocator.prefs.userId ?: return
        lifecycleScope.launch {
            val list: List<PersonCard> = when (mode) {
                MODE_REQUESTS -> {
                    val requests = ServiceLocator.repo.friendRequests(me)
                    requestIds.clear()
                    requests.forEach { requestIds[it.user.id] = it.relation }
                    requests.map { it.copy(relation = PersonCard.RECEIVED) }
                }
                MODE_FRIENDS -> ServiceLocator.repo.friends(me)
                else -> ServiceLocator.repo.suggestedPeople(me, 25)
            }
            adapter.submit(list)
            b.tvFriendsEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            b.tvFriendsEmpty.text = when (mode) {
                MODE_REQUESTS -> "No pending friend requests."
                MODE_FRIENDS -> "You have no friends yet. Try suggestions."
                else -> "No suggestions right now."
            }
        }
    }

    private fun handleAction(card: PersonCard) {
        val me = ServiceLocator.prefs.userId ?: return
        lifecycleScope.launch {
            when (card.relation) {
                PersonCard.RECEIVED -> {
                    val requestId = requestIds[card.user.id]
                    if (requestId != null) {
                        ServiceLocator.repo.respondRequest(requestId, true)
                        Utils.toast(requireContext(), "You are now friends")
                    }
                }
                PersonCard.NONE -> {
                    ServiceLocator.repo.sendFriendRequest(me, card.user.id)
                        .onSuccess { Utils.toast(requireContext(), "Friend request sent") }
                        .onFailure { Utils.toast(requireContext(), it.message ?: "Could not send request") }
                }
            }
            load()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }

    companion object {
        private const val MODE_REQUESTS = 0
        private const val MODE_FRIENDS = 1
        private const val MODE_SUGGESTIONS = 2
    }
}

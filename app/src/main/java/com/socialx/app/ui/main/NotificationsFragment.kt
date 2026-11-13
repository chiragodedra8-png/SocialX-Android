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
import com.socialx.app.databinding.FragmentNotificationsBinding
import com.socialx.app.ui.notifications.NotificationsAdapter
import com.socialx.app.ui.profile.UserProfileActivity
import kotlinx.coroutines.launch

class NotificationsFragment : Fragment() {

    private var _b: FragmentNotificationsBinding? = null
    private val b get() = _b!!
    private lateinit var adapter: NotificationsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?): View {
        _b = FragmentNotificationsBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = NotificationsAdapter { notification ->
            startActivity(
                Intent(requireContext(), UserProfileActivity::class.java)
                    .putExtra(UserProfileActivity.EXTRA_USER_ID, notification.actorId)
            )
        }
        b.notificationsList.layoutManager = LinearLayoutManager(requireContext())
        b.notificationsList.adapter = adapter
        b.btnMarkRead.setOnClickListener {
            val me = ServiceLocator.prefs.userId ?: return@setOnClickListener
            lifecycleScope.launch {
                ServiceLocator.repo.markNotificationsRead(me)
                load()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    private fun load() {
        val me = ServiceLocator.prefs.userId ?: return
        lifecycleScope.launch {
            val list = ServiceLocator.repo.notifications(me)
            adapter.submit(list)
            b.tvNotifEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}

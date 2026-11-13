package com.socialx.app.ui.chat

import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.socialx.app.core.ServiceLocator
import kotlinx.coroutines.launch

object NewChatDialog {
    fun show(fragment: Fragment, onPick: (String) -> Unit) {
        val me = ServiceLocator.prefs.userId ?: return
        fragment.lifecycleScope.launch {
            val friends = ServiceLocator.repo.friends(me)
            if (friends.isEmpty()) {
                com.socialx.app.core.Utils.toast(fragment.requireContext(), "Add a friend first to start a chat")
                return@launch
            }
            val names = friends.map { "${it.user.name}  @${it.user.username}" }.toTypedArray()
            androidx.appcompat.app.AlertDialog.Builder(fragment.requireContext())
                .setTitle("New chat")
                .setItems(names) { _, which -> onPick(friends[which].user.id) }
                .show()
        }
    }
}

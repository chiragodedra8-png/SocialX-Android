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
import com.socialx.app.databinding.FragmentChatsBinding
import com.socialx.app.ui.chat.ChatActivity
import com.socialx.app.ui.chat.ChatAdapter
import com.socialx.app.ui.chat.NewChatDialog
import kotlinx.coroutines.launch

class ChatsFragment : Fragment() {

    private var _b: FragmentChatsBinding? = null
    private val b get() = _b!!
    private lateinit var adapter: ChatAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?): View {
        _b = FragmentChatsBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = ChatAdapter { chat ->
            startActivity(
                Intent(requireContext(), ChatActivity::class.java)
                    .putExtra(ChatActivity.EXTRA_CONVERSATION_ID, chat.conversationId)
                    .putExtra(ChatActivity.EXTRA_OTHER_ID, chat.other.id)
            )
        }
        b.chatsList.layoutManager = LinearLayoutManager(requireContext())
        b.chatsList.adapter = adapter
        b.btnNewChat.setOnClickListener {
            NewChatDialog.show(this) { otherId ->
                lifecycleScope.launch {
                    val me = ServiceLocator.prefs.userId ?: return@launch
                    val conversationId = ServiceLocator.repo.openConversation(me, otherId)
                    startActivity(
                        Intent(requireContext(), ChatActivity::class.java)
                            .putExtra(ChatActivity.EXTRA_CONVERSATION_ID, conversationId)
                            .putExtra(ChatActivity.EXTRA_OTHER_ID, otherId)
                    )
                }
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
            val chats = ServiceLocator.repo.conversations(me)
            adapter.submit(chats)
            b.tvChatsEmpty.visibility = if (chats.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}

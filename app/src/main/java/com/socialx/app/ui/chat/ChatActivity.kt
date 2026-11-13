package com.socialx.app.ui.chat

import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.socialx.app.core.ServiceLocator
import com.socialx.app.core.Utils
import com.socialx.app.data.MediaStoreHelper
import com.socialx.app.data.Message
import com.socialx.app.databinding.ActivityChatBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ChatActivity : AppCompatActivity() {

    private lateinit var b: ActivityChatBinding
    private lateinit var adapter: MessageAdapter
    private var conversationId = ""
    private var otherId = ""
    private var meId = ""
    private var pollJob: Job? = null

    private val pickMedia = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val kind = MediaStoreHelper.mediaKind(this, it)
            val path = MediaStoreHelper.copyToApp(this, it, kind)
            if (path != null) send("", path, kind)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityChatBinding.inflate(layoutInflater)
        setContentView(b.root)
        conversationId = intent.getStringExtra(EXTRA_CONVERSATION_ID).orEmpty()
        otherId = intent.getStringExtra(EXTRA_OTHER_ID).orEmpty()
        meId = ServiceLocator.prefs.userId.orEmpty()

        b.toolbar.setNavigationOnClickListener { finish() }
        adapter = MessageAdapter { message -> confirmDelete(message) }
        b.messagesList.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        b.messagesList.adapter = adapter

        b.btnSend.setOnClickListener {
            val text = b.etMessage.text?.toString().orEmpty().trim()
            if (text.isNotEmpty()) {
                b.etMessage.setText("")
                send(text, "", "")
            }
        }
        b.btnAttach.setOnClickListener { pickMedia.launch("*/*") }

        b.etMessage.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                lifecycleScope.launch {
                    ServiceLocator.repo.setTyping(meId, conversationId, !s.isNullOrBlank())
                }
            }
            override fun beforeTextChanged(s: CharSequence?, a: Int, c: Int, d: Int) = Unit
            override fun onTextChanged(s: CharSequence?, a: Int, c: Int, d: Int) = Unit
        })

        loadHeader()
        loadMessages()
        startPolling()
    }

    private fun loadHeader() {
        lifecycleScope.launch {
            val other = ServiceLocator.repo.otherParticipant(conversationId, meId)
            if (other != null) {
                b.toolbar.title = other.name
                b.toolbar.subtitle = if (other.online) "Online" else "Last seen ${Utils.timeAgo(other.lastSeen)} ago"
            }
        }
    }

    private fun loadMessages() {
        lifecycleScope.launch {
            val list = ServiceLocator.repo.messages(conversationId, meId)
            adapter.submit(list)
            if (list.isNotEmpty()) b.messagesList.scrollToPosition(list.size - 1)
            ServiceLocator.repo.markConversationRead(conversationId, meId)
        }
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = lifecycleScope.launch {
            while (true) {
                delay(1500)
                val list = ServiceLocator.repo.messages(conversationId, meId)
                val typing = ServiceLocator.repo.isTyping(conversationId, otherId)
                b.tvTyping.visibility = if (typing) android.view.View.VISIBLE else android.view.View.GONE
                if (list.size != adapter.itemCount || list.any { it.readAt > 0 }) {
                    adapter.submit(list)
                    b.messagesList.scrollToPosition(if (list.isEmpty()) 0 else list.size - 1)
                }
                ServiceLocator.repo.markConversationRead(conversationId, meId)
            }
        }
    }

    private fun send(text: String, mediaPath: String, mediaType: String) {
        lifecycleScope.launch {
            ServiceLocator.repo.sendMessage(conversationId, meId, text, mediaPath, mediaType)
                .onSuccess {
                    adapter.add(it)
                    b.messagesList.scrollToPosition(adapter.itemCount - 1)
                }
                .onFailure { Utils.toast(this@ChatActivity, it.message ?: "Could not send") }
        }
    }

    private fun confirmDelete(message: Message) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete message?")
            .setMessage("This removes the message for everyone in this chat.")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    ServiceLocator.repo.deleteMessage(message.id, meId)
                    loadMessages()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        pollJob?.cancel()
    }

    companion object {
        const val EXTRA_CONVERSATION_ID = "conversation_id"
        const val EXTRA_OTHER_ID = "other_id"
    }
}

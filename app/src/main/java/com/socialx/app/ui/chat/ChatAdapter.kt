package com.socialx.app.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.socialx.app.core.Utils
import com.socialx.app.core.loadAvatar
import com.socialx.app.data.Chat
import com.socialx.app.databinding.ItemChatBinding

class ChatAdapter(private val onOpen: (Chat) -> Unit) : RecyclerView.Adapter<ChatAdapter.VH>() {

    private val items = mutableListOf<Chat>()

    fun submit(list: List<Chat>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemChatBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    inner class VH(private val b: ItemChatBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(chat: Chat) {
            b.chatName.text = chat.other.name
            b.chatLast.text = chat.lastText
            b.chatTime.text = Utils.timeAgo(chat.lastAt)
            b.chatAvatar.loadAvatar(chat.other.avatar, chat.other.name)
            b.onlineDot.visibility = if (chat.other.online) View.VISIBLE else View.GONE
            if (chat.unread > 0) {
                b.unreadBadge.visibility = View.VISIBLE
                b.unreadBadge.text = if (chat.unread > 9) "9+" else chat.unread.toString()
            } else {
                b.unreadBadge.visibility = View.GONE
            }
            b.root.setOnClickListener { onOpen(chat) }
        }
    }
}

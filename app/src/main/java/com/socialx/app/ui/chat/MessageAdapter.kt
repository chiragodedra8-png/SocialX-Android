package com.socialx.app.ui.chat

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.socialx.app.R
import com.socialx.app.core.Utils
import com.socialx.app.core.loadMedia
import com.socialx.app.data.Message
import com.socialx.app.databinding.ItemMessageBinding

class MessageAdapter(
    private val onDelete: (Message) -> Unit
) : RecyclerView.Adapter<MessageAdapter.VH>() {

    private val items = mutableListOf<Message>()

    fun submit(list: List<Message>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    fun add(message: Message) {
        items.add(message)
        notifyItemInserted(items.size - 1)
    }

    fun update(message: Message) {
        val index = items.indexOfFirst { it.id == message.id }
        if (index >= 0) {
            items[index] = message
            notifyItemChanged(index)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    inner class VH(private val b: ItemMessageBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(message: Message) {
            val context = b.root.context
            if (message.mine) {
                b.bubbleRow.gravity = Gravity.END
                b.bubble.setBackgroundResource(R.drawable.bg_bubble_me)
                b.messageText.setTextColor(ContextCompat.getColor(context, R.color.white))
                b.metaRow.gravity = Gravity.END
            } else {
                b.bubbleRow.gravity = Gravity.START
                b.bubble.setBackgroundResource(R.drawable.bg_bubble_other)
                b.messageText.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                b.metaRow.gravity = Gravity.START
            }

            if (message.deleted) {
                b.messageText.text = "This message was deleted"
                b.messageText.alpha = 0.6f
            } else {
                b.messageText.alpha = 1f
                b.messageText.text = message.text
                b.messageText.visibility = if (message.text.isBlank()) View.GONE else View.VISIBLE
            }

            if (message.mediaUri.isNotBlank() && !message.deleted) {
                b.messageImage.visibility = View.VISIBLE
                b.messageImage.loadMedia(message.mediaUri)
            } else {
                b.messageImage.visibility = View.GONE
            }

            b.messageTime.text = Utils.clock(message.createdAt)
            b.messageStatus.text = when {
                !message.mine -> ""
                message.readAt > 0 -> "✓✓ Read"
                else -> "✓ Sent"
            }
            b.bubble.setOnLongClickListener {
                if (message.mine && !message.deleted) onDelete(message)
                true
            }
        }
    }
}

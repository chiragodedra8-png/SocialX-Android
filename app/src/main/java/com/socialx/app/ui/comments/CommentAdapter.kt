package com.socialx.app.ui.comments

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.socialx.app.core.Utils
import com.socialx.app.core.loadAvatar
import com.socialx.app.data.Comment
import com.socialx.app.databinding.ItemCommentBinding

class CommentAdapter(
    private val onReply: (Comment) -> Unit
) : RecyclerView.Adapter<CommentAdapter.VH>() {

    private val items = mutableListOf<Comment>()

    fun submit(list: List<Comment>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    fun add(comment: Comment) {
        items.add(comment)
        notifyItemInserted(items.size - 1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    inner class VH(private val b: ItemCommentBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(comment: Comment) {
            b.commentAvatar.loadAvatar(comment.authorAvatar, comment.authorName)
            val text = SpannableStringBuilder()
            val nameEnd = comment.authorName.length
            text.append(comment.authorName)
            text.setSpan(StyleSpan(Typeface.BOLD), 0, nameEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            if (comment.replyToName != null) {
                val prefix = "  ▸ ${comment.replyToName}\n"
                text.append(prefix)
            } else {
                text.append("\n")
            }
            text.append(comment.text)
            b.commentBody.text = text
            b.commentTime.text = Utils.timeAgo(comment.createdAt)
            b.btnReply.setOnClickListener { onReply(comment) }
        }
    }
}

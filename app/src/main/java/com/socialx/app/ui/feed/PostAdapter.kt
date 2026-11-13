package com.socialx.app.ui.feed

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.socialx.app.R
import com.socialx.app.core.ServiceLocator
import com.socialx.app.core.Utils
import com.socialx.app.core.loadAvatar
import com.socialx.app.core.loadMedia
import com.socialx.app.data.Post
import com.socialx.app.databinding.ItemPostBinding
import com.socialx.app.ui.comments.CommentsActivity
import kotlinx.coroutines.launch
import java.io.File

class PostAdapter(
    private val owner: LifecycleOwner,
    private val meId: String,
    private val onOpenProfile: (String) -> Unit
) : RecyclerView.Adapter<PostAdapter.VH>() {

    private val items = mutableListOf<Post>()

    fun submit(list: List<Post>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    fun append(list: List<Post>) {
        val start = items.size
        items.addAll(list)
        notifyItemRangeInserted(start, list.size)
    }

    fun remove(postId: String) {
        val index = items.indexOfFirst { it.id == postId }
        if (index >= 0) {
            items.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    inner class VH(private val b: ItemPostBinding) : RecyclerView.ViewHolder(b.root) {

        fun bind(post: Post) {
            val context = b.root.context
            b.tvAuthor.text = post.authorName
            b.tvTime.text = Utils.timeAgo(post.createdAt)
            b.authorAvatar.loadAvatar(post.authorAvatar, post.authorName)
            b.tvText.text = post.text
            b.tvText.visibility = if (post.text.isBlank()) View.GONE else View.VISIBLE

            if (post.mediaUri.isNotBlank()) {
                b.mediaContainer.visibility = View.VISIBLE
                if (post.mediaType == "video") {
                    b.postImage.visibility = View.GONE
                    b.postVideo.visibility = View.VISIBLE
                    b.videoPlay.visibility = View.VISIBLE
                    b.postVideo.setVideoPath(post.mediaUri)
                    b.videoPlay.setOnClickListener {
                        b.videoPlay.visibility = View.GONE
                        b.postVideo.start()
                    }
                } else {
                    b.postImage.visibility = View.VISIBLE
                    b.postVideo.visibility = View.GONE
                    b.videoPlay.visibility = View.GONE
                    b.postImage.loadMedia(post.mediaUri)
                }
            } else {
                b.mediaContainer.visibility = View.GONE
            }

            if (post.sharedFromName != null) {
                b.sharedCard.visibility = View.VISIBLE
                b.tvSharedFrom.text = "${post.sharedFromName}'s post"
                b.tvSharedText.text = post.sharedFromText.orEmpty()
            } else {
                b.sharedCard.visibility = View.GONE
            }

            b.tvLikeCount.text = if (post.likeCount > 0) "${Utils.compactCount(post.likeCount)} likes" else ""
            b.tvCommentCount.text =
                if (post.commentCount > 0) "${Utils.compactCount(post.commentCount)} comments" else ""

            paintLike(post.likedByMe)
            b.iconSave.setColorFilter(
                ContextCompat.getColor(context, if (post.savedByMe) R.color.brand_blue else R.color.text_secondary)
            )

            b.btnLike.setOnClickListener {
                owner.lifecycleScope.launch {
                    val liked = ServiceLocator.repo.toggleLike(post.id, meId)
                    val updated = post.copy(
                        likedByMe = liked,
                        likeCount = post.likeCount + if (liked) 1 else -1
                    )
                    val index = items.indexOfFirst { it.id == post.id }
                    if (index >= 0) {
                        items[index] = updated
                        notifyItemChanged(index)
                    }
                }
            }

            b.btnSave.setOnClickListener {
                owner.lifecycleScope.launch {
                    val saved = ServiceLocator.repo.toggleSave(post.id, meId)
                    val index = items.indexOfFirst { it.id == post.id }
                    if (index >= 0) {
                        items[index] = post.copy(savedByMe = saved)
                        notifyItemChanged(index)
                    }
                }
            }

            b.btnComment.setOnClickListener { openComments(post) }
            b.tvCommentCount.setOnClickListener { openComments(post) }

            b.btnShare.setOnClickListener {
                owner.lifecycleScope.launch {
                    ServiceLocator.repo.sharePost(post.id, meId, "")
                        .onFailure { Utils.toast(context, it.message ?: "Could not share") }
                        .onSuccess { Utils.toast(context, "Shared to your profile") }
                }
            }

            b.btnMore.setOnClickListener { showMore(post) }
            b.authorAvatar.setOnClickListener { onOpenProfile(post.userId) }
            b.tvAuthor.setOnClickListener { onOpenProfile(post.userId) }
            b.postImage.setOnClickListener {
                if (post.mediaUri.isNotBlank() && File(post.mediaUri).exists()) {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.setDataAndType(android.net.Uri.fromFile(File(post.mediaUri)), "image/*")
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    try {
                        context.startActivity(intent)
                    } catch (ignored: Exception) {
                    }
                }
            }
        }

        private fun paintLike(liked: Boolean) {
            val context = b.root.context
            b.iconLike.setColorFilter(
                ContextCompat.getColor(context, if (liked) R.color.like_red else R.color.text_secondary)
            )
            b.tvLikeLabel.setTextColor(
                ContextCompat.getColor(context, if (liked) R.color.like_red else R.color.text_secondary)
            )
            b.tvLikeLabel.text = if (liked) "Liked" else "Like"
        }

        private fun openComments(post: Post) {
            val intent = Intent(b.root.context, CommentsActivity::class.java)
            intent.putExtra(CommentsActivity.EXTRA_POST_ID, post.id)
            b.root.context.startActivity(intent)
        }

        private fun showMore(post: Post) {
            val context = b.root.context
            val options = if (post.userId == meId) {
                arrayOf("Delete post", "Save post", "Report post")
            } else {
                arrayOf("Save post", "Report post", "Block ${post.authorName}")
            }
            androidx.appcompat.app.AlertDialog.Builder(context)
                .setItems(options) { _, which ->
                    owner.lifecycleScope.launch {
                        when {
                            options[which].startsWith("Delete") -> {
                                ServiceLocator.repo.deletePost(post.id, meId).onSuccess {
                                    remove(post.id)
                                    Utils.toast(context, "Post deleted")
                                }
                            }
                            options[which].startsWith("Save") -> {
                                ServiceLocator.repo.toggleSave(post.id, meId)
                                Utils.toast(context, "Saved posts updated")
                            }
                            options[which].startsWith("Block") -> {
                                ServiceLocator.repo.blockUser(meId, post.userId)
                                remove(post.id)
                                Utils.toast(context, "User blocked")
                            }
                            else -> {
                                ServiceLocator.repo.report(meId, "post", post.id, "Reported from feed")
                                Utils.toast(context, "Reported. Thanks for letting us know.")
                            }
                        }
                    }
                }
                .show()
        }
    }
}

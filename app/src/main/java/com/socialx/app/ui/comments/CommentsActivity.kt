package com.socialx.app.ui.comments

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.socialx.app.core.ServiceLocator
import com.socialx.app.core.Utils
import com.socialx.app.data.Comment
import com.socialx.app.databinding.ActivityCommentsBinding
import kotlinx.coroutines.launch

class CommentsActivity : AppCompatActivity() {

    private lateinit var b: ActivityCommentsBinding
    private lateinit var adapter: CommentAdapter
    private var postId = ""
    private var replyTo: Comment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityCommentsBinding.inflate(layoutInflater)
        setContentView(b.root)
        postId = intent.getStringExtra(EXTRA_POST_ID).orEmpty()
        b.toolbar.setNavigationOnClickListener { finish() }

        adapter = CommentAdapter { comment ->
            replyTo = comment
            b.tvReplyingTo.visibility = View.VISIBLE
            b.tvReplyingTo.text = "Replying to ${comment.authorName}  ·  tap to cancel"
            b.tvReplyingTo.setOnClickListener {
                replyTo = null
                b.tvReplyingTo.visibility = View.GONE
            }
            b.etComment.requestFocus()
        }
        b.commentsList.layoutManager = LinearLayoutManager(this)
        b.commentsList.adapter = adapter
        b.btnSendComment.setOnClickListener { send() }
        load()
    }

    private fun load() {
        lifecycleScope.launch {
            val comments = ServiceLocator.repo.comments(postId)
            adapter.submit(comments)
            b.toolbar.title = "Comments (${comments.size})"
        }
    }

    private fun send() {
        val text = b.etComment.text?.toString().orEmpty().trim()
        if (text.isEmpty()) return
        val me = ServiceLocator.prefs.userId ?: return
        b.etComment.setText("")
        lifecycleScope.launch {
            ServiceLocator.repo.addComment(postId, me, text, replyTo?.id)
                .onSuccess {
                    adapter.add(it)
                    b.commentsList.scrollToPosition(adapter.itemCount - 1)
                    replyTo = null
                    b.tvReplyingTo.visibility = View.GONE
                }
                .onFailure { Utils.toast(this@CommentsActivity, it.message ?: "Could not comment") }
        }
    }

    companion object {
        const val EXTRA_POST_ID = "post_id"
    }
}

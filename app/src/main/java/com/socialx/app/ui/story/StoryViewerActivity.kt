package com.socialx.app.ui.story

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.socialx.app.core.ServiceLocator
import com.socialx.app.core.Utils
import com.socialx.app.core.loadAvatar
import com.socialx.app.core.loadMedia
import com.socialx.app.data.Story
import com.socialx.app.databinding.ActivityStoryViewerBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class StoryViewerActivity : AppCompatActivity() {

    private lateinit var b: ActivityStoryViewerBinding
    private var stories: List<Story> = emptyList()
    private var index = 0
    private var progressJob: Job? = null
    private var viewerId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityStoryViewerBinding.inflate(layoutInflater)
        setContentView(b.root)
        viewerId = ServiceLocator.prefs.userId.orEmpty()
        val userId = intent.getStringExtra(EXTRA_USER_ID).orEmpty()

        b.btnCloseStory.setOnClickListener { finish() }
        b.storyImage.setOnClickListener { next() }

        lifecycleScope.launch {
            stories = ServiceLocator.repo.activeStories(viewerId).filter { it.userId == userId }
            if (stories.isEmpty()) {
                Utils.toast(this@StoryViewerActivity, "This story has expired")
                finish()
                return@launch
            }
            show(0)
        }
    }

    private fun show(position: Int) {
        val story = stories.getOrNull(position) ?: run { finish(); return }
        index = position
        b.storyName.text = story.userName
        b.storyTime.text = Utils.timeAgo(story.createdAt)
        b.storyAvatar.loadAvatar(story.userAvatar, story.userName)
        b.storyCaption.text = story.caption
        b.storyCaption.visibility = if (story.caption.isBlank()) View.GONE else View.VISIBLE

        if (story.mediaType == "video") {
            b.storyImage.visibility = View.GONE
            b.storyVideo.visibility = View.VISIBLE
            b.storyVideo.setVideoPath(story.mediaUri)
            b.storyVideo.setOnPreparedListener { it.isLooping = false; it.start() }
        } else {
            b.storyVideo.visibility = View.GONE
            b.storyImage.visibility = View.VISIBLE
            b.storyImage.loadMedia(story.mediaUri)
        }

        lifecycleScope.launch {
            ServiceLocator.repo.markStoryViewed(story.id, viewerId)
            if (story.mine) {
                val viewers = ServiceLocator.repo.storyViewers(story.id)
                b.tvViewers.visibility = View.VISIBLE
                b.tvViewers.text = "👁 ${viewers.size} viewers"
                b.tvViewers.setOnClickListener {
                    if (viewers.isEmpty()) {
                        Toast.makeText(this@StoryViewerActivity, "No viewers yet", Toast.LENGTH_SHORT).show()
                    } else {
                        androidx.appcompat.app.AlertDialog.Builder(this@StoryViewerActivity)
                            .setTitle("Viewers")
                            .setItems(viewers.map { it.name }.toTypedArray(), null)
                            .show()
                    }
                }
            } else {
                b.tvViewers.visibility = View.GONE
            }
        }

        progressJob?.cancel()
        progressJob = lifecycleScope.launch {
            b.storyProgress.progress = 0
            val duration = if (story.mediaType == "video") 15_000L else 5_000L
            val steps = 100
            repeat(steps) { step ->
                delay(duration / steps)
                b.storyProgress.progress = step + 1
            }
            next()
        }
    }

    private fun next() {
        if (index + 1 < stories.size) show(index + 1) else finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        progressJob?.cancel()
    }

    companion object {
        const val EXTRA_USER_ID = "story_user_id"
    }
}

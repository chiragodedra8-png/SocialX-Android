package com.socialx.app.ui.story

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.socialx.app.core.ServiceLocator
import com.socialx.app.core.Utils
import com.socialx.app.core.loadMedia
import com.socialx.app.data.MediaStoreHelper
import com.socialx.app.databinding.ActivityStoryCreateBinding
import kotlinx.coroutines.launch

class StoryCreateActivity : AppCompatActivity() {

    private lateinit var b: ActivityStoryCreateBinding
    private var mediaPath = ""
    private var mediaType = ""

    private val pick = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val kind = MediaStoreHelper.mediaKind(this, it)
            MediaStoreHelper.copyToApp(this, it, kind)?.let { path ->
                mediaPath = path
                mediaType = kind
                b.storyPreview.visibility = View.VISIBLE
                b.pickPrompt.visibility = View.GONE
                b.storyPreview.loadMedia(path)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityStoryCreateBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.toolbar.setNavigationOnClickListener { finish() }
        b.btnPick.setOnClickListener { pick.launch("*/*") }
        b.btnShareStory.setOnClickListener { share() }
        pick.launch("*/*")
    }

    private fun share() {
        val me = ServiceLocator.prefs.userId ?: return
        if (mediaPath.isEmpty()) {
            Utils.toast(this, "Pick a photo or video first")
            return
        }
        b.btnShareStory.isEnabled = false
        lifecycleScope.launch {
            ServiceLocator.repo.createStory(
                me, mediaPath, mediaType, b.etCaption.text?.toString().orEmpty()
            ).onSuccess {
                Utils.toast(this@StoryCreateActivity, "Story shared for 24 hours")
                finish()
            }.onFailure {
                b.btnShareStory.isEnabled = true
                Utils.toast(this@StoryCreateActivity, it.message ?: "Could not share story")
            }
        }
    }
}

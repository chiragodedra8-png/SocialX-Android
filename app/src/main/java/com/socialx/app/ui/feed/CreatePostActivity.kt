package com.socialx.app.ui.feed

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.socialx.app.core.ServiceLocator
import com.socialx.app.core.Utils
import com.socialx.app.core.loadAvatar
import com.socialx.app.core.loadMedia
import com.socialx.app.data.MediaStoreHelper
import com.socialx.app.databinding.ActivityCreatePostBinding
import kotlinx.coroutines.launch

class CreatePostActivity : AppCompatActivity() {

    private lateinit var b: ActivityCreatePostBinding
    private var mediaPath = ""
    private var mediaType = ""

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { handlePicked(it, "image") }
    }
    private val pickVideo = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { handlePicked(it, "video") }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityCreatePostBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.toolbar.setNavigationOnClickListener { finish() }

        lifecycleScope.launch {
            ServiceLocator.repo.currentUser()?.let {
                b.myAvatar.loadAvatar(it.avatar, it.name)
                b.tvMyName.text = it.name
            }
        }

        b.btnPickImage.setOnClickListener { pickImage.launch("image/*") }
        b.btnPickVideo.setOnClickListener { pickVideo.launch("video/*") }
        b.btnPost.setOnClickListener { submit() }

        if (intent.getBooleanExtra(EXTRA_PICK_IMAGE, false)) pickImage.launch("image/*")
    }

    private fun handlePicked(uri: Uri, kind: String) {
        val path = MediaStoreHelper.copyToApp(this, uri, kind)
        if (path == null) {
            Utils.toast(this, "Could not read that file")
            return
        }
        mediaPath = path
        mediaType = kind
        if (kind == "video") {
            b.previewImage.visibility = View.GONE
            b.previewVideo.visibility = View.VISIBLE
            b.previewVideo.setVideoPath(path)
        } else {
            b.previewVideo.visibility = View.GONE
            b.previewImage.visibility = View.VISIBLE
            b.previewImage.loadMedia(path)
        }
    }

    private fun submit() {
        val text = b.etPostText.text?.toString().orEmpty().trim()
        if (text.isEmpty() && mediaPath.isEmpty()) {
            Utils.toast(this, "Write something or add media")
            return
        }
        val userId = ServiceLocator.prefs.userId ?: return
        b.btnPost.isEnabled = false
        lifecycleScope.launch {
            ServiceLocator.repo.createPost(userId, text, mediaPath, mediaType)
                .onSuccess {
                    Utils.toast(this@CreatePostActivity, "Posted")
                    finish()
                }
                .onFailure {
                    b.btnPost.isEnabled = true
                    Utils.toast(this@CreatePostActivity, it.message ?: "Could not post")
                }
        }
    }

    companion object {
        const val EXTRA_PICK_IMAGE = "pick_image"
    }
}

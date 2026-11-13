package com.socialx.app.ui.profile

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
import com.socialx.app.data.User
import com.socialx.app.databinding.ActivityEditProfileBinding
import kotlinx.coroutines.launch

class EditProfileActivity : AppCompatActivity() {

    private lateinit var b: ActivityEditProfileBinding
    private var me: User? = null
    private var avatarPath = ""
    private var coverPath = ""

    private val pickAvatar = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            MediaStoreHelper.copyToApp(this, it, "image")?.let { path ->
                avatarPath = path
                b.avatarPreview.loadMedia(path)
            }
        }
    }
    private val pickCover = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            MediaStoreHelper.copyToApp(this, it, "image")?.let { path ->
                coverPath = path
                b.coverPreview.visibility = View.VISIBLE
                b.coverPreview.loadMedia(path)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.toolbar.setNavigationOnClickListener { finish() }
        b.btnChangeAvatar.setOnClickListener { pickAvatar.launch("image/*") }
        b.btnChangeCover.setOnClickListener { pickCover.launch("image/*") }
        b.btnSave.setOnClickListener { save() }

        lifecycleScope.launch {
            val user = ServiceLocator.repo.currentUser() ?: return@launch
            me = user
            b.etName.setText(user.name)
            b.etBio.setText(user.bio)
            b.etPhone.setText(user.phone)
            b.switchPrivate.isChecked = user.isPrivate
            b.avatarPreview.loadAvatar(user.avatar, user.name)
            if (user.cover.isNotBlank()) {
                b.coverPreview.visibility = View.VISIBLE
                b.coverPreview.loadMedia(user.cover)
            }
        }
    }

    private fun save() {
        val user = me ?: return
        val name = b.etName.text?.toString()?.trim().orEmpty()
        val bio = b.etBio.text?.toString().orEmpty().trim()
        if (name.isBlank()) {
            Utils.toast(this, "Name cannot be empty")
            return
        }
        b.btnSave.isEnabled = false
        lifecycleScope.launch {
            ServiceLocator.repo.updateProfile(
                user.id, name, bio, avatarPath, coverPath, b.switchPrivate.isChecked
            ).onSuccess {
                Utils.toast(this@EditProfileActivity, "Profile updated")
                finish()
            }.onFailure {
                b.btnSave.isEnabled = true
                Utils.toast(this@EditProfileActivity, it.message ?: "Could not save")
            }
        }
    }
}

package com.socialx.app.ui.settings

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.socialx.app.core.ServiceLocator
import com.socialx.app.core.Utils
import com.socialx.app.databinding.ActivitySettingsBinding
import com.socialx.app.ui.auth.AuthActivity
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var b: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.toolbar.setNavigationOnClickListener { finish() }

        val prefs = ServiceLocator.prefs
        when (prefs.darkMode) {
            AppCompatDelegate.MODE_NIGHT_NO -> b.themeLight.isChecked = true
            AppCompatDelegate.MODE_NIGHT_YES -> b.themeDark.isChecked = true
            else -> b.themeSystem.isChecked = true
        }
        b.themeSystem.setOnClickListener { prefs.darkMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM }
        b.themeLight.setOnClickListener { prefs.darkMode = AppCompatDelegate.MODE_NIGHT_NO }
        b.themeDark.setOnClickListener { prefs.darkMode = AppCompatDelegate.MODE_NIGHT_YES }

        b.switchPrivate.isChecked = prefs.privateAccount
        b.switchOnline.isChecked = prefs.showOnlineStatus
        b.switchRequests.isChecked = prefs.messageRequests

        b.switchPrivate.setOnCheckedChangeListener { _, checked -> prefs.privateAccount = checked }
        b.switchOnline.setOnCheckedChangeListener { _, checked -> prefs.showOnlineStatus = checked }
        b.switchRequests.setOnCheckedChangeListener { _, checked -> prefs.messageRequests = checked }

        b.rowChangePassword.setOnClickListener { changePassword() }
        b.rowTwoStep.setOnClickListener { toggleTwoStep() }
        b.rowBlocked.setOnClickListener { showBlocked() }
        b.rowDemo.setOnClickListener { seedDemo() }
        b.rowDelete.setOnClickListener { deleteAccount() }
        b.btnLogout.setOnClickListener { logout() }
    }

    private fun changePassword() {
        val me = ServiceLocator.prefs.userId ?: return
        val container = FrameLayout(this).apply { setPadding(48, 24, 48, 0) }
        val current = EditText(this).apply {
            hint = "Current password"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val fresh = EditText(this).apply {
            hint = "New password"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            addView(current)
            addView(fresh)
        }
        container.addView(layout)

        AlertDialog.Builder(this)
            .setTitle("Change password")
            .setView(container)
            .setPositiveButton("Update") { _, _ ->
                lifecycleScope.launch {
                    ServiceLocator.repo.changePassword(
                        me, current.text.toString(), fresh.text.toString()
                    ).onSuccess { Utils.toast(this@SettingsActivity, "Password updated") }
                        .onFailure { Utils.toast(this@SettingsActivity, it.message ?: "Failed") }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun toggleTwoStep() {
        val prefs = ServiceLocator.prefs
        prefs.twoStep = !prefs.twoStep
        Utils.toast(this, if (prefs.twoStep) "Two-step verification on" else "Two-step verification off")
    }

    private fun showBlocked() {
        val me = ServiceLocator.prefs.userId ?: return
        lifecycleScope.launch {
            val blocked = ServiceLocator.repo.blockedUsers(me)
            if (blocked.isEmpty()) {
                Utils.toast(this@SettingsActivity, "You have not blocked anyone")
                return@launch
            }
            val names = blocked.map { it.name }.toTypedArray()
            AlertDialog.Builder(this@SettingsActivity)
                .setTitle("Blocked users")
                .setItems(names) { _, which ->
                    lifecycleScope.launch {
                        ServiceLocator.repo.unblockUser(me, blocked[which].id)
                        Utils.toast(this@SettingsActivity, "${blocked[which].name} unblocked")
                    }
                }
                .setNegativeButton("Close", null)
                .show()
        }
    }

    private fun seedDemo() {
        val me = ServiceLocator.prefs.userId ?: return
        lifecycleScope.launch {
            ServiceLocator.repo.seedDemoContent(me)
                .onSuccess { Utils.toast(this@SettingsActivity, "Demo people and posts added") }
                .onFailure { Utils.toast(this@SettingsActivity, it.message ?: "Not available") }
        }
    }

    private fun deleteAccount() {
        val me = ServiceLocator.prefs.userId ?: return
        val input = EditText(this).apply {
            hint = "Password"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val container = FrameLayout(this).apply {
            setPadding(48, 24, 48, 0)
            addView(input)
        }
        AlertDialog.Builder(this)
            .setTitle("Delete account?")
            .setMessage("This permanently removes your profile, posts, messages and friends.")
            .setView(container)
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    ServiceLocator.repo.deleteAccount(me, input.text.toString())
                        .onSuccess { goToAuth() }
                        .onFailure { Utils.toast(this@SettingsActivity, it.message ?: "Failed") }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun logout() {
        AlertDialog.Builder(this)
            .setTitle("Log out?")
            .setPositiveButton("Log out") { _, _ ->
                ServiceLocator.repo.logout()
                goToAuth()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun goToAuth() {
        startActivity(
            Intent(this, AuthActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        finish()
    }
}

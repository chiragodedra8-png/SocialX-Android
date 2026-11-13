package com.socialx.app.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.socialx.app.R
import com.socialx.app.core.ServiceLocator
import com.socialx.app.data.User
import com.socialx.app.databinding.ActivityMainBinding
import com.socialx.app.ui.search.SearchActivity

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    var me: User? = null
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)
        setSupportActionBar(b.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)

        b.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> show(HomeFragment(), getString(R.string.app_name))
                R.id.nav_friends -> show(FriendsFragment(), getString(R.string.friends))
                R.id.nav_chats -> show(ChatsFragment(), getString(R.string.chats))
                R.id.nav_alerts -> show(NotificationsFragment(), getString(R.string.alerts))
                R.id.nav_profile -> show(ProfileFragment(), getString(R.string.profile))
            }
            true
        }
        if (savedInstanceState == null) {
            b.bottomNav.selectedItemId = R.id.nav_home
        }
        loadMe()
    }

    fun loadMe() {
        lifecycleScope.launch {
            me = ServiceLocator.repo.currentUser()
        }
    }

    private fun show(fragment: Fragment, title: String) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.mainContainer, fragment)
            .commit()
        supportActionBar?.title = title
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_search -> {
                startActivity(Intent(this, SearchActivity::class.java))
                true
            }
            R.id.action_chat -> {
                b.bottomNav.selectedItemId = R.id.nav_chats
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}

package com.socialx.app.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.socialx.app.core.ServiceLocator
import com.socialx.app.databinding.ActivitySplashBinding
import com.socialx.app.ui.main.MainActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    private lateinit var b: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(b.root)

        lifecycleScope.launch {
            delay(900)
            val user = ServiceLocator.repo.currentUser()
            val target = if (user == null) AuthActivity::class.java else MainActivity::class.java
            startActivity(Intent(this@SplashActivity, target))
            finish()
        }
    }
}

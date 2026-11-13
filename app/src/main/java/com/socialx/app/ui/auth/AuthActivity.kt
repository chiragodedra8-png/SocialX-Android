package com.socialx.app.ui.auth

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import com.socialx.app.R
import com.socialx.app.databinding.ActivityAuthBinding

class AuthActivity : AppCompatActivity() {

    private lateinit var b: ActivityAuthBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(b.root)
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(R.id.authContainer, LoginFragment())
            }
        }
    }

    fun showSignUp() = supportFragmentManager.commit {
        setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
        replace(R.id.authContainer, SignUpFragment())
    }

    fun showLogin() = supportFragmentManager.commit {
        setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
        replace(R.id.authContainer, LoginFragment())
    }

    fun showForgot() = supportFragmentManager.commit {
        setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
        replace(R.id.authContainer, ForgotFragment())
    }
}

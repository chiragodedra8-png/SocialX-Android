package com.socialx.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.socialx.app.core.ServiceLocator
import com.socialx.app.core.Utils
import com.socialx.app.databinding.FragmentLoginBinding
import com.socialx.app.ui.main.MainActivity
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private var _b: FragmentLoginBinding? = null
    private val b get() = _b!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?): View {
        _b = FragmentLoginBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        b.btnLogin.setOnClickListener { doLogin() }
        b.tvForgot.setOnClickListener { (activity as? AuthActivity)?.showForgot() }
        b.btnGoSignup.setOnClickListener { (activity as? AuthActivity)?.showSignUp() }
    }

    private fun doLogin() {
        val identifier = b.etIdentifier.text?.toString()?.trim().orEmpty()
        val password = b.etPassword.text?.toString().orEmpty()
        if (identifier.isEmpty()) {
            Utils.toast(requireContext(), "Enter your email, phone or username")
            return
        }
        if (password.isEmpty()) {
            Utils.toast(requireContext(), "Enter your password")
            return
        }
        b.loginProgress.visibility = View.VISIBLE
        b.btnLogin.isEnabled = false
        lifecycleScope.launch {
            val result = ServiceLocator.repo.login(identifier, password)
            b.loginProgress.visibility = View.GONE
            b.btnLogin.isEnabled = true
            result.onSuccess {
                startActivity(Intent(requireContext(), MainActivity::class.java))
                requireActivity().finish()
            }.onFailure {
                Utils.toast(requireContext(), it.message ?: "Login failed")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}

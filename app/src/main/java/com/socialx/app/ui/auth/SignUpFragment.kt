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
import com.socialx.app.databinding.FragmentSignupBinding
import com.socialx.app.ui.main.MainActivity
import kotlinx.coroutines.launch

class SignUpFragment : Fragment() {

    private var _b: FragmentSignupBinding? = null
    private val b get() = _b!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?): View {
        _b = FragmentSignupBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        b.btnSignup.setOnClickListener { doSignUp() }
        b.btnGoLogin.setOnClickListener { (activity as? AuthActivity)?.showLogin() }
    }

    private fun doSignUp() {
        val name = b.etName.text?.toString()?.trim().orEmpty()
        val username = b.etUsername.text?.toString()?.trim().orEmpty()
        val email = b.etEmail.text?.toString()?.trim().orEmpty()
        val phone = b.etPhone.text?.toString()?.trim().orEmpty()
        val password = b.etPassword.text?.toString().orEmpty()
        val question = b.etSecurityQuestion.text?.toString()?.trim().orEmpty()
        val answer = b.etSecurityAnswer.text?.toString()?.trim().orEmpty()

        b.signupProgress.visibility = View.VISIBLE
        b.btnSignup.isEnabled = false
        lifecycleScope.launch {
            val result = ServiceLocator.repo.signUp(
                name, username, email, phone, password,
                question.ifBlank { "What is your favourite colour?" }, answer
            )
            b.signupProgress.visibility = View.GONE
            b.btnSignup.isEnabled = true
            result.onSuccess {
                Utils.toast(requireContext(), "Welcome to SocialX, ${it.name.split(" ").first()}!")
                startActivity(Intent(requireContext(), MainActivity::class.java))
                requireActivity().finish()
            }.onFailure {
                Utils.toast(requireContext(), it.message ?: "Could not create account")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}

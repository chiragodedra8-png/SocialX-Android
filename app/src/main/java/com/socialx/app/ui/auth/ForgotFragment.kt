package com.socialx.app.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.socialx.app.core.ServiceLocator
import com.socialx.app.core.Utils
import com.socialx.app.databinding.FragmentForgotBinding
import kotlinx.coroutines.launch

class ForgotFragment : Fragment() {

    private var _b: FragmentForgotBinding? = null
    private val b get() = _b!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?): View {
        _b = FragmentForgotBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        b.btnBack.setOnClickListener { (activity as? AuthActivity)?.showLogin() }
        b.btnReset.setOnClickListener { reset() }
    }

    private fun reset() {
        val identifier = b.etIdentifier.text?.toString()?.trim().orEmpty()
        val answer = b.etAnswer.text?.toString()?.trim().orEmpty()
        val password = b.etNewPassword.text?.toString().orEmpty()
        if (identifier.isEmpty()) {
            Utils.toast(requireContext(), "Enter your account details")
            return
        }
        val question = ServiceLocator.repo.securityQuestionFor(identifier)
        if (question != null && b.tvQuestion.text?.toString()?.startsWith("Security question") != true) {
            b.tvQuestion.text = "Security question: $question"
            if (answer.isEmpty()) return
        }
        if (answer.isEmpty() || password.isEmpty()) {
            Utils.toast(requireContext(), "Fill in the answer and a new password")
            return
        }
        lifecycleScope.launch {
            ServiceLocator.repo.resetPassword(identifier, answer, password)
                .onSuccess {
                    Utils.toast(requireContext(), "Password updated. Please log in.")
                    (activity as? AuthActivity)?.showLogin()
                }
                .onFailure { Utils.toast(requireContext(), it.message ?: "Reset failed") }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}

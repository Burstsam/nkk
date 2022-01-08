package org.mosad.teapod.ui.activity.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import org.mosad.teapod.R
import org.mosad.teapod.databinding.FragmentOnLoginBinding
import org.mosad.teapod.parser.crunchyroll.Crunchyroll
import org.mosad.teapod.preferences.EncryptedPreferences

class OnLoginFragment: Fragment() {

    private lateinit var binding: FragmentOnLoginBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentOnLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initActions()
    }

    private fun initActions() {
        binding.buttonLogin.setOnClickListener {
            onLogin()
        }

        binding.editTextPassword.setOnEditorActionListener { _, actionId, _ ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    onLogin()
                    false // false will hide the keyboards
                }
                else -> false
            }
        }

    }

    private fun onLogin() {
        // get login credentials from gui
        val email = binding.editTextLogin.text.toString()
        val password = binding.editTextPassword.text.toString()

        binding.buttonLogin.isClickable = false
        // FIXME, this seems to run blocking
        lifecycleScope.launch {
            // try login credentials
            val login = Crunchyroll.login(email, password)

            if (login) {
                // save the credentials and show the main activity
                EncryptedPreferences.saveCredentials(email, password, requireContext())
                if (activity is OnboardingActivity) (activity as OnboardingActivity).launchMainActivity()
            } else {
                withContext(Dispatchers.Main) {
                    binding.textLoginDesc.text = getString(R.string.on_login_failed)
                    binding.buttonLogin.isClickable = true
                }
            }
        }
    }

}
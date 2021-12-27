package org.mosad.teapod.ui.activity.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import org.mosad.teapod.databinding.FragmentOnLoginBinding
import org.mosad.teapod.preferences.EncryptedPreferences

class OnLoginFragment: Fragment() {

    private lateinit var binding: FragmentOnLoginBinding
    private var loginJob: Job? = null

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
            // get login credentials from gui
            val email = binding.editTextLogin.text.toString()
            val password = binding.editTextPassword.text.toString()

            EncryptedPreferences.saveCredentials(email, password, requireContext()) // save the credentials

            binding.buttonLogin.isClickable = false
            loginJob = lifecycleScope.launch {
                // TODO
//                if (AoDParser.login()) {
//                    // if login was successful, switch to main
//                    if (activity is OnboardingActivity) {
//                            (activity as OnboardingActivity).launchMainActivity()
//                    }
//                } else {
//                    withContext(Dispatchers.Main) {
//                        binding.textLoginDesc.text = getString(R.string.on_login_failed)
//                        binding.buttonLogin.isClickable = true
//                    }
//                }
            }
        }
    }

}
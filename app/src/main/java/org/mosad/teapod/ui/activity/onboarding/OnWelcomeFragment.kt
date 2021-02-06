package org.mosad.teapod.ui.activity.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.mosad.teapod.databinding.FragmentOnWelcomeBinding

class OnWelcomeFragment: Fragment() {

    private lateinit var binding: FragmentOnWelcomeBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentOnWelcomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initActions()
    }

    private fun initActions() {
        binding.buttonGetStarted.setOnClickListener {
            if (activity is OnboardingActivity) {
                (activity as OnboardingActivity).nextFragment()
            }
        }
    }
}
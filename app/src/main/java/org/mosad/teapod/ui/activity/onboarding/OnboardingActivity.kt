package org.mosad.teapod.ui.activity.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import org.mosad.teapod.ui.activity.main.MainActivity
import org.mosad.teapod.databinding.ActivityOnboardingBinding

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var pagerAdapter: FragmentStateAdapter

    private val fragments = arrayOf(OnLoginFragment())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        pagerAdapter = ScreenSlidePagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { _, _ -> }.attach()

        // we don't use the skip button, instead we use the start button to skip the last fragment
        binding.buttonSkip.visibility = View.GONE

        // hide tab layout if only one tab is displayed
        if (fragments.size <= 1) {
            binding.tabLayout.visibility = View.GONE
        }
    }

    override fun onBackPressed() {
        if (binding.viewPager.currentItem == 0) {
            super.onBackPressed()
        } else {
            binding.viewPager.currentItem = binding.viewPager.currentItem - 1
        }
    }

    fun nextFragment() {
        if (binding.viewPager.currentItem < fragments.size - 1) {
            binding.viewPager.currentItem++
        } else {
            launchMainActivity()
        }
    }

    fun btnNextClick(@Suppress("UNUSED_PARAMETER")v: View) {
        //nextFragment() // currently not used in Teapod
    }

    fun btnSkipClick(@Suppress("UNUSED_PARAMETER")v: View) {
        //launchMainActivity() // currently not used in Teapod
    }

    fun launchMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    /**
     * A simple pager adapter
     */
    private inner class ScreenSlidePagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = fragments.size

        override fun createFragment(position: Int): Fragment = fragments[position]
    }


}
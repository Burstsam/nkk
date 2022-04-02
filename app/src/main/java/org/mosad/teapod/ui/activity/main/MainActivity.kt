/**
 * Teapod
 *
 * Copyright 2020-2022  <seil0@mosad.xyz>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 *
 */

package org.mosad.teapod.ui.activity.main

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.google.android.material.navigation.NavigationBarView
import kotlinx.coroutines.*
import org.mosad.teapod.R
import org.mosad.teapod.databinding.ActivityMainBinding
import org.mosad.teapod.parser.crunchyroll.Crunchyroll
import org.mosad.teapod.preferences.EncryptedPreferences
import org.mosad.teapod.preferences.Preferences
import org.mosad.teapod.ui.activity.main.fragments.AccountFragment
import org.mosad.teapod.ui.activity.main.fragments.HomeFragment
import org.mosad.teapod.ui.activity.main.fragments.LibraryFragment
import org.mosad.teapod.ui.activity.main.fragments.SearchFragment
import org.mosad.teapod.ui.activity.onboarding.OnboardingActivity
import org.mosad.teapod.ui.activity.player.PlayerActivity
import org.mosad.teapod.ui.components.LoginModalBottomSheet
import org.mosad.teapod.util.DataTypes
import org.mosad.teapod.util.metadb.MetaDBController
import java.util.*
import kotlin.system.measureTimeMillis

class MainActivity : AppCompatActivity(), NavigationBarView.OnItemSelectedListener {
    private val classTag = javaClass.name

    private lateinit var binding: ActivityMainBinding
    private var activeBaseFragment: Fragment = HomeFragment() // the currently active fragment, home at the start

    companion object {
        lateinit var instance: MainActivity
    }

    init {
        instance = this
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Handle the splash screen transition.
        installSplashScreen()

        super.onCreate(savedInstanceState)

        load() // start the initial loading
        theme.applyStyle(getThemeResource(), true)

        binding = ActivityMainBinding.inflate(layoutInflater)
        binding.navView.setOnItemSelectedListener(this)
        setContentView(binding.root)

        supportFragmentManager.commit {
            replace(R.id.nav_host_fragment, activeBaseFragment, activeBaseFragment.javaClass.simpleName)
        }
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        } else {
            if (activeBaseFragment !is HomeFragment) {
                binding.navView.selectedItemId = R.id.navigation_home
            } else {
                super.onBackPressed()
            }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        }

        val ret = when (item.itemId) {
            R.id.navigation_home -> {
                activeBaseFragment = HomeFragment()
                true
            }
            R.id.navigation_library -> {
                activeBaseFragment = LibraryFragment()
                true
            }
            R.id.navigation_search -> {
                activeBaseFragment = SearchFragment()
                true
            }
            R.id.navigation_account -> {
                activeBaseFragment = AccountFragment()
                true
            }
            else -> false
        }

        supportFragmentManager.commit {
            replace(R.id.nav_host_fragment, activeBaseFragment, activeBaseFragment.javaClass.simpleName)
        }

        return ret
    }

    private fun getThemeResource(): Int {
        return when (Preferences.theme) {
            DataTypes.Theme.LIGHT -> R.style.AppTheme_Light
            else -> R.style.AppTheme_Dark
        }
    }

    /**
     * initial loading and login are run in parallel, as initial loading doesn't require
     * any login cookies
     */
    private fun load() {
        val time = measureTimeMillis {
            // load all saved stuff here
            Preferences.load(this)
            EncryptedPreferences.readCredentials(this)

            // load meta db at the start, it doesn't depend on any third party
            val metaJob = initMetaDB()

            // always initialize the api token
            Crunchyroll.initBasicApiToken()

            // show onboarding if no password is set, or login fails
            if (EncryptedPreferences.password.isEmpty() || !Crunchyroll.login(
                    EncryptedPreferences.login,
                    EncryptedPreferences.password
                )
            ) {
                showOnboarding()
            } else {
                runBlocking {
                    initCrunchyroll().joinAll()
                    metaJob.join() // meta loading should be done here
                }
            }
        }
        Log.i(classTag, "loading in $time ms")
    }

    private fun initCrunchyroll(): List<Job> {
        val scope = CoroutineScope(Dispatchers.IO + CoroutineName("InitialCrunchyLoading"))
        return listOf(
            scope.launch { Crunchyroll.index() },
            scope.launch { Crunchyroll.account() },
            scope.launch {
                // update the local preferred content language, since it may have changed
                val locale = Locale.forLanguageTag(Crunchyroll.profile().preferredContentSubtitleLanguage)
                Preferences.savePreferredLocal(this@MainActivity, locale)

            }
        )
    }

    private fun initMetaDB(): Job {
        val scope = CoroutineScope(Dispatchers.IO + CoroutineName("InitialMetaDBLoading"))
        return scope.launch { MetaDBController.list() }
    }

    private fun showLoginDialog() {
        val loginModal = LoginModalBottomSheet().apply {
            positiveAction = {
                EncryptedPreferences.saveCredentials(login, password, requireContext())

                // TODO only dismiss if login was successful
                this.dismiss()
            }
            negativeAction = {
                Log.i(classTag, "Login canceled, exiting.")
                finish()
            }
        }
        loginModal.show(this.supportFragmentManager, LoginModalBottomSheet.TAG)
    }

    /**
     * start the onboarding activity and finish the main activity
     */
    private fun showOnboarding() {
        startActivity(Intent(this, OnboardingActivity::class.java))
        finish()
    }

    /**
     * start the player as new activity
     */
    fun startPlayer(seasonId: String, episodeId: String) {
        val intent = Intent(this, PlayerActivity::class.java).apply {
            putExtra(getString(R.string.intent_season_id), seasonId)
            putExtra(getString(R.string.intent_episode_id), episodeId)
        }
        startActivity(intent)
    }

    /**
     * use custom restart instead of recreate(), since it has animations
     */
    fun restart() {
        val restartIntent = intent
        restartIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        finish()
        startActivity(restartIntent)
    }

}
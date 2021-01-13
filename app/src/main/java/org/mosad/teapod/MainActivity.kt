/**
 * Teapod
 *
 * Copyright 2020-2021  <seil0@mosad.xyz>
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

package org.mosad.teapod

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.callbacks.onDismiss
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import org.mosad.teapod.databinding.ActivityMainBinding
import org.mosad.teapod.parser.AoDParser
import org.mosad.teapod.player.PlayerActivity
import org.mosad.teapod.preferences.EncryptedPreferences
import org.mosad.teapod.preferences.Preferences
import org.mosad.teapod.ui.components.LoginDialog
import org.mosad.teapod.ui.fragments.AccountFragment
import org.mosad.teapod.ui.fragments.HomeFragment
import org.mosad.teapod.ui.fragments.LibraryFragment
import org.mosad.teapod.ui.fragments.SearchFragment
import org.mosad.teapod.util.DataTypes
import org.mosad.teapod.util.StorageController
import java.net.SocketTimeoutException
import kotlin.system.exitProcess
import kotlin.system.measureTimeMillis

class MainActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding
    private var activeBaseFragment: Fragment = HomeFragment() // the currently active fragment, home at the start

    companion object {
        var wasInitialized = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!wasInitialized) { load() }
        theme.applyStyle(getThemeResource(), true)

        binding = ActivityMainBinding.inflate(layoutInflater)
        binding.navView.setOnNavigationItemSelectedListener(this)
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
            DataTypes.Theme.DARK -> R.style.AppTheme_Dark
            else -> R.style.AppTheme_Light
        }
    }

    /**
     * initial loading and login are run in parallel, as initial loading doesn't require
     * any login cookies
     */
    private fun load() {
        val time = measureTimeMillis {
            val loadingJob = AoDParser.initialLoading() // start the initial loading

            // load all saved stuff here
            Preferences.load(this)
            EncryptedPreferences.readCredentials(this)
            StorageController.load(this)

            try {
                // make sure credentials are set, run's async
                if (EncryptedPreferences.password.isEmpty()) {
                    showLoginDialog(true)
                } else {
                    // try to login in, as most sites can only bee loaded once loged in
                    if (!AoDParser.login()) showLoginDialog(false)
                }
            } catch (ex: SocketTimeoutException) {
                Log.w(javaClass.name, "Timeout during login!")

                // show waring dialog before finishing
                MaterialDialog(this).show {
                    title(R.string.dialog_timeout_head)
                    message(R.string.dialog_timeout_desc)
                    onDismiss { exitAndRemoveTask() }
                }
            }

            runBlocking { loadingJob.joinAll() } // wait for initial loading to finish
        }
        Log.i(javaClass.name, "loading and login in $time ms")

        wasInitialized = true
    }

    private fun showLoginDialog(firstTry: Boolean) {
        LoginDialog(this, firstTry).positiveButton {
            EncryptedPreferences.saveCredentials(login, password, context)

            if (!AoDParser.login()) {
                showLoginDialog(false)
                Log.w(javaClass.name, "Login failed, please try again.")
            }
        }.negativeButton {
            Log.i(javaClass.name, "Login canceled, exiting.")
            finish()
        }.show()
    }

    /**
     * Show a fragment on top of the current fragment.
     * The current fragment is replaced and the new one is added
     * to the back stack.
     */
    fun showFragment(fragment: Fragment) {
        supportFragmentManager.commit {
            replace(R.id.nav_host_fragment, fragment, fragment.javaClass.simpleName)
            addToBackStack(fragment.javaClass.name)
            show(fragment)
        }
    }

    /**
     * start the player as new activity
     */
    fun startPlayer(mediaId: Int, episodeId: Int) {
        val intent = Intent(this, PlayerActivity::class.java).apply {
            putExtra(getString(R.string.intent_media_id), mediaId)
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

    /**
     * exit and remove the app from tasks
     */
    fun exitAndRemoveTask() {
        this.finishAndRemoveTask()
        exitProcess(0)
    }

}
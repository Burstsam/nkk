/**
 * Teapod
 *
 * Copyright 2020  <seil0@mosad.xyz>
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
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.mosad.teapod.parser.AoDParser
import org.mosad.teapod.preferences.EncryptedPreferences
import org.mosad.teapod.ui.fragments.MediaFragment
import org.mosad.teapod.ui.fragments.AccountFragment
import org.mosad.teapod.ui.components.LoginDialog
import org.mosad.teapod.ui.fragments.HomeFragment
import org.mosad.teapod.ui.fragments.LibraryFragment
import org.mosad.teapod.ui.fragments.SearchFragment
import org.mosad.teapod.ui.fragments.LoadingFragment
import org.mosad.teapod.util.StorageController
import org.mosad.teapod.util.TMDBApiController
import kotlin.system.measureTimeMillis

class MainActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {

    private var activeBaseFragment: Fragment = HomeFragment() // the currently active fragment, home at the start

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        nav_view.setOnNavigationItemSelectedListener(this)

        load()

        supportFragmentManager.commit {
            replace(R.id.nav_host_fragment, activeBaseFragment, activeBaseFragment.javaClass.simpleName)
        }
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        } else {
            if (activeBaseFragment !is HomeFragment) {
                nav_view.selectedItemId = R.id.navigation_home
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

    private fun load() {
        // running login and list in parallel does not bring any speed improvements
        val time = measureTimeMillis {
        // make sure credentials are set
            EncryptedPreferences.readCredentials(this)
            if (EncryptedPreferences.password.isEmpty()) {
                showLoginDialog(true)
            } else {
                // try to login in, as most sites can only bee loaded once loged in
                if (!AoDParser().login()) showLoginDialog(false)
            }

            StorageController.load(this)

            // move to AoDParser
            val newEPJob = GlobalScope.async {
                AoDParser().listNewEpisodes()
            }

            val listJob = GlobalScope.async {
                AoDParser().listAnimes() // initially load all media
            }

            runBlocking {
                newEPJob.await()
                listJob.await()
            }


            // TODO load home screen, can be parallel to listAnimes
        }
        Log.i(javaClass.name, "login and list in $time ms")
    }

    /**
     * Show the media fragment for the selected media.
     * While loading show the loading fragment.
     * The loading and media fragment are not stored in activeBaseFragment,
     * as the don't replace a fragment but are added on top of one.
     */
    fun showMediaFragment(mediaId: Int) = GlobalScope.launch {
        val loadingFragment = LoadingFragment()
        supportFragmentManager.commit {
            add(R.id.nav_host_fragment, loadingFragment, "MediaFragment")
            show(loadingFragment)
        }

        // load the streams for the selected media
        val media = AoDParser().getMediaById(mediaId)
        val tmdb = TMDBApiController().search(media.info.title, media.type)

        val mediaFragment = MediaFragment(media, tmdb)
        supportFragmentManager.commit {
            add(R.id.nav_host_fragment, mediaFragment, "MediaFragment")
            addToBackStack(null)
            show(mediaFragment)
        }

        supportFragmentManager.commit {
            remove(loadingFragment)
        }
    }

    fun startPlayer(streamUrl: String) {
        val intent = Intent(this, PlayerActivity::class.java).apply {
            putExtra(getString(R.string.intent_stream_url), streamUrl)
        }
        startActivity(intent)
    }

    private fun showLoginDialog(firstTry: Boolean) {
        LoginDialog(this, firstTry).positiveButton {
            EncryptedPreferences.saveCredentials(login, password, context)

            if (!AoDParser().login()) {
                showLoginDialog(false)
                Log.w(javaClass.name, "Login failed, please try again.")
            }
        }.negativeButton {
            Log.i(javaClass.name, "Login canceled, exiting.")
            finish()
        }.show()
    }
}
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
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.mosad.teapod.parser.AoDParser
import org.mosad.teapod.preferences.EncryptedPreferences
import org.mosad.teapod.ui.MediaFragment
import org.mosad.teapod.ui.account.AccountFragment
import org.mosad.teapod.ui.components.LoginDialog
import org.mosad.teapod.ui.home.HomeFragment
import org.mosad.teapod.ui.library.LibraryFragment
import org.mosad.teapod.ui.search.SearchFragment
import org.mosad.teapod.util.*

class MainActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {

    private var activeFragment: Fragment = HomeFragment() // the currently active fragment, home at the start

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        navView.setOnNavigationItemSelectedListener(this)

        load()
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        } else {
            if (activeFragment !is HomeFragment) {
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
                activeFragment = HomeFragment()
                true
            }
            R.id.navigation_library -> {
                activeFragment = LibraryFragment()
                true
            }
            R.id.navigation_search -> {
                activeFragment = SearchFragment()
                true
            }
            R.id.navigation_account -> {
                activeFragment = AccountFragment()
                true
            }
            else -> false
        }

        supportFragmentManager.commit {
            replace(R.id.nav_host_fragment, activeFragment)
        }

        return ret
    }

    private fun load() {
        EncryptedPreferences.readCredentials(this)

        // make sure credentials are set and valid
        if (EncryptedPreferences.password.isEmpty()) {
            showLoginDialog(true)
        } else if (!AoDParser().login()) {
            showLoginDialog(false)
        }
    }

    /**
     * show the media fragment for the selected media
     * while loading show the loading fragment
     */
    fun showMediaFragment(media: Media) = GlobalScope.launch {
        val loadingFragment = LoadingFragment()
        supportFragmentManager.commit {
            add(R.id.nav_host_fragment, loadingFragment, "MediaFragment")
            show(loadingFragment)
        }

        // load the streams for the selected media
        media.episodes = AoDParser().loadStreams(media)
        val tmdb = TMDBApiController().search(media.title, media.type)

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
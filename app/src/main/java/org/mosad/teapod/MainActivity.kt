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
import org.mosad.teapod.parser.AoDParser
import org.mosad.teapod.preferences.EncryptedPreferences
import org.mosad.teapod.ui.MediaFragment
import org.mosad.teapod.ui.account.AccountFragment
import org.mosad.teapod.ui.components.LoginDialog
import org.mosad.teapod.ui.home.HomeFragment
import org.mosad.teapod.ui.library.LibraryFragment
import org.mosad.teapod.ui.search.SearchFragment
import org.mosad.teapod.util.Media
import org.mosad.teapod.util.TMDBApiController

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

        if (EncryptedPreferences.password.isEmpty()) {
            Log.i(javaClass.name, "please login!")

            LoginDialog(this).positiveButton {
                EncryptedPreferences.saveCredentials(login, password, context)
            }.negativeButton {
                Log.i(javaClass.name, "Login canceled, exiting.")
                finish()
            }.show()
        }
    }

    fun showDetailFragment(media: Media) {
        media.episodes = AoDParser().loadStreams(media) // load the streams for the selected media

        val tmdb = TMDBApiController().search(media.title, media.type)

        val mediaFragment = MediaFragment(media, tmdb)
        supportFragmentManager.commit {
            add(R.id.nav_host_fragment, mediaFragment, "MediaFragment")
            addToBackStack(null)
            show(mediaFragment)
        }
    }

    fun startPlayer(streamUrl: String) {
        val intent = Intent(this, PlayerActivity::class.java).apply {
            putExtra(getString(R.string.intent_stream_url), streamUrl)
        }
        startActivity(intent)
    }
}
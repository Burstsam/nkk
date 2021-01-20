package org.mosad.teapod.activity.main.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import org.mosad.teapod.BuildConfig
import org.mosad.teapod.activity.main.MainActivity
import org.mosad.teapod.R
import org.mosad.teapod.databinding.FragmentAccountBinding
import org.mosad.teapod.parser.AoDParser
import org.mosad.teapod.preferences.EncryptedPreferences
import org.mosad.teapod.preferences.Preferences
import org.mosad.teapod.ui.components.LoginDialog
import org.mosad.teapod.util.DataTypes.Theme
import org.mosad.teapod.util.showFragment

class AccountFragment : Fragment() {

    private lateinit var binding: FragmentAccountBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.textAccountLogin.text = EncryptedPreferences.login
        binding.textInfoAboutDesc.text = getString(R.string.info_about_desc, BuildConfig.VERSION_NAME, getString(R.string.build_time))
        binding.textThemeSelected.text = when (Preferences.theme) {
            Theme.DARK -> getString(R.string.theme_dark)
            else -> getString(R.string.theme_light)
        }

        binding.switchSecondary.isChecked = Preferences.preferSecondary
        binding.switchAutoplay.isChecked = Preferences.autoplay

        initActions()
    }

    private fun initActions() {
        binding.linearAccountLogin.setOnClickListener {
            showLoginDialog(true)
        }

        binding.linearTheme.setOnClickListener {
            showThemeDialog()
        }

        binding.linearInfo.setOnClickListener {
            activity?.showFragment(AboutFragment())
        }

        binding.switchSecondary.setOnClickListener {
            Preferences.savePreferSecondary(requireContext(), binding.switchSecondary.isChecked)
        }

        binding.switchAutoplay.setOnClickListener {
            Preferences.saveAutoplay(requireContext(), binding.switchAutoplay.isChecked)
        }
    }

    private fun showLoginDialog(firstTry: Boolean) {
        LoginDialog(requireContext(), firstTry).positiveButton {
            EncryptedPreferences.saveCredentials(login, password, context)

            if (!AoDParser.login()) {
                showLoginDialog(false)
                Log.w(javaClass.name, "Login failed, please try again.")
            }
        }.show {
            login = EncryptedPreferences.login
            password = ""
        }
    }

    private fun showThemeDialog() {
        val themes = listOf(
            resources.getString(R.string.theme_light),
            resources.getString(R.string.theme_dark)
        )

        MaterialDialog(requireContext()).show {
            title(R.string.theme)
            listItemsSingleChoice(items = themes, initialSelection = Preferences.theme.ordinal) { _, index, _ ->
                when(index) {
                    0 -> Preferences.saveTheme(context, Theme.LIGHT)
                    1 -> Preferences.saveTheme(context, Theme.DARK)
                    else -> Preferences.saveTheme(context, Theme.LIGHT)
                }

                (activity as MainActivity).restart()
            }
        }
    }
}
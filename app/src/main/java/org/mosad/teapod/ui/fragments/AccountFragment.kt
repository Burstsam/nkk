package org.mosad.teapod.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.afollestad.materialdialogs.MaterialDialog
import de.psdev.licensesdialog.LicensesDialog
import kotlinx.android.synthetic.main.fragment_account.*
import org.mosad.teapod.BuildConfig
import org.mosad.teapod.R
import org.mosad.teapod.parser.AoDParser
import org.mosad.teapod.preferences.EncryptedPreferences
import org.mosad.teapod.preferences.Preferences
import org.mosad.teapod.ui.components.LoginDialog

class AccountFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_account, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        text_account_login.text = EncryptedPreferences.login
        text_info_about_desc.text = getString(R.string.info_about_desc, BuildConfig.VERSION_NAME, getString(R.string.build_time))
        switch_secondary.isChecked = Preferences.preferSecondary
        switch_autoplay.isChecked = Preferences.autoplay

        initActions()
    }

    private fun initActions() {
        linear_account_login.setOnClickListener {
            showLoginDialog(true)
        }

        linear_about.setOnClickListener {
            MaterialDialog(requireContext())
                .title(R.string.info_about)
                .message(R.string.info_about_dialog)
                .show()
        }

        text_licenses.setOnClickListener {
            val selectedTheme = requireContext().applicationInfo.theme

            val dialogCss = when (selectedTheme) {
                R.style.AppTheme_Dark -> R.string.license_dialog_style_dark
                else -> R.string.license_dialog_style_light
            }

            val themeId = when (selectedTheme) {
                R.style.AppTheme_Dark -> R.style.LicensesDialogTheme_Dark
                else -> R.style.AppTheme_Light
            }

            LicensesDialog.Builder(requireContext())
                .setNotices(R.raw.notices)
                .setTitle(R.string.licenses)
                .setIncludeOwnLicense(true)
                .setThemeResourceId(themeId)
                .setNoticesCssStyle(dialogCss)
                .build()
                .show()
        }

        switch_secondary.setOnClickListener {
            Preferences.savePreferSecondary(requireContext(), switch_secondary.isChecked)
        }

        switch_autoplay.setOnClickListener {
            Preferences.saveAutoplay(requireContext(), switch_autoplay.isChecked)
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
}
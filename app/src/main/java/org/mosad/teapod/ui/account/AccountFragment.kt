package org.mosad.teapod.ui.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.afollestad.materialdialogs.MaterialDialog
import kotlinx.android.synthetic.main.fragment_account.*
import org.mosad.teapod.BuildConfig
import org.mosad.teapod.R
import org.mosad.teapod.preferences.EncryptedPreferences
import org.mosad.teapod.ui.components.LoginDialog

class AccountFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_account, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        text_account_login.text = EncryptedPreferences.login
        text_info_about_desc.text = getString(R.string.info_about_desc, BuildConfig.VERSION_NAME, getString(R.string.build_time))

        initActions()
    }

    private fun initActions() {
        linear_account_login.setOnClickListener {
            LoginDialog(requireContext()).positiveButton {
                EncryptedPreferences.saveCredentials(login, password, context)
            }.show {
                login = EncryptedPreferences.login
                password = ""
            }
        }

        linear_about.setOnClickListener {
            MaterialDialog(requireContext())
                .title(R.string.info_about)
                .message(R.string.info_about_dialog)
                .show()
        }
    }
}
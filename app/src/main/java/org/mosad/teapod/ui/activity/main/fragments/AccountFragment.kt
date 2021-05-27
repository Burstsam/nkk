package org.mosad.teapod.ui.activity.main.fragments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.mosad.teapod.BuildConfig
import org.mosad.teapod.R
import org.mosad.teapod.databinding.FragmentAccountBinding
import org.mosad.teapod.parser.AoDParser
import org.mosad.teapod.preferences.EncryptedPreferences
import org.mosad.teapod.preferences.Preferences
import org.mosad.teapod.ui.activity.main.MainActivity
import org.mosad.teapod.ui.components.LoginDialog
import org.mosad.teapod.util.DataTypes.Theme
import org.mosad.teapod.util.StorageController
import org.mosad.teapod.util.showFragment

class AccountFragment : Fragment() {

    private lateinit var binding: FragmentAccountBinding

    private val getUriExport = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.also { uri ->
                StorageController.exportMyList(requireContext(), uri)
            }
        }
    }

    private val getUriImport = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.also { uri ->
                val success = StorageController.importMyList(requireContext(), uri)
                if (success == 0) {
                    Toast.makeText(
                        context, getString(R.string.import_data_success),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // load subscription (async) info before anything else
        binding.textAccountSubscription.text = getString(R.string.account_subscription, getString(R.string.loading))
        GlobalScope.launch {
            binding.textAccountSubscription.text = getString(
                R.string.account_subscription,
                AoDParser.getSubscriptionInfoAsync().await()
            )
        }

        binding.textAccountLogin.text = EncryptedPreferences.login
        binding.textInfoAboutDesc.text = getString(R.string.info_about_desc, BuildConfig.VERSION_NAME, getString(R.string.build_time))
        binding.textThemeSelected.text = when (Preferences.theme) {
            Theme.DARK -> getString(R.string.theme_dark)
            else -> getString(R.string.theme_light)
        }

        binding.switchSecondary.isChecked = Preferences.preferSecondary
        binding.switchAutoplay.isChecked = Preferences.autoplay

        binding.linearDevSettings.isVisible = Preferences.devSettings

        initActions()
    }

    private fun initActions() {
        binding.linearAccountLogin.setOnClickListener {
            showLoginDialog(true)
        }

        binding.linearAccountSubscription.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AoDParser.getSubscriptionUrl())))
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

        binding.linearExportData.setOnClickListener {
            val i = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "text/json"
                putExtra(Intent.EXTRA_TITLE, "my-list.json")
            }
            getUriExport.launch(i)
        }

        binding.linearImportData.setOnClickListener {
            val i = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }
            getUriImport.launch(i)
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
                    else -> Preferences.saveTheme(context, Theme.DARK)
                }

                (activity as MainActivity).restart()
            }
        }
    }

}
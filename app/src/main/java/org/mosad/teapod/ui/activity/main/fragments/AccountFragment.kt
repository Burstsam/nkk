package org.mosad.teapod.ui.activity.main.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.mosad.teapod.BuildConfig
import org.mosad.teapod.R
import org.mosad.teapod.databinding.FragmentAccountBinding
import org.mosad.teapod.parser.crunchyroll.Crunchyroll
import org.mosad.teapod.parser.crunchyroll.Profile
import org.mosad.teapod.parser.crunchyroll.supportedLocals
import org.mosad.teapod.preferences.EncryptedPreferences
import org.mosad.teapod.preferences.Preferences
import org.mosad.teapod.ui.activity.main.MainActivity
import org.mosad.teapod.ui.components.LoginDialog
import org.mosad.teapod.util.DataTypes.Theme
import org.mosad.teapod.util.showFragment
import org.mosad.teapod.util.toDisplayString
import java.util.*

class AccountFragment : Fragment() {

    private lateinit var binding: FragmentAccountBinding
    private var profile: Deferred<Profile> = lifecycleScope.async {
        Crunchyroll.profile()
    }

    private val getUriExport = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.also { uri ->
                //StorageController.exportMyList(requireContext(), uri)
            }
        }
    }

    private val getUriImport = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.also { uri ->
//                val success = StorageController.importMyList(requireContext(), uri)
//                if (success == 0) {
//                    Toast.makeText(
//                        context, getString(R.string.import_data_success),
//                        Toast.LENGTH_SHORT
//                    ).show()
//                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.textAccountLogin.text = EncryptedPreferences.login

        // TODO reimplement for cr, if possible (maybe account status would be better? (premium))
        // load subscription (async) info before anything else
        binding.textAccountSubscription.text = getString(R.string.account_subscription, getString(R.string.loading))
        lifecycleScope.launch {
            binding.textAccountSubscription.text = getString(
                R.string.account_subscription,
                "TODO"
            )
        }

        // add preferred subtitles
        lifecycleScope.launch {
            binding.textSettingsContentLanguageDesc.text = Locale.forLanguageTag(
                profile.await().preferredContentSubtitleLanguage
            ).displayLanguage
        }
        binding.switchSecondary.isChecked = Preferences.preferSubbed
        binding.switchAutoplay.isChecked = Preferences.autoplay
        binding.textThemeSelected.text = when (Preferences.theme) {
            Theme.DARK -> getString(R.string.theme_dark)
            else -> getString(R.string.theme_light)
        }

        binding.linearDevSettings.isVisible = Preferences.devSettings
        binding.switchUpdatePlayhead.isChecked = Preferences.updatePlayhead

        binding.textInfoAboutDesc.text = getString(R.string.info_about_desc, BuildConfig.VERSION_NAME, getString(R.string.build_time))

        initActions()
    }

    private fun initActions() {
        binding.linearAccountLogin.setOnClickListener {
            showLoginDialog(true)
        }

        binding.linearAccountSubscription.setOnClickListener {
            // TODO
            //startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AoDParser.getSubscriptionUrl())))
        }


        binding.linearSettingsContentLanguage.setOnClickListener {
            showContentLanguageSelection()
        }

        binding.switchSecondary.setOnClickListener {
            Preferences.savePreferSecondary(requireContext(), binding.switchSecondary.isChecked)
        }

        binding.switchAutoplay.setOnClickListener {
            Preferences.saveAutoplay(requireContext(), binding.switchAutoplay.isChecked)
        }

        binding.linearTheme.setOnClickListener {
            showThemeDialog()
        }

        binding.linearInfo.setOnClickListener {
            activity?.showFragment(AboutFragment())
        }

        binding.switchUpdatePlayhead.setOnClickListener {
            Preferences.saveUpdatePlayhead(requireContext(), binding.switchUpdatePlayhead.isChecked)
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

            // TODO
//            if (!AoDParser.login()) {
//                showLoginDialog(false)
//                Log.w(javaClass.name, "Login failed, please try again.")
//            }
        }.show {
            login = EncryptedPreferences.login
            password = ""
        }
    }

    private fun showContentLanguageSelection() {
        // we should be able to use the index of supportedLocals for language selection, items is GUI only
        val items = supportedLocals.map {
            it.toDisplayString(getString(R.string.settings_content_language_none))
        }.toTypedArray()

        var initialSelection: Int
        // profile should be completed here, therefore blocking
        runBlocking {
            initialSelection = supportedLocals.indexOf(Locale.forLanguageTag(
                profile.await().preferredContentSubtitleLanguage))
            if (initialSelection < 0) initialSelection = supportedLocals.lastIndex
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.settings_content_language)
            .setSingleChoiceItems(items, initialSelection){ dialog, which ->
                updatePrefContentLanguage(supportedLocals[which])
                dialog.dismiss()
            }
            .show()
    }

    @kotlinx.coroutines.ExperimentalCoroutinesApi
    private fun updatePrefContentLanguage(preferredLocale: Locale) {
        lifecycleScope.launch {
            Crunchyroll.postPrefSubLanguage(preferredLocale.toLanguageTag())

        }.invokeOnCompletion {
            // update the local preferred content language
            Preferences.savePreferredLocal(requireContext(), preferredLocale)

            // update profile since the language selection might have changed
            profile = lifecycleScope.async { Crunchyroll.profile() }
            profile.invokeOnCompletion {
                // update language once loading profile is completed
                binding.textSettingsContentLanguageDesc.text = Locale.forLanguageTag(
                    profile.getCompleted().preferredContentSubtitleLanguage
                ).displayLanguage
            }
        }
    }

    private fun showThemeDialog() {
        val items = arrayOf(
            resources.getString(R.string.theme_light),
            resources.getString(R.string.theme_dark)
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.settings_content_language)
            .setSingleChoiceItems(items, Preferences.theme.ordinal){ _, which ->
                when(which) {
                    0 -> Preferences.saveTheme(requireContext(), Theme.LIGHT)
                    1 -> Preferences.saveTheme(requireContext(), Theme.DARK)
                    else -> Preferences.saveTheme(requireContext(), Theme.DARK)
                }

                (activity as MainActivity).restart()
            }
            .show()
    }

}
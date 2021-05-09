package org.mosad.teapod.ui.activity.main.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RawRes
import androidx.fragment.app.Fragment
import com.afollestad.materialdialogs.MaterialDialog
import org.mosad.teapod.BuildConfig
import org.mosad.teapod.R
import org.mosad.teapod.databinding.FragmentAboutBinding
import org.mosad.teapod.databinding.ItemComponentBinding
import org.mosad.teapod.preferences.Preferences
import org.mosad.teapod.util.DataTypes.License
import org.mosad.teapod.util.ThirdPartyComponent
import java.lang.StringBuilder
import java.util.Timer
import kotlin.concurrent.schedule

class AboutFragment : Fragment() {

    private lateinit var binding: FragmentAboutBinding

    private val teapodRepoUrl = "https://git.mosad.xyz/Seil0/teapod"
    private val devClickMax = 5
    private var devClickCount = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentAboutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.textVersionDesc.text = getString(R.string.version_desc, BuildConfig.VERSION_NAME, getString(R.string.build_time))

        getThirdPartyComponents().forEach { thirdParty ->
            val componentBinding = ItemComponentBinding.inflate(layoutInflater) //(R.layout.item_component, container, false)
            componentBinding.textComponentTitle.text = thirdParty.name
            componentBinding.textComponentDesc.text = getString(
                R.string.third_party_component_desc,
                thirdParty.year,
                thirdParty.copyrightOwner,
                thirdParty.license.short
            )
            componentBinding.linearComponent.setOnClickListener {
                showLicense(thirdParty.license)
            }

            binding.linearThirdParty.addView(componentBinding.root)
        }

        initActions()
    }

    private fun initActions() {
        binding.imageAppIcon.setOnClickListener {
            checkDevSettings()
        }

        binding.linearSource.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(teapodRepoUrl)))
        }

        binding.linearLicense.setOnClickListener {
            MaterialDialog(requireContext())
                .title(text = License.GPL3.long)
                .message(text = parseLicense(R.raw.gpl_3_full))
                .show()
        }
    }

    /**
     * check if dev settings shall be enabled
     */
    private fun checkDevSettings() {
        // if the dev settings are already enabled show a toast
        if (Preferences.devSettings) {
            Toast.makeText(context, getString(R.string.dev_settings_already), Toast.LENGTH_SHORT).show()
            return
        }

        // reset dev settings count after 5 seconds
        if (devClickCount == 0) {
            Timer("", false).schedule(5000) {
                devClickCount = 0
            }
        }
        devClickCount++

        if (devClickCount == devClickMax) {
            Preferences.saveDevSettings(requireContext(), true)
            Toast.makeText(context, getString(R.string.dev_settings_enabled), Toast.LENGTH_SHORT).show()
        }
    }

    private fun getThirdPartyComponents(): List<ThirdPartyComponent> {
        return listOf(
            ThirdPartyComponent("AndroidX", "", "The Android Open Source Project",
                "https://developer.android.com/jetpack/androidx", License.APACHE2),
            ThirdPartyComponent("Material Components for Android", "2020", "The Android Open Source Project",
                "https://github.com/material-components/material-components-android", License.APACHE2),
            ThirdPartyComponent("ExoPlayer", "2014 - 2020", "The Android Open Source Project",
                "https://github.com/google/ExoPlayer", License.APACHE2),
            ThirdPartyComponent("Gson", "2008", "Google Inc.",
                "https://github.com/google/gson", License.APACHE2),
            ThirdPartyComponent("Material design icons", "2020", "Google Inc.",
                "https://github.com/google/material-design-icons", License.APACHE2),
            ThirdPartyComponent("Material Dialogs", "", "Aidan Follestad",
                "https://github.com/afollestad/material-dialogs", License.APACHE2),
            ThirdPartyComponent("Jsoup", "2009 - 2020", "Jonathan Hedley",
                "https://jsoup.org/", License.MIT),
            ThirdPartyComponent("kotlinx.coroutines", "2016 - 2019", "JetBrains",
                "https://github.com/Kotlin/kotlinx.coroutines", License.APACHE2),
            ThirdPartyComponent("Glide", "2014", "Google Inc.",
                "https://github.com/bumptech/glide", License.BSD2),
            ThirdPartyComponent("Glide Transformations", "2020", "Wasabeef",
                "https://github.com/wasabeef/glide-transformations", License.APACHE2)
        )
    }

    private fun showLicense(license: License) {
        val licenseText = when(license) {
            License.APACHE2 -> parseLicense(R.raw.al_20_full)
            License.BSD2 -> parseLicense(R.raw.bsd_2_full)
            License.GPL3 -> parseLicense(R.raw.gpl_3_full)
            License.MIT -> parseLicense(R.raw.mit_full)
        }

        MaterialDialog(requireContext())
            .title(text = license.long)
            .message(text = licenseText)
            .show()
    }

    private fun parseLicense(@RawRes id: Int): String {
        val sb = StringBuilder()

        resources.openRawResource(id).bufferedReader().forEachLine {
            if (it.isEmpty()) {
                sb.appendLine(" ")
            } else {
                sb.append(it.trim() + " ")
            }
        }

        return sb.toString()
    }

}
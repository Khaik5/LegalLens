package com.example.lagallens.presentation.feature.profile.information.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import com.example.lagallens.BuildConfig
import com.example.lagallens.R
import com.example.lagallens.databinding.ActivityProfileInformationBinding
import com.example.lagallens.databinding.ItemInformationCardBinding
import com.example.lagallens.presentation.common.utils.applySystemBarInsets

class ProfileInformationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        val binding = ActivityProfileInformationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()
        val privacy = intent.getBooleanExtra(EXTRA_PRIVACY, false)
        binding.informationTitle.setText(if (privacy) R.string.privacy_screen_title else R.string.about_screen_title)
        binding.informationPrivacy.isVisible = privacy
        binding.informationAbout.isVisible = !privacy
        binding.informationBack.setOnClickListener { finish() }
        binding.informationVersion.text = getString(R.string.about_version, BuildConfig.VERSION_NAME)
        bindCard(binding.aboutAssistant, R.string.about_assistant_title, R.string.about_assistant_description)
        bindCard(binding.aboutTeam, R.string.about_team_title, R.string.about_team_placeholder)
        bindCard(binding.aboutContact, R.string.about_contact_title, R.string.about_contact_placeholder)
        bindCard(binding.privacyOcr, R.string.privacy_ocr_title, R.string.privacy_ocr_body)
        bindCard(binding.privacyStorage, R.string.privacy_storage_title, R.string.privacy_storage_body)
        bindCard(binding.privacySecurity, R.string.privacy_security_title, R.string.privacy_security_body)
        bindCard(binding.privacyRights, R.string.privacy_rights_title, R.string.privacy_rights_body)
        bindCard(binding.privacySafety, R.string.privacy_safety_title, R.string.privacy_safety_body)
        binding.privacySafety.root.setBackgroundResource(R.drawable.bg_compare_notice)
        bindCard(binding.privacyPolicy, R.string.privacy_policy_title, R.string.privacy_policy_body)
    }

    private fun bindCard(binding: ItemInformationCardBinding, title: Int, body: Int) {
        binding.informationCardTitle.setText(title)
        binding.informationCardBody.setText(body)
    }

    companion object {
        private const val EXTRA_PRIVACY = "profile_information_privacy"
        fun createIntent(context: Context, privacy: Boolean): Intent = Intent(context, ProfileInformationActivity::class.java).putExtra(EXTRA_PRIVACY, privacy)
    }
}

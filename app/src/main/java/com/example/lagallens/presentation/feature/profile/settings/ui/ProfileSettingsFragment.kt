package com.example.lagallens.presentation.feature.profile.settings.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.lagallens.LegalLensApplication
import com.example.lagallens.R
import com.example.lagallens.databinding.FragmentProfileSettingsBinding
import com.example.lagallens.databinding.ItemProfileSettingToggleBinding
import com.example.lagallens.presentation.feature.profile.settings.contract.ProfileAppearance
import com.example.lagallens.presentation.feature.profile.settings.contract.ProfileLanguage
import com.example.lagallens.presentation.feature.profile.settings.contract.ProfileSettingsUiEffect
import com.example.lagallens.presentation.feature.profile.settings.contract.ProfileSettingsUiEvent
import com.example.lagallens.presentation.feature.profile.settings.contract.ProfileSettingsUiState
import com.example.lagallens.presentation.feature.profile.settings.viewmodel.ProfileSettingsViewModel
import com.example.lagallens.presentation.feature.profile.settings.viewmodel.ProfileSettingsViewModelFactory
import kotlinx.coroutines.launch

class ProfileSettingsFragment : Fragment() {
    private var _binding: FragmentProfileSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<ProfileSettingsViewModel> {
        val application = requireActivity().application as LegalLensApplication
        ProfileSettingsViewModelFactory(
            application.observeProfileSettingsUseCase,
            application.updateProfileAppearanceUseCase,
            application.updateProfileLanguageUseCase
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindView()
        collectUiState()
        collectUiEffect()
    }

    private fun bindView() {
        binding.btnBack.setOnClickListener {
            viewModel.onEvent(ProfileSettingsUiEvent.BackClicked)
        }
        bindToggle(
            binding.togglePush,
            R.string.profile_settings_push_title,
            R.string.profile_settings_push_description,
            ProfileSettingsUiEvent::PushToggled
        )
        bindToggle(
            binding.toggleEmail,
            R.string.profile_settings_email_title,
            R.string.profile_settings_email_description,
            ProfileSettingsUiEvent::EmailToggled
        )
        bindToggle(
            binding.toggleInApp,
            R.string.profile_settings_in_app_title,
            R.string.profile_settings_in_app_description,
            ProfileSettingsUiEvent::InAppToggled
        )
        bindToggle(
            binding.toggleBiometric,
            R.string.profile_settings_biometric_title,
            R.string.profile_settings_biometric_description,
            ProfileSettingsUiEvent::BiometricToggled
        )
        binding.btnSystemAppearance.setOnClickListener {
            viewModel.onEvent(ProfileSettingsUiEvent.AppearanceSelected(ProfileAppearance.SYSTEM))
        }
        binding.btnLightAppearance.setOnClickListener {
            viewModel.onEvent(ProfileSettingsUiEvent.AppearanceSelected(ProfileAppearance.LIGHT))
        }
        binding.btnDarkAppearance.setOnClickListener {
            viewModel.onEvent(ProfileSettingsUiEvent.AppearanceSelected(ProfileAppearance.DARK))
        }
        binding.rowLanguage.setOnClickListener {
            viewModel.onEvent(ProfileSettingsUiEvent.LanguageClicked)
        }
        parentFragmentManager.setFragmentResultListener(
            ProfileLanguageBottomSheet.RESULT_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            viewModel.onEvent(
                ProfileSettingsUiEvent.LanguageSelected(ProfileLanguageBottomSheet.readLanguage(bundle))
            )
        }
    }

    private fun bindToggle(
        toggleBinding: ItemProfileSettingToggleBinding,
        titleRes: Int,
        descriptionRes: Int,
        event: (Boolean) -> ProfileSettingsUiEvent
    ) {
        toggleBinding.tvSettingTitle.setText(titleRes)
        toggleBinding.tvSettingDescription.setText(descriptionRes)
        toggleBinding.swSetting.setOnCheckedChangeListener { _, isChecked ->
            viewModel.onEvent(event(isChecked))
        }
    }

    private fun collectUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun collectUiEffect() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiEffect.collect { effect ->
                    when (effect) {
                        ProfileSettingsUiEffect.NavigateBack -> findNavController().navigateUp()
                        is ProfileSettingsUiEffect.ShowLanguagePicker -> {
                            ProfileLanguageBottomSheet.newInstance(effect.language).show(
                                parentFragmentManager,
                                ProfileLanguageBottomSheet.TAG
                            )
                        }
                    }
                }
            }
        }
    }

    private fun render(state: ProfileSettingsUiState) {
        updateToggle(binding.togglePush.swSetting, state.isPushEnabled)
        updateToggle(binding.toggleEmail.swSetting, state.isEmailEnabled)
        updateToggle(binding.toggleInApp.swSetting, state.isInAppEnabled)
        updateToggle(binding.toggleBiometric.swSetting, state.isBiometricEnabled)
        renderAppearanceOption(binding.btnSystemAppearance, state.appearance == ProfileAppearance.SYSTEM)
        renderAppearanceOption(binding.btnLightAppearance, state.appearance == ProfileAppearance.LIGHT)
        renderAppearanceOption(binding.btnDarkAppearance, state.appearance == ProfileAppearance.DARK)
        binding.tvLanguageValue.setText(state.language.toDisplayName())
    }

    private fun updateToggle(toggle: SwitchCompat, isChecked: Boolean) {
        if (toggle.isChecked != isChecked) {
            toggle.isChecked = isChecked
        }
    }

    private fun renderAppearanceOption(view: android.widget.TextView, isSelected: Boolean) {
        view.setBackgroundResource(
            if (isSelected) R.drawable.bg_profile_settings_theme_selected
            else R.drawable.bg_profile_settings_theme_unselected
        )
        view.setTextColor(
            requireContext().getColor(
                if (isSelected) R.color.legal_lens_auth_accent else R.color.legal_lens_secondary_text
            )
        )
    }

    private fun ProfileLanguage.toDisplayName(): Int {
        return when (this) {
            ProfileLanguage.VIETNAMESE -> R.string.profile_settings_language_vietnamese
            ProfileLanguage.ENGLISH -> R.string.profile_settings_language_english
            ProfileLanguage.JAPANESE -> R.string.profile_settings_language_japanese
            ProfileLanguage.FRENCH -> R.string.profile_settings_language_french
            ProfileLanguage.CHINESE -> R.string.profile_settings_language_chinese
            ProfileLanguage.KOREAN -> R.string.profile_settings_language_korean
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}



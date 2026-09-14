package com.example.lagallens.presentation.feature.profile.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.lagallens.R
import com.example.lagallens.databinding.FragmentProfileBinding
import com.example.lagallens.databinding.ItemProfileActionBinding
import com.example.lagallens.presentation.feature.home.ui.HomeActivity
import com.example.lagallens.presentation.feature.profile.contract.ProfileUiEffect
import com.example.lagallens.presentation.feature.profile.contract.ProfileUiEvent
import com.example.lagallens.presentation.feature.profile.contract.ProfileUiState
import com.example.lagallens.presentation.feature.profile.dialog.ProfileLogoutDialog
import com.example.lagallens.presentation.feature.profile.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<ProfileViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindView()
        bindLogoutResult()
        collectUiState()
        collectUiEffect()
    }

    private fun bindView() {
        bindAction(
            binding.actionEditProfile,
            R.drawable.ic_profile_edit,
            R.string.profile_edit,
            ProfileUiEvent.EditProfileClicked
        )
        bindAction(
            binding.actionChangePassword,
            R.drawable.ic_profile_lock,
            R.string.profile_change_password,
            ProfileUiEvent.ChangePasswordClicked
        )
        bindAction(
            binding.actionNotificationSettings,
            R.drawable.ic_profile_notification,
            R.string.profile_notification_settings,
            ProfileUiEvent.NotificationSettingsClicked
        )
        bindAction(binding.actionPrivacy, R.drawable.ic_profile_shield, R.string.profile_privacy)
        bindAction(binding.actionAppearance, R.drawable.ic_profile_language, R.string.profile_appearance)
        bindAction(binding.actionAbout, R.drawable.ic_profile_info, R.string.profile_about)
        binding.btnAvatarEdit.setOnClickListener {
            viewModel.onEvent(ProfileUiEvent.EditProfileClicked)
        }
        binding.btnSignOut.setOnClickListener {
            viewModel.onEvent(ProfileUiEvent.LogoutClicked)
        }
    }

    private fun bindAction(
        actionBinding: ItemProfileActionBinding,
        iconRes: Int,
        titleRes: Int,
        event: ProfileUiEvent = ProfileUiEvent.ActionClicked(titleRes)
    ) {
        actionBinding.ivProfileActionIcon.setImageResource(iconRes)
        actionBinding.tvProfileActionTitle.setText(titleRes)
        actionBinding.root.setOnClickListener {
            viewModel.onEvent(event)
        }
    }

    private fun bindLogoutResult() {
        parentFragmentManager.setFragmentResultListener(
            ProfileLogoutDialog.RESULT_KEY,
            viewLifecycleOwner
        ) { _, _ ->
            viewModel.onEvent(ProfileUiEvent.LogoutConfirmed)
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
                        is ProfileUiEffect.ShowActionUnavailable -> showActionUnavailable(effect.actionTitleRes)
                        ProfileUiEffect.NavigateToEditProfile -> findNavController().navigate(
                            R.id.action_profileFragment_to_profileEditFragment
                        )
                        ProfileUiEffect.NavigateToNotificationSettings -> findNavController().navigate(
                            R.id.action_profileFragment_to_profileSettingsFragment
                        )
                        ProfileUiEffect.NavigateToChangePassword -> findNavController().navigate(
                            R.id.action_profileFragment_to_profilePasswordFragment
                        )
                        ProfileUiEffect.ShowLogoutConfirmation -> showLogoutConfirmation()
                        ProfileUiEffect.NavigateToAuthentication -> navigateToAuthentication()
                    }
                }
            }
        }
    }

    private fun render(state: ProfileUiState) {
        binding.tvProfileName.text = state.name
        binding.tvProfileEmail.text = state.email
        binding.tvProfileAvatar.text = state.name.initials()
        binding.tvProfileStatus.setText(state.statusRes)
        binding.tvProfileContractCount.setText(state.contractCountRes)
        binding.tvProfileAnalysisCount.setText(state.analysisCountRes)
        binding.tvProfileChatCount.setText(state.chatCountRes)
    }

    private fun showActionUnavailable(actionTitleRes: Int) {
        Toast.makeText(
            requireContext(),
            getString(R.string.profile_action_unavailable, getString(actionTitleRes)),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun showLogoutConfirmation() {
        if (parentFragmentManager.findFragmentByTag(ProfileLogoutDialog.TAG) == null) {
            ProfileLogoutDialog.newInstance(binding.tvProfileName.text.toString()).show(
                parentFragmentManager,
                ProfileLogoutDialog.TAG
            )
        }
    }

    private fun navigateToAuthentication() {
        startActivity(
            Intent(requireContext(), HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
    }

    private fun String.initials(): String =
        trim().split(" ").filter(String::isNotBlank).takeLast(2).joinToString("") { it.first().toString() }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}



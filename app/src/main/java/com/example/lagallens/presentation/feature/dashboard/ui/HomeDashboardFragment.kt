package com.example.lagallens.presentation.feature.dashboard.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.lagallens.R
import com.example.lagallens.databinding.FragmentHomeDashboardBinding
import com.example.lagallens.presentation.common.utils.MotionTokens
import com.example.lagallens.presentation.common.utils.animateContentEntrance
import com.example.lagallens.presentation.common.utils.bindPressFeedback
import com.example.lagallens.presentation.feature.camera.permission.ui.CameraPermissionActivity
import com.example.lagallens.presentation.feature.compare.select.ui.CompareSelectActivity
import com.example.lagallens.presentation.feature.contracts.detail.ui.ContractDetailFragment
import com.example.lagallens.presentation.feature.dashboard.contract.HomeDashboardUiEffect
import com.example.lagallens.presentation.feature.dashboard.contract.HomeDashboardUiEvent
import com.example.lagallens.presentation.feature.dashboard.viewmodel.HomeDashboardViewModel
import com.example.lagallens.presentation.feature.upload.ui.UploadSourceActivity
import kotlinx.coroutines.launch

class HomeDashboardFragment : Fragment() {
    private var _binding: FragmentHomeDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel by viewModels<HomeDashboardViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindView()
        playEntranceMotion(view)
        collectUiEffect()
    }

    private fun playEntranceMotion(view: View) {
        view.doOnPreDraw {
            val currentBinding = _binding ?: return@doOnPreDraw
            val offset = resources.getDimension(R.dimen.dp_16)
            currentBinding.dashboardGreeting.animateContentEntrance(-offset, MotionTokens.EMPHASIS_DURATION_MILLIS)
            listOf(
                currentBinding.statTotalContracts.root,
                currentBinding.statAnalyzedContracts.root,
                currentBinding.statReviewContracts.root
            ).forEachIndexed { index, card ->
                card.animateContentEntrance(offset, MotionTokens.EMPHASIS_DURATION_MILLIS, 60L * index)
            }
            currentBinding.dashboardQuickActions.animateContentEntrance(0f, MotionTokens.EMPHASIS_DURATION_MILLIS, 120L)
            listOf(currentBinding.recentApprovedContract.root, currentBinding.recentReviewContract.root)
                .forEachIndexed { index, card ->
                    for (child in 0 until card.childCount) {
                        card.getChildAt(child).animateContentEntrance(
                            0f, MotionTokens.EMPHASIS_DURATION_MILLIS, 180L + 60L * index
                        )
                    }
                }
        }
    }

    private fun bindView() {
        listOf(
            binding.shortcutUpload.root, binding.shortcutCamera.root,
            binding.shortcutAnalyze.root, binding.shortcutCompare.root, binding.tvViewAllContracts,
            binding.recentApprovedContract.root, binding.recentReviewContract.root
        ).forEach { it.bindPressFeedback() }
        binding.shortcutUpload.root.setOnClickListener {
            viewModel.onEvent(HomeDashboardUiEvent.UploadClicked)
        }
        binding.shortcutCamera.root.setOnClickListener {
            viewModel.onEvent(HomeDashboardUiEvent.CameraClicked)
        }
        binding.shortcutAnalyze.root.setOnClickListener {
            viewModel.onEvent(HomeDashboardUiEvent.AnalyzeClicked)
        }
        binding.shortcutCompare.root.setOnClickListener {
            viewModel.onEvent(HomeDashboardUiEvent.CompareClicked)
        }
        binding.tvViewAllContracts.setOnClickListener {
            viewModel.onEvent(HomeDashboardUiEvent.ViewAllContractsClicked)
        }
        binding.recentApprovedContract.root.setOnClickListener {
            viewModel.onEvent(HomeDashboardUiEvent.ContractClicked(CONTRACT_OFFICE_LEASE))
        }
        binding.recentReviewContract.root.setOnClickListener {
            viewModel.onEvent(HomeDashboardUiEvent.ContractClicked(CONTRACT_COLLABORATOR))
        }
    }

    private fun collectUiEffect() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiEffect.collect { effect ->
                    when (effect) {
                        HomeDashboardUiEffect.NavigateToUploadSource -> startActivity(
                            Intent(requireContext(), UploadSourceActivity::class.java)
                        )

                        HomeDashboardUiEffect.NavigateToCamera -> startActivity(
                            CameraPermissionActivity.createIntent(requireContext())
                        )

                        HomeDashboardUiEffect.NavigateToContracts -> navigateToContracts()
                        HomeDashboardUiEffect.NavigateToCompare -> startActivity(CompareSelectActivity.createIntent(requireContext()))
                        is HomeDashboardUiEffect.NavigateToContractDetail -> navigateToContractDetail(
                            effect.contractId
                        )

                        is HomeDashboardUiEffect.ShowMessage -> {
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.dashboard_unavailable, getString(effect.titleRes)),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
    }

    private fun navigateToContracts() {
        val navController = findNavController()
        if (navController.currentDestination?.id == R.id.homeDashboardFragment) {
            navController.navigate(R.id.contractListFragment)
        }
    }

    private fun navigateToContractDetail(contractId: String) {
        val navController = findNavController()
        if (navController.currentDestination?.id == R.id.homeDashboardFragment) {
            navController.navigate(
                R.id.contractDetailFragment,
                Bundle().apply {
                    putString(ContractDetailFragment.ARG_CONTRACT_ID, contractId)
                }
            )
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private companion object {
        const val CONTRACT_OFFICE_LEASE = "office-lease"
        const val CONTRACT_COLLABORATOR = "collaborator"
    }
}

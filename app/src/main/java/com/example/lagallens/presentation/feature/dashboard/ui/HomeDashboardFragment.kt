package com.example.lagallens.presentation.feature.dashboard.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.lagallens.LegalLensApplication
import com.example.lagallens.R
import com.example.lagallens.databinding.FragmentHomeDashboardBinding
import com.example.lagallens.presentation.common.utils.MotionTokens
import com.example.lagallens.presentation.common.utils.animateContentEntrance
import com.example.lagallens.presentation.common.utils.bindPressFeedback
import com.example.lagallens.presentation.feature.camera.permission.ui.CameraPermissionActivity
import com.example.lagallens.presentation.feature.compare.select.ui.CompareSelectActivity
import com.example.lagallens.presentation.feature.contracts.contract.ContractRiskLevel
import com.example.lagallens.presentation.feature.contracts.detail.ui.ContractDetailFragment
import com.example.lagallens.presentation.feature.dashboard.contract.DashboardRecentContract
import com.example.lagallens.presentation.feature.dashboard.contract.HomeDashboardUiState
import com.example.lagallens.presentation.feature.dashboard.contract.HomeDashboardUiEffect
import com.example.lagallens.presentation.feature.dashboard.contract.HomeDashboardUiEvent
import com.example.lagallens.presentation.feature.dashboard.viewmodel.HomeDashboardViewModel
import com.example.lagallens.presentation.feature.upload.ui.UploadSourceActivity
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.launch

class HomeDashboardFragment : Fragment() {
    private var _binding: FragmentHomeDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel by viewModels<HomeDashboardViewModel> {
        val app = requireActivity().application as LegalLensApplication
        HomeDashboardViewModel.Factory(app.contractRepository, app.userProfileRepository)
    }

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
        collectUiState()
        collectUiEffect()
    }

    override fun onResume() {
        super.onResume()
        viewModel.onEvent(HomeDashboardUiEvent.ScreenResumed)
    }

    private fun collectUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: HomeDashboardUiState) = with(binding) {
        tvDashboardUserName.text = state.userName
        tvDashboardAvatar.text = state.userName.trim().split(" ").filter(String::isNotBlank).takeLast(2).joinToString("") { it.first().toString() }
        tvDashboardAvatar.contentDescription = state.userName
        val hasError = state.errorMessageRes != null
        fun count(value: Int) = when { state.isLoading -> "\u2026"; hasError -> "\u2014"; else -> value.toString() }
        statTotalContracts.tvStatValue.text = count(state.totalContracts)
        statAnalyzedContracts.tvStatValue.text = count(state.analyzedContracts)
        statReviewContracts.tvStatValue.text = count(state.reviewContracts)
        bindRecentContract(recentApprovedContract.root, state.recentContracts.getOrNull(0))
        bindRecentContract(recentReviewContract.root, state.recentContracts.getOrNull(1))
        val isEmpty = !state.isLoading && !hasError && state.recentContracts.isEmpty()
        tvDashboardRecentMessage.isVisible = hasError || isEmpty
        tvDashboardRecentMessage.setText(state.errorMessageRes ?: R.string.dashboard_recent_empty)
        btnDashboardRetry.isVisible = hasError
    }

    /** Both card layouts share view IDs, so one binding path covers either position. */
    private fun bindRecentContract(card: View, recent: DashboardRecentContract?) {
        card.isVisible = recent != null
        if (recent == null) return
        val item = recent.contract
        val isHighRisk = item.riskLevel == ContractRiskLevel.HIGH || item.riskLevel == ContractRiskLevel.CRITICAL
        val accentColor = ContextCompat.getColor(requireContext(), if (isHighRisk) R.color.dashboard_red else R.color.dashboard_green)
        card.findViewById<TextView>(R.id.tvRecentTitle).text = item.titleOverride
        card.findViewById<TextView>(R.id.tvRecentSubtitle).text = getString(
            R.string.dashboard_contract_subtitle,
            getString(item.detailTypeRes),
            DateFormat.getDateInstance(DateFormat.SHORT).format(Date(recent.updatedAtMillis))
        )
        card.findViewById<ImageView>(R.id.ivRecentIcon).setBackgroundResource(if (isHighRisk) R.drawable.bg_dashboard_status_red else R.drawable.bg_dashboard_icon_green)
        card.findViewById<TextView>(R.id.tvRecentStatus).apply {
            setText(item.statusRes)
            setTextColor(accentColor)
            setBackgroundResource(if (isHighRisk) R.drawable.bg_dashboard_status_red else R.drawable.bg_dashboard_status_green)
        }
        card.findViewById<ImageView>(R.id.ivRecentRisk).setImageResource(item.riskIconRes)
        card.findViewById<TextView>(R.id.tvRecentRisk).apply {
            setText(item.riskRes)
            setTextColor(ContextCompat.getColor(requireContext(), item.riskTextColorRes))
        }
        card.setOnClickListener { viewModel.onEvent(HomeDashboardUiEvent.ContractClicked(item.id)) }
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
        binding.btnDashboardRetry.setOnClickListener {
            viewModel.onEvent(HomeDashboardUiEvent.RetryClicked)
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
}

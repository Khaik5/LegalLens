package com.example.lagallens.presentation.feature.notifications.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lagallens.R
import com.example.lagallens.databinding.FragmentNotificationBinding
import com.example.lagallens.presentation.feature.notifications.adapter.NotificationAdapter
import com.example.lagallens.presentation.feature.notifications.contract.NotificationFilter
import com.example.lagallens.presentation.feature.notifications.contract.NotificationUiEffect
import com.example.lagallens.presentation.feature.notifications.contract.NotificationUiEvent
import com.example.lagallens.presentation.feature.notifications.contract.NotificationUiState
import com.example.lagallens.presentation.feature.notifications.viewmodel.NotificationViewModel
import com.example.lagallens.presentation.feature.contracts.detail.ui.ContractDetailFragment
import kotlinx.coroutines.launch

class NotificationFragment : Fragment() {
    private var _binding: FragmentNotificationBinding? = null
    private val binding get() = _binding!!
    private val viewModel by viewModels<NotificationViewModel>()
    private val notificationAdapter = NotificationAdapter { notification ->
        viewModel.onEvent(NotificationUiEvent.NotificationClicked(notification.id))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindView()
        collectUiState()
        collectUiEffect()
    }

    private fun bindView() {
        binding.rvNotifications.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = notificationAdapter
        }
        binding.tvMarkAllRead.setOnClickListener {
            viewModel.onEvent(NotificationUiEvent.MarkAllReadClicked)
        }
        binding.tvFilterAll.setOnClickListener { selectFilter(NotificationFilter.ALL) }
        binding.tvFilterAnalysis.setOnClickListener { selectFilter(NotificationFilter.ANALYSIS) }
        binding.tvFilterOcr.setOnClickListener { selectFilter(NotificationFilter.OCR) }
        binding.tvFilterReminder.setOnClickListener { selectFilter(NotificationFilter.REMINDER) }
    }

    private fun selectFilter(filter: NotificationFilter) {
        viewModel.onEvent(NotificationUiEvent.FilterSelected(filter))
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
                        is NotificationUiEffect.NavigateToContractDetail -> {
                            val navController = findNavController()
                            if (navController.currentDestination?.id == R.id.notificationFragment) {
                                navController.navigate(
                                    R.id.contractDetailFragment,
                                    bundleOf(ContractDetailFragment.ARG_CONTRACT_ID to effect.contractId)
                                )
                            }
                        }
                        is NotificationUiEffect.ShowMessage -> {
                            Toast.makeText(requireContext(), effect.messageRes, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun render(state: NotificationUiState) {
        val hasNotifications = state.notifications.isNotEmpty()
        notificationAdapter.submitList(state.notifications)
        binding.rvNotifications.isVisible = hasNotifications
        binding.emptyNotificationState.isVisible = !hasNotifications
        binding.filterScrollView.isVisible = hasNotifications
        binding.tvMarkAllRead.isVisible = hasNotifications
        updateFilterStyle(binding.tvFilterAll, state.selectedFilter == NotificationFilter.ALL)
        updateFilterStyle(binding.tvFilterAnalysis, state.selectedFilter == NotificationFilter.ANALYSIS)
        updateFilterStyle(binding.tvFilterOcr, state.selectedFilter == NotificationFilter.OCR)
        updateFilterStyle(binding.tvFilterReminder, state.selectedFilter == NotificationFilter.REMINDER)
    }

    private fun updateFilterStyle(view: TextView, isSelected: Boolean) {
        view.setBackgroundResource(
            if (isSelected) R.drawable.bg_contract_filter_active else R.drawable.bg_contract_filter_inactive
        )
        view.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (isSelected) R.color.white else R.color.legal_lens_secondary_text
            )
        )
    }

    override fun onDestroyView() {
        binding.rvNotifications.adapter = null
        _binding = null
        super.onDestroyView()
    }
}

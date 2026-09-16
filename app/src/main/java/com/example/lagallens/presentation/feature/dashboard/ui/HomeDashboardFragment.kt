package com.example.lagallens.presentation.feature.dashboard.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.lagallens.R
import com.example.lagallens.databinding.FragmentHomeDashboardBinding
import com.example.lagallens.presentation.feature.dashboard.contract.HomeDashboardUiEffect
import com.example.lagallens.presentation.feature.dashboard.contract.HomeDashboardUiEvent
import com.example.lagallens.presentation.feature.dashboard.viewmodel.HomeDashboardViewModel
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
        collectUiEffect()
    }

    private fun bindView() {
        binding.shortcutUpload.root.setOnClickListener {
            viewModel.onEvent(HomeDashboardUiEvent.ShortcutClicked(R.string.dashboard_upload))
        }
        binding.shortcutCamera.root.setOnClickListener {
            viewModel.onEvent(HomeDashboardUiEvent.ShortcutClicked(R.string.dashboard_capture))
        }
        binding.shortcutAnalyze.root.setOnClickListener {
            viewModel.onEvent(HomeDashboardUiEvent.ShortcutClicked(R.string.dashboard_analyze))
        }
        binding.shortcutCompare.root.setOnClickListener {
            viewModel.onEvent(HomeDashboardUiEvent.ShortcutClicked(R.string.dashboard_compare))
        }
        binding.tvViewAllContracts.setOnClickListener {
            viewModel.onEvent(HomeDashboardUiEvent.ViewAllContractsClicked)
        }
        binding.recentApprovedContract.root.setOnClickListener {
            viewModel.onEvent(HomeDashboardUiEvent.ContractClicked)
        }
        binding.recentReviewContract.root.setOnClickListener {
            viewModel.onEvent(HomeDashboardUiEvent.ContractClicked)
        }
    }

    private fun collectUiEffect() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiEffect.collect { effect ->
                    when (effect) {
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

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

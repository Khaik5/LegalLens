package com.example.lagallens.presentation.feature.notifications.ui

import android.graphics.Canvas
import android.graphics.Color
import androidx.core.graphics.drawable.toDrawable
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
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.example.lagallens.R
import com.example.lagallens.databinding.FragmentNotificationBinding
import com.example.lagallens.presentation.common.utils.bindPressFeedback
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

    override fun onResume() {
        super.onResume()
        viewModel.onEvent(NotificationUiEvent.ScreenOpened)
    }

    private fun bindView() {
        listOf(binding.tvMarkAllRead, binding.tvFilterAll, binding.tvFilterAnalysis,
            binding.tvFilterOcr, binding.tvFilterReminder).forEach { it.bindPressFeedback() }
        binding.rvNotifications.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = notificationAdapter
        }
        ItemTouchHelper(SwipeToDeleteCallback()).attachToRecyclerView(binding.rvNotifications)
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
                        is NotificationUiEffect.ShowDeletedWithUndo -> showUndoSnackbar(effect.notificationId)
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
        state.errorMessageRes?.let { errorRes ->
            binding.notificationEmptyTitle.setText(R.string.notification_load_error_title)
            binding.notificationEmptyDescription.setText(errorRes)
        } ?: run {
            binding.notificationEmptyTitle.setText(R.string.notification_empty_title)
            binding.notificationEmptyDescription.setText(R.string.notification_empty_description)
        }
        binding.filterScrollView.isVisible = true
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

    override fun onStop() {
        viewModel.onEvent(NotificationUiEvent.ScreenStopped)
        super.onStop()
    }

    private fun showUndoSnackbar(notificationId: String) {
        Snackbar.make(binding.root, R.string.notification_deleted, Snackbar.LENGTH_LONG)
            // Above the bottom navigation of the hosting screen, which would otherwise cover it.
            .setAnchorView(requireActivity().findViewById<View?>(R.id.bottomNavigationContainer)?.takeIf { it.isShown })
            .setAction(R.string.notification_undo) { viewModel.onEvent(NotificationUiEvent.UndoDeleteClicked(notificationId)) }
            .addCallback(object : Snackbar.Callback() {
                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    if (event != DISMISS_EVENT_ACTION) viewModel.onEvent(NotificationUiEvent.DeleteUndoWindowClosed(notificationId))
                }
            })
            .show()
    }

    /** Swipe left shows a red delete background with a trash icon under the card. */
    private inner class SwipeToDeleteCallback : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
        private val background by lazy { ContextCompat.getColor(requireContext(), R.color.contract_risk_high).toDrawable() }
        private val icon by lazy {
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_camera_delete)?.mutate()?.apply { setTint(Color.WHITE) }
        }

        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val item = notificationAdapter.currentList.getOrNull(viewHolder.bindingAdapterPosition) ?: return
            viewModel.onEvent(NotificationUiEvent.NotificationSwiped(item.id))
        }

        override fun onChildDraw(
            canvas: Canvas,
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            dX: Float,
            dY: Float,
            actionState: Int,
            isCurrentlyActive: Boolean
        ) {
            val card = viewHolder.itemView
            if (dX < 0) {
                background.setBounds(card.right + dX.toInt(), card.top, card.right, card.bottom)
                background.draw(canvas)
                icon?.let { trash ->
                    val size = resources.getDimensionPixelSize(R.dimen.dp_24)
                    val margin = resources.getDimensionPixelSize(R.dimen.dp_24)
                    val top = card.top + (card.height - size) / 2
                    trash.setBounds(card.right - margin - size, top, card.right - margin, top + size)
                    if (card.right + dX < card.right - margin - size) trash.draw(canvas)
                }
            }
            super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        }
    }

    override fun onDestroyView() {
        binding.rvNotifications.adapter = null
        _binding = null
        super.onDestroyView()
    }
}

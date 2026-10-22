package com.example.lagallens.presentation.feature.creditstore.unlock.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.lagallens.R
import com.example.lagallens.databinding.ActivityUnlockConfirmationBinding
import com.example.lagallens.presentation.feature.creditstore.unlock.contract.UnlockConfirmationUiEffect
import com.example.lagallens.presentation.feature.creditstore.unlock.contract.UnlockConfirmationUiEvent
import com.example.lagallens.presentation.feature.creditstore.unlock.contract.UnlockConfirmationUiState
import com.example.lagallens.presentation.feature.creditstore.unlock.dialog.UnlockProcessingDialog
import com.example.lagallens.presentation.feature.creditstore.unlock.dialog.UnlockSuccessDialog
import com.example.lagallens.presentation.feature.creditstore.unlock.viewmodel.UnlockConfirmationViewModel
import com.example.lagallens.presentation.feature.creditstore.flow.ui.UnlockedAnalysisResultActivity
import com.example.lagallens.presentation.feature.creditstore.flow.ui.InsufficientCreditsActivity
import kotlinx.coroutines.launch

class UnlockConfirmationActivity : AppCompatActivity() {
    private val binding by lazy { ActivityUnlockConfirmationBinding.inflate(layoutInflater) }
    private val viewModel by viewModels<UnlockConfirmationViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        supportFragmentManager.setFragmentResultListener(UnlockSuccessDialog.RESULT_KEY, this) { _, _ ->
            startActivity(UnlockedAnalysisResultActivity.createIntent(this))
            finish()
        }
        supportFragmentManager.setFragmentResultListener(UnlockProcessingDialog.RESULT_KEY, this) { _, _ ->
            showUnlockSuccessDialog()
        }
        bindView()
        collectViewModel()
        viewModel.onEvent(
            UnlockConfirmationUiEvent.ScreenOpened(
                analysisResultId = intent.getStringExtra(EXTRA_ANALYSIS_RESULT_ID).orEmpty(),
                availableCredits = intent.getIntExtra(EXTRA_AVAILABLE_CREDITS, 0),
                unlockCost = intent.getIntExtra(EXTRA_UNLOCK_CREDIT_COST, 0)
            )
        )
        bindInsets()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                viewModel.onEvent(UnlockConfirmationUiEvent.BackClicked)
            }
        })
    }

    private fun bindView() = with(binding) {
        unlockConfirmationBack.setOnClickListener {
            viewModel.onEvent(UnlockConfirmationUiEvent.BackClicked)
        }
        unlockConfirmationCancel.setOnClickListener {
            viewModel.onEvent(UnlockConfirmationUiEvent.CancelClicked)
        }
        unlockConfirmationConfirm.setOnClickListener {
            viewModel.onEvent(UnlockConfirmationUiEvent.ConfirmClicked)
        }
    }

    private fun collectViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.uiEffect.collect(::handleEffect) }
            }
        }
    }

    private fun bindInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.unlockConfirmationRoot) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun render(state: UnlockConfirmationUiState) = with(binding) {
        unlockConfirmationResultMeta.text = getString(
            R.string.unlock_confirmation_result_meta,
            state.analysisResultId
        )
        unlockConfirmationBalanceBefore.text = getString(
            R.string.unlock_confirmation_credit_value,
            state.availableCredits
        )
        unlockConfirmationCost.text = getString(
            R.string.unlock_confirmation_credit_deduction,
            state.unlockCost
        )
        unlockConfirmationBalanceAfter.text = getString(
            R.string.unlock_confirmation_credit_value,
            state.remainingCredits
        )
        unlockConfirmationConfirm.text = getString(
            R.string.unlock_confirmation_confirm,
            state.unlockCost
        )
        unlockConfirmationConfirm.isEnabled =
            !state.isConfirmationRequested && !state.isInsufficientCreditsRequested
    }

    private fun handleEffect(effect: UnlockConfirmationUiEffect) {
        when (effect) {
            UnlockConfirmationUiEffect.NavigateBack -> finish()
            UnlockConfirmationUiEffect.NavigateToInsufficientCredits -> {
                startActivity(InsufficientCreditsActivity.createIntent(this))
                finish()
            }
            is UnlockConfirmationUiEffect.UnlockRequested -> {
                showUnlockProcessingDialog()
            }
        }
    }

    private fun showUnlockProcessingDialog() {
        if (supportFragmentManager.findFragmentByTag(UnlockProcessingDialog.TAG) == null) {
            UnlockProcessingDialog().show(supportFragmentManager, UnlockProcessingDialog.TAG)
        }
    }

    private fun showUnlockSuccessDialog() {
        if (supportFragmentManager.findFragmentByTag(UnlockSuccessDialog.TAG) == null) {
            UnlockSuccessDialog().show(supportFragmentManager, UnlockSuccessDialog.TAG)
        }
    }

    companion object {
        const val EXTRA_ANALYSIS_RESULT_ID = "extra_analysis_result_id"
        const val EXTRA_AVAILABLE_CREDITS = "extra_available_credits"
        const val EXTRA_UNLOCK_CREDIT_COST = "extra_unlock_credit_cost"

        fun createIntent(
            context: Context,
            analysisResultId: String,
            availableCredits: Int,
            unlockCost: Int
        ): Intent = Intent(context, UnlockConfirmationActivity::class.java)
            .putExtra(EXTRA_ANALYSIS_RESULT_ID, analysisResultId)
            .putExtra(EXTRA_AVAILABLE_CREDITS, availableCredits)
            .putExtra(EXTRA_UNLOCK_CREDIT_COST, unlockCost)
    }
}

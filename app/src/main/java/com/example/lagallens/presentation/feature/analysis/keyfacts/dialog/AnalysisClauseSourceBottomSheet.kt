package com.example.lagallens.presentation.feature.analysis.keyfacts.dialog

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.example.lagallens.R
import com.example.lagallens.databinding.BottomSheetAnalysisClauseSourceBinding
import com.example.lagallens.presentation.feature.analysis.keyfacts.contract.AnalysisClauseSource
import com.example.lagallens.presentation.feature.analysis.keyfacts.ui.AnalysisKeyFactsActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AnalysisClauseSourceBottomSheet : BottomSheetDialogFragment() {
    private var _binding: BottomSheetAnalysisClauseSourceBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(requireContext(), theme).apply {
            setOnShowListener {
                val bottomSheet = findViewById<FrameLayout>(
                    com.google.android.material.R.id.design_bottom_sheet
                ) ?: return@setOnShowListener
                bottomSheet.setBackgroundColor(Color.TRANSPARENT)
                BottomSheetBehavior.from(bottomSheet).apply {
                    skipCollapsed = true
                    state = BottomSheetBehavior.STATE_EXPANDED
                }
                window?.apply {
                    navigationBarColor = ContextCompat.getColor(requireContext(), R.color.white)
                    WindowCompat.getInsetsController(this, decorView).isAppearanceLightNavigationBars = true
                    setDimAmount(FIGMA_DIM_AMOUNT)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAnalysisClauseSourceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args = requireArguments()
        val source = AnalysisClauseSource(args.getString(REFERENCE_RES_KEY).orEmpty(), args.getString(TITLE_RES_KEY).orEmpty(), args.getString(EXCERPT_RES_KEY).orEmpty())
        if ((activity as? com.example.lagallens.presentation.feature.analysis.keyfacts.contract.AnalysisClauseSourceOwner)?.isVerifiedClause(source) != true) { dismissAllowingStateLoss(); return }
        binding.analysisClauseSourceReference.text = source.reference
        binding.analysisClauseSourceTitle.text = source.title
        binding.analysisClauseSourceExcerpt.text = source.excerpt
        binding.analysisClauseSourceCloseIcon.setOnClickListener { dismiss() }
        binding.analysisClauseSourceCloseButton.setOnClickListener { dismiss() }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "analysis_clause_source_bottom_sheet"
        private const val REFERENCE_RES_KEY = "analysis_clause_source_reference_res"
        private const val TITLE_RES_KEY = "analysis_clause_source_title_res"
        private const val EXCERPT_RES_KEY = "analysis_clause_source_excerpt_res"
        private const val FIGMA_DIM_AMOUNT = 0.4f

        fun newInstance(source: AnalysisClauseSource): AnalysisClauseSourceBottomSheet {
            return AnalysisClauseSourceBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(REFERENCE_RES_KEY, source.reference)
                    putString(TITLE_RES_KEY, source.title)
                    putString(EXCERPT_RES_KEY, source.excerpt)
                }
            }
        }
    }
}

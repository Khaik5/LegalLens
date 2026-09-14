package com.example.lagallens.presentation.feature.profile.settings.ui

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.example.lagallens.R
import com.example.lagallens.databinding.BottomSheetProfileLanguageBinding
import com.example.lagallens.presentation.feature.profile.settings.contract.ProfileLanguage
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ProfileLanguageBottomSheet : BottomSheetDialogFragment() {
    private var _binding: BottomSheetProfileLanguageBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(requireContext(), theme).apply {
            setOnShowListener {
                val bottomSheet = findViewById<FrameLayout>(
                    com.google.android.material.R.id.design_bottom_sheet
                ) ?: return@setOnShowListener
                bottomSheet.setBackgroundColor(Color.TRANSPARENT)
                BottomSheetBehavior.from(bottomSheet).state = BottomSheetBehavior.STATE_EXPANDED
                window?.navigationBarColor = ContextCompat.getColor(
                    requireContext(),
                    R.color.dashboard_surface
                )
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetProfileLanguageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rgLanguages.check(languageToId(selectedLanguage))
        binding.rgLanguages.setOnCheckedChangeListener { _, checkedId ->
            val language = idToLanguage(checkedId) ?: return@setOnCheckedChangeListener
            parentFragmentManager.setFragmentResult(
                RESULT_KEY,
                Bundle().apply { putString(LANGUAGE_KEY, language.name) }
            )
            dismiss()
        }
    }

    private val selectedLanguage: ProfileLanguage
        get() = ProfileLanguage.entries.find { it.name == arguments?.getString(SELECTED_LANGUAGE_KEY) }
            ?: ProfileLanguage.VIETNAMESE

    private fun languageToId(language: ProfileLanguage): Int {
        return when (language) {
            ProfileLanguage.VIETNAMESE -> R.id.rbVietnamese
            ProfileLanguage.ENGLISH -> R.id.rbEnglish
            ProfileLanguage.JAPANESE -> R.id.rbJapanese
            ProfileLanguage.FRENCH -> R.id.rbFrench
            ProfileLanguage.CHINESE -> R.id.rbChinese
            ProfileLanguage.KOREAN -> R.id.rbKorean
        }
    }

    private fun idToLanguage(id: Int): ProfileLanguage? {
        return when (id) {
            R.id.rbVietnamese -> ProfileLanguage.VIETNAMESE
            R.id.rbEnglish -> ProfileLanguage.ENGLISH
            R.id.rbJapanese -> ProfileLanguage.JAPANESE
            R.id.rbFrench -> ProfileLanguage.FRENCH
            R.id.rbChinese -> ProfileLanguage.CHINESE
            R.id.rbKorean -> ProfileLanguage.KOREAN
            else -> null
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "profile_language"
        const val RESULT_KEY = "profile_language_result"
        private const val SELECTED_LANGUAGE_KEY = "selected_language"
        private const val LANGUAGE_KEY = "language"

        fun newInstance(language: ProfileLanguage): ProfileLanguageBottomSheet {
            return ProfileLanguageBottomSheet().apply {
                arguments = Bundle().apply { putString(SELECTED_LANGUAGE_KEY, language.name) }
            }
        }

        fun readLanguage(bundle: Bundle): ProfileLanguage {
            return ProfileLanguage.entries.find { it.name == bundle.getString(LANGUAGE_KEY) }
                ?: ProfileLanguage.VIETNAMESE
        }
    }
}



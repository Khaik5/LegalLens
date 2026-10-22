package com.example.lagallens.presentation.feature.creditstore.unlock.dialog

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.lagallens.databinding.DialogUnlockProcessingBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class UnlockProcessingDialog : DialogFragment() {
    private var _binding: DialogUnlockProcessingBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return Dialog(requireContext()).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setCanceledOnTouchOutside(false)
            window?.apply {
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setDimAmount(DIM_AMOUNT)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogUnlockProcessingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isCancelable = false
        viewLifecycleOwner.lifecycleScope.launch {
            delay(UNLOCK_PROCESSING_DELAY_MILLIS)
            parentFragmentManager.setFragmentResult(RESULT_KEY, Bundle.EMPTY)
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "unlock_processing"
        const val RESULT_KEY = "unlock_processing_result"
        private const val DIM_AMOUNT = 0.22f
        private const val UNLOCK_PROCESSING_DELAY_MILLIS = 900L
    }
}

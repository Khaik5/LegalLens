package com.example.lagallens.presentation.feature.profile.dialog

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
import com.example.lagallens.R
import com.example.lagallens.databinding.DialogProfileLogoutBinding

class ProfileLogoutDialog : DialogFragment() {
    private var _binding: DialogProfileLogoutBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return Dialog(requireContext()).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
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
        _binding = DialogProfileLogoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvLogoutMessage.text = getString(R.string.profile_logout_message, profileName)
        binding.btnCancel.setOnClickListener { dismiss() }
        binding.btnConfirmLogout.setOnClickListener {
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

    private val profileName: String
        get() = arguments?.getString(PROFILE_NAME_KEY).orEmpty()

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "profile_logout"
        const val RESULT_KEY = "profile_logout_result"
        private const val PROFILE_NAME_KEY = "profile_name"
        private const val DIM_AMOUNT = 0.4f

        fun newInstance(profileName: String): ProfileLogoutDialog {
            return ProfileLogoutDialog().apply {
                arguments = Bundle().apply { putString(PROFILE_NAME_KEY, profileName) }
            }
        }
    }
}

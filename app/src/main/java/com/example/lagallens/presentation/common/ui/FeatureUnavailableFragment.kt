package com.example.lagallens.presentation.common.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.lagallens.databinding.FragmentFeatureUnavailableBinding

class FeatureUnavailableFragment : Fragment() {
    private var _binding: FragmentFeatureUnavailableBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeatureUnavailableBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvFeatureTitle.text = findNavController().currentDestination?.label
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

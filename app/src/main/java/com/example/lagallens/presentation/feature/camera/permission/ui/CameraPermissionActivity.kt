package com.example.lagallens.presentation.feature.camera.permission.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.lagallens.R
import com.example.lagallens.databinding.ActivityCameraPermissionBinding
import com.example.lagallens.presentation.feature.camera.capture.ui.CameraCaptureActivity
import com.example.lagallens.presentation.feature.camera.permission.contract.CameraPermissionUiEffect
import com.example.lagallens.presentation.feature.camera.permission.contract.CameraPermissionUiEvent
import com.example.lagallens.presentation.feature.camera.permission.contract.CameraPermissionUiState
import com.example.lagallens.presentation.feature.camera.permission.viewmodel.CameraPermissionViewModel
import kotlinx.coroutines.launch

class CameraPermissionActivity : AppCompatActivity() {
    private val binding by lazy { ActivityCameraPermissionBinding.inflate(layoutInflater) }
    private val viewModel by viewModels<CameraPermissionViewModel>()
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onEvent(CameraPermissionUiEvent.PermissionResult(granted))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        setContentView(binding.root)
        applyWindowInsets()
        bindView()
        collectViewModel()
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.cameraPermissionRoot) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun bindView() {
        binding.cameraPermissionBack.setOnClickListener {
            viewModel.onEvent(CameraPermissionUiEvent.LaterClicked)
        }
        binding.cameraPermissionAllow.setOnClickListener {
            viewModel.onEvent(CameraPermissionUiEvent.AllowClicked)
        }
        binding.cameraPermissionLater.setOnClickListener {
            viewModel.onEvent(CameraPermissionUiEvent.LaterClicked)
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

    private fun render(state: CameraPermissionUiState) {
        binding.cameraPermissionAllow.isEnabled = !state.isRequestingPermission
        binding.cameraPermissionLater.isEnabled = !state.isRequestingPermission
    }

    private fun handleEffect(effect: CameraPermissionUiEffect) {
        when (effect) {
            CameraPermissionUiEffect.RequestCameraPermission -> permissionLauncher.launch(Manifest.permission.CAMERA)
            CameraPermissionUiEffect.NavigateBack -> finish()
            CameraPermissionUiEffect.NavigateToCameraCapture -> {
                startActivity(CameraCaptureActivity.createIntent(this))
                finish()
            }
            CameraPermissionUiEffect.ShowPermissionDenied -> {
                Toast.makeText(this, R.string.camera_permission_denied, Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        fun createIntent(context: Context): Intent = Intent(context, CameraPermissionActivity::class.java)
    }
}

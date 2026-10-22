package com.example.lagallens.presentation.feature.main.ui

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import com.example.lagallens.R
import com.example.lagallens.databinding.ActivityMainBinding
import com.example.lagallens.presentation.common.view.FloatingPremiumSticker
import com.example.lagallens.presentation.common.utils.applySystemBarInsets
import com.example.lagallens.presentation.feature.creditstore.hub.ui.CreditHubActivity
import com.example.lagallens.presentation.feature.upload.ui.UploadSourceActivity

class MainActivity : AppCompatActivity() {
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val premiumSticker by lazy { FloatingPremiumSticker(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        binding.mainNavHost.applySystemBarInsets(top = true, bottom = false)
        binding.bottomNavigationContainer.applySystemBarInsets(
            top = false, bottom = true, expandHeightForBottomInset = true
        )
        bindPremiumSticker()

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.mainNavHost) as NavHostFragment
        val navController = navHostFragment.navController

        bindBottomNavigation(navController)
        binding.btnAddContract.setOnClickListener {
            startActivity(Intent(this, UploadSourceActivity::class.java))
        }
    }

    private fun bindBottomNavigation(navController: NavController) {
        binding.navHome.setOnClickListener { navigateTo(navController, R.id.homeDashboardFragment) }
        binding.navContracts.setOnClickListener { navigateTo(navController, R.id.contractListFragment) }
        binding.navNotifications.setOnClickListener { navigateTo(navController, R.id.notificationFragment) }
        binding.navProfile.setOnClickListener { navigateTo(navController, R.id.profileFragment) }
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val isContractDetail = destination.id == R.id.contractDetailFragment
            val hidesBottomNavigation = isContractDetail ||
                destination.id == R.id.profileEditFragment ||
                destination.id == R.id.profileSettingsFragment ||
                destination.id == R.id.profilePasswordFragment
            binding.bottomNavigationContainer.isVisible = !hidesBottomNavigation
            binding.btnAddContract.isVisible = !hidesBottomNavigation
            binding.navHome.isSelected = destination.id == R.id.homeDashboardFragment
            binding.navContracts.isSelected = destination.id == R.id.contractListFragment ||
                destination.id == R.id.contractSearchFragment || isContractDetail
            binding.navNotifications.isSelected = destination.id == R.id.notificationFragment
            binding.navProfile.isSelected = destination.id == R.id.profileFragment
            binding.premiumStickerLayer.isVisible = destination.id in stickerDestinationIds
        }
    }

    private fun bindPremiumSticker() {
        binding.premiumStickerLayer.addView(
            premiumSticker,
            FrameLayout.LayoutParams(
                resources.getDimensionPixelSize(R.dimen.dp_56),
                resources.getDimensionPixelSize(R.dimen.dp_56),
                Gravity.TOP or Gravity.START
            )
        )
        premiumSticker.setOnStickerClick {
            startActivity(CreditHubActivity.createIntent(this))
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.premiumStickerLayer) { _, windowInsets ->
            premiumSticker.setSafeTopInset(
                windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            )
            windowInsets
        }
        ViewCompat.requestApplyInsets(binding.premiumStickerLayer)
    }

    private fun navigateTo(navController: NavController, destinationId: Int) {
        if (navController.currentDestination?.id == destinationId) return
        navController.navigate(
            destinationId, null, navOptions {
                launchSingleTop = true
                restoreState = true
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
            })
    }

    private companion object {
        val stickerDestinationIds = setOf(
            R.id.homeDashboardFragment,
            R.id.contractListFragment,
            R.id.notificationFragment,
            R.id.profileFragment
        )
    }
}

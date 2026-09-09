package com.example.lagallens.presentation.feature.main.ui

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import com.example.lagallens.R
import com.example.lagallens.databinding.ActivityMainBinding
import com.example.lagallens.presentation.common.utils.applySystemBarInsets

class MainActivity : AppCompatActivity() {
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        binding.mainNavHost.applySystemBarInsets(top = true, bottom = false)
        binding.bottomNavigationContainer.applySystemBarInsets(
            top = false,
            bottom = true,
            expandHeightForBottomInset = true
        )

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.mainNavHost) as NavHostFragment
        val navController = navHostFragment.navController

        bindBottomNavigation(navController)
        binding.btnAddContract.setOnClickListener {
            navController.navigate(R.id.addContractPlaceholder)
        }
    }

    private fun bindBottomNavigation(navController: NavController) {
        binding.navHome.setOnClickListener { navigateTo(navController, R.id.homeDashboardFragment) }
        binding.navContracts.setOnClickListener { navigateTo(navController, R.id.contractListFragment) }
        binding.navNotifications.setOnClickListener { navigateTo(navController, R.id.notificationsPlaceholder) }
        binding.navProfile.setOnClickListener { navigateTo(navController, R.id.profilePlaceholder) }
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val isContractDetail = destination.id == R.id.contractDetailFragment
            binding.bottomNavigationContainer.isVisible = !isContractDetail
            binding.btnAddContract.isVisible = !isContractDetail
            binding.navHome.isSelected = destination.id == R.id.homeDashboardFragment
            binding.navContracts.isSelected = destination.id == R.id.contractListFragment ||
                destination.id == R.id.contractSearchFragment || isContractDetail
            binding.navNotifications.isSelected = destination.id == R.id.notificationsPlaceholder
            binding.navProfile.isSelected = destination.id == R.id.profilePlaceholder
        }
    }

    private fun navigateTo(navController: NavController, destinationId: Int) {
        if (navController.currentDestination?.id == destinationId) return
        navController.navigate(
            destinationId,
            null,
            navOptions {
                launchSingleTop = true
                restoreState = true
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
            }
        )
    }
}

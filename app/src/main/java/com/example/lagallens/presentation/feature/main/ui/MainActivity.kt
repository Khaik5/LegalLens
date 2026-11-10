package com.example.lagallens.presentation.feature.main.ui

import android.content.Context
import android.content.Intent
import com.example.lagallens.presentation.feature.contracts.detail.ui.ContractDetailFragment
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
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
import androidx.lifecycle.lifecycleScope
import com.example.lagallens.LegalLensApplication
import com.example.lagallens.R
import com.example.lagallens.presentation.feature.login.ui.LoginActivity
import com.example.lagallens.presentation.feature.home.ui.HomeActivity
import kotlinx.coroutines.launch
import com.example.lagallens.databinding.ActivityMainBinding
import com.example.lagallens.presentation.common.view.FloatingPremiumSticker
import com.example.lagallens.presentation.common.utils.applySystemBarInsets
import com.example.lagallens.presentation.common.utils.bindPressFeedback
import com.example.lagallens.presentation.common.utils.MotionTokens
import com.example.lagallens.presentation.feature.creditstore.hub.ui.CreditHubActivity
import com.example.lagallens.presentation.feature.upload.ui.UploadSourceActivity

class MainActivity : AppCompatActivity() {
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val premiumSticker by lazy { FloatingPremiumSticker(this) }
    private var isResumed = false
    private var hasSelectedTab = false
    private var passwordChangePending = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as LegalLensApplication
        if (app.currentSessionKey() == null) {
            openAuthentication()
            return
        }
        enableEdgeToEdge()
        setContentView(binding.root)
        lifecycleScope.launch {
            app.observeSessionKeys().collect { session ->
                if (session == null && !passwordChangePending) openAuthentication()
            }
        }
        binding.mainNavHost.applySystemBarInsets(top = true, bottom = false)
        binding.bottomNavigationContainer.applySystemBarInsets(
            top = false,
            bottom = true,
            expandHeightForBottomInset = true
        )
        bindPremiumSticker()

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.mainNavHost) as NavHostFragment
        val navController = navHostFragment.navController

        bindBottomNavigation(navController)
        binding.btnAddContract.bindPressFeedback()
        binding.btnAddContract.setOnClickListener {
            startActivity(Intent(this, UploadSourceActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP))
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onResumeFragments() {
        super.onResumeFragments()
        val contractId = intent.getStringExtra(EXTRA_UPLOADED_CONTRACT) ?: return
        val session = intent.getStringExtra(EXTRA_UPLOAD_SESSION)
        intent.removeExtra(EXTRA_UPLOADED_CONTRACT)
        intent.removeExtra(EXTRA_UPLOAD_SESSION)
        if (session == null || (application as LegalLensApplication).currentSessionKey() != session) return
        val host = supportFragmentManager.findFragmentById(R.id.mainNavHost) as? NavHostFragment ?: return
        host.navController.navigate(
            R.id.contractDetailFragment,
            Bundle().apply { putString(ContractDetailFragment.ARG_CONTRACT_ID, contractId) }
        )
    }

    internal fun setPasswordChangePending(pending: Boolean) {
        if (passwordChangePending == pending) return
        passwordChangePending = pending
        if (!pending && !isChangingConfigurations && (application as LegalLensApplication).currentSessionKey() == null) {
            openAuthentication()
        }
    }

    internal fun openAuthentication() {
        if (isFinishing) return
        val app = application as LegalLensApplication
        val destination = if (app.usesBackendAuthentication) LoginActivity::class.java else HomeActivity::class.java
        startActivity(
            Intent(this, destination).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
    }

    override fun onResume() {
        super.onResume()
        isResumed = true
        premiumSticker.setMotionActive(binding.premiumStickerLayer.isVisible)
    }

    override fun onPause() {
        isResumed = false
        premiumSticker.setMotionActive(false)
        super.onPause()
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
            updateTab(binding.navHome, destination.id == R.id.homeDashboardFragment)
            updateTab(binding.navContracts, destination.id == R.id.contractListFragment ||
                destination.id == R.id.contractSearchFragment || isContractDetail)
            updateTab(binding.navNotifications, destination.id == R.id.notificationFragment)
            updateTab(binding.navProfile, destination.id == R.id.profileFragment)
            hasSelectedTab = true
            binding.premiumStickerLayer.isVisible = destination.id in stickerDestinationIds
            premiumSticker.setMotionActive(isResumed && binding.premiumStickerLayer.isVisible)
        }
    }

    private fun updateTab(tab: LinearLayout, selected: Boolean) {
        val changed = tab.isSelected != selected
        tab.isSelected = selected
        if (!changed) return
        val icon: View = tab.getChildAt(0)
        icon.animate().cancel()
        icon.scaleX = 1f
        icon.scaleY = 1f
        if (selected && hasSelectedTab && MotionTokens.areAnimationsEnabled()) {
            icon.scaleX = 0.95f
            icon.scaleY = 0.95f
            icon.animate().scaleX(1f).scaleY(1f)
                .setDuration(MotionTokens.STANDARD_DURATION_MILLIS)
                .setInterpolator(MotionTokens.linearOutSlowIn).start()
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
            startActivity(CreditHubActivity.createIntent(this).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP))
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
        if (destinationId == navController.graph.findStartDestination().id &&
            navController.popBackStack(destinationId, false)
        ) return
        navController.navigate(
            destinationId,
            null,
            navOptions {
                launchSingleTop = true
                restoreState = true
                anim { enter = R.anim.anim_main_tab_enter }
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
            }
        )
    }

    companion object {
        private const val EXTRA_UPLOADED_CONTRACT = "uploaded_contract"
        private const val EXTRA_UPLOAD_SESSION = "upload_owner_session"

        fun contractDetailIntent(context: Context, contractId: String, session: String): Intent =
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(EXTRA_UPLOADED_CONTRACT, contractId)
                .putExtra(EXTRA_UPLOAD_SESSION, session)

        private val stickerDestinationIds = setOf(
            R.id.contractListFragment,
            R.id.notificationFragment
        )
    }
}

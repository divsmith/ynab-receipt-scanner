package com.ynab.receiptscanner.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.ynab.receiptscanner.R
import com.ynab.receiptscanner.databinding.ActivityMainBinding
import com.ynab.receiptscanner.ui.auth.AuthActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main activity with single-activity architecture using Navigation Component
 * Handles bottom navigation, OAuth callbacks, and authentication state
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val viewModel: MainViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupNavigation()
        observeAuthState()
        handleDeepLink(intent)
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }
    
    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        
        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        
        // Top-level destinations (no back button in toolbar)
        val appBarConfiguration = AppBarConfiguration(
            setOf(R.id.homeFragment, R.id.cameraFragment, R.id.settingsFragment)
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        
        // Setup bottom navigation
        binding.bottomNavigation.setupWithNavController(navController)
        
        // Hide bottom nav on non-top-level destinations
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.homeFragment, R.id.cameraFragment, R.id.settingsFragment -> {
                    binding.bottomNavigation.visibility = View.VISIBLE
                }
                else -> {
                    binding.bottomNavigation.visibility = View.GONE
                }
            }
        }
    }
    
    private fun observeAuthState() {
        viewModel.isAuthenticated.observe(this) { isAuthenticated ->
            if (!isAuthenticated) {
                navigateToAuth()
            }
        }
        
        viewModel.checkAuthStatus()
    }
    
    private fun navigateToAuth() {
        val intent = Intent(this, AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    /**
     * Handle OAuth callback deep link
     */
    private fun handleDeepLink(intent: Intent?) {
        val data: Uri? = intent?.data
        if (data != null && data.scheme == "ynab-receipt-scanner" && data.host == "oauth") {
            // Extract authorization code
            val code = data.getQueryParameter("code")
            if (code != null) {
                viewModel.handleOAuthCallback(code)
            }
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}

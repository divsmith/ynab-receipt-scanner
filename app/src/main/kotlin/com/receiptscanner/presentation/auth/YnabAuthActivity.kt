package com.receiptscanner.presentation.auth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import com.receiptscanner.BuildConfig
import com.receiptscanner.R
import com.receiptscanner.databinding.ActivityYnabAuthBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Activity handling YNAB OAuth 2.0 authentication flow
 * 
 * This activity:
 * 1. Initiates OAuth flow by opening YNAB authorization URL in Chrome Custom Tab
 * 2. Handles OAuth callback via custom URL scheme (receiptscanner://oauth/callback)
 * 3. Exchanges authorization code for access token
 * 
 * OAuth credentials are configured via gradle.properties (see OAUTH_SETUP.md for details)
 */
@AndroidEntryPoint
class YnabAuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityYnabAuthBinding
    private val viewModel: YnabAuthViewModel by viewModels()

    companion object {
        // OAuth credentials loaded from BuildConfig (configured in gradle.properties)
        val YNAB_CLIENT_ID: String get() = BuildConfig.YNAB_CLIENT_ID
        val YNAB_CLIENT_SECRET: String get() = BuildConfig.YNAB_CLIENT_SECRET
        val REDIRECT_URI: String get() = BuildConfig.YNAB_REDIRECT_URI
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityYnabAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeAuthState()
        
        // Check if this is a callback from OAuth
        handleOAuthCallback(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOAuthCallback(intent)
    }

    private fun setupUI() {
        binding.btnConnectYnab.setOnClickListener {
            startOAuthFlow()
        }

        binding.btnCheckAuthStatus.setOnClickListener {
            viewModel.checkAuthStatus()
        }

        binding.btnLogout.setOnClickListener {
            viewModel.logout()
            updateUI(YnabAuthState.Idle)
        }
    }

    private fun observeAuthState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.authState.collect { state ->
                    updateUI(state)
                }
            }
        }
    }

    private fun updateUI(state: YnabAuthState) {
        when (state) {
            is YnabAuthState.Idle -> {
                binding.tvStatus.text = getString(R.string.auth_status_not_connected)
                binding.btnConnectYnab.isEnabled = true
                binding.btnCheckAuthStatus.isEnabled = true
                binding.btnLogout.isEnabled = false
                binding.progressBar.visibility = View.GONE
            }
            is YnabAuthState.Loading -> {
                binding.tvStatus.text = getString(R.string.auth_status_loading)
                binding.btnConnectYnab.isEnabled = false
                binding.btnCheckAuthStatus.isEnabled = false
                binding.progressBar.visibility = View.VISIBLE
            }
            is YnabAuthState.AuthorizationReady -> {
                // Open authorization URL in Chrome Custom Tab
                openAuthorizationUrl(state.authorizationUrl)
            }
            is YnabAuthState.Authenticated -> {
                binding.tvStatus.text = getString(
                    R.string.auth_status_connected,
                    state.token.accessToken.take(10) + "..."
                )
                binding.btnConnectYnab.isEnabled = false
                binding.btnCheckAuthStatus.isEnabled = true
                binding.btnLogout.isEnabled = true
                binding.progressBar.visibility = View.GONE
                
                Snackbar.make(
                    binding.root,
                    R.string.auth_success,
                    Snackbar.LENGTH_SHORT
                ).show()
            }
            is YnabAuthState.Error -> {
                binding.tvStatus.text = getString(R.string.auth_status_error, state.message)
                binding.btnConnectYnab.isEnabled = true
                binding.btnCheckAuthStatus.isEnabled = true
                binding.btnLogout.isEnabled = false
                binding.progressBar.visibility = View.GONE
                
                Snackbar.make(
                    binding.root,
                    state.message,
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun startOAuthFlow() {
        viewModel.startOAuthFlow(
            clientId = YNAB_CLIENT_ID,
            redirectUri = REDIRECT_URI
        )
    }

    private fun openAuthorizationUrl(url: String) {
        try {
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
            
            customTabsIntent.launchUrl(this, Uri.parse(url))
        } catch (e: Exception) {
            Timber.e(e, "Error opening authorization URL")
            // Fallback to browser
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                Snackbar.make(
                    binding.root,
                    R.string.auth_error_no_browser,
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun handleOAuthCallback(intent: Intent?) {
        val data = intent?.data
        if (data != null && data.scheme == "receiptscanner" && data.host == "oauth") {
            val code = data.getQueryParameter("code")
            val state = data.getQueryParameter("state")
            val error = data.getQueryParameter("error")

            when {
                error != null -> {
                    Timber.e("OAuth error: $error")
                    Snackbar.make(
                        binding.root,
                        getString(R.string.auth_error_oauth, error),
                        Snackbar.LENGTH_LONG
                    ).show()
                }
                code != null && state != null -> {
                    // Exchange code for token
                    viewModel.handleAuthCallback(
                        code = code,
                        state = state,
                        clientId = YNAB_CLIENT_ID,
                        clientSecret = YNAB_CLIENT_SECRET,
                        redirectUri = REDIRECT_URI
                    )
                }
                else -> {
                    Timber.w("Invalid OAuth callback: missing code or state")
                }
            }
        }
    }
}

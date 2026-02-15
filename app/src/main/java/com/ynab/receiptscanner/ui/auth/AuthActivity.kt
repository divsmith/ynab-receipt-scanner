package com.ynab.receiptscanner.ui.auth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.ynab.receiptscanner.R
import com.ynab.receiptscanner.databinding.ActivityAuthBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Activity for handling YNAB OAuth authentication
 * Uses Chrome Custom Tabs for OAuth flow
 */
@AndroidEntryPoint
class AuthActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityAuthBinding
    private val viewModel: AuthViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        observeAuthState()
        handleDeepLink(intent)
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleDeepLink(it) }
    }
    
    private fun setupUI() {
        binding.btnSignInOauth.setOnClickListener {
            startOAuthFlow()
        }
        
        binding.btnSignInPat.setOnClickListener {
            showPersonalAccessTokenDialog()
        }
        
        binding.btnRetry.setOnClickListener {
            viewModel.clearError()
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
    
    private fun updateUI(state: AuthState) {
        when (state) {
            is AuthState.SignedOut -> {
                binding.progressBar.visibility = View.GONE
                binding.layoutSignedOut.visibility = View.VISIBLE
                binding.layoutError.visibility = View.GONE
                binding.tvStatus.text = getString(R.string.auth_signed_out)
            }
            
            is AuthState.SigningIn -> {
                binding.progressBar.visibility = View.VISIBLE
                binding.layoutSignedOut.visibility = View.GONE
                binding.layoutError.visibility = View.GONE
                binding.tvStatus.text = getString(R.string.auth_signing_in)
            }
            
            is AuthState.SignedIn -> {
                binding.progressBar.visibility = View.GONE
                binding.layoutSignedOut.visibility = View.GONE
                binding.layoutError.visibility = View.GONE
                
                val message = if (state.usingPersonalAccessToken) {
                    "Signed in with Personal Access Token"
                } else {
                    "Signed in with OAuth"
                }
                
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                
                // Return to previous activity or finish
                finish()
            }
            
            is AuthState.Error -> {
                binding.progressBar.visibility = View.GONE
                binding.layoutSignedOut.visibility = View.GONE
                binding.layoutError.visibility = View.VISIBLE
                binding.tvError.text = state.message
            }
        }
    }
    
    private fun startOAuthFlow() {
        val authUrl = viewModel.startOAuthFlow()
        
        if (authUrl != null) {
            // Open in Chrome Custom Tabs
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
            
            try {
                customTabsIntent.launchUrl(this, Uri.parse(authUrl))
            } catch (e: Exception) {
                // Fallback to regular browser
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
                if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent)
                } else {
                    Toast.makeText(
                        this,
                        "No browser found to open authentication page",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    
    private fun showPersonalAccessTokenDialog() {
        val editText = android.widget.EditText(this).apply {
            hint = "Enter your Personal Access Token"
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        
        AlertDialog.Builder(this)
            .setTitle("Personal Access Token")
            .setMessage("For testing purposes only. Get your PAT from:\nhttps://app.youneedabudget.com/settings/developer")
            .setView(editText)
            .setPositiveButton("Sign In") { _, _ ->
                val token = editText.text.toString().trim()
                if (token.isNotBlank()) {
                    viewModel.setPersonalAccessToken(token)
                } else {
                    Toast.makeText(this, "Token cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun handleDeepLink(intent: Intent) {
        val data = intent.data ?: return
        
        // Check if this is an OAuth callback
        if (data.scheme == "ynabreceipt" && data.host == "oauth" && data.path == "/callback") {
            val code = data.getQueryParameter("code")
            val error = data.getQueryParameter("error")
            
            when {
                code != null -> {
                    // Handle successful authorization
                    viewModel.handleOAuthCallback(code)
                }
                error != null -> {
                    // Handle OAuth error
                    Toast.makeText(
                        this,
                        "Authentication cancelled or failed: $error",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}

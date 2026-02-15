package com.ynab.receiptscanner.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.ynab.receiptscanner.R
import com.ynab.receiptscanner.databinding.ActivityOnboardingBinding
import com.ynab.receiptscanner.ui.auth.AuthActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * Onboarding activity for first-time users
 * Shows introduction pages with ViewPager2
 */
@AndroidEntryPoint
class OnboardingActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityOnboardingBinding
    private val viewModel: OnboardingViewModel by viewModels()
    
    private lateinit var pagerAdapter: OnboardingPagerAdapter
    private val pages = listOf(
        OnboardingPage(
            imageRes = R.drawable.onboarding_scan,
            title = "Scan Receipts",
            description = "Capture receipts with your camera and extract transaction details automatically."
        ),
        OnboardingPage(
            imageRes = R.drawable.onboarding_review,
            title = "Review & Edit",
            description = "Verify extracted information and make corrections before syncing to YNAB."
        ),
        OnboardingPage(
            imageRes = R.drawable.onboarding_sync,
            title = "Auto-Sync to YNAB",
            description = "Seamlessly sync transactions to your YNAB budget with a single tap."
        )
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupViewPager()
        setupListeners()
        observeViewModel()
    }
    
    private fun setupViewPager() {
        pagerAdapter = OnboardingPagerAdapter(pages)
        binding.viewPager.adapter = pagerAdapter
        
        // Setup page indicator
        TabLayoutMediator(binding.pageIndicator, binding.viewPager) { _, _ -> }.attach()
        
        // Page change callback
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateButtons(position)
            }
        })
        
        updateButtons(0)
    }
    
    private fun setupListeners() {
        binding.skipButton.setOnClickListener {
            viewModel.skipOnboarding()
        }
        
        binding.nextButton.setOnClickListener {
            val currentItem = binding.viewPager.currentItem
            if (currentItem < pages.size - 1) {
                binding.viewPager.currentItem = currentItem + 1
            } else {
                viewModel.completeOnboarding()
            }
        }
    }
    
    private fun updateButtons(position: Int) {
        if (position == pages.size - 1) {
            binding.skipButton.visibility = View.GONE
            binding.nextButton.text = getString(R.string.get_started)
        } else {
            binding.skipButton.visibility = View.VISIBLE
            binding.nextButton.text = getString(R.string.next)
        }
    }
    
    private fun observeViewModel() {
        viewModel.navigateToAuth.observe(this) { shouldNavigate ->
            if (shouldNavigate) {
                navigateToAuth()
            }
        }
    }
    
    private fun navigateToAuth() {
        val intent = Intent(this, AuthActivity::class.java)
        startActivity(intent)
        finish()
    }
}

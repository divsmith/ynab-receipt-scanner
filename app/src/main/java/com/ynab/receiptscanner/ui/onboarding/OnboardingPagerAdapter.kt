package com.ynab.receiptscanner.ui.onboarding

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ynab.receiptscanner.databinding.PageOnboardingBinding

/**
 * ViewPager2 adapter for onboarding pages
 */
class OnboardingPagerAdapter(
    private val pages: List<OnboardingPage>
) : RecyclerView.Adapter<OnboardingPagerAdapter.OnboardingViewHolder>() {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
        val binding = PageOnboardingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OnboardingViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
        holder.bind(pages[position])
    }
    
    override fun getItemCount(): Int = pages.size
    
    class OnboardingViewHolder(
        private val binding: PageOnboardingBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(page: OnboardingPage) {
            binding.onboardingImage.setImageResource(page.imageRes)
            binding.onboardingTitle.text = page.title
            binding.onboardingDescription.text = page.description
        }
    }
}

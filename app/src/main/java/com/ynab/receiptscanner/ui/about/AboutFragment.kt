package com.ynab.receiptscanner.ui.about

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.ynab.receiptscanner.BuildConfig
import com.ynab.receiptscanner.databinding.FragmentAboutBinding

/**
 * About screen showing app information, version, and links
 */
class AboutFragment : Fragment() {
    
    private var _binding: FragmentAboutBinding? = null
    private val binding get() = _binding!!
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAboutBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }
    
    private fun setupViews() {
        // Display app version
        binding.textVersion.text = "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
        
        // Privacy Policy link
        binding.buttonPrivacyPolicy.setOnClickListener {
            openUrl("https://ynab-receipt-scanner.example.com/privacy")
        }
        
        // Terms of Service link
        binding.buttonTermsOfService.setOnClickListener {
            openUrl("https://ynab-receipt-scanner.example.com/terms")
        }
        
        // GitHub repository link
        binding.buttonGithub.setOnClickListener {
            openUrl("https://github.com/example/ynab-receipt-scanner")
        }
        
        // Open source licenses
        binding.buttonLicenses.setOnClickListener {
            // TODO: Implement licenses dialog or navigate to licenses screen
            openUrl("https://github.com/example/ynab-receipt-scanner/blob/main/LICENSE")
        }
        
        // Contact/Support email
        binding.buttonContact.setOnClickListener {
            sendEmail(
                to = "support@ynab-receipt-scanner.example.com",
                subject = "YNAB Receipt Scanner Support"
            )
        }
        
        // YNAB website (not affiliated disclaimer)
        binding.buttonYnabWebsite.setOnClickListener {
            openUrl("https://www.youneedabudget.com")
        }
    }
    
    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            // Handle case where no browser is available
            e.printStackTrace()
        }
    }
    
    private fun sendEmail(to: String, subject: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf(to))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(
                    Intent.EXTRA_TEXT,
                    """
                    App Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})
                    Android Version: ${android.os.Build.VERSION.RELEASE}
                    Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}
                    
                    Please describe your issue or feedback below:
                    
                    """.trimIndent()
                )
            }
            startActivity(Intent.createChooser(intent, "Send Email"))
        } catch (e: Exception) {
            // Handle case where no email client is available
            e.printStackTrace()
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

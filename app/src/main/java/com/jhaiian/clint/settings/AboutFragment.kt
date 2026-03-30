package com.jhaiian.clint.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.jhaiian.clint.R
import com.jhaiian.clint.databinding.FragmentAboutBinding

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
        populateVersionInfo()
        setupLinks()
    }

    private fun populateVersionInfo() {
        val pInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
        val versionName = pInfo.versionName
        val versionCode = pInfo.longVersionCode
        val arch = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"
        binding.tvVersionInfo.text = getString(R.string.about_version_info, versionName, versionCode, arch)
    }

    private fun setupLinks() {
        makeClickable(binding.tvAuthorLink, "https://linktr.ee/jhaiian")
        makeClickable(binding.tvGithubLink, "https://github.com/jhaiian/Clint-Browser")
        makeClickable(binding.tvPrivacyPolicyLink, "https://github.com/jhaiian/Clint-Browser/blob/main/PRIVACY_POLICY.md")
        makeClickable(binding.tvTermsLink, "https://github.com/jhaiian/Clint-Browser/blob/main/TERMS_OF_SERVICE.md")
        makeClickable(binding.tvDiscordLink, "https://discord.gg/4kUe4yPQ32")
        makeClickable(binding.tvKofiLink, "https://ko-fi.com/jhaiian")
        makeClickable(binding.tvLicenseLink, "https://www.gnu.org/licenses/gpl-3.0.html")
        makeClickable(binding.tvContactEmail, "mailto:jhaiianbetter@gmail.com")
        makeClickable(binding.tvContributorsLink, "https://github.com/jhaiian/Clint-Browser/blob/main/Contributors.md")
    }

    private fun makeClickable(view: TextView, url: String) {
        view.setOnClickListener {
            val intent = if (url.startsWith("mailto:")) {
                Intent(Intent.ACTION_SENDTO, Uri.parse(url)).apply {
                    putExtra(Intent.EXTRA_SUBJECT, "Clint Browser")
                }
            } else {
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
            }
            runCatching { startActivity(intent) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

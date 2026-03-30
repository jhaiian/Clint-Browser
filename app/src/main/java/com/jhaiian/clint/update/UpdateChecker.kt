package com.jhaiian.clint.update

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.BulletSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.graphics.Typeface
import android.widget.TextView
import android.widget.ScrollView
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jhaiian.clint.R
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.Executors

object UpdateChecker {

    private const val STABLE_URL =
        "https://raw.githubusercontent.com/jhaiian/Clint-Browser/main/Update/Stable.json"
    private const val BETA_URL =
        "https://raw.githubusercontent.com/jhaiian/Clint-Browser/main/Update/Beta.json"

    private val executor = Executors.newSingleThreadExecutor()
    private val client = OkHttpClient()

    fun check(activity: Activity, isBeta: Boolean, silent: Boolean) {
        val url = if (isBeta) BETA_URL else STABLE_URL
        executor.submit {
            try {
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: return@submit
                val json = JSONObject(body)
                val remoteVersion = json.getString("version")
                val remoteVersionCode = json.getLong("versionCode")
                val changelog = json.getString("changelog")
                val downloads = json.getJSONObject("downloads")

                val arch = getDeviceArch()
                val downloadUrl = downloads.optString(arch).takeIf { it.isNotEmpty() }
                    ?: downloads.optString("universal").takeIf { it.isNotEmpty() }

                val currentVersionCode = activity.packageManager
                    .getPackageInfo(activity.packageName, 0).longVersionCode

                val hasUpdate = remoteVersionCode > currentVersionCode

                activity.runOnUiThread {
                    if (hasUpdate) {
                        showUpdateDialog(activity, remoteVersion, changelog, downloadUrl, isBeta)
                    } else if (!silent) {
                        showNoUpdateDialog(activity)
                    }
                }
            } catch (_: Exception) {
                if (!silent) {
                    activity.runOnUiThread { showErrorDialog(activity) }
                }
            }
        }
    }

    private fun showUpdateDialog(
        activity: Activity,
        version: String,
        rawChangelog: String,
        downloadUrl: String?,
        isBeta: Boolean
    ) {
        val channelLabel = if (isBeta) " (Beta)" else ""
        val changelog = extractLatestChangelog(rawChangelog)

        val scrollView = ScrollView(activity).apply {
            val tv = TextView(activity).apply {
                text = buildChangelogSpannable(changelog)
                setPadding(64, 24, 64, 24)
                setTextColor(0xCCFFFFFF.toInt())
                textSize = 13f
                movementMethod = LinkMovementMethod.getInstance()
            }
            addView(tv)
        }

        MaterialAlertDialogBuilder(activity, R.style.ThemeOverlay_ClintBrowser_Dialog)
            .setTitle("v$version$channelLabel available")
            .setView(scrollView)
            .setNegativeButton("Later", null)
            .apply {
                if (!downloadUrl.isNullOrEmpty()) {
                    setPositiveButton("Download") { _, _ ->
                        activity.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
                        )
                    }
                } else {
                    setPositiveButton("View on GitHub") { _, _ ->
                        activity.startActivity(
                            Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://github.com/jhaiian/Clint-Browser/releases"))
                        )
                    }
                }
            }
            .show()
    }

    private fun showNoUpdateDialog(activity: Activity) {
        MaterialAlertDialogBuilder(activity, R.style.ThemeOverlay_ClintBrowser_Dialog)
            .setTitle("You're up to date")
            .setMessage("Clint Browser is running the latest version.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showErrorDialog(activity: Activity) {
        MaterialAlertDialogBuilder(activity, R.style.ThemeOverlay_ClintBrowser_Dialog)
            .setTitle("Update check failed")
            .setMessage("Could not reach the update server. Please check your connection and try again.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun extractLatestChangelog(raw: String): String {
        val lines = raw.split("\n")
        val result = mutableListOf<String>()
        var inSection = false
        for (line in lines) {
            if (line.trimStart().startsWith("## ")) {
                if (inSection) break
                inSection = true
                result.add(line)
            } else if (inSection) {
                result.add(line)
            }
        }
        return result.joinToString("\n").trim()
    }

    private fun buildChangelogSpannable(markdown: String): SpannableStringBuilder {
        val sb = SpannableStringBuilder()
        for (line in markdown.split("\n")) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("## ") -> {
                    val text = trimmed.removePrefix("## ").trim() + "\n"
                    val start = sb.length
                    sb.append(text)
                    sb.setSpan(StyleSpan(Typeface.BOLD), start, sb.length - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    sb.setSpan(RelativeSizeSpan(1.15f), start, sb.length - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                trimmed.startsWith("### ") -> {
                    val text = trimmed.removePrefix("### ").trim() + "\n"
                    val start = sb.length
                    sb.append(text)
                    sb.setSpan(StyleSpan(Typeface.BOLD), start, sb.length - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                trimmed.startsWith("- ") -> {
                    val text = trimmed.removePrefix("- ").trim() + "\n"
                    val start = sb.length
                    sb.append(text)
                    sb.setSpan(BulletSpan(16), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                trimmed.isEmpty() -> sb.append("\n")
                else -> sb.append(trimmed + "\n")
            }
        }
        return sb
    }

    private fun getDeviceArch(): String {
        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: return "universal"
        return when {
            abi.contains("arm64") -> "arm64-v8a"
            abi.contains("armeabi") -> "armeabi-v7a"
            abi.contains("x86_64") -> "x86_64"
            abi.contains("x86") -> "x86"
            else -> "universal"
        }
    }


}

package com.spotifyhub.auth

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

object AuthLauncher {
    enum class LaunchMode {
        CustomTabs,
        BrowserIntent,
        Unavailable,
    }

    fun launch(context: Context, uri: Uri): LaunchMode {
        return try {
            CustomTabsIntent.Builder().build().launchUrl(context, uri)
            LaunchMode.CustomTabs
        } catch (_: ActivityNotFoundException) {
            val fallbackIntent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return if (fallbackIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(fallbackIntent)
                LaunchMode.BrowserIntent
            } else {
                LaunchMode.Unavailable
            }
        }
    }
}


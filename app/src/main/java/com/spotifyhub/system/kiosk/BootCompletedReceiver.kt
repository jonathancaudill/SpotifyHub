package com.spotifyhub.system.kiosk

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.spotifyhub.app.MainActivity

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(launchIntent)
    }
}


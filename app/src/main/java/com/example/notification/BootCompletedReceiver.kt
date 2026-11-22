package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.repository.GateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = GateRepository(context.applicationContext)
                val profile = repo.getLatestProfile()
                if (profile?.unlockMonitorEnabled == true &&
                    QuizNotificationHelper.hasNotificationPermission(context)
                ) {
                    UnlockMonitorService.start(context.applicationContext)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

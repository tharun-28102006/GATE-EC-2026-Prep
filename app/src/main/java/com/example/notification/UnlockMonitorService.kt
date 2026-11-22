package com.example.notification

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.repository.GateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private const val TAG = "UnlockMonitorService"

/**
 * Ultra-low-memory foreground monitor listening exclusively for Intent.ACTION_USER_PRESENT
 * (device unlock) with zero polling loops. Delivers a non-repeating GATE 2026 EC quiz notification
 * directly from the local Room cache.
 */
class UnlockMonitorService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var repository: GateRepository

    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_USER_PRESENT) {
                handleDeviceUnlocked()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        repository = GateRepository(applicationContext)

        val notification = QuizNotificationHelper.buildMonitorNotification(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                QuizNotificationHelper.NOTIF_ID_MONITOR,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(QuizNotificationHelper.NOTIF_ID_MONITOR, notification)
        }

        val filter = IntentFilter(Intent.ACTION_USER_PRESENT)
        ContextCompat.registerReceiver(
            this,
            unlockReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        Log.d(TAG, "Unlock quiz monitor started and listening for ACTION_USER_PRESENT.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(unlockReceiver)
        } catch (_: IllegalArgumentException) {
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun handleDeviceUnlocked() {
        serviceScope.launch {
            try {
                val profile = repository.getLatestProfile()
                if (profile != null && !profile.unlockMonitorEnabled) {
                    stopSelf()
                    return@launch
                }

                val cooldownMs = (profile?.unlockCooldownMinutes ?: 0) * 60_000L
                val elapsed = System.currentTimeMillis() - (profile?.lastUnlockNotifMs ?: 0L)
                if (cooldownMs > 0L && elapsed < cooldownMs) {
                    Log.d(TAG, "Unlock quiz skipped due to cooldown (${cooldownMs / 60000}m).")
                    return@launch
                }

                val question = repository.popNextOfflineQuestionForUnlock() ?: return@launch
                QuizNotificationHelper.postUnlockQuizNotification(applicationContext, question)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to post unlock quiz notification", e)
            }
        }
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, UnlockMonitorService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, UnlockMonitorService::class.java))
        }
    }
}

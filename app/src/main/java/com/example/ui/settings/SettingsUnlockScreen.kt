package com.example.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.StudyProfileEntity
import com.example.notification.QuizNotificationHelper
import com.example.ui.theme.GateIcons
import com.example.ui.theme.LocalGateColors

@Composable
fun SettingsUnlockScreen(
    profile: StudyProfileEntity,
    userEmail: String,
    unseenPoolCount: Int,
    isRefillingPool: Boolean,
    onToggleUnlockMonitor: (Boolean) -> Unit,
    onChangeCooldownMinutes: (Int) -> Unit,
    onSendTestNotification: () -> Unit,
    onChangeDailyGoal: (Int) -> Unit,
    onToggleDarkTheme: (Boolean) -> Unit,
    onRefillOfflinePool: () -> Unit,
    onSyncCloudNow: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val gateColors = LocalGateColors.current
    var pendingEnableAfterPermission by remember { mutableStateOf(false) }
    var pendingTestAfterPermission by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            if (pendingEnableAfterPermission) {
                pendingEnableAfterPermission = false
                onToggleUnlockMonitor(true)
            }
            if (pendingTestAfterPermission) {
                pendingTestAfterPermission = false
                onSendTestNotification()
            }
        } else {
            pendingEnableAfterPermission = false
            pendingTestAfterPermission = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Surface(
            color = gateColors.examHeader,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Unlock Quiz & Cloud Sync",
                        style = MaterialTheme.typography.titleLarge,
                        color = gateColors.examHeaderContent,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Lock-screen notifications, offline cache & account",
                        style = MaterialTheme.typography.labelSmall,
                        color = gateColors.examHeaderContent.copy(alpha = 0.85f)
                    )
                }
                Icon(
                    imageVector = GateIcons.Tune,
                    contentDescription = "Unlock and sync settings",
                    tint = gateColors.examHeaderContent
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Phone-Unlock Lock-Screen Quiz Card
            item {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, gateColors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = GateIcons.NotificationsActive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text(
                                        text = "Ask on Every Phone Unlock",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Posts a lock-screen interactive GATE EC quiz (inline A–D reply) from local cache with minimal RAM.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Switch(
                                checked = profile.unlockMonitorEnabled,
                                onCheckedChange = { wantEnabled ->
                                    if (wantEnabled && !QuizNotificationHelper.hasNotificationPermission(context)) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            pendingEnableAfterPermission = true
                                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    } else {
                                        onToggleUnlockMonitor(wantEnabled)
                                    }
                                },
                                modifier = Modifier.testTag("unlock_monitor_switch")
                            )
                        }

                        HorizontalDivider(color = gateColors.border)

                        Text(
                            text = "Notification Cooldown",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                0 to "Every unlock",
                                5 to "5m",
                                15 to "15m",
                                30 to "30m"
                            ).forEach { (mins, label) ->
                                FilterChip(
                                    selected = profile.unlockCooldownMinutes == mins,
                                    onClick = { onChangeCooldownMinutes(mins) },
                                    label = { Text(label) }
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (!QuizNotificationHelper.hasNotificationPermission(context) &&
                                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                                ) {
                                    pendingTestAfterPermission = true
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    onSendTestNotification()
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("send_test_unlock_quiz_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Send Test Lock-Screen Quiz Now")
                        }
                    }
                }
            }

            // 2. Exam Desk Preferences
            item {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, gateColors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Exam Desk Preferences",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Daily Target Questions",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${profile.dailyGoal} Qs / day",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = profile.dailyGoal.toFloat(),
                            onValueChange = { onChangeDailyGoal(it.toInt()) },
                            valueRange = 5f..40f,
                            steps = 6
                        )

                        HorizontalDivider(color = gateColors.border)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = GateIcons.DarkMode,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Column {
                                    Text(
                                        text = "Night Exam Dark Mode",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Low-glare high-contrast palette for late-night revision",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = profile.darkExamTheme,
                                onCheckedChange = onToggleDarkTheme,
                                modifier = Modifier.testTag("dark_exam_theme_switch")
                            )
                        }
                    }
                }
            }

            // 3. Offline Pool & Multi-Device Cloud Sync
            item {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, gateColors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = GateIcons.CloudDone,
                                contentDescription = null,
                                tint = gateColors.answered
                            )
                            Column {
                                Text(
                                    text = "Cloud Sync & Offline Cache",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Signed in as ${userEmail.ifBlank { profile.displayName }} • $unseenPoolCount questions ready offline",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onRefillOfflinePool,
                                enabled = !isRefillingPool,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Icon(GateIcons.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (isRefillingPool) "Caching..." else "Refill AI")
                            }
                            OutlinedButton(
                                onClick = onSyncCloudNow,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("sync_cloud_now_button")
                            ) {
                                Icon(GateIcons.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Sync Now")
                            }
                        }

                        HorizontalDivider(color = gateColors.border)

                        OutlinedButton(
                            onClick = onSignOut,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, gateColors.wrong.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("sign_out_button")
                        ) {
                            Icon(
                                imageVector = GateIcons.Logout,
                                contentDescription = null,
                                tint = gateColors.wrong,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Sign Out",
                                color = gateColors.wrong
                            )
                        }
                    }
                }
            }
        }
    }
}

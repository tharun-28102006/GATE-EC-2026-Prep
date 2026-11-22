package com.example

import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.remote.configureAppCheck
import com.example.notification.QuizNotificationHelper
import com.example.notification.UnlockMonitorService
import com.example.ui.GateViewModel
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOutSafely
import com.example.ui.practice.PracticeDeskScreen
import com.example.ui.progress.ProgressTrackerScreen
import com.example.ui.saved.SavedOfflineScreen
import com.example.ui.settings.SettingsUnlockScreen
import com.example.ui.theme.GateExamTheme
import com.example.ui.theme.GateIcons
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

private enum class GateTab(
    val routeId: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    PRACTICE("practice_desk", "Practice", GateIcons.MenuBook, GateIcons.MenuBook),
    SAVED("saved_offline", "Saved", GateIcons.Bookmark, GateIcons.BookmarkBorder),
    PROGRESS("progress_tracker", "Progress", GateIcons.BarChart, GateIcons.BarChart),
    UNLOCK("unlock_sync", "Unlock", GateIcons.NotificationsActive, GateIcons.NotificationsActiveOutlined)
}

class MainActivity : ComponentActivity() {

    private var deepLinkQuestionId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure Firebase App Check with launch intent BEFORE Compose UI or ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        QuizNotificationHelper.ensureChannels(this)
        deepLinkQuestionId = intent?.getStringExtra(QuizNotificationHelper.EXTRA_OPEN_QUESTION_ID)

        setContent {
            GateRootApp(
                deepLinkQuestionId = deepLinkQuestionId,
                onDeepLinkConsumed = { deepLinkQuestionId = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val qId = intent.getStringExtra(QuizNotificationHelper.EXTRA_OPEN_QUESTION_ID)
        if (!qId.isNullOrBlank()) {
            deepLinkQuestionId = qId
        }
    }
}

@Composable
private fun GateRootApp(
    deepLinkQuestionId: String?,
    onDeepLinkConsumed: () -> Unit
) {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    val user = currentUser
    if (user == null) {
        GateExamTheme(darkTheme = false) {
            AuthScreen(
                onAuthSuccess = {
                    currentUser = Firebase.auth.currentUser
                }
            )
        }
    } else {
        val uid = user.uid
        val displayName = user.displayName ?: "GATE EC Candidate"
        val userEmail = user.email.orEmpty()

        key(uid) {
            val context = LocalContext.current
            val application = context.applicationContext as Application
            val gateViewModel: GateViewModel = viewModel(
                key = uid,
                factory = GateViewModel.Factory(application, uid, displayName)
            )
            AuthenticatedGateApp(
                viewModel = gateViewModel,
                userEmail = userEmail,
                deepLinkQuestionId = deepLinkQuestionId,
                onDeepLinkConsumed = onDeepLinkConsumed
            )
        }
    }
}

@Composable
private fun AuthenticatedGateApp(
    viewModel: GateViewModel,
    userEmail: String,
    deepLinkQuestionId: String?,
    onDeepLinkConsumed: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val practiceState by viewModel.practiceState.collectAsStateWithLifecycle()
    val savedQuestions by viewModel.savedQuestions.collectAsStateWithLifecycle()
    val unseenPoolCount by viewModel.unseenPoolCount.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val progressSummary by viewModel.progressSummary.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(GateTab.PRACTICE) }

    // Return to Practice Desk on back press from secondary tabs
    BackHandler(enabled = currentTab != GateTab.PRACTICE) {
        currentTab = GateTab.PRACTICE
    }

    // Handle deep link from lock-screen quiz notification tap
    LaunchedEffect(deepLinkQuestionId) {
        if (!deepLinkQuestionId.isNullOrBlank()) {
            currentTab = GateTab.PRACTICE
            viewModel.openQuestionById(deepLinkQuestionId)
            onDeepLinkConsumed()
        }
    }

    GateExamTheme(darkTheme = profile.darkExamTheme) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavRail = maxWidth >= 600.dp

            val contentSlot: @Composable (Modifier) -> Unit = { contentMod ->
                Box(modifier = contentMod) {
                    when (currentTab) {
                        GateTab.PRACTICE -> PracticeDeskScreen(
                            practiceState = practiceState,
                            progressSummary = progressSummary,
                            unseenPoolCount = unseenPoolCount,
                            onSelectOption = viewModel::selectOption,
                            onClearSelection = viewModel::clearSelection,
                            onSubmitAnswer = viewModel::submitCurrentAnswer,
                            onNextQuestion = viewModel::loadNextQuestion,
                            onToggleSave = viewModel::toggleSaveCurrentQuestion,
                            onDismissBanner = viewModel::dismissBanner
                        )

                        GateTab.SAVED -> SavedOfflineScreen(
                            savedQuestions = savedQuestions,
                            unseenPoolCount = unseenPoolCount,
                            isRefillingPool = practiceState.isRefillingPool,
                            onRefillPool = viewModel::refillOfflineAiPool,
                            onRemoveSaved = viewModel::removeSavedQuestion,
                            onRecordRetry = viewModel::recordOfflineRetryAttempt
                        )

                        GateTab.PROGRESS -> ProgressTrackerScreen(
                            summary = progressSummary
                        )

                        GateTab.UNLOCK -> SettingsUnlockScreen(
                            profile = profile,
                            userEmail = userEmail,
                            unseenPoolCount = unseenPoolCount,
                            isRefillingPool = practiceState.isRefillingPool,
                            onToggleUnlockMonitor = viewModel::setUnlockMonitorEnabled,
                            onChangeCooldownMinutes = viewModel::setUnlockCooldownMinutes,
                            onSendTestNotification = viewModel::triggerTestUnlockNotification,
                            onChangeDailyGoal = viewModel::setDailyGoal,
                            onToggleDarkTheme = viewModel::setDarkExamTheme,
                            onRefillOfflinePool = viewModel::refillOfflineAiPool,
                            onSyncCloudNow = viewModel::syncCloudNow,
                            onSignOut = {
                                UnlockMonitorService.stop(context)
                                signOutSafely(credentialManager, scope)
                            }
                        )
                    }
                }
            }

            if (useNavRail) {
                Row(modifier = Modifier.fillMaxSize()) {
                    NavigationRail {
                        GateTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("nav_tab_${tab.routeId}")
                            )
                        }
                    }
                    contentSlot(Modifier.weight(1f).fillMaxSize())
                }
            } else {
                Scaffold(
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    bottomBar = {
                        NavigationBar {
                            GateTab.entries.forEach { tab ->
                                val selected = currentTab == tab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { currentTab = tab },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = { Text(tab.label) },
                                    modifier = Modifier.testTag("nav_tab_${tab.routeId}")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    contentSlot(
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}

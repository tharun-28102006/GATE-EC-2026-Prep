package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AttemptEntity
import com.example.data.local.CachedQuestionEntity
import com.example.data.local.StudyProfileEntity
import com.example.data.repository.GateRepository
import com.example.domain.GateEcSyllabus
import com.example.notification.QuizNotificationHelper
import com.example.notification.UnlockMonitorService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PracticeUiState(
    val isLoading: Boolean = true,
    val question: CachedQuestionEntity? = null,
    val selectedOptionIndex: Int? = null,
    val isSubmitted: Boolean = false,
    val lastScoreDelta: Double = 0.0,
    val elapsedSeconds: Int = 0,
    val isOnline: Boolean = true,
    val errorMessage: String? = null,
    val statusBanner: String? = null,
    val isRefillingPool: Boolean = false
)

data class DayActivityPoint(
    val dateKey: String,
    val label: String,
    val attempted: Int,
    val correct: Int,
    val netScore: Double
) {
    val accuracyPercent: Float
        get() = if (attempted == 0) 0f else (correct.toFloat() / attempted.toFloat()) * 100f
}

data class SectionMastery(
    val sectionId: String,
    val code: String,
    val name: String,
    val shortName: String,
    val attempted: Int,
    val correct: Int,
    val netScore: Double
) {
    val accuracyPercent: Int
        get() = if (attempted == 0) 0 else ((correct * 100.0) / attempted).toInt()
}

data class ProgressSummary(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val dailyGoal: Int = 10,
    val todayCount: Int = 0,
    val totalAttempted: Int = 0,
    val totalCorrect: Int = 0,
    val netGateScore: Double = 0.0,
    val notificationAnsweredCount: Int = 0,
    val last30Days: List<DayActivityPoint> = emptyList(),
    val recentTrendDays: List<DayActivityPoint> = emptyList(),
    val sectionMastery: List<SectionMastery> = emptyList(),
    val remainingTopicsInCycle: Int = 36
) {
    val overallAccuracyPercent: Int
        get() = if (totalAttempted == 0) 0 else ((totalCorrect * 100.0) / totalAttempted).toInt()
}

class GateViewModel(
    application: Application,
    private val userId: String,
    private val displayName: String
) : AndroidViewModel(application) {

    private val repository = GateRepository(application.applicationContext)

    private val _practiceState = MutableStateFlow(PracticeUiState())
    val practiceState: StateFlow<PracticeUiState> = _practiceState.asStateFlow()

    val unseenPoolCount: StateFlow<Int> = repository.observeUnseenPoolCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val savedQuestions: StateFlow<List<CachedQuestionEntity>> = repository.observeSavedQuestions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val profile: StateFlow<StudyProfileEntity> = repository.observeProfile(userId)
        .combine(MutableStateFlow(Unit)) { entity, _ ->
            entity ?: StudyProfileEntity(userId = userId, displayName = displayName)
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            StudyProfileEntity(userId = userId, displayName = displayName)
        )

    val attempts: StateFlow<List<AttemptEntity>> = repository.observeAttempts(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val progressSummary: StateFlow<ProgressSummary> = combine(profile, attempts) { prof, attList ->
        buildProgressSummary(prof, attList)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProgressSummary())

    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            repository.ensureSeededAndProfileReady(userId, displayName)
            repository.startCloudSync(viewModelScope, userId) { syncErr ->
                _practiceState.update { it.copy(statusBanner = "Cloud sync paused: $syncErr") }
            }
            loadNextQuestion(preferFreshAi = false)
        }
    }

    fun openQuestionById(questionId: String) {
        viewModelScope.launch {
            val found = repository.getQuestionById(questionId)
            if (found != null) {
                stopTimer()
                val alreadySubmitted = found.attempted && found.lastSelectedIndex in 0..3
                val scoreDelta = if (alreadySubmitted) {
                    if (found.lastSelectedIndex == found.correctIndex) found.marks.toDouble()
                    else -found.negativeMarks
                } else 0.0

                _practiceState.update {
                    it.copy(
                        isLoading = false,
                        question = found,
                        selectedOptionIndex = if (alreadySubmitted) found.lastSelectedIndex else null,
                        isSubmitted = alreadySubmitted,
                        lastScoreDelta = scoreDelta,
                        elapsedSeconds = 0,
                        isOnline = repository.isOnline(),
                        errorMessage = null
                    )
                }
                if (!alreadySubmitted) startTimer()
            }
        }
    }

    fun loadNextQuestion(preferFreshAi: Boolean = false) {
        viewModelScope.launch {
            stopTimer()
            _practiceState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    statusBanner = if (preferFreshAi) "Generating fresh GATE 2026 EC question with AI..." else null,
                    isOnline = repository.isOnline()
                )
            }
            try {
                val res = repository.fetchNextNonRepeatingQuestion(
                    userId = userId,
                    preferFreshAi = preferFreshAi
                )
                res.onSuccess { question ->
                    _practiceState.update {
                        it.copy(
                            isLoading = false,
                            question = question,
                            selectedOptionIndex = null,
                            isSubmitted = false,
                            lastScoreDelta = 0.0,
                            elapsedSeconds = 0,
                            isOnline = repository.isOnline(),
                            errorMessage = null,
                            statusBanner = null
                        )
                    }
                    startTimer()
                }.onFailure { err ->
                    _practiceState.update {
                        it.copy(
                            isLoading = false,
                            isOnline = repository.isOnline(),
                            errorMessage = err.localizedMessage ?: "Unable to load next GATE question.",
                            statusBanner = null
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            }
        }
    }

    fun selectOption(index: Int) {
        if (_practiceState.value.isSubmitted) return
        _practiceState.update { it.copy(selectedOptionIndex = index.coerceIn(0, 3)) }
    }

    fun clearSelection() {
        if (_practiceState.value.isSubmitted) return
        _practiceState.update { it.copy(selectedOptionIndex = null) }
    }

    fun submitCurrentAnswer() {
        val state = _practiceState.value
        val question = state.question ?: return
        val chosen = state.selectedOptionIndex ?: return
        if (state.isSubmitted) return

        stopTimer()
        viewModelScope.launch {
            val attempt = repository.submitAnswer(
                userId = userId,
                question = question,
                selectedIndex = chosen,
                timeSpentSec = state.elapsedSeconds,
                answeredVia = "IN_APP"
            )
            _practiceState.update {
                it.copy(
                    isSubmitted = true,
                    lastScoreDelta = attempt.scoreDelta,
                    question = question.copy(attempted = true, lastSelectedIndex = chosen)
                )
            }
        }
    }

    fun toggleSaveCurrentQuestion() {
        val question = _practiceState.value.question ?: return
        viewModelScope.launch {
            val nowSaved = repository.toggleSaveOffline(userId, question)
            _practiceState.update {
                it.copy(
                    question = question.copy(savedOffline = nowSaved),
                    statusBanner = if (nowSaved) "Saved for offline review & cloud sync" else "Removed from saved bank"
                )
            }
        }
    }

    fun removeSavedQuestion(question: CachedQuestionEntity) {
        viewModelScope.launch {
            if (question.savedOffline) {
                repository.toggleSaveOffline(userId, question)
                if (_practiceState.value.question?.id == question.id) {
                    _practiceState.update {
                        it.copy(question = it.question?.copy(savedOffline = false))
                    }
                }
            }
        }
    }

    fun recordOfflineRetryAttempt(question: CachedQuestionEntity, selectedIndex: Int) {
        viewModelScope.launch {
            repository.submitAnswer(
                userId = userId,
                question = question,
                selectedIndex = selectedIndex,
                timeSpentSec = 30,
                answeredVia = "IN_APP"
            )
        }
    }

    fun refillOfflineAiPool() {
        if (_practiceState.value.isRefillingPool) return
        viewModelScope.launch {
            _practiceState.update {
                it.copy(
                    isRefillingPool = true,
                    statusBanner = "Generating 3 fresh non-repeating GATE EC questions for offline use..."
                )
            }
            val result = repository.refillOfflineAiPool(userId, batchSize = 3)
            result.onSuccess { added ->
                _practiceState.update {
                    it.copy(
                        isRefillingPool = false,
                        statusBanner = "Cached $added fresh AI GATE 2026 EC questions for offline study."
                    )
                }
            }.onFailure { err ->
                _practiceState.update {
                    it.copy(
                        isRefillingPool = false,
                        statusBanner = err.localizedMessage ?: "Could not refill offline AI pool."
                    )
                }
            }
        }
    }

    fun syncCloudNow() {
        viewModelScope.launch {
            _practiceState.update { it.copy(statusBanner = "Syncing study progress with cloud...") }
            val result = repository.flushPendingToCloud(userId)
            result.onSuccess {
                _practiceState.update { it.copy(statusBanner = "Cloud sync complete across all devices.") }
            }.onFailure { err ->
                _practiceState.update {
                    it.copy(statusBanner = "Sync pending (offline): ${err.localizedMessage ?: "Retry when online"}")
                }
            }
        }
    }

    fun setUnlockMonitorEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updatePreferences(userId = userId, unlockMonitorEnabled = enabled)
            val ctx = getApplication<Application>().applicationContext
            if (enabled) {
                UnlockMonitorService.start(ctx)
                _practiceState.update {
                    it.copy(statusBanner = "Unlock Quiz Monitor active: a fresh topic triggers on every unlock.")
                }
            } else {
                UnlockMonitorService.stop(ctx)
                _practiceState.update {
                    it.copy(statusBanner = "Unlock Quiz Monitor paused.")
                }
            }
        }
    }

    fun setUnlockCooldownMinutes(minutes: Int) {
        viewModelScope.launch {
            repository.updatePreferences(userId = userId, unlockCooldownMinutes = minutes)
        }
    }

    fun setDailyGoal(goal: Int) {
        viewModelScope.launch {
            repository.updatePreferences(userId = userId, dailyGoal = goal.coerceIn(5, 50))
        }
    }

    fun setDarkExamTheme(enabled: Boolean) {
        viewModelScope.launch {
            repository.updatePreferences(userId = userId, darkExamTheme = enabled)
        }
    }

    fun triggerTestUnlockNotification() {
        viewModelScope.launch {
            val ctx = getApplication<Application>().applicationContext
            if (!QuizNotificationHelper.hasNotificationPermission(ctx)) {
                _practiceState.update {
                    it.copy(statusBanner = "Grant notification permission first to preview lock-screen quiz.")
                }
                return@launch
            }
            val next = repository.popNextOfflineQuestionForUnlock()
                ?: _practiceState.value.question
            if (next != null) {
                QuizNotificationHelper.postUnlockQuizNotification(ctx, next)
                _practiceState.update {
                    it.copy(statusBanner = "Lock-screen GATE quiz notification sent! Check your notification shade.")
                }
            } else {
                _practiceState.update {
                    it.copy(statusBanner = "Refill the offline pool first to send a quiz notification.")
                }
            }
        }
    }

    fun dismissBanner() {
        _practiceState.update { it.copy(statusBanner = null) }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                _practiceState.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun buildProgressSummary(
        prof: StudyProfileEntity,
        attemptList: List<AttemptEntity>
    ): ProgressSummary {
        val today = GateRepository.todayDateKey()
        val byDate = attemptList.groupBy { it.dateKey }
        val todayCount = byDate[today]?.size ?: 0
        val notifCount = attemptList.count { it.answeredVia == "NOTIFICATION" }

        val last30 = (29 downTo 0).map { daysAgo ->
            val key = GateRepository.dateKeyDaysAgo(daysAgo)
            val dayAttempts = byDate[key].orEmpty()
            DayActivityPoint(
                dateKey = key,
                label = key.takeLast(5),
                attempted = dayAttempts.size,
                correct = dayAttempts.count { it.correctAnswer },
                netScore = dayAttempts.sumOf { it.scoreDelta }
            )
        }

        val recentTrend = last30.filter { it.attempted > 0 }.takeLast(10)

        val bySection = attemptList.groupBy { it.sectionId }
        val sectionStats = GateEcSyllabus.sections.map { sec ->
            val secAttempts = bySection[sec.id].orEmpty()
            SectionMastery(
                sectionId = sec.id,
                code = sec.code,
                name = sec.name,
                shortName = sec.shortName,
                attempted = secAttempts.size,
                correct = secAttempts.count { it.correctAnswer },
                netScore = secAttempts.sumOf { it.scoreDelta }
            )
        }

        val totalAtt = maxOf(prof.totalAttempted, attemptList.size)
        val totalCor = maxOf(prof.totalCorrect, attemptList.count { it.correctAnswer })
        val netScore = if (attemptList.isNotEmpty() && prof.totalAttempted == 0) {
            attemptList.sumOf { it.scoreDelta }
        } else {
            prof.netGateScore
        }

        return ProgressSummary(
            currentStreak = prof.currentStreak,
            longestStreak = prof.longestStreak,
            dailyGoal = prof.dailyGoal,
            todayCount = todayCount,
            totalAttempted = totalAtt,
            totalCorrect = totalCor,
            netGateScore = netScore,
            notificationAnsweredCount = notifCount,
            last30Days = last30,
            recentTrendDays = recentTrend,
            sectionMastery = sectionStats,
            remainingTopicsInCycle = prof.deckList().size.ifZero { GateEcSyllabus.subtopics.size }
        )
    }

    private fun Int.ifZero(fallback: () -> Int): Int = if (this == 0) fallback() else this

    class Factory(
        private val application: Application,
        private val userId: String,
        private val displayName: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GateViewModel(application, userId, displayName) as T
        }
    }
}

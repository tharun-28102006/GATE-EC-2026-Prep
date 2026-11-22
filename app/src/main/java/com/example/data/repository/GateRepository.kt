package com.example.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.data.local.AttemptEntity
import com.example.data.local.CachedQuestionEntity
import com.example.data.local.GateDatabase
import com.example.data.local.StudyProfileEntity
import com.example.data.remote.GateAiGenerator
import com.example.data.remote.GateFirestoreDataSource
import com.example.domain.GateEcSyllabus
import com.example.domain.GateSeedQuestions
import com.example.domain.TopicRotationEngine
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

private const val TAG = "GateRepository"

class GateRepository(private val appContext: Context) {

    private val dao = GateDatabase.getInstance(appContext).gateDao()
    private val firestore = GateFirestoreDataSource(appContext)
    private val aiGenerator = GateAiGenerator()
    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun observeUnseenPoolCount(): Flow<Int> = dao.observeUnseenCount()
    fun observeSavedQuestions(): Flow<List<CachedQuestionEntity>> = dao.observeSavedQuestions()
    fun observeAttemptedQuestions(): Flow<List<CachedQuestionEntity>> = dao.observeAttemptedQuestions()
    fun observeAttempts(userId: String): Flow<List<AttemptEntity>> = dao.observeAttempts(userId)
    fun observeProfile(userId: String): Flow<StudyProfileEntity?> = dao.observeProfile(userId)

    fun isOnline(): Boolean {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun ensureSeededAndProfileReady(userId: String, displayName: String) {
        if (dao.getTotalQuestionCount() == 0) {
            val seeds = GateSeedQuestions.all.mapIndexed { idx, s ->
                CachedQuestionEntity(
                    id = s.id,
                    sectionId = s.sectionId,
                    subtopicId = s.subtopicId,
                    questionText = s.questionText,
                    optionA = s.options[0],
                    optionB = s.options[1],
                    optionC = s.options[2],
                    optionD = s.options[3],
                    correctIndex = s.correctIndex,
                    marks = s.marks,
                    negativeMarks = s.negativeMarks,
                    difficulty = s.difficulty,
                    explanation = s.explanation,
                    keyFormula = s.keyFormula,
                    source = "SEED",
                    savedOffline = false,
                    attempted = false,
                    createdAtMs = System.currentTimeMillis() - (20 - idx) * 1000L
                )
            }
            dao.insertQuestionsIgnore(seeds)
        }

        val existing = dao.getProfile(userId)
        if (existing == null) {
            val initialDeck = TopicRotationEngine.buildSectionInterleavedDeck()
            val fresh = StudyProfileEntity(
                userId = userId,
                displayName = displayName.ifBlank { "GATE EC Candidate" },
                remainingTopicDeckCsv = initialDeck.joinToString(",")
            )
            dao.upsertProfile(fresh)
            if (isOnline() && Firebase.auth.currentUser?.uid == userId) {
                firestore.upsertUserProfile(fresh)
            }
        }
        // Keep Room lean by pruning old unsaved attempted questions older than 14 days
        val fourteenDaysAgo = System.currentTimeMillis() - 14L * 24L * 60L * 60L * 1000L
        dao.pruneOldUnsavedQuestions(fourteenDaysAgo)
    }

    /**
     * Attaches real-time Firestore listeners for cross-device cloud sync while signed in,
     * merging cloud profile, saved questions, and attempts into local Room storage.
     */
    fun startCloudSync(
        scope: CoroutineScope,
        userId: String,
        onSyncError: (String) -> Unit
    ) {
        if (Firebase.auth.currentUser?.uid != userId) return

        // 1. Sync remote profile
        scope.launch(Dispatchers.IO) {
            firestore.observeUserProfile()
                .catch { error ->
                    val firestoreErr = generateSequence(error) { it.cause }
                        .filterIsInstance<com.google.firebase.firestore.FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreErr == null && error is CancellationException) throw error
                    onSyncError((firestoreErr ?: error).message ?: "Profile sync error")
                }
                .collect { remote ->
                    if (remote != null) {
                        val local = dao.getProfile(userId)
                        if (local == null || remote.totalAttempted >= local.totalAttempted) {
                            val mergedDeck = remote.remainingTopicDeck
                                .ifEmpty { local?.deckList().orEmpty() }
                                .ifEmpty { TopicRotationEngine.buildSectionInterleavedDeck(remote.lastSectionId) }
                            val merged = StudyProfileEntity(
                                userId = userId,
                                displayName = remote.displayName.ifBlank { local?.displayName ?: "GATE EC Candidate" },
                                dailyGoal = remote.dailyGoal.coerceIn(1, 100),
                                currentStreak = maxOf(remote.currentStreak, local?.currentStreak ?: 0),
                                longestStreak = maxOf(remote.longestStreak, local?.longestStreak ?: 0),
                                lastActiveDate = if (remote.lastActiveDate >= (local?.lastActiveDate ?: "")) remote.lastActiveDate else local!!.lastActiveDate,
                                lastSectionId = remote.lastSectionId.ifBlank { local?.lastSectionId ?: "" },
                                remainingTopicDeckCsv = mergedDeck.joinToString(","),
                                totalAttempted = maxOf(remote.totalAttempted, local?.totalAttempted ?: 0),
                                totalCorrect = maxOf(remote.totalCorrect, local?.totalCorrect ?: 0),
                                netGateScore = if (remote.totalAttempted >= (local?.totalAttempted ?: 0)) remote.netGateScore else local!!.netGateScore,
                                unlockMonitorEnabled = local?.unlockMonitorEnabled ?: remote.unlockMonitorEnabled,
                                unlockCooldownMinutes = local?.unlockCooldownMinutes ?: remote.unlockCooldownMinutes,
                                darkExamTheme = remote.darkExamTheme,
                                lastUnlockNotifMs = local?.lastUnlockNotifMs ?: 0L,
                                updatedAtMs = System.currentTimeMillis()
                            )
                            dao.upsertProfile(merged)
                        }
                    }
                }
        }

        // 2. Sync remote saved questions into offline Room store
        scope.launch(Dispatchers.IO) {
            firestore.observeSavedQuestions()
                .catch { error ->
                    val firestoreErr = generateSequence(error) { it.cause }
                        .filterIsInstance<com.google.firebase.firestore.FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreErr == null && error is CancellationException) throw error
                    onSyncError((firestoreErr ?: error).message ?: "Saved questions sync error")
                }
                .collect { remoteSaved ->
                    remoteSaved.forEach { q ->
                        if (q.options.size == 4 && q.id.isNotBlank()) {
                            val existing = dao.getQuestionById(q.id)
                            dao.upsertQuestion(
                                CachedQuestionEntity(
                                    id = q.id,
                                    sectionId = q.sectionId,
                                    subtopicId = q.subtopicId,
                                    questionText = q.questionText,
                                    optionA = q.options[0],
                                    optionB = q.options[1],
                                    optionC = q.options[2],
                                    optionD = q.options[3],
                                    correctIndex = q.correctIndex.coerceIn(0, 3),
                                    marks = if (q.marks == 2) 2 else 1,
                                    negativeMarks = q.negativeMarks,
                                    difficulty = q.difficulty,
                                    explanation = q.explanation,
                                    keyFormula = q.keyFormula,
                                    source = q.source,
                                    savedOffline = true,
                                    attempted = existing?.attempted ?: false,
                                    lastSelectedIndex = existing?.lastSelectedIndex ?: -1,
                                    createdAtMs = q.createdAt?.toDate()?.time ?: System.currentTimeMillis()
                                )
                            )
                        }
                    }
                }
        }

        // 3. Sync remote attempt history into local Room store for charts/trends
        scope.launch(Dispatchers.IO) {
            firestore.observeAttempts()
                .catch { error ->
                    val firestoreErr = generateSequence(error) { it.cause }
                        .filterIsInstance<com.google.firebase.firestore.FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreErr == null && error is CancellationException) throw error
                    onSyncError((firestoreErr ?: error).message ?: "Attempts sync error")
                }
                .collect { remoteAttempts ->
                    val entities = remoteAttempts.filter { it.id.isNotBlank() }.map { a ->
                        AttemptEntity(
                            id = a.id,
                            userId = userId,
                            questionId = a.questionId,
                            sectionId = a.sectionId,
                            subtopicId = a.subtopicId,
                            selectedIndex = a.selectedIndex,
                            correctAnswer = a.correctAnswer,
                            scoreDelta = a.scoreDelta,
                            timeSpentSec = a.timeSpentSec,
                            answeredVia = a.answeredVia,
                            dateKey = a.dateKey,
                            createdAtMs = a.createdAt?.toDate()?.time ?: System.currentTimeMillis(),
                            syncedToCloud = true
                        )
                    }
                    if (entities.isNotEmpty()) {
                        dao.upsertAttempts(entities)
                    }
                }
        }

        // 4. Flush any pending offline attempts & saved questions up to cloud
        scope.launch(Dispatchers.IO) {
            flushPendingToCloud(userId)
        }
    }

    suspend fun flushPendingToCloud(userId: String): Result<Unit> {
        if (!isOnline() || Firebase.auth.currentUser?.uid != userId) {
            return Result.failure(IllegalStateException("Offline or unauthenticated"))
        }
        return try {
            val unsynced = dao.getUnsyncedAttempts(userId)
            unsynced.forEach { att ->
                val res = firestore.logAttemptToCloud(att)
                if (res.isSuccess) {
                    dao.markAttemptSynced(att.id)
                }
            }
            val saved = dao.getSavedQuestionsSnapshot()
            saved.forEach { q ->
                firestore.saveQuestionToCloud(q)
            }
            dao.getProfile(userId)?.let { profile ->
                firestore.upsertUserProfile(profile)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Cloud flush error", e)
            Result.failure(e)
        }
    }

    suspend fun getQuestionById(id: String): CachedQuestionEntity? = dao.getQuestionById(id)

    /**
     * Serves the next GATE 2026 EC question using the non-repeating section-aware TopicRotationEngine.
     * - Prefers fresh AI generation when online if no unseen question is cached for the selected subtopic.
     * - Works seamlessly offline by falling back to cached unseen questions from strictly different sections.
     * - Automatically tops up the offline AI cache in the background when online and buffer < 6.
     */
    suspend fun fetchNextNonRepeatingQuestion(
        userId: String,
        preferFreshAi: Boolean = false
    ): Result<CachedQuestionEntity> {
        val profile = dao.getProfile(userId) ?: run {
            ensureSeededAndProfileReady(userId, "GATE EC Candidate")
            dao.getProfile(userId)!!
        }

        val rotation = TopicRotationEngine.nextSubtopic(
            currentDeck = profile.deckList(),
            lastSectionId = profile.lastSectionId
        )
        val targetSubtopic = rotation.selectedSubtopic

        val updatedProfile = profile.copy(
            lastSectionId = rotation.nextLastSectionId,
            remainingTopicDeckCsv = rotation.remainingDeck.joinToString(","),
            updatedAtMs = System.currentTimeMillis()
        )

        // 1. If user explicitly asks for fresh AI or no cached question exists for this exact subtopic
        val cachedForSubtopic = if (!preferFreshAi) dao.getUnseenBySubtopic(targetSubtopic.id) else null
        if (cachedForSubtopic != null) {
            dao.upsertProfile(updatedProfile)
            triggerBackgroundRefillIfNeeded(updatedProfile)
            return Result.success(cachedForSubtopic)
        }

        // 2. If online, generate a brand-new GATE 2026 EC question via Firebase AI Logic
        if (isOnline()) {
            val aiResult = aiGenerator.generateQuestionForSubtopic(targetSubtopic)
            if (aiResult.isSuccess) {
                val freshQuestion = aiResult.getOrThrow()
                dao.upsertQuestion(freshQuestion)
                dao.upsertProfile(updatedProfile)
                if (Firebase.auth.currentUser?.uid == userId) {
                    backgroundScope.launch { firestore.upsertUserProfile(updatedProfile) }
                }
                triggerBackgroundRefillIfNeeded(updatedProfile)
                return Result.success(freshQuestion)
            } else {
                // Fall back to any cached unseen question from a different section before returning error
                val fallback = dao.getAnyUnseenExcludingSection(profile.lastSectionId)
                    ?: dao.getAnyUnseenQuestion()
                if (fallback != null) {
                    val fallbackProfile = updatedProfile.copy(lastSectionId = fallback.sectionId)
                    dao.upsertProfile(fallbackProfile)
                    return Result.success(fallback)
                }
                return aiResult
            }
        }

        // 3. Offline mode: pull an unseen cached question from a strictly different section
        val offlineNext = dao.getAnyUnseenExcludingSection(profile.lastSectionId)
            ?: dao.getAnyUnseenQuestion()

        return if (offlineNext != null) {
            val offlineProfile = updatedProfile.copy(lastSectionId = offlineNext.sectionId)
            dao.upsertProfile(offlineProfile)
            Result.success(offlineNext)
        } else {
            Result.failure(
                IllegalStateException("Offline question pool is empty. Connect to the internet to generate fresh GATE 2026 EC AI questions, or review your Saved Offline Bank.")
            )
        }
    }

    /**
     * Ultra-fast zero-network question picker for UnlockMonitorService (called on phone unlock).
     */
    suspend fun popNextOfflineQuestionForUnlock(): CachedQuestionEntity? {
        val profile = dao.getLatestProfile()
        if (profile == null) {
            if (dao.getTotalQuestionCount() == 0) {
                ensureSeededAndProfileReady("offline_local", "GATE EC Candidate")
            }
            return dao.getAnyUnseenQuestion()
        }

        val rotation = TopicRotationEngine.nextSubtopic(
            currentDeck = profile.deckList(),
            lastSectionId = profile.lastSectionId
        )

        val picked = dao.getUnseenBySubtopic(rotation.selectedSubtopic.id)
            ?: dao.getAnyUnseenExcludingSection(profile.lastSectionId)
            ?: dao.getAnyUnseenQuestion()
            ?: return null

        val updatedProfile = profile.copy(
            lastSectionId = picked.sectionId,
            remainingTopicDeckCsv = rotation.remainingDeck.joinToString(","),
            lastUnlockNotifMs = System.currentTimeMillis(),
            updatedAtMs = System.currentTimeMillis()
        )
        dao.upsertProfile(updatedProfile)
        triggerBackgroundRefillIfNeeded(updatedProfile)
        return picked
    }

    /**
     * Proactively refills 3 fresh AI-generated GATE 2026 EC questions across upcoming distinct sections
     * so offline mode and phone-unlock notifications always have fresh questions ready.
     */
    suspend fun refillOfflineAiPool(userId: String, batchSize: Int = 3): Result<Int> {
        if (!isOnline()) {
            return Result.failure(IllegalStateException("Internet connection required to generate fresh AI questions."))
        }
        val profile = dao.getProfile(userId)
        val upcoming = TopicRotationEngine.peekUpcomingSubtopics(
            currentDeck = profile?.deckList().orEmpty(),
            lastSectionId = profile?.lastSectionId.orEmpty(),
            count = batchSize
        )
        val generated = aiGenerator.generateBatchForSubtopics(upcoming)
        return generated.map { list ->
            list.forEach { dao.upsertQuestion(it) }
            list.size
        }
    }

    private fun triggerBackgroundRefillIfNeeded(profile: StudyProfileEntity) {
        if (!isOnline()) return
        backgroundScope.launch {
            try {
                if (dao.getUnseenCount() < 6) {
                    val upcoming = TopicRotationEngine.peekUpcomingSubtopics(
                        currentDeck = profile.deckList(),
                        lastSectionId = profile.lastSectionId,
                        count = 3
                    )
                    val batch = aiGenerator.generateBatchForSubtopics(upcoming)
                    batch.getOrNull()?.forEach { dao.upsertQuestion(it) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Background AI cache refill skipped: ${e.message}")
            }
        }
    }

    suspend fun submitAnswer(
        userId: String,
        question: CachedQuestionEntity,
        selectedIndex: Int,
        timeSpentSec: Int,
        answeredVia: String = "IN_APP"
    ): AttemptEntity {
        val isCorrect = selectedIndex == question.correctIndex
        val scoreDelta = if (isCorrect) {
            question.marks.toDouble()
        } else {
            -question.negativeMarks
        }

        val todayKey = todayDateKey()
        val attemptId = "att_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        val attempt = AttemptEntity(
            id = attemptId,
            userId = userId,
            questionId = question.id,
            sectionId = question.sectionId,
            subtopicId = question.subtopicId,
            selectedIndex = selectedIndex.coerceIn(0, 3),
            correctAnswer = isCorrect,
            scoreDelta = scoreDelta,
            timeSpentSec = timeSpentSec.coerceIn(0, 7200),
            answeredVia = answeredVia,
            dateKey = todayKey,
            createdAtMs = System.currentTimeMillis(),
            syncedToCloud = false
        )

        dao.markQuestionAttempted(question.id, selectedIndex)
        dao.upsertAttempt(attempt)

        val currentProfile = dao.getProfile(userId) ?: StudyProfileEntity(userId = userId)
        val newStreak = computeUpdatedStreak(
            currentStreak = currentProfile.currentStreak,
            lastActiveDate = currentProfile.lastActiveDate,
            todayKey = todayKey
        )
        val updatedProfile = currentProfile.copy(
            currentStreak = newStreak,
            longestStreak = maxOf(currentProfile.longestStreak, newStreak),
            lastActiveDate = todayKey,
            lastSectionId = question.sectionId,
            totalAttempted = currentProfile.totalAttempted + 1,
            totalCorrect = currentProfile.totalCorrect + if (isCorrect) 1 else 0,
            netGateScore = ((currentProfile.netGateScore + scoreDelta) * 100.0).toInt() / 100.0,
            updatedAtMs = System.currentTimeMillis()
        )
        dao.upsertProfile(updatedProfile)

        if (isOnline() && Firebase.auth.currentUser?.uid == userId) {
            backgroundScope.launch {
                val res = firestore.logAttemptToCloud(attempt)
                if (res.isSuccess) {
                    dao.markAttemptSynced(attempt.id)
                }
                firestore.upsertUserProfile(updatedProfile)
            }
        }

        return attempt
    }

    suspend fun toggleSaveOffline(userId: String, question: CachedQuestionEntity): Boolean {
        val newState = !question.savedOffline
        dao.setSavedOffline(question.id, newState)
        if (isOnline() && Firebase.auth.currentUser?.uid == userId) {
            backgroundScope.launch {
                if (newState) {
                    firestore.saveQuestionToCloud(question.copy(savedOffline = true))
                } else {
                    firestore.deleteSavedQuestionFromCloud(question.id)
                }
            }
        }
        return newState
    }

    suspend fun saveOfflineById(userId: String, questionId: String) {
        val q = dao.getQuestionById(questionId) ?: return
        if (!q.savedOffline) {
            dao.setSavedOffline(questionId, true)
            if (isOnline() && Firebase.auth.currentUser?.uid == userId) {
                firestore.saveQuestionToCloud(q.copy(savedOffline = true))
            }
        }
    }

    suspend fun updatePreferences(
        userId: String,
        dailyGoal: Int? = null,
        unlockMonitorEnabled: Boolean? = null,
        unlockCooldownMinutes: Int? = null,
        darkExamTheme: Boolean? = null
    ) {
        val current = dao.getProfile(userId) ?: StudyProfileEntity(userId = userId)
        val updated = current.copy(
            dailyGoal = dailyGoal?.coerceIn(1, 100) ?: current.dailyGoal,
            unlockMonitorEnabled = unlockMonitorEnabled ?: current.unlockMonitorEnabled,
            unlockCooldownMinutes = unlockCooldownMinutes?.coerceIn(0, 180) ?: current.unlockCooldownMinutes,
            darkExamTheme = darkExamTheme ?: current.darkExamTheme,
            updatedAtMs = System.currentTimeMillis()
        )
        dao.upsertProfile(updated)
        if (isOnline() && Firebase.auth.currentUser?.uid == userId) {
            backgroundScope.launch {
                firestore.upsertUserProfile(updated)
            }
        }
    }

    suspend fun getLatestProfile(): StudyProfileEntity? = dao.getLatestProfile()

    companion object {
        fun todayDateKey(): String {
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            return fmt.format(Calendar.getInstance().time)
        }

        fun dateKeyDaysAgo(daysAgo: Int): String {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            return fmt.format(cal.time)
        }

        fun computeUpdatedStreak(
            currentStreak: Int,
            lastActiveDate: String,
            todayKey: String
        ): Int {
            if (lastActiveDate == todayKey) {
                return if (currentStreak <= 0) 1 else currentStreak
            }
            val yesterdayKey = dateKeyDaysAgo(1)
            return if (lastActiveDate == yesterdayKey) {
                currentStreak + 1
            } else {
                1
            }
        }
    }
}

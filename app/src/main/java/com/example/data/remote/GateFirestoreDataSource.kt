package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.data.local.AttemptEntity
import com.example.data.local.CachedQuestionEntity
import com.example.data.local.StudyProfileEntity
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class GateFirestoreDataSource(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        }
        return uid
    }

    private fun sanitizeDocId(raw: String): String {
        val clean = raw.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(128)
        return clean.ifBlank { "doc_${System.currentTimeMillis()}" }
    }

    fun observeUserProfile(): Flow<FirestoreUserProfile?> = flow {
        val uid = requireUserId()
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            FirestoreUserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeSavedQuestions(): Flow<List<FirestoreSavedQuestion>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/savedQuestions"
        emitAll(
            db.collection("users").document(uid).collection("savedQuestions")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        FirestoreSavedQuestion::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeAttempts(): Flow<List<FirestoreAttempt>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/attempts"
        emitAll(
            db.collection("users").document(uid).collection("attempts")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        FirestoreAttempt::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun upsertUserProfile(profile: StudyProfileEntity): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid)
            docPath = docRef.path

            val safeDeck = profile.deckList().take(40)
            val safeTotalAttempted = profile.totalAttempted.coerceIn(0, 1_000_000)
            val safeTotalCorrect = profile.totalCorrect.coerceIn(0, safeTotalAttempted)
            val safeDisplayName = profile.displayName.trim().take(100)
            val safeDailyGoal = profile.dailyGoal.coerceIn(1, 100)
            val safeCooldown = profile.unlockCooldownMinutes.coerceIn(0, 180)
            val safeStreak = profile.currentStreak.coerceIn(0, 10_000)
            val safeLongest = profile.longestStreak.coerceIn(safeStreak, 10_000)
            val safeDate = profile.lastActiveDate.take(10)
            val safeSection = profile.lastSectionId.take(40)

            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                val updatePayload = mapOf(
                    "displayName" to safeDisplayName,
                    "dailyGoal" to safeDailyGoal,
                    "currentStreak" to safeStreak,
                    "longestStreak" to safeLongest,
                    "lastActiveDate" to safeDate,
                    "lastSectionId" to safeSection,
                    "remainingTopicDeck" to safeDeck,
                    "totalAttempted" to safeTotalAttempted,
                    "totalCorrect" to safeTotalCorrect,
                    "netGateScore" to profile.netGateScore.coerceIn(-1_000_000.0, 2_000_000.0),
                    "unlockMonitorEnabled" to profile.unlockMonitorEnabled,
                    "unlockCooldownMinutes" to safeCooldown,
                    "darkExamTheme" to profile.darkExamTheme,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updatePayload).await()
            } else {
                val createPayload = mapOf(
                    "userId" to uid,
                    "displayName" to safeDisplayName,
                    "dailyGoal" to safeDailyGoal,
                    "currentStreak" to safeStreak,
                    "longestStreak" to safeLongest,
                    "lastActiveDate" to safeDate,
                    "lastSectionId" to safeSection,
                    "remainingTopicDeck" to safeDeck,
                    "totalAttempted" to safeTotalAttempted,
                    "totalCorrect" to safeTotalCorrect,
                    "netGateScore" to profile.netGateScore.coerceIn(-1_000_000.0, 2_000_000.0),
                    "unlockMonitorEnabled" to profile.unlockMonitorEnabled,
                    "unlockCooldownMinutes" to safeCooldown,
                    "darkExamTheme" to profile.darkExamTheme,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(createPayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveQuestionToCloud(question: CachedQuestionEntity): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/savedQuestions"
        return try {
            val uid = requireUserId()
            val safeId = sanitizeDocId(question.id)
            val docRef = db.collection("users").document(uid)
                .collection("savedQuestions").document(safeId)
            docPath = docRef.path

            val safeDiff = if (question.difficulty in listOf("Easy", "Medium", "Hard")) {
                question.difficulty
            } else {
                "Medium"
            }
            val safeSource = if (question.source in listOf("AI", "SEED")) question.source else "AI"
            val safeMarks = if (question.marks == 2) 2 else 1

            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                val updatePayload = mapOf(
                    "sectionId" to question.sectionId.take(40),
                    "subtopicId" to question.subtopicId.take(60),
                    "questionText" to question.questionText.take(3000),
                    "options" to question.optionsList(),
                    "correctIndex" to question.correctIndex.coerceIn(0, 3),
                    "marks" to safeMarks,
                    "negativeMarks" to question.negativeMarks.coerceIn(0.0, 2.0),
                    "difficulty" to safeDiff,
                    "explanation" to question.explanation.take(6000),
                    "keyFormula" to question.keyFormula.take(800),
                    "source" to safeSource,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updatePayload).await()
            } else {
                val createPayload = mapOf(
                    "userId" to uid,
                    "sectionId" to question.sectionId.take(40),
                    "subtopicId" to question.subtopicId.take(60),
                    "questionText" to question.questionText.take(3000),
                    "options" to question.optionsList(),
                    "correctIndex" to question.correctIndex.coerceIn(0, 3),
                    "marks" to safeMarks,
                    "negativeMarks" to question.negativeMarks.coerceIn(0.0, 2.0),
                    "difficulty" to safeDiff,
                    "explanation" to question.explanation.take(6000),
                    "keyFormula" to question.keyFormula.take(800),
                    "source" to safeSource,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(createPayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteSavedQuestionFromCloud(questionId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/savedQuestions"
        return try {
            val uid = requireUserId()
            val safeId = sanitizeDocId(questionId)
            val docRef = db.collection("users").document(uid)
                .collection("savedQuestions").document(safeId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun logAttemptToCloud(attempt: AttemptEntity): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/attempts"
        return try {
            val uid = requireUserId()
            val safeId = sanitizeDocId(attempt.id)
            val docRef = db.collection("users").document(uid)
                .collection("attempts").document(safeId)
            docPath = docRef.path

            val safeVia = if (attempt.answeredVia in listOf("IN_APP", "NOTIFICATION")) {
                attempt.answeredVia
            } else {
                "IN_APP"
            }
            val safeDateKey = if (attempt.dateKey.length == 10) attempt.dateKey else "2026-01-01"

            val payload = mapOf(
                "userId" to uid,
                "questionId" to sanitizeDocId(attempt.questionId),
                "sectionId" to attempt.sectionId.take(40),
                "subtopicId" to attempt.subtopicId.take(60),
                "selectedIndex" to attempt.selectedIndex.coerceIn(0, 3),
                "correctAnswer" to attempt.correctAnswer,
                "scoreDelta" to attempt.scoreDelta.coerceIn(-2.0, 2.0),
                "timeSpentSec" to attempt.timeSpentSec.coerceIn(0, 7200),
                "answeredVia" to safeVia,
                "dateKey" to safeDateKey,
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }
}

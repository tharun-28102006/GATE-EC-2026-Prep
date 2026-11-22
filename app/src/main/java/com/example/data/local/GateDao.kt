package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GateDao {
    // --- Questions ---
    @Query("SELECT COUNT(*) FROM cached_questions")
    suspend fun getTotalQuestionCount(): Int

    @Query("SELECT COUNT(*) FROM cached_questions WHERE attempted = 0")
    fun observeUnseenCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM cached_questions WHERE attempted = 0")
    suspend fun getUnseenCount(): Int

    @Query("SELECT * FROM cached_questions WHERE id = :id LIMIT 1")
    suspend fun getQuestionById(id: String): CachedQuestionEntity?

    @Query("SELECT * FROM cached_questions WHERE subtopicId = :subtopicId AND attempted = 0 ORDER BY createdAtMs ASC LIMIT 1")
    suspend fun getUnseenBySubtopic(subtopicId: String): CachedQuestionEntity?

    @Query("SELECT * FROM cached_questions WHERE sectionId != :excludeSectionId AND attempted = 0 ORDER BY createdAtMs ASC LIMIT 1")
    suspend fun getAnyUnseenExcludingSection(excludeSectionId: String): CachedQuestionEntity?

    @Query("SELECT * FROM cached_questions WHERE attempted = 0 ORDER BY createdAtMs ASC LIMIT 1")
    suspend fun getAnyUnseenQuestion(): CachedQuestionEntity?

    @Query("SELECT * FROM cached_questions WHERE savedOffline = 1 ORDER BY createdAtMs DESC")
    fun observeSavedQuestions(): Flow<List<CachedQuestionEntity>>

    @Query("SELECT * FROM cached_questions WHERE savedOffline = 1")
    suspend fun getSavedQuestionsSnapshot(): List<CachedQuestionEntity>

    @Query("SELECT * FROM cached_questions WHERE attempted = 1 ORDER BY createdAtMs DESC LIMIT 200")
    fun observeAttemptedQuestions(): Flow<List<CachedQuestionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQuestion(question: CachedQuestionEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertQuestionsIgnore(questions: List<CachedQuestionEntity>)

    @Query("UPDATE cached_questions SET savedOffline = :saved WHERE id = :questionId")
    suspend fun setSavedOffline(questionId: String, saved: Boolean)

    @Query("UPDATE cached_questions SET attempted = 1, lastSelectedIndex = :selectedIndex WHERE id = :questionId")
    suspend fun markQuestionAttempted(questionId: String, selectedIndex: Int)

    @Query("DELETE FROM cached_questions WHERE attempted = 1 AND savedOffline = 0 AND createdAtMs < :olderThanMs")
    suspend fun pruneOldUnsavedQuestions(olderThanMs: Long)

    // --- Attempts ---
    @Query("SELECT * FROM question_attempts WHERE userId = :userId ORDER BY createdAtMs DESC LIMIT 500")
    fun observeAttempts(userId: String): Flow<List<AttemptEntity>>

    @Query("SELECT * FROM question_attempts WHERE userId = :userId AND syncedToCloud = 0")
    suspend fun getUnsyncedAttempts(userId: String): List<AttemptEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAttempt(attempt: AttemptEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAttempts(attempts: List<AttemptEntity>)

    @Query("UPDATE question_attempts SET syncedToCloud = 1 WHERE id = :attemptId")
    suspend fun markAttemptSynced(attemptId: String)

    // --- Study Profile ---
    @Query("SELECT * FROM study_profile WHERE userId = :userId LIMIT 1")
    fun observeProfile(userId: String): Flow<StudyProfileEntity?>

    @Query("SELECT * FROM study_profile WHERE userId = :userId LIMIT 1")
    suspend fun getProfile(userId: String): StudyProfileEntity?

    @Query("SELECT * FROM study_profile ORDER BY updatedAtMs DESC LIMIT 1")
    suspend fun getLatestProfile(): StudyProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: StudyProfileEntity)

    @Query("UPDATE study_profile SET lastUnlockNotifMs = :timestampMs WHERE userId = :userId")
    suspend fun updateLastUnlockNotifMs(userId: String, timestampMs: Long)
}

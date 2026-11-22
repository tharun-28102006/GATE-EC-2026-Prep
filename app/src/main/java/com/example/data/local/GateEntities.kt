package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_questions")
data class CachedQuestionEntity(
    @PrimaryKey val id: String,
    val sectionId: String,
    val subtopicId: String,
    val questionText: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctIndex: Int,
    val marks: Int,
    val negativeMarks: Double,
    val difficulty: String,
    val explanation: String,
    val keyFormula: String,
    val source: String, // "AI" or "SEED"
    val savedOffline: Boolean = false,
    val attempted: Boolean = false,
    val lastSelectedIndex: Int = -1,
    val createdAtMs: Long = System.currentTimeMillis()
) {
    fun optionsList(): List<String> = listOf(optionA, optionB, optionC, optionD)
}

@Entity(tableName = "question_attempts")
data class AttemptEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val questionId: String,
    val sectionId: String,
    val subtopicId: String,
    val selectedIndex: Int,
    val correctAnswer: Boolean,
    val scoreDelta: Double,
    val timeSpentSec: Int,
    val answeredVia: String, // "IN_APP" or "NOTIFICATION"
    val dateKey: String, // YYYY-MM-DD
    val createdAtMs: Long = System.currentTimeMillis(),
    val syncedToCloud: Boolean = false
)

@Entity(tableName = "study_profile")
data class StudyProfileEntity(
    @PrimaryKey val userId: String,
    val displayName: String = "GATE EC Candidate",
    val dailyGoal: Int = 10,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastActiveDate: String = "",
    val lastSectionId: String = "",
    val remainingTopicDeckCsv: String = "",
    val totalAttempted: Int = 0,
    val totalCorrect: Int = 0,
    val netGateScore: Double = 0.0,
    val unlockMonitorEnabled: Boolean = false,
    val unlockCooldownMinutes: Int = 0,
    val darkExamTheme: Boolean = false,
    val lastUnlockNotifMs: Long = 0L,
    val updatedAtMs: Long = System.currentTimeMillis()
) {
    fun deckList(): List<String> =
        remainingTopicDeckCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}

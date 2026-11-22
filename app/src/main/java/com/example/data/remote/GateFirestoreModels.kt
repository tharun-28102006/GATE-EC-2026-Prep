package com.example.data.remote

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class FirestoreUserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "GATE EC Candidate",
    val dailyGoal: Int = 10,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastActiveDate: String = "",
    val lastSectionId: String = "",
    val remainingTopicDeck: List<String> = emptyList(),
    val totalAttempted: Int = 0,
    val totalCorrect: Int = 0,
    val netGateScore: Double = 0.0,
    val unlockMonitorEnabled: Boolean = false,
    val unlockCooldownMinutes: Int = 0,
    val darkExamTheme: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FirestoreSavedQuestion(
    @DocumentId val id: String = "",
    val userId: String = "",
    val sectionId: String = "",
    val subtopicId: String = "",
    val questionText: String = "",
    val options: List<String> = emptyList(),
    val correctIndex: Int = 0,
    val marks: Int = 1,
    val negativeMarks: Double = 0.33,
    val difficulty: String = "Medium",
    val explanation: String = "",
    val keyFormula: String = "",
    val source: String = "AI",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FirestoreAttempt(
    @DocumentId val id: String = "",
    val userId: String = "",
    val questionId: String = "",
    val sectionId: String = "",
    val subtopicId: String = "",
    val selectedIndex: Int = 0,
    val correctAnswer: Boolean = false,
    val scoreDelta: Double = 0.0,
    val timeSpentSec: Int = 0,
    val answeredVia: String = "IN_APP",
    val dateKey: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

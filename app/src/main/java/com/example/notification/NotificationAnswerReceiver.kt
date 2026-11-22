package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.RemoteInput
import com.example.data.repository.GateRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "NotificationAnswerRx"

class NotificationAnswerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val questionId = intent.getStringExtra(QuizNotificationHelper.EXTRA_QUESTION_ID) ?: return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = GateRepository(context.applicationContext)
                val userId = Firebase.auth.currentUser?.uid
                    ?: repo.getLatestProfile()?.userId
                    ?: "offline_local"

                when (intent.action) {
                    QuizNotificationHelper.ACTION_QUICK_ANSWER -> {
                        val replyBundle = RemoteInput.getResultsFromIntent(intent)
                        val rawInput = replyBundle?.getCharSequence(QuizNotificationHelper.KEY_QUICK_ANSWER)
                            ?.toString()
                            ?.trim()
                            ?.uppercase()
                            ?: return@launch

                        val selectedIdx = when {
                            rawInput.startsWith("A") || rawInput == "1" -> 0
                            rawInput.startsWith("B") || rawInput == "2" -> 1
                            rawInput.startsWith("C") || rawInput == "3" -> 2
                            rawInput.startsWith("D") || rawInput == "4" -> 3
                            else -> -1
                        }

                        val question = repo.getQuestionById(questionId) ?: return@launch
                        if (selectedIdx in 0..3) {
                            val attempt = repo.submitAnswer(
                                userId = userId,
                                question = question,
                                selectedIndex = selectedIdx,
                                timeSpentSec = 15,
                                answeredVia = "NOTIFICATION"
                            )
                            QuizNotificationHelper.postGradedResultNotification(
                                context = context.applicationContext,
                                question = question,
                                selectedIndex = selectedIdx,
                                isCorrect = attempt.correctAnswer,
                                scoreDelta = attempt.scoreDelta
                            )
                        } else {
                            // Re-post prompt if user typed invalid text
                            QuizNotificationHelper.postUnlockQuizNotification(
                                context.applicationContext,
                                question
                            )
                        }
                    }

                    QuizNotificationHelper.ACTION_SAVE_OFFLINE -> {
                        repo.saveOfflineById(userId, questionId)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling notification action", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

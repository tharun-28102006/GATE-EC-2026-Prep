package com.example.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.CachedQuestionEntity
import com.example.domain.GateEcSyllabus

object QuizNotificationHelper {
    const val CHANNEL_QUIZ = "gate_unlock_quiz_channel"
    const val CHANNEL_MONITOR = "gate_unlock_monitor_channel"

    const val NOTIF_ID_MONITOR = 4001
    const val NOTIF_ID_QUIZ = 4002

    const val ACTION_QUICK_ANSWER = "com.example.action.QUICK_ANSWER"
    const val ACTION_SAVE_OFFLINE = "com.example.action.SAVE_OFFLINE"

    const val KEY_QUICK_ANSWER = "key_quick_answer"
    const val EXTRA_QUESTION_ID = "extra_question_id"
    const val EXTRA_OPEN_QUESTION_ID = "extra_open_question_id"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val quizChannel = NotificationChannel(
                CHANNEL_QUIZ,
                "GATE Unlock Quiz",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Unobtrusive GATE 2026 EC quiz prompts triggered when you unlock your phone."
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                enableVibration(false)
            }

            val monitorChannel = NotificationChannel(
                CHANNEL_MONITOR,
                "Unlock Quiz Monitor",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Keeps the low-memory phone-unlock quiz listener ready."
                setShowBadge(false)
            }

            nm.createNotificationChannel(quizChannel)
            nm.createNotificationChannel(monitorChannel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun buildMonitorNotification(context: Context): android.app.Notification {
        ensureChannels(context)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPending = PendingIntent.getActivity(
            context,
            100,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_MONITOR)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("GATE EC 2026 Unlock Quiz Ready")
            .setContentText("Low-memory monitor active • New non-repeating topic on every unlock")
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(openPending)
            .build()
    }

    fun postUnlockQuizNotification(context: Context, question: CachedQuestionEntity) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)

        val section = GateEcSyllabus.sectionById(question.sectionId)
        val subtopic = GateEcSyllabus.subtopicById(question.subtopicId)
        val marksTag = "+${question.marks} / -${question.negativeMarks}"

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_QUESTION_ID, question.id)
        }
        val openPending = PendingIntent.getActivity(
            context,
            question.id.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Direct lock-screen quick-answer via RemoteInput with smart choices A, B, C, D
        val remoteInput = RemoteInput.Builder(KEY_QUICK_ANSWER)
            .setLabel("Tap or type A, B, C, or D")
            .setChoices(arrayOf("A", "B", "C", "D"))
            .build()

        val answerIntent = Intent(context, NotificationAnswerReceiver::class.java).apply {
            action = ACTION_QUICK_ANSWER
            putExtra(EXTRA_QUESTION_ID, question.id)
        }
        val answerFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val answerPending = PendingIntent.getBroadcast(
            context,
            2001,
            answerIntent,
            answerFlags
        )

        val answerAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_send,
            "Answer (A–D)",
            answerPending
        )
            .addRemoteInput(remoteInput)
            .setAllowGeneratedReplies(false)
            .build()

        val saveIntent = Intent(context, NotificationAnswerReceiver::class.java).apply {
            action = ACTION_SAVE_OFFLINE
            putExtra(EXTRA_QUESTION_ID, question.id)
        }
        val savePending = PendingIntent.getBroadcast(
            context,
            2002,
            saveIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val expandedBody = buildString {
            appendLine(question.questionText)
            appendLine()
            appendLine("A) ${question.optionA}")
            appendLine("B) ${question.optionB}")
            appendLine("C) ${question.optionC}")
            append("D) ${question.optionD}")
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_QUIZ)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("GATE EC • ${section.shortName} ($marksTag)")
            .setContentText(question.questionText)
            .setSubText(subtopic.name)
            .setStyle(NotificationCompat.BigTextStyle().bigText(expandedBody))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openPending)
            .addAction(answerAction)
            .addAction(android.R.drawable.ic_menu_save, "Save Offline", savePending)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIF_ID_QUIZ, notification)
    }

    fun postGradedResultNotification(
        context: Context,
        question: CachedQuestionEntity,
        selectedIndex: Int,
        isCorrect: Boolean,
        scoreDelta: Double
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)

        val correctLetter = ('A' + question.correctIndex).toString()
        val chosenLetter = ('A' + selectedIndex).toString()
        val scoreStr = if (scoreDelta >= 0) "+%.2f".format(scoreDelta) else "%.2f".format(scoreDelta)

        val title = if (isCorrect) {
            "Correct ($scoreStr marks) • Option $correctLetter"
        } else {
            "Incorrect ($scoreStr) • You: $chosenLetter, Ans: $correctLetter"
        }

        val body = buildString {
            if (question.keyFormula.isNotBlank()) {
                appendLine("Formula: ${question.keyFormula}")
                appendLine()
            }
            append(question.explanation.take(420))
            if (question.explanation.length > 420) append("… (Tap for full derivation)")
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_QUESTION_ID, question.id)
        }
        val openPending = PendingIntent.getActivity(
            context,
            question.id.hashCode() + 7,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_QUIZ)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText("Tap to open full step-by-step GATE derivation")
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openPending)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIF_ID_QUIZ, notification)
    }
}

package com.example.ui.practice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.CachedQuestionEntity
import com.example.domain.GateEcSyllabus
import com.example.ui.PracticeUiState
import com.example.ui.ProgressSummary
import com.example.ui.theme.GateIcons
import com.example.ui.theme.IbmPlexMonoFamily
import com.example.ui.theme.LocalGateColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeDeskScreen(
    practiceState: PracticeUiState,
    progressSummary: ProgressSummary,
    unseenPoolCount: Int,
    onSelectOption: (Int) -> Unit,
    onClearSelection: () -> Unit,
    onSubmitAnswer: () -> Unit,
    onNextQuestion: (Boolean) -> Unit,
    onToggleSave: () -> Unit,
    onDismissBanner: () -> Unit
) {
    val gateColors = LocalGateColors.current
    var showTopicPalette by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Official GATE CBT Top Exam Header Strip
        GateExamTopHeader(
            elapsedSeconds = practiceState.elapsedSeconds,
            isOnline = practiceState.isOnline,
            remainingTopicsInCycle = progressSummary.remainingTopicsInCycle,
            onOpenPalette = { showTopicPalette = true }
        )

        // Optional status notification banner
        AnimatedVisibility(visible = practiceState.statusBanner != null) {
            practiceState.statusBanner?.let { banner ->
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = banner,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onDismissBanner,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Dismiss status banner",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            when {
                practiceState.isLoading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Preparing Next Non-Repeating GATE EC Topic...",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Offline buffer: $unseenPoolCount ready • Zero topic repetition",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                practiceState.errorMessage != null && practiceState.question == null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = gateColors.wrongContainer,
                            border = BorderStroke(1.dp, gateColors.wrong.copy(alpha = 0.4f)),
                            modifier = Modifier.widthIn(max = 460.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Question Load Interrupted",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = gateColors.wrong
                                )
                                Text(
                                    text = practiceState.errorMessage,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Button(
                                    onClick = { onNextQuestion(false) },
                                    modifier = Modifier.testTag("retry_question_button")
                                ) {
                                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Retry")
                                }
                            }
                        }
                    }
                }

                practiceState.question != null -> {
                    val question = practiceState.question
                    val section = GateEcSyllabus.sectionById(question.sectionId)
                    val subtopic = GateEcSyllabus.subtopicById(question.subtopicId)

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 720.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // GATE CBT Section + Marking Strip
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, gateColors.border),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = gateColors.examHeader
                                        ) {
                                            Text(
                                                text = section.code,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = gateColors.examHeaderContent,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = section.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = gateColors.answeredContainer
                                    ) {
                                        Text(
                                            text = "+${question.marks} / -${question.negativeMarks}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = gateColors.answered,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Topic: ${subtopic.name}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        DifficultyPill(question.difficulty)
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                text = if (question.source == "AI") "AI 2026" else "OFFLINE",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Official Exam Paper Question Card
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, gateColors.border),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("question_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "QUESTION • MCQ (SINGLE CORRECT)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    TextButton(
                                        onClick = onToggleSave,
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.testTag("save_offline_button")
                                    ) {
                                        Icon(
                                            imageVector = if (question.savedOffline) GateIcons.Bookmark else GateIcons.BookmarkBorder,
                                            contentDescription = "Save question for offline review",
                                            tint = if (question.savedOffline) gateColors.review else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = if (question.savedOffline) "Saved Offline" else "Save Offline",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (question.savedOffline) gateColors.review else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                HorizontalDivider(color = gateColors.border)

                                Text(
                                    text = question.questionText,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // 4 Bordered GATE Radio Options (A, B, C, D)
                        val options = question.optionsList()
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            options.forEachIndexed { idx, optText ->
                                GateOptionTile(
                                    optionLetter = ('A' + idx).toString(),
                                    optionText = optText,
                                    isSelected = practiceState.selectedOptionIndex == idx,
                                    isSubmitted = practiceState.isSubmitted,
                                    isCorrectOption = idx == question.correctIndex,
                                    onClick = { onSelectOption(idx) },
                                    modifier = Modifier.testTag("option_tile_$idx")
                                )
                            }
                        }

                        // Post-Submission Detailed Explanation Panel
                        AnimatedVisibility(
                            visible = practiceState.isSubmitted,
                            enter = fadeIn() + expandVertically()
                        ) {
                            DetailedExplanationPanel(
                                question = question,
                                selectedIndex = practiceState.selectedOptionIndex ?: -1,
                                scoreDelta = practiceState.lastScoreDelta
                            )
                        }

                        // Fresh AI Question Shortcut when online
                        if (practiceState.isSubmitted && practiceState.isOnline) {
                            OutlinedButton(
                                onClick = { onNextQuestion(true) },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("generate_fresh_ai_button")
                            ) {
                                Icon(
                                    imageVector = GateIcons.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Generate Fresh AI Question (Next Topic)")
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }

        // Authentic GATE CBT Bottom Action Strip
        if (practiceState.question != null && !practiceState.isLoading) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, gateColors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!practiceState.isSubmitted) {
                        OutlinedButton(
                            onClick = onClearSelection,
                            enabled = practiceState.selectedOptionIndex != null,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("clear_response_button")
                        ) {
                            Text("Clear")
                        }
                        Button(
                            onClick = onSubmitAnswer,
                            enabled = practiceState.selectedOptionIndex != null,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("submit_answer_button")
                        ) {
                            Text(
                                text = "Submit & Check Solution",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    } else {
                        Button(
                            onClick = { onNextQuestion(false) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = gateColors.answered
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("next_question_button")
                        ) {
                            Text(
                                text = "Next Question (Different Topic)",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showTopicPalette) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showTopicPalette = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            GateTopicPaletteSheet(
                progressSummary = progressSummary,
                unseenPoolCount = unseenPoolCount,
                onDismiss = { showTopicPalette = false }
            )
        }
    }
}

@Composable
private fun GateExamTopHeader(
    elapsedSeconds: Int,
    isOnline: Boolean,
    remainingTopicsInCycle: Int,
    onOpenPalette: () -> Unit
) {
    val gateColors = LocalGateColors.current
    val mins = elapsedSeconds / 60
    val secs = elapsedSeconds % 60
    val formattedTime = "%02d:%02d".format(mins, secs)

    Surface(
        color = gateColors.examHeader,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "GATE 2026 • EC",
                        style = MaterialTheme.typography.titleMedium,
                        color = gateColors.examHeaderContent,
                        fontWeight = FontWeight.Bold
                    )
                    if (!isOnline) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = gateColors.streak
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = GateIcons.CloudOff,
                                    contentDescription = "Offline mode",
                                    tint = gateColors.examHeaderContent,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "OFFLINE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = gateColors.examHeaderContent
                                )
                            }
                        }
                    }
                }
                Text(
                    text = "Non-Repeating Cycle: $remainingTopicsInCycle/36 topics left",
                    style = MaterialTheme.typography.labelSmall,
                    color = gateColors.examHeaderContent.copy(alpha = 0.8f)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = gateColors.examHeaderContent.copy(alpha = 0.14f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = GateIcons.Timer,
                            contentDescription = "Question timer",
                            tint = gateColors.examHeaderContent,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.labelMedium,
                            color = gateColors.examHeaderContent,
                            fontFamily = IbmPlexMonoFamily
                        )
                    }
                }

                IconButton(
                    onClick = onOpenPalette,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("open_topic_palette_button")
                ) {
                    Icon(
                        imageVector = GateIcons.GridView,
                        contentDescription = "Open GATE EC Topic Palette",
                        tint = gateColors.examHeaderContent
                    )
                }
            }
        }
    }
}

@Composable
private fun GateOptionTile(
    optionLetter: String,
    optionText: String,
    isSelected: Boolean,
    isSubmitted: Boolean,
    isCorrectOption: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gateColors = LocalGateColors.current

    val containerColor = when {
        isSubmitted && isCorrectOption -> gateColors.answeredContainer
        isSubmitted && isSelected && !isCorrectOption -> gateColors.wrongContainer
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

    val borderColor = when {
        isSubmitted && isCorrectOption -> gateColors.answered
        isSubmitted && isSelected && !isCorrectOption -> gateColors.wrong
        isSelected -> MaterialTheme.colorScheme.primary
        else -> gateColors.border
    }

    val badgeBg = when {
        isSubmitted && isCorrectOption -> gateColors.answered
        isSubmitted && isSelected && !isCorrectOption -> gateColors.wrong
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val badgeInk = when {
        isSubmitted && (isCorrectOption || isSelected) -> MaterialTheme.colorScheme.onPrimary
        isSelected -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = containerColor,
        border = BorderStroke(if (isSelected || (isSubmitted && isCorrectOption)) 2.dp else 1.dp, borderColor),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = !isSubmitted, onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(badgeBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = optionLetter,
                    style = MaterialTheme.typography.labelMedium,
                    color = badgeInk,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = optionText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            if (isSubmitted && isCorrectOption) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Correct option",
                    tint = gateColors.answered,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun DetailedExplanationPanel(
    question: CachedQuestionEntity,
    selectedIndex: Int,
    scoreDelta: Double
) {
    val gateColors = LocalGateColors.current
    val isCorrect = selectedIndex == question.correctIndex
    val correctLetter = ('A' + question.correctIndex).toString()
    val scoreText = if (scoreDelta >= 0) "+%.2f".format(scoreDelta) else "%.2f".format(scoreDelta)

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, if (isCorrect) gateColors.answered else gateColors.wrong),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("detailed_explanation_panel")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Verdict Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isCorrect) gateColors.answeredContainer else gateColors.wrongContainer)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = GateIcons.Lightbulb,
                        contentDescription = null,
                        tint = if (isCorrect) gateColors.answered else gateColors.wrong,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (isCorrect) "Correct! Answer: Option $correctLetter" else "Incorrect • Correct Answer: Option $correctLetter",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (isCorrect) gateColors.answered else gateColors.wrong,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isCorrect) gateColors.answered else gateColors.wrong
                ) {
                    Text(
                        text = "$scoreText pts",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (question.keyFormula.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = gateColors.formulaBg,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = GateIcons.Functions,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "KEY FORMULA / LAW",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = question.keyFormula,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = IbmPlexMonoFamily,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Text(
                    text = "DETAILED SOLUTION & EXPLANATION",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = question.explanation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun DifficultyPill(difficulty: String) {
    val gateColors = LocalGateColors.current
    val (bg, ink) = when (difficulty) {
        "Easy" -> gateColors.answeredContainer to gateColors.answered
        "Hard" -> gateColors.wrongContainer to gateColors.wrong
        else -> gateColors.streakContainer to gateColors.streak
    }
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = bg
    ) {
        Text(
            text = difficulty.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = ink,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun GateTopicPaletteSheet(
    progressSummary: ProgressSummary,
    unseenPoolCount: Int,
    onDismiss: () -> Unit
) {
    val gateColors = LocalGateColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "GATE 2026 EC Syllabus Palette",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${progressSummary.remainingTopicsInCycle}/36 subtopics left in non-repeating rotation • $unseenPoolCount cached offline",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, contentDescription = "Close topic palette")
            }
        }

        HorizontalDivider(color = gateColors.border)

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(progressSummary.sectionMastery) { sec ->
                val statusColor = when {
                    sec.attempted == 0 -> gateColors.unvisited
                    sec.accuracyPercent >= 70 -> gateColors.answeredContainer
                    else -> gateColors.streakContainer
                }
                val badgeInk = when {
                    sec.attempted == 0 -> MaterialTheme.colorScheme.onSurfaceVariant
                    sec.accuracyPercent >= 70 -> gateColors.answered
                    else -> gateColors.streak
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, gateColors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = statusColor
                            ) {
                                Text(
                                    text = sec.code,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = badgeInk,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = sec.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${sec.attempted} answered • ${sec.correct} correct",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = if (sec.attempted == 0) "—" else "${sec.accuracyPercent}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = badgeInk,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

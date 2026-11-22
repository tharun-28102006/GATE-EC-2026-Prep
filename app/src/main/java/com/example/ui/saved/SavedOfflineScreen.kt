package com.example.ui.saved

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.CachedQuestionEntity
import com.example.domain.GateEcSyllabus
import com.example.ui.theme.GateIcons
import com.example.ui.theme.IbmPlexMonoFamily
import com.example.ui.theme.LocalGateColors

@Composable
fun SavedOfflineScreen(
    savedQuestions: List<CachedQuestionEntity>,
    unseenPoolCount: Int,
    isRefillingPool: Boolean,
    onRefillPool: () -> Unit,
    onRemoveSaved: (CachedQuestionEntity) -> Unit,
    onRecordRetry: (CachedQuestionEntity, Int) -> Unit
) {
    val gateColors = LocalGateColors.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedSectionId by remember { mutableStateOf<String?>(null) }
    var retryQuestion by remember { mutableStateOf<CachedQuestionEntity?>(null) }

    val filtered = remember(savedQuestions, searchQuery, selectedSectionId) {
        savedQuestions.filter { q ->
            val matchesSection = selectedSectionId == null || q.sectionId == selectedSectionId
            val matchesQuery = searchQuery.isBlank() ||
                q.questionText.contains(searchQuery, ignoreCase = true) ||
                q.explanation.contains(searchQuery, ignoreCase = true) ||
                GateEcSyllabus.subtopicById(q.subtopicId).name.contains(searchQuery, ignoreCase = true)
            matchesSection && matchesQuery
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Surface(
            color = gateColors.examHeader,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Offline Saved Bank",
                        style = MaterialTheme.typography.titleLarge,
                        color = gateColors.examHeaderContent,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${savedQuestions.size} saved for offline review • Cloud synced",
                        style = MaterialTheme.typography.labelSmall,
                        color = gateColors.examHeaderContent.copy(alpha = 0.85f)
                    )
                }
                Icon(
                    imageVector = GateIcons.OfflinePin,
                    contentDescription = "Available offline anytime",
                    tint = gateColors.examHeaderContent
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Offline AI Pool Buffer Card
            item {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, gateColors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Offline AI Question Buffer",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$unseenPoolCount unseen GATE EC questions cached locally for airplane mode & unlock quizzes.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Button(
                            onClick = onRefillPool,
                            enabled = !isRefillingPool,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            modifier = Modifier.testTag("refill_offline_pool_button")
                        ) {
                            if (isRefillingPool) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(
                                    imageVector = GateIcons.CloudDownload,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                            Text(if (isRefillingPool) "Loading..." else "+3 AI")
                        }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search saved questions, formulas, topics...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search saved bank") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("saved_search_input")
                )
            }

            // Section Filter Chips
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedSectionId == null,
                            onClick = { selectedSectionId = null },
                            label = { Text("All (${savedQuestions.size})") }
                        )
                    }
                    items(GateEcSyllabus.sections) { sec ->
                        val count = savedQuestions.count { it.sectionId == sec.id }
                        if (count > 0) {
                            FilterChip(
                                selected = selectedSectionId == sec.id,
                                onClick = {
                                    selectedSectionId = if (selectedSectionId == sec.id) null else sec.id
                                },
                                label = { Text("${sec.code} ($count)") }
                            )
                        }
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, gateColors.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = GateIcons.OfflinePin,
                                contentDescription = null,
                                tint = gateColors.review,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = if (savedQuestions.isEmpty()) "No Saved Questions Yet" else "No Matches Found",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (savedQuestions.isEmpty()) {
                                    "Tap \"Save Offline\" on any Practice question or lock-screen notification to build your personal GATE 2026 EC revision deck. It works 100% offline and syncs across devices."
                                } else {
                                    "Try clearing your search or selecting a different EC syllabus section."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filtered, key = { it.id }) { question ->
                    SavedQuestionCard(
                        question = question,
                        onRemove = { onRemoveSaved(question) },
                        onRetry = { retryQuestion = question }
                    )
                }
            }
        }
    }

    retryQuestion?.let { q ->
        OfflineRetryDialog(
            question = q,
            onDismiss = { retryQuestion = null },
            onSubmit = { idx ->
                onRecordRetry(q, idx)
            }
        )
    }
}

@Composable
private fun SavedQuestionCard(
    question: CachedQuestionEntity,
    onRemove: () -> Unit,
    onRetry: () -> Unit
) {
    val gateColors = LocalGateColors.current
    val section = GateEcSyllabus.sectionById(question.sectionId)
    val subtopic = GateEcSyllabus.subtopicById(question.subtopicId)
    var expanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, gateColors.border),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("saved_question_card_${question.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                        color = gateColors.reviewContainer
                    ) {
                        Text(
                            text = section.code,
                            style = MaterialTheme.typography.labelSmall,
                            color = gateColors.review,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = subtopic.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = GateIcons.BookmarkRemove,
                        contentDescription = "Remove from saved offline bank",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Text(
                text = question.questionText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onRetry,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(GateIcons.Replay, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Retry Offline")
                }

                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Hide Solution" else "View Solution")
                    Icon(
                        imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = null
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HorizontalDivider(color = gateColors.border)
                    val correctLetter = ('A' + question.correctIndex).toString()
                    val correctText = question.optionsList().getOrElse(question.correctIndex) { "" }
                    Text(
                        text = "Correct Answer: ($correctLetter) $correctText",
                        style = MaterialTheme.typography.titleSmall,
                        color = gateColors.answered,
                        fontWeight = FontWeight.Bold
                    )
                    if (question.keyFormula.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = gateColors.formulaBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = question.keyFormula,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = IbmPlexMonoFamily),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                    Text(
                        text = question.explanation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun OfflineRetryDialog(
    question: CachedQuestionEntity,
    onDismiss: () -> Unit,
    onSubmit: (Int) -> Unit
) {
    val gateColors = LocalGateColors.current
    var selected by remember { mutableStateOf<Int?>(null) }
    var submitted by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Offline Quick Retry (+${question.marks}/-${question.negativeMarks})",
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = question.questionText,
                    style = MaterialTheme.typography.bodyMedium
                )
                question.optionsList().forEachIndexed { idx, opt ->
                    val isCorrect = idx == question.correctIndex
                    val bg = when {
                        submitted && isCorrect -> gateColors.answeredContainer
                        submitted && selected == idx && !isCorrect -> gateColors.wrongContainer
                        selected == idx -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = bg,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !submitted) { selected = idx }
                    ) {
                        Text(
                            text = "(${'A' + idx}) $opt",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                if (submitted) {
                    Text(
                        text = question.explanation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            if (!submitted) {
                Button(
                    onClick = {
                        selected?.let {
                            submitted = true
                            onSubmit(it)
                        }
                    },
                    enabled = selected != null
                ) {
                    Text("Check")
                }
            } else {
                Button(onClick = onDismiss) {
                    Text("Done")
                }
            }
        },
        dismissButton = {
            if (!submitted) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

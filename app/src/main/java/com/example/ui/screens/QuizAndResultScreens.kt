package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.LeaderboardEntry
import com.example.data.QuizSet
import com.example.data.TestAttempt
import com.example.data.UiState
import com.example.ui.viewmodel.ActiveQuizUiState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    state: ActiveQuizUiState,
    onSelectOption: (Int, Int) -> Unit,
    onJumpToQuestion: (Int) -> Unit,
    onSubmit: () -> Unit,
    onExit: () -> Unit
) {
    val quiz = state.quiz ?: return
    var showConfirmExit by remember { mutableStateOf(false) }
    var showConfirmSubmit by remember { mutableStateOf(false) }

    BackHandler { showConfirmExit = true }

    val qIndex = state.currentQuestionIndex.coerceIn(0, (quiz.questions.size - 1).coerceAtLeast(0))
    val currentQuestion = quiz.questions.getOrNull(qIndex)
    val selectedOption = state.selectedAnswers.getOrNull(qIndex) ?: -1
    val answeredCount = state.selectedAnswers.count { it >= 0 }

    val mins = state.secondsRemaining / 60
    val secs = state.secondsRemaining % 60
    val timeText = String.format(Locale.US, "%02d:%02d", mins, secs)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = quiz.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1
                        )
                        Text(
                            text = "${state.subjectName} • $answeredCount/${quiz.questions.size} answered",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { showConfirmExit = true }, modifier = Modifier.testTag("quiz_exit_button")) {
                        Icon(Icons.Rounded.Close, contentDescription = "Exit Test")
                    }
                },
                actions = {
                    Surface(
                        color = if (state.secondsRemaining < 60) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.Timer,
                                contentDescription = "Time left",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = timeText,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                LinearProgressIndicator(
                    progress = { (qIndex + 1).toFloat() / quiz.questions.size.coerceAtLeast(1) },
                    modifier = Modifier.fillMaxWidth()
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(quiz.questions) { idx, _ ->
                        val isCurrent = idx == qIndex
                        val isAnswered = (state.selectedAnswers.getOrNull(idx) ?: -1) >= 0
                        Surface(
                            onClick = { onJumpToQuestion(idx) },
                            shape = CircleShape,
                            color = when {
                                isCurrent -> MaterialTheme.colorScheme.primary
                                isAnswered -> MaterialTheme.colorScheme.tertiaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${idx + 1}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = when {
                                        isCurrent -> MaterialTheme.colorScheme.onPrimary
                                        isAnswered -> MaterialTheme.colorScheme.onTertiaryContainer
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }

                if (!state.errorMessage.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = state.errorMessage,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                if (currentQuestion != null) {
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Text(
                                    text = "Question ${qIndex + 1} of ${quiz.questions.size}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = currentQuestion.question,
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }

                            itemsIndexed(currentQuestion.options) { optIdx, optText ->
                                val letter = listOf("A", "B", "C", "D").getOrElse(optIdx) { "?" }
                                val isPicked = selectedOption == optIdx
                                Surface(
                                    onClick = { onSelectOption(qIndex, optIdx) },
                                    shape = MaterialTheme.shapes.medium,
                                    color = if (isPicked) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                    },
                                    border = if (isPicked) {
                                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                                    } else {
                                        BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("quiz_option_${qIndex}_$optIdx")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isPicked) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.surface
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = letter,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = if (isPicked) MaterialTheme.colorScheme.onPrimary
                                                else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = optText,
                                            style = MaterialTheme.typography.bodyLarge,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { onJumpToQuestion(qIndex - 1) },
                        enabled = qIndex > 0,
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Prev")
                    }

                    if (qIndex < quiz.questions.lastIndex) {
                        Button(
                            onClick = { onJumpToQuestion(qIndex + 1) },
                            modifier = Modifier.weight(1f).height(50.dp)
                        ) {
                            Text("Next")
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    } else {
                        Button(
                            onClick = { showConfirmSubmit = true },
                            enabled = !state.isSubmitting,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("quiz_submit_button")
                        ) {
                            if (state.isSubmitting) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                            } else {
                                Text("Submit Test")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showConfirmSubmit) {
        AlertDialog(
            onDismissRequest = { showConfirmSubmit = false },
            title = { Text("Submit Objective Test?") },
            text = {
                Text("You answered $answeredCount out of ${quiz.questions.size} questions. Ready to view your Bihar Board score report & rank?")
            },
            confirmButton = {
                Button(onClick = {
                    showConfirmSubmit = false
                    onSubmit()
                }) {
                    Text("Submit Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmSubmit = false }) {
                    Text("Review")
                }
            }
        )
    }

    if (showConfirmExit) {
        AlertDialog(
            onDismissRequest = { showConfirmExit = false },
            title = { Text("Exit Practice Test?") },
            text = { Text("Your current answers will not be saved.") },
            confirmButton = {
                Button(onClick = {
                    showConfirmExit = false
                    onExit()
                }) {
                    Text("Exit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmExit = false }) {
                    Text("Continue Test")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestResultScreen(
    attempt: TestAttempt,
    quiz: QuizSet?,
    leaderboardState: UiState<List<LeaderboardEntry>>,
    currentUserId: String,
    onDone: () -> Unit
) {
    BackHandler { onDone() }

    var showWrongOnly by remember { mutableStateOf(false) }
    val leaderboard = (leaderboardState as? UiState.Success)?.data.orEmpty()
    val myRank = remember(leaderboard, currentUserId) {
        val idx = leaderboard.indexOfFirst { it.userId == currentUserId }
        if (idx >= 0) idx + 1 else null
    }
    val pctText = String.format(Locale.US, "%.1f%%", attempt.percentage)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Test Performance Report") },
                navigationIcon = {
                    IconButton(onClick = onDone, modifier = Modifier.testTag("result_done_button")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = attempt.quizTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            Text(
                                text = pctText,
                                style = MaterialTheme.typography.displaySmall,
                                color = Color.White
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.secondary,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Rounded.EmojiEvents,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (myRank != null) "Live Rank: #$myRank" else "Rank Syncing...",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSecondary
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                StatPill("Total", "${attempt.totalQuestions}", Color.White)
                                StatPill("Correct", "${attempt.correctCount}", Color(0xFF6EE7B7))
                                StatPill("Wrong", "${attempt.wrongCount}", Color(0xFFFCA5A5))
                            }
                        }
                    }
                }

                if (quiz != null) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Answer Key & Review",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = !showWrongOnly,
                                    onClick = { showWrongOnly = false },
                                    label = { Text("All") }
                                )
                                FilterChip(
                                    selected = showWrongOnly,
                                    onClick = { showWrongOnly = true },
                                    label = { Text("Wrong Only") }
                                )
                            }
                        }
                    }

                    val reviewItems = quiz.questions.mapIndexed { index, question ->
                        val picked = attempt.answers.getOrNull(index)?.toInt() ?: -1
                        Triple(index, question, picked)
                    }.filter { (_, question, picked) ->
                        !showWrongOnly || picked != question.correctIndex.toInt()
                    }

                    if (reviewItems.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Rounded.CheckCircle,
                                title = "100% Accuracy!",
                                subtitle = "Zero wrong answers in this test. Awesome preparation!"
                            )
                        }
                    } else {
                        itemsIndexed(reviewItems) { _, (qIdx, question, picked) ->
                            val isCorrect = picked == question.correctIndex.toInt()
                            val correctOpt = question.options.getOrElse(question.correctIndex.toInt()) { "" }
                            val pickedOpt = if (picked in question.options.indices) question.options[picked] else "Skipped"

                            Card(
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(
                                    1.dp,
                                    if (isCorrect) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)
                                    else MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (isCorrect) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
                                            contentDescription = if (isCorrect) "Correct" else "Wrong",
                                            tint = if (isCorrect) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Q${qIdx + 1}: ${question.question}",
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                    }

                                    Text(
                                        text = "Your Answer: $pickedOpt",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isCorrect) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                                    )
                                    if (!isCorrect) {
                                        Text(
                                            text = "Correct Answer: $correctOpt",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                    if (question.explanation.isNotBlank()) {
                                        HorizontalDivider()
                                        Text(
                                            text = "Explanation: ${question.explanation}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Button(
                        onClick = onDone,
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Text("Back to Subjects")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleLarge, color = valueColor, fontWeight = FontWeight.Bold)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f))
    }
}

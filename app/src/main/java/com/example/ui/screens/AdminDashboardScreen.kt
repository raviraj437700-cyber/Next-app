package com.example.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Publish
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ChapterFolder
import com.example.data.McqQuestion
import com.example.data.QuizSet
import com.example.data.SubjectFolder
import com.example.data.TestAttempt
import com.example.data.UiState
import com.example.data.UserProfile
import com.example.ui.theme.PdfRedBadge
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.AiPdfUiState
import java.util.Locale

@Composable
fun AdminDashboardScreen(
    adminViewModel: AdminViewModel,
    onBackToStudentApp: () -> Unit
) {
    BackHandler { onBackToStudentApp() }

    val usersState by adminViewModel.allUsers.collectAsStateWithLifecycle()
    val attempts by adminViewModel.allAttempts.collectAsStateWithLifecycle()
    val subjectsState by adminViewModel.allSubjects.collectAsStateWithLifecycle()
    val chapters by adminViewModel.allChapters.collectAsStateWithLifecycle()
    val quizzes by adminViewModel.allQuizzes.collectAsStateWithLifecycle()
    val chatThreads by adminViewModel.allChatThreads.collectAsStateWithLifecycle()
    val selectedChatStudentId by adminViewModel.selectedChatStudentId.collectAsStateWithLifecycle()
    val selectedChatMessages by adminViewModel.selectedChatMessages.collectAsStateWithLifecycle()
    val announcements by adminViewModel.allAnnouncements.collectAsStateWithLifecycle()
    val studyGroups by adminViewModel.allStudyGroups.collectAsStateWithLifecycle()
    val aiState by adminViewModel.aiState.collectAsStateWithLifecycle()
    val toastMsg by adminViewModel.adminToast.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(toastMsg) {
        toastMsg?.let {
            snackbarHostState.showSnackbar(it)
            adminViewModel.clearToast()
        }
    }

    val users = (usersState as? UiState.Success)?.data.orEmpty()
    val subjects = (subjectsState as? UiState.Success)?.data.orEmpty()

    // 0 = Dashboard, 1 = Subjects & Chapters (with inline PDF upload), 2 = Chats & Groups, 3 = Notices
    var activeTab by remember { mutableIntStateOf(0) }
    var triggerAddSubject by remember { mutableStateOf(false) }
    var triggerInlinePdfMode by remember { mutableStateOf(false) }

    SoftStudyBackdrop {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)) {
                    NavigationBarItem(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        icon = { Icon(Icons.Rounded.Dashboard, contentDescription = "Dashboard") },
                        label = { Text("Dashboard") }
                    )
                    NavigationBarItem(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        icon = { Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = "Subjects") },
                        label = { Text("Subjects") }
                    )
                    NavigationBarItem(
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        icon = { Icon(Icons.AutoMirrored.Rounded.Chat, contentDescription = "Chat") },
                        label = { Text("Chats") }
                    )
                    NavigationBarItem(
                        selected = activeTab == 3,
                        onClick = { activeTab = 3 },
                        icon = { Icon(Icons.Rounded.Campaign, contentDescription = "Notices") },
                        label = { Text("Notices") }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .statusBarsPadding(),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 720.dp)
                ) {
                    // Clean Top Header bar matching Admin Dashboard mockup
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBackToStudentApp,
                            modifier = Modifier.testTag("admin_back_button")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to Student View")
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Admin Panel",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Manage Subjects, Chapter PDFs, AI Tests & Users",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when (activeTab) {
                            0 -> AdminOverviewTab(
                                users = users,
                                attempts = attempts,
                                subjectsCount = subjects.size,
                                quizzesCount = quizzes.size,
                                onSeedStarter = { adminViewModel.seedStarterPack() },
                                onQuickAddSubject = {
                                    triggerAddSubject = true
                                    activeTab = 1
                                },
                                onQuickGenerateQuiz = {
                                    triggerInlinePdfMode = true
                                    activeTab = 1
                                },
                                onQuickSendNotice = { activeTab = 3 }
                            )
                            1 -> AdminFoldersAndPdfTab(
                                subjects = subjects,
                                chapters = chapters,
                                quizzes = quizzes,
                                aiState = aiState,
                                triggerAddSubject = triggerAddSubject,
                                onConsumedAddSubject = { triggerAddSubject = false },
                                triggerInlinePdfMode = triggerInlinePdfMode,
                                onConsumedInlinePdf = { triggerInlinePdfMode = false },
                                onSaveSubject = adminViewModel::saveSubject,
                                onDeleteSubject = adminViewModel::deleteSubject,
                                onSaveChapter = adminViewModel::saveChapter,
                                onDeleteChapter = adminViewModel::deleteChapter,
                                onSaveQuiz = adminViewModel::saveQuiz,
                                onDeleteQuiz = adminViewModel::deleteQuiz,
                                onGenerateFromPdf = adminViewModel::generateQuizFromPdf,
                                onUpdateDraftQuestion = adminViewModel::updateDraftQuestion,
                                onRemoveDraftQuestion = adminViewModel::removeDraftQuestion,
                                onPublishDraft = adminViewModel::publishAiDraft,
                                onResetAiState = adminViewModel::resetAiState
                            )
                            2 -> AdminChatInboxTab(
                                threads = chatThreads,
                                studyGroups = studyGroups,
                                selectedStudentId = selectedChatStudentId,
                                messages = selectedChatMessages,
                                onSelectThread = adminViewModel::selectChatThread,
                                onSendReply = adminViewModel::sendAdminReply,
                                onDeleteGroup = adminViewModel::deleteStudyGroup
                            )
                            else -> AdminAnnouncementsTab(
                                users = users,
                                announcements = announcements,
                                onSendAnnouncement = adminViewModel::sendAnnouncement,
                                onDeleteAnnouncement = adminViewModel::deleteAnnouncement
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminOverviewTab(
    users: List<UserProfile>,
    attempts: List<TestAttempt>,
    subjectsCount: Int,
    quizzesCount: Int,
    onSeedStarter: () -> Unit,
    onQuickAddSubject: () -> Unit,
    onQuickGenerateQuiz: () -> Unit,
    onQuickSendNotice: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 2x2 Vibrant Gradient KPI Cards matching Admin Dashboard mockup
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AdminGradientKpiCard(
                        label = "Total Users",
                        value = "${users.size}",
                        icon = Icons.Rounded.People,
                        colors = listOf(Color(0xFF38BDF8), Color(0xFF2563EB)),
                        modifier = Modifier.weight(1f)
                    )
                    AdminGradientKpiCard(
                        label = "Total Quizzes",
                        value = "$quizzesCount",
                        icon = Icons.Rounded.Quiz,
                        colors = listOf(Color(0xFFC084FC), Color(0xFF7E22CE)),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AdminGradientKpiCard(
                        label = "Test Attempts",
                        value = "${attempts.size}",
                        icon = Icons.Rounded.CheckCircle,
                        colors = listOf(Color(0xFF34D399), Color(0xFF059669)),
                        modifier = Modifier.weight(1f)
                    )
                    AdminGradientKpiCard(
                        label = "Total Subjects",
                        value = "$subjectsCount",
                        icon = Icons.Rounded.Folder,
                        colors = listOf(Color(0xFF818CF8), Color(0xFF4338CA)),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Quick Actions Row matching mockup
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Quick Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AdminQuickActionChip(
                        icon = Icons.Rounded.Add,
                        label = "Add Subject",
                        tint = Color(0xFF7C3AED),
                        bg = Color(0xFFEDE9FE),
                        onClick = onQuickAddSubject,
                        modifier = Modifier.weight(1f)
                    )
                    AdminQuickActionChip(
                        icon = Icons.Rounded.PictureAsPdf,
                        label = "PDF Quiz",
                        tint = PdfRedBadge,
                        bg = Color(0xFFFEE2E2),
                        onClick = onQuickGenerateQuiz,
                        modifier = Modifier.weight(1f)
                    )
                    AdminQuickActionChip(
                        icon = Icons.Rounded.Campaign,
                        label = "Send Notice",
                        tint = Color(0xFF2563EB),
                        bg = Color(0xFFDBEAFE),
                        onClick = onQuickSendNotice,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (subjectsCount == 0) {
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Quick Start: Bihar Board Curriculum",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Add Class 10 Maths, Science, Social Science folders + sample bilingual objective sets in 1 tap.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Button(onClick = onSeedStarter, modifier = Modifier.testTag("admin_seed_starter_button")) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Seed Bihar Board Starter Pack")
                        }
                    }
                }
            }
        }

        item {
            Text("Student Performance Tracking", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        if (users.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Rounded.People,
                    title = "No Students Registered Yet",
                    subtitle = "Students signing in via Google will appear here with their live scores & average percentages."
                )
            }
        } else {
            items(users, key = { it.userId }) { user ->
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = user.displayName.take(1).uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(user.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${user.email} • ${user.boardClass}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "${user.totalScore} pts",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${user.testsTaken} tests • ${String.format(Locale.US, "%.1f%%", user.averagePercent)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminGradientKpiCard(
    label: String,
    value: String,
    icon: ImageVector,
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .shadow(6.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(colors))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.9f))
                Spacer(modifier = Modifier.height(4.dp))
                Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun AdminQuickActionChip(
    icon: ImageVector,
    label: String,
    tint: Color,
    bg: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        shadowElevation = 2.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
            }
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

/**
 * Subjects -> Chapters -> Inline Chapter PDF Upload & AI Quiz Generator (matching user's mockup & request).
 */
@Composable
private fun AdminFoldersAndPdfTab(
    subjects: List<SubjectFolder>,
    chapters: List<ChapterFolder>,
    quizzes: List<QuizSet>,
    aiState: AiPdfUiState,
    triggerAddSubject: Boolean,
    onConsumedAddSubject: () -> Unit,
    triggerInlinePdfMode: Boolean,
    onConsumedInlinePdf: () -> Unit,
    onSaveSubject: (String?, String, String, String, String, Long, Boolean) -> Unit,
    onDeleteSubject: (String) -> Unit,
    onSaveChapter: (String?, String, String, String, Long, Boolean) -> Unit,
    onDeleteChapter: (String) -> Unit,
    onSaveQuiz: (String?, String, String, String, Long, String, Boolean, List<McqQuestion>) -> Unit,
    onDeleteQuiz: (String) -> Unit,
    onGenerateFromPdf: (android.content.Context, Uri, String, String, Int, String) -> Unit,
    onUpdateDraftQuestion: (Int, McqQuestion) -> Unit,
    onRemoveDraftQuestion: (Int) -> Unit,
    onPublishDraft: (String, String, String, Long, Boolean) -> Unit,
    onResetAiState: () -> Unit
) {
    val context = LocalContext.current

    var selectedSubjectId by remember(subjects) { mutableStateOf(subjects.firstOrNull()?.id) }
    val selectedSubject = subjects.firstOrNull { it.id == selectedSubjectId }
    val chaptersForSubject = remember(chapters, selectedSubjectId) {
        chapters.filter { it.subjectId == selectedSubjectId }
    }
    var selectedChapterId by remember(chaptersForSubject) { mutableStateOf(chaptersForSubject.firstOrNull()?.id) }
    val selectedChapter = chaptersForSubject.firstOrNull { it.id == selectedChapterId }

    // Drill-down: null = Subject/Chapter Folder browser; non-null = Inline Chapter PDF AI Quiz Generator!
    var pdfTargetChapter by remember { mutableStateOf<ChapterFolder?>(null) }

    var showSubjectDialog by remember { mutableStateOf<SubjectFolder?>(null) }
    var isCreatingSubject by remember { mutableStateOf(false) }

    var showChapterDialog by remember { mutableStateOf<ChapterFolder?>(null) }
    var isCreatingChapter by remember { mutableStateOf(false) }

    var showQuizDialog by remember { mutableStateOf<QuizSet?>(null) }
    var isCreatingQuiz by remember { mutableStateOf(false) }

    LaunchedEffect(triggerAddSubject) {
        if (triggerAddSubject) {
            isCreatingSubject = true
            onConsumedAddSubject()
        }
    }

    LaunchedEffect(triggerInlinePdfMode) {
        if (triggerInlinePdfMode) {
            pdfTargetChapter = selectedChapter ?: chaptersForSubject.firstOrNull() ?: chapters.firstOrNull()
            onConsumedInlinePdf()
        }
    }

    // Inline Chapter PDF Upload & AI Question Generator screen (e.g. Maths -> Real Numbers -> Upload PDF)
    val activePdfChapter = pdfTargetChapter
    if (activePdfChapter != null) {
        val parentSubject = subjects.firstOrNull { it.id == activePdfChapter.subjectId } ?: selectedSubject
        if (parentSubject != null) {
            InlineChapterPdfGeneratorView(
                subject = parentSubject,
                chapter = activePdfChapter,
                aiState = aiState,
                onBack = {
                    onResetAiState()
                    pdfTargetChapter = null
                },
                onGenerateFromPdf = { uri, count, lang ->
                    onGenerateFromPdf(context, uri, parentSubject.name, activePdfChapter.title, count, lang)
                },
                onUpdateDraftQuestion = onUpdateDraftQuestion,
                onRemoveDraftQuestion = onRemoveDraftQuestion,
                onPublishDraft = { title, duration, publishNow ->
                    onPublishDraft(parentSubject.id, activePdfChapter.id, title, duration, publishNow)
                    pdfTargetChapter = null
                },
                onResetAiState = onResetAiState
            )
            return
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Subjects Header + List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("1. Subject Folders", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Select a subject to manage chapters & upload PDFs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(
                    onClick = { isCreatingSubject = true },
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("admin_add_subject_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Subject")
                }
            }
        }

        items(subjects, key = { it.id }) { sub ->
            val isSel = selectedSubject?.id == sub.id
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                shadowElevation = if (isSel) 4.dp else 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedSubjectId = sub.id }
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.linearGradient(subjectGradientColors(sub.iconKey, 0))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(subjectIconForKey(sub.iconKey), contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${sub.name} (${sub.boardClass})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            if (sub.published) "Live to Students" else "Hidden Draft",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { showSubjectDialog = sub }) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit Subject")
                    }
                    IconButton(onClick = { onDeleteSubject(sub.id) }) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Delete Subject", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        // 2. Chapters in Selected Subject — with inline [PDF] Upload & AI Test button!
        if (selectedSubject != null) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "2. Chapters in ${selectedSubject.name.substringBefore(" (")}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Tap [PDF] on any chapter to upload PDF & generate quiz",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Button(
                        onClick = { isCreatingChapter = true },
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("admin_add_chapter_button")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Chapter")
                    }
                }
            }

            if (chaptersForSubject.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Rounded.Folder,
                        title = "No Chapters in ${selectedSubject.name}",
                        subtitle = "Tap '+ Add Chapter' (e.g. Real Numbers), then upload a PDF right inside the chapter to auto-generate MCQs!"
                    )
                }
            } else {
                items(chaptersForSubject, key = { it.id }) { chap ->
                    val isSel = selectedChapterId == chap.id
                    val quizzesInChap = quizzes.filter { it.chapterId == chap.id }
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                        shadowElevation = if (isSel) 5.dp else 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedChapterId = if (isSel) null else chap.id }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Ch ${chap.chapterNumber}: ${chap.title}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "${quizzesInChap.size} tests • ${if (chap.published) "Published" else "Draft"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                // Vibrant red [PDF] button directly on Chapter row matching UI mockup!
                                Surface(
                                    onClick = {
                                        selectedChapterId = chap.id
                                        onResetAiState()
                                        pdfTargetChapter = chap
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = PdfRedBadge,
                                    modifier = Modifier.testTag("chapter_pdf_button_${chap.id}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Rounded.PictureAsPdf, contentDescription = "Upload PDF", tint = Color.White, modifier = Modifier.size(16.dp))
                                        Text("PDF", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                                IconButton(onClick = { showChapterDialog = chap }) {
                                    Icon(Icons.Rounded.Edit, contentDescription = "Edit Chapter")
                                }
                                IconButton(onClick = { onDeleteChapter(chap.id) }) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Delete Chapter", tint = MaterialTheme.colorScheme.error)
                                }
                            }

                            AnimatedVisibility(visible = isSel) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        GradientPrimaryButton(
                                            onClick = {
                                                onResetAiState()
                                                pdfTargetChapter = chap
                                            },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Upload PDF → Make Test", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                        }
                                        OutlinedButton(
                                            onClick = { isCreatingQuiz = true },
                                            shape = RoundedCornerShape(14.dp)
                                        ) {
                                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Manual", style = MaterialTheme.typography.labelMedium)
                                        }
                                    }

                                    quizzesInChap.forEach { quiz ->
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(quiz.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                                    Text(
                                                        "${quiz.questions.size} MCQs • ${quiz.durationMinutes} min • ${if (quiz.published) "Public" else "Draft"}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                // One-tap Publish / Unpublish toggle
                                                Switch(
                                                    checked = quiz.published,
                                                    onCheckedChange = { newPub ->
                                                        onSaveQuiz(
                                                            quiz.id,
                                                            quiz.subjectId,
                                                            quiz.chapterId,
                                                            quiz.title,
                                                            quiz.durationMinutes,
                                                            quiz.sourceType,
                                                            newPub,
                                                            quiz.questions
                                                        )
                                                    }
                                                )
                                                IconButton(onClick = { showQuizDialog = quiz }) {
                                                    Icon(Icons.Rounded.Edit, contentDescription = "Edit Quiz")
                                                }
                                                IconButton(onClick = { onDeleteQuiz(quiz.id) }) {
                                                    Icon(Icons.Rounded.Delete, contentDescription = "Delete Quiz", tint = MaterialTheme.colorScheme.error)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (isCreatingSubject || showSubjectDialog != null) {
        val editing = showSubjectDialog
        SubjectEditorDialog(
            initial = editing,
            onDismiss = {
                isCreatingSubject = false
                showSubjectDialog = null
            },
            onSave = { name, desc, boardCls, iconKey, orderIdx, pub ->
                onSaveSubject(editing?.id, name, desc, boardCls, iconKey, orderIdx, pub)
                isCreatingSubject = false
                showSubjectDialog = null
            }
        )
    }

    if ((isCreatingChapter || showChapterDialog != null) && selectedSubject != null) {
        val editing = showChapterDialog
        ChapterEditorDialog(
            initial = editing,
            onDismiss = {
                isCreatingChapter = false
                showChapterDialog = null
            },
            onSave = { title, summary, chNum, pub ->
                onSaveChapter(editing?.id, selectedSubject.id, title, summary, chNum, pub)
                isCreatingChapter = false
                showChapterDialog = null
            }
        )
    }

    if ((isCreatingQuiz || showQuizDialog != null) && selectedSubject != null && selectedChapter != null) {
        val editing = showQuizDialog
        QuizEditorDialog(
            initial = editing,
            onDismiss = {
                isCreatingQuiz = false
                showQuizDialog = null
            },
            onSave = { title, duration, pub, qs ->
                onSaveQuiz(
                    editing?.id,
                    selectedSubject.id,
                    selectedChapter.id,
                    title,
                    duration,
                    editing?.sourceType ?: "manual",
                    pub,
                    qs
                )
                isCreatingQuiz = false
                showQuizDialog = null
            }
        )
    }
}

@Composable
private fun InlineChapterPdfGeneratorView(
    subject: SubjectFolder,
    chapter: ChapterFolder,
    aiState: AiPdfUiState,
    onBack: () -> Unit,
    onGenerateFromPdf: (Uri, Int, String) -> Unit,
    onUpdateDraftQuestion: (Int, McqQuestion) -> Unit,
    onRemoveDraftQuestion: (Int) -> Unit,
    onPublishDraft: (String, Long, Boolean) -> Unit,
    onResetAiState: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    var pickedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var pickedPdfName by remember { mutableStateOf<String?>(null) }
    var questionCount by remember { mutableIntStateOf(10) }
    var languageMode by remember { mutableStateOf("Hindi + English (Bilingual)") }
    var quizTitleOverride by remember { mutableStateOf("") }
    var durationStr by remember { mutableStateOf("15") }
    var publishImmediately by remember { mutableStateOf(true) }
    var editingQuestionPair by remember { mutableStateOf<Pair<Int, McqQuestion>?>(null) }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pickedPdfUri = uri
            var name = "Selected Chapter PDF"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) {
                    name = cursor.getString(nameIndex) ?: name
                }
            }
            pickedPdfName = name
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to Chapters")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Generate Questions from PDF", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "${subject.name.substringBefore(" (")} → Ch ${chapter.chapterNumber}: ${chapter.title}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                shadowElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("admin_pick_pdf_button")
                    ) {
                        Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, tint = PdfRedBadge)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(pickedPdfName ?: "Upload Chapter PDF File")
                    }

                    Text("Question Language", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Hindi + English (Bilingual)", "Hindi", "English").forEach { lang ->
                            FilterChip(
                                selected = languageMode == lang,
                                onClick = { languageMode = lang },
                                label = { Text(if (lang.contains("Bilingual")) "Bilingual" else lang) }
                            )
                        }
                    }

                    // Number of Questions stepper (- 10 +) matching mockup
                    Text("Number of Questions", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = { questionCount = (questionCount - 5).coerceAtLeast(5) }
                            ) {
                                Icon(Icons.Rounded.Remove, contentDescription = "Decrease")
                            }
                            Text(
                                text = "$questionCount",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = { questionCount = (questionCount + 5).coerceAtMost(30) }
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = "Increase")
                            }
                        }
                    }

                    GradientPrimaryButton(
                        onClick = {
                            val uri = pickedPdfUri ?: return@GradientPrimaryButton
                            onGenerateFromPdf(uri, questionCount, languageMode)
                        },
                        enabled = pickedPdfUri != null && aiState !is AiPdfUiState.Analyzing,
                        modifier = Modifier.fillMaxWidth().testTag("admin_generate_pdf_quiz_button")
                    ) {
                        if (aiState is AiPdfUiState.Analyzing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Analyzing PDF & Generating MCQs...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate Questions ✨", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        when (aiState) {
            is AiPdfUiState.Error -> {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("PDF Analysis Error", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
                            Text(aiState.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }
            is AiPdfUiState.DraftReady -> {
                val draft = aiState.draft
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("${draft.questions.size} MCQs Ready for ${chapter.title}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            OutlinedTextField(
                                value = quizTitleOverride.ifBlank { draft.quizTitle },
                                onValueChange = { quizTitleOverride = it },
                                label = { Text("Test Title") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = durationStr,
                                onValueChange = { durationStr = it.filter(Char::isDigit) },
                                label = { Text("Duration (Minutes)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Publish immediately to students", modifier = Modifier.weight(1f))
                                Switch(checked = publishImmediately, onCheckedChange = { publishImmediately = it })
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = onResetAiState, modifier = Modifier.weight(1f)) {
                                    Text("Discard")
                                }
                                Button(
                                    onClick = {
                                        onPublishDraft(
                                            quizTitleOverride.ifBlank { draft.quizTitle },
                                            (durationStr.toLongOrNull() ?: 15L).coerceIn(1L, 180L),
                                            publishImmediately
                                        )
                                        quizTitleOverride = ""
                                    },
                                    modifier = Modifier.weight(1f).testTag("admin_publish_ai_quiz_button")
                                ) {
                                    Icon(Icons.Rounded.Publish, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (publishImmediately) "Publish" else "Save Draft")
                                }
                            }
                        }
                    }
                }

                itemsIndexed(draft.questions) { idx, q ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Q${idx + 1}. ${q.question}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                IconButton(onClick = { editingQuestionPair = idx to q }) {
                                    Icon(Icons.Rounded.Edit, contentDescription = "Edit Question")
                                }
                                if (draft.questions.size > 1) {
                                    IconButton(onClick = { onRemoveDraftQuestion(idx) }) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "Remove Question")
                                    }
                                }
                            }
                            q.options.forEachIndexed { optIdx, opt ->
                                val isAns = optIdx == q.correctIndex.toInt()
                                Text(
                                    text = "${listOf("A", "B", "C", "D")[optIdx]}. $opt ${if (isAns) "✓" else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isAns) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isAns) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
            else -> Unit
        }
    }

    editingQuestionPair?.let { (idx, question) ->
        EditMcqDialog(
            initial = question,
            onDismiss = { editingQuestionPair = null },
            onSave = { updated ->
                onUpdateDraftQuestion(idx, updated)
                editingQuestionPair = null
            }
        )
    }
}

@Composable
private fun SubjectEditorDialog(
    initial: SubjectFolder?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, Long, Boolean) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var desc by remember { mutableStateOf(initial?.description ?: "") }
    var boardCls by remember { mutableStateOf(initial?.boardClass ?: "Class 10") }
    var iconKey by remember { mutableStateOf(initial?.iconKey ?: "calculate") }
    var orderStr by remember { mutableStateOf((initial?.orderIndex ?: 1L).toString()) }
    var published by remember { mutableStateOf(initial?.published ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add Subject" else "Edit Subject", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Subject Name (e.g. Mathematics)") }, singleLine = true)
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description (Optional)") })
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Class 10", "Class 12").forEach { cls ->
                        FilterChip(selected = boardCls == cls, onClick = { boardCls = cls }, label = { Text(cls) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("calculate", "science", "bolt", "public", "translate").forEach { ik ->
                        FilterChip(selected = iconKey == ik, onClick = { iconKey = ik }, label = { Text(ik.take(4)) })
                    }
                }
                OutlinedTextField(
                    value = orderStr,
                    onValueChange = { orderStr = it.filter(Char::isDigit) },
                    label = { Text("Order Index") },
                    singleLine = true
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Published to Students", modifier = Modifier.weight(1f))
                    Switch(checked = published, onCheckedChange = { published = it })
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank()) {
                    onSave(name, desc, boardCls, iconKey, orderStr.toLongOrNull() ?: 1L, published)
                }
            }) { Text("Save Subject") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ChapterEditorDialog(
    initial: ChapterFolder?,
    onDismiss: () -> Unit,
    onSave: (String, String, Long, Boolean) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var summary by remember { mutableStateOf(initial?.summary ?: "") }
    var numStr by remember { mutableStateOf((initial?.chapterNumber ?: 1L).toString()) }
    var published by remember { mutableStateOf(initial?.published ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add Chapter" else "Edit Chapter", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Chapter Title (e.g. Real Numbers)") })
                OutlinedTextField(value = summary, onValueChange = { summary = it }, label = { Text("Summary") })
                OutlinedTextField(
                    value = numStr,
                    onValueChange = { numStr = it.filter(Char::isDigit) },
                    label = { Text("Chapter Number") },
                    singleLine = true
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Published to Students", modifier = Modifier.weight(1f))
                    Switch(checked = published, onCheckedChange = { published = it })
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (title.isNotBlank()) {
                    onSave(title, summary, (numStr.toLongOrNull() ?: 1L).coerceAtLeast(1L), published)
                }
            }) { Text("Save Chapter") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun QuizEditorDialog(
    initial: QuizSet?,
    onDismiss: () -> Unit,
    onSave: (String, Long, Boolean, List<McqQuestion>) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "Practice Set 1") }
    var durationStr by remember { mutableStateOf((initial?.durationMinutes ?: 15L).toString()) }
    var published by remember { mutableStateOf(initial?.published ?: true) }
    val questions = remember {
        mutableStateListOf<McqQuestion>().apply {
            if (initial != null && initial.questions.isNotEmpty()) addAll(initial.questions)
            else add(
                McqQuestion(
                    question = "Sample Question?",
                    options = listOf("Option A", "Option B", "Option C", "Option D"),
                    correctIndex = 0L,
                    explanation = ""
                )
            )
        }
    }
    var editingIndex by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Create Quiz" else "Edit Quiz", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Quiz Title") })
                }
                item {
                    OutlinedTextField(
                        value = durationStr,
                        onValueChange = { durationStr = it.filter(Char::isDigit) },
                        label = { Text("Duration (Minutes)") }
                    )
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Published", modifier = Modifier.weight(1f))
                        Switch(checked = published, onCheckedChange = { published = it })
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Questions (${questions.size})", style = MaterialTheme.typography.titleSmall)
                        TextButton(onClick = {
                            questions.add(McqQuestion("New Question", listOf("A", "B", "C", "D"), 0L, ""))
                            editingIndex = questions.lastIndex
                        }) { Text("+ Add Q") }
                    }
                }
                items(questions.size) { idx ->
                    val q = questions[idx]
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Q${idx + 1}. ${q.question}", modifier = Modifier.weight(1f), maxLines = 1)
                        IconButton(onClick = { editingIndex = idx }) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Edit")
                        }
                        if (questions.size > 1) {
                            IconButton(onClick = { questions.removeAt(idx) }) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Remove")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (title.isNotBlank() && questions.isNotEmpty()) {
                    onSave(title, (durationStr.toLongOrNull() ?: 15L).coerceIn(1L, 180L), published, questions.toList())
                }
            }) { Text("Save Quiz") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    editingIndex?.let { idx ->
        if (idx in questions.indices) {
            EditMcqDialog(
                initial = questions[idx],
                onDismiss = { editingIndex = null },
                onSave = { updated ->
                    questions[idx] = updated
                    editingIndex = null
                }
            )
        }
    }
}

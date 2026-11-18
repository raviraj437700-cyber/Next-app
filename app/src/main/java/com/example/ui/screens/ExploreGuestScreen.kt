package com.example.ui.screens

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.GuestPreviewCatalog
import com.example.ui.theme.CyanGlowProgress
import com.example.ui.theme.GoldMedal
import com.example.ui.theme.HeroVioletEnd
import com.example.ui.theme.HeroVioletMid
import com.example.ui.theme.HeroVioletStart

@Composable
fun ExploreGuestScreen(
    onAuthenticated: () -> Unit
) {
    var selectedClassFilter by remember { mutableStateOf("All") }
    var selectedSubjectId by remember { mutableStateOf<String?>(GuestPreviewCatalog.previewSubjects.firstOrNull()?.id) }
    var signInReason by remember { mutableStateOf<String?>(null) }
    var authError by remember { mutableStateOf<String?>(null) }

    val filteredSubjects = remember(selectedClassFilter) {
        if (selectedClassFilter == "All") GuestPreviewCatalog.previewSubjects
        else GuestPreviewCatalog.previewSubjects.filter { it.boardClass == selectedClassFilter }
    }
    val activeSubject = filteredSubjects.firstOrNull { it.id == selectedSubjectId } ?: filteredSubjects.firstOrNull()
    val chapters = remember(activeSubject?.id) {
        GuestPreviewCatalog.previewChaptersBySubject[activeSubject?.id].orEmpty()
    }

    SoftStudyBackdrop {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                StudyBottomBar(
                    selectedTab = 0,
                    onSelectTab = { tab ->
                        if (tab != 0) {
                            authError = null
                            signInReason = when (tab) {
                                1 -> "Login with Google to view the live Bihar Board Leaderboard and your rank."
                                2 -> "Login with Google to create or join Study Groups and chat with Admin."
                                else -> "Login with Google to track your progress, test history, and profile."
                            }
                        }
                    },
                    onCenterPracticeClick = {
                        authError = null
                        signInReason = "Sign in with Google to start objective chapter tests and get instant reports."
                    },
                    firstLabel = "Explore",
                    secondLabel = "Rankings"
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .statusBarsPadding(),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 640.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Hello, Scholar 👋",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Keep Learning, Keep Growing!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    onClick = {
                                        authError = null
                                        signInReason = "Login with Google to check announcements & alerts."
                                    },
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surface,
                                    shadowElevation = 2.dp
                                ) {
                                    Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Rounded.Notifications,
                                            contentDescription = "Notices",
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                                Button(
                                    onClick = {
                                        authError = null
                                        signInReason = "Sign in with Google to attempt chapter tests, join Study Groups, and view live rankings."
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                    modifier = Modifier.testTag("guest_top_signin_button")
                                ) {
                                    Icon(Icons.Rounded.Login, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Login")
                                }
                            }
                        }
                    }

                    // Hero Progress / Guest Welcome Card matching mockup
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(10.dp, RoundedCornerShape(24.dp))
                                .clip(RoundedCornerShape(24.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(HeroVioletStart, HeroVioletMid, HeroVioletEnd)
                                    )
                                )
                                .clickable {
                                    authError = null
                                    signInReason = "Sign in with Google to save scores and track your Bihar Board progress."
                                }
                                .padding(20.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Bihar Board • BSEB Prep",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = Color.White.copy(alpha = 0.9f)
                                        )
                                        Text(
                                            text = "Explore & Practice",
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color.White.copy(alpha = 0.16f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Rounded.EmojiEvents,
                                            contentDescription = null,
                                            tint = GoldMedal,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                                LinearProgressIndicator(
                                    progress = { 0.65f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(CircleShape),
                                    color = CyanGlowProgress,
                                    trackColor = Color.White.copy(alpha = 0.25f),
                                    strokeCap = StrokeCap.Round
                                )
                                Text(
                                    text = "Browse subjects freely below • Tap any chapter test to sign in & start",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.92f)
                                )
                            }
                        }
                    }

                    // 3 Quick Action Cards matching mockup
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            GuestQuickTile(
                                icon = Icons.Rounded.EmojiEvents,
                                label = "Leaderboard",
                                tint = GoldMedal,
                                bgTint = Color(0xFFFEF3C7),
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    authError = null
                                    signInReason = "Login with Google to view live Bihar Board rankings."
                                }
                            )
                            GuestQuickTile(
                                icon = Icons.AutoMirrored.Rounded.Chat,
                                label = "Chat",
                                tint = Color(0xFF10B981),
                                bgTint = Color(0xFFD1FAE5),
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    authError = null
                                    signInReason = "Login with Google to ask doubts and read Admin notices."
                                }
                            )
                            GuestQuickTile(
                                icon = Icons.Rounded.Groups,
                                label = "Study Groups",
                                tint = Color(0xFF3B82F6),
                                bgTint = Color(0xFFDBEAFE),
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    authError = null
                                    signInReason = "Login with Google to create or join peer Study Groups."
                                }
                            )
                        }
                    }

                    // Explore Subjects header + Class filter
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Explore Subjects",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(listOf("All", "Class 10", "Class 12")) { label ->
                                        FilterChip(
                                            selected = selectedClassFilter == label,
                                            onClick = { selectedClassFilter = label },
                                            label = { Text(label) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3-column vibrant gradient tiles matching mockup
                    item {
                        val rows = filteredSubjects.chunked(3)
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            rows.forEachIndexed { rIdx, rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    rowItems.forEachIndexed { cIdx, subject ->
                                        val globalIdx = rIdx * 3 + cIdx
                                        val grad = subjectGradientColors(subject.iconKey, globalIdx)
                                        val isSelected = activeSubject?.id == subject.id
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .shadow(if (isSelected) 10.dp else 4.dp, RoundedCornerShape(20.dp))
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(Brush.linearGradient(grad))
                                                .clickable { selectedSubjectId = subject.id }
                                                .padding(10.dp)
                                                .testTag("guest_subject_${subject.id}"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(44.dp)
                                                        .clip(RoundedCornerShape(14.dp))
                                                        .background(Color.White.copy(alpha = 0.24f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = subjectIconForKey(subject.iconKey),
                                                        contentDescription = subject.name,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(26.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = subject.name.substringBefore(" ("),
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    textAlign = TextAlign.Center
                                                )
                                                Text(
                                                    text = subject.boardClass,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White.copy(alpha = 0.85f)
                                                )
                                            }
                                        }
                                    }
                                    repeat(3 - rowItems.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    // Preview Chapters inside selected subject
                    if (activeSubject != null) {
                        item {
                            Text(
                                text = "${activeSubject.name.substringBefore(" (")} Chapters",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        items(chapters, key = { it.id }) { ch ->
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                                shadowElevation = 2.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Rounded.MenuBook,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = ch.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = ch.summary,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            authError = null
                                            signInReason = "Sign in with Google to start \"${ch.title}\" objective test and get your score report & rank."
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                        modifier = Modifier.testTag("guest_start_test_${ch.id}")
                                    ) {
                                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Test")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (signInReason != null) {
        SignInPromptDialog(
            reason = signInReason!!,
            errorMessage = authError,
            onAuthSuccess = {
                signInReason = null
                authError = null
                onAuthenticated()
            },
            onAuthError = { msg -> authError = msg },
            onDismiss = { signInReason = null }
        )
    }
}

@Composable
private fun GuestQuickTile(
    icon: ImageVector,
    label: String,
    tint: Color,
    bgTint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        shadowElevation = 3.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(bgTint),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(24.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

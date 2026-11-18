package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.example.data.Announcement
import com.example.data.ChapterFolder
import com.example.data.LeaderboardEntry
import com.example.data.QuizSet
import com.example.data.SubjectFolder
import com.example.data.UiState
import com.example.data.UserProfile
import com.example.ui.theme.BronzeMedal
import com.example.ui.theme.CyanGlowProgress
import com.example.ui.theme.GoldMedal
import com.example.ui.theme.HeroVioletEnd
import com.example.ui.theme.HeroVioletMid
import com.example.ui.theme.HeroVioletStart
import com.example.ui.theme.SilverMedal
import java.util.Locale

@Composable
fun StudentHomeScreen(
    profileState: UiState<UserProfile?>,
    subjectsState: UiState<List<SubjectFolder>>,
    leaderboardState: UiState<List<LeaderboardEntry>>,
    announcements: List<Announcement>,
    isAdmin: Boolean,
    onSubjectClick: (SubjectFolder) -> Unit,
    onOpenAnnouncements: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenChat: () -> Unit,
    onOpenGroups: () -> Unit,
    onOpenAdminPanel: () -> Unit
) {
    var classFilter by remember { mutableStateOf("All") }
    var showAllSubjects by remember { mutableStateOf(false) }

    val profile = (profileState as? UiState.Success)?.data
    val leaderboard = (leaderboardState as? UiState.Success)?.data.orEmpty()
    val myRank = remember(leaderboard, profile) {
        val uid = profile?.userId ?: return@remember null
        val idx = leaderboard.indexOfFirst { it.userId == uid }
        if (idx >= 0) idx + 1 else null
    }
    val latestNotice = announcements.firstOrNull()
    val firstName = profile?.displayName?.trim()?.substringBefore(" ")?.ifBlank { "Scholar" } ?: "Scholar"
    val avgPercent = (profile?.averagePercent ?: 0.0).coerceIn(0.0, 100.0)

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Greeting Header matching mockup
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hello, $firstName 👋",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Keep Learning, Keep Growing!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (isAdmin) {
                            Surface(
                                onClick = onOpenAdminPanel,
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shadowElevation = 2.dp,
                                modifier = Modifier.testTag("topbar_admin_button")
                            ) {
                                Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Rounded.AdminPanelSettings,
                                        contentDescription = "Admin Panel",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        Surface(
                            onClick = onOpenAnnouncements,
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 3.dp
                        ) {
                            Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.Notifications,
                                    contentDescription = "Announcements",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                                if (announcements.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = (-9).dp, y = 9.dp)
                                            .size(9.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFEF4444))
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Hero Progress Purple Gradient Card matching mockup
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(12.dp, RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(listOf(HeroVioletStart, HeroVioletMid, HeroVioletEnd))
                        )
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
                                    text = "Your Progress",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                                Text(
                                    text = String.format(Locale.US, "%.0f%%", avgPercent),
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color.White.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.EmojiEvents,
                                    contentDescription = "Trophy",
                                    tint = GoldMedal,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }

                        LinearProgressIndicator(
                            progress = { (avgPercent / 100.0).toFloat().coerceIn(0.05f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(9.dp)
                                .clip(CircleShape),
                            color = CyanGlowProgress,
                            trackColor = Color.White.copy(alpha = 0.25f),
                            strokeCap = StrokeCap.Round
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${profile?.boardClass ?: "Class 10"} • ${profile?.testsTaken ?: 0} Tests Taken",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.92f)
                            )
                            Surface(
                                color = Color.White.copy(alpha = 0.22f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.clickable { onOpenLeaderboard() }
                            ) {
                                Text(
                                    text = if (myRank != null) "Rank #$myRank • ${profile?.totalScore ?: 0} pts"
                                    else "${profile?.totalScore ?: 0} pts",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 3 Quick Action Tiles matching mockup (Leaderboard, Chat, Study Groups)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HomeQuickActionCard(
                        icon = Icons.Rounded.EmojiEvents,
                        label = "Leaderboard",
                        tint = GoldMedal,
                        bgTint = Color(0xFFFEF3C7),
                        modifier = Modifier.weight(1f),
                        onClick = onOpenLeaderboard
                    )
                    HomeQuickActionCard(
                        icon = Icons.AutoMirrored.Rounded.Chat,
                        label = "Chat",
                        tint = Color(0xFF10B981),
                        bgTint = Color(0xFFD1FAE5),
                        modifier = Modifier.weight(1f),
                        onClick = onOpenChat
                    )
                    HomeQuickActionCard(
                        icon = Icons.Rounded.Groups,
                        label = "Study Groups",
                        tint = Color(0xFF3B82F6),
                        bgTint = Color(0xFFDBEAFE),
                        modifier = Modifier.weight(1f),
                        onClick = onOpenGroups
                    )
                }
            }

            if (isAdmin) {
                item {
                    Surface(
                        onClick = onOpenAdminPanel,
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_admin_shortcut")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.AdminPanelSettings,
                                    contentDescription = "Admin Panel",
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Admin Control Panel",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Add subjects/chapters, upload PDFs for AI tests & manage users",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                )
                            }
                            Icon(
                                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            if (latestNotice != null) {
                item {
                    Surface(
                        onClick = onOpenAnnouncements,
                        shape = RoundedCornerShape(18.dp),
                        color = if (latestNotice.priority == "urgent") {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                        },
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (latestNotice.priority == "urgent") MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.primary
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.Campaign,
                                    contentDescription = "Announcement",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = latestNotice.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = latestNotice.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Explore Subjects Section Header + Class Chips
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Explore Subjects",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = { showAllSubjects = !showAllSubjects }) {
                            Text(if (showAllSubjects) "Show Less" else "See All →")
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("All", "Class 10", "Class 12")) { label ->
                            FilterChip(
                                selected = classFilter == label,
                                onClick = { classFilter = label },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }

            when (subjectsState) {
                is UiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) { CircularProgressIndicator() }
                    }
                }
                is UiState.Error -> {
                    item {
                        EmptyStateCard(
                            icon = Icons.Rounded.FolderOpen,
                            title = "Could not load subjects",
                            subtitle = subjectsState.message
                        )
                    }
                }
                is UiState.Success -> {
                    val filtered = if (classFilter == "All") subjectsState.data
                    else subjectsState.data.filter { it.boardClass == classFilter }

                    if (filtered.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Rounded.FolderOpen,
                                title = "No subjects published yet",
                                subtitle = if (isAdmin) {
                                    "Open Admin Control Panel -> Subjects to add subjects or seed Bihar Board starter sets."
                                } else {
                                    "Your teacher/admin will publish Bihar Board subject folders shortly."
                                }
                            )
                        }
                    } else {
                        val visibleList = if (showAllSubjects) filtered else filtered.take(6)
                        val rows = visibleList.chunked(3)
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                rows.forEachIndexed { rIdx, rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        rowItems.forEachIndexed { cIdx, subject ->
                                            val globalIdx = rIdx * 3 + cIdx
                                            val grad = subjectGradientColors(subject.iconKey, globalIdx)
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .aspectRatio(1f)
                                                    .shadow(6.dp, RoundedCornerShape(20.dp))
                                                    .clip(RoundedCornerShape(20.dp))
                                                    .background(Brush.linearGradient(grad))
                                                    .clickable { onSubjectClick(subject) }
                                                    .padding(10.dp)
                                                    .testTag("subject_card_${subject.id}"),
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
                    }
                }
            }

            // Top Performers strip matching mockup
            if (leaderboard.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Top Performers",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = onOpenLeaderboard) {
                                Text("See All →")
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                            shadowElevation = 3.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            LazyRow(
                                contentPadding = PaddingValues(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                itemsIndexed(leaderboard.take(6)) { idx, entry ->
                                    val rank = idx + 1
                                    val badgeColor = when (rank) {
                                        1 -> GoldMedal
                                        2 -> SilverMedal
                                        3 -> BronzeMedal
                                        else -> MaterialTheme.colorScheme.primary
                                    }
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .width(72.dp)
                                            .clickable { onOpenLeaderboard() }
                                    ) {
                                        Box(contentAlignment = Alignment.BottomCenter) {
                                            Box(
                                                modifier = Modifier
                                                    .size(54.dp)
                                                    .clip(CircleShape)
                                                    .border(2.5.dp, badgeColor, CircleShape)
                                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = entry.displayName.take(1).uppercase(),
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            Surface(
                                                color = badgeColor,
                                                shape = CircleShape,
                                                modifier = Modifier.offset(y = 4.dp)
                                            ) {
                                                Text(
                                                    text = "#$rank",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = entry.displayName.substringBefore(" "),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = String.format(Locale.US, "%.1f%%", entry.averagePercent),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
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

@Composable
private fun HomeQuickActionCard(
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
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectDetailScreen(
    subject: SubjectFolder,
    chaptersState: UiState<List<ChapterFolder>>,
    selectedChapterId: String?,
    quizzesState: UiState<List<QuizSet>>,
    onSelectChapter: (ChapterFolder) -> Unit,
    onStartQuiz: (QuizSet) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    SoftStudyBackdrop {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = subject.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${subject.boardClass} • Chapter-wise Objective Practice",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("subject_back_button")) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (chaptersState) {
                        is UiState.Loading -> {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) { CircularProgressIndicator() }
                            }
                        }
                        is UiState.Error -> {
                            item {
                                EmptyStateCard(
                                    icon = Icons.Rounded.FolderOpen,
                                    title = "Failed to load chapters",
                                    subtitle = chaptersState.message
                                )
                            }
                        }
                        is UiState.Success -> {
                            if (chaptersState.data.isEmpty()) {
                                item {
                                    EmptyStateCard(
                                        icon = Icons.Rounded.FolderOpen,
                                        title = "No chapters added yet",
                                        subtitle = "Chapters and PDF-generated quizzes for ${subject.name} will appear here."
                                    )
                                }
                            } else {
                                items(chaptersState.data, key = { it.id }) { chapter ->
                                    val isSelected = selectedChapterId == chapter.id
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                                        shadowElevation = if (isSelected) 6.dp else 2.dp,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { onSelectChapter(chapter) }
                                                    .padding(16.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(44.dp)
                                                        .clip(RoundedCornerShape(14.dp))
                                                        .background(
                                                            Brush.linearGradient(
                                                                listOf(HeroVioletStart, HeroVioletEnd)
                                                            )
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "${chapter.chapterNumber}",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = chapter.title,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                    if (chapter.summary.isNotBlank()) {
                                                        Text(
                                                            text = chapter.summary,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                                Icon(
                                                    imageVector = if (isSelected) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                                    contentDescription = "Toggle chapter tests"
                                                )
                                            }

                                            AnimatedVisibility(visible = isSelected) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                                                    when (quizzesState) {
                                                        is UiState.Loading -> {
                                                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                                        }
                                                        is UiState.Error -> {
                                                            Text(
                                                                text = quizzesState.message,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.error
                                                            )
                                                        }
                                                        is UiState.Success -> {
                                                            if (quizzesState.data.isEmpty()) {
                                                                Text(
                                                                    text = "No practice sets published in this chapter yet.",
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            } else {
                                                                quizzesState.data.forEach { quiz ->
                                                                    Surface(
                                                                        shape = RoundedCornerShape(14.dp),
                                                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                                                    ) {
                                                                        Row(
                                                                            modifier = Modifier
                                                                                .fillMaxWidth()
                                                                                .padding(12.dp),
                                                                            verticalAlignment = Alignment.CenterVertically
                                                                        ) {
                                                                            Column(modifier = Modifier.weight(1f)) {
                                                                                Row(
                                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                                                ) {
                                                                                    Text(
                                                                                        text = quiz.title,
                                                                                        style = MaterialTheme.typography.titleSmall,
                                                                                        fontWeight = FontWeight.SemiBold,
                                                                                        modifier = Modifier.weight(1f, fill = false)
                                                                                    )
                                                                                    if (quiz.sourceType == "ai_pdf") {
                                                                                        Icon(
                                                                                            Icons.Rounded.AutoAwesome,
                                                                                            contentDescription = "AI PDF Set",
                                                                                            tint = MaterialTheme.colorScheme.primary,
                                                                                            modifier = Modifier.size(16.dp)
                                                                                        )
                                                                                    }
                                                                                }
                                                                                Row(
                                                                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                                                    verticalAlignment = Alignment.CenterVertically
                                                                                ) {
                                                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                                                        Icon(
                                                                                            Icons.Rounded.Quiz,
                                                                                            contentDescription = null,
                                                                                            modifier = Modifier.size(14.dp),
                                                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                                                        )
                                                                                        Spacer(modifier = Modifier.width(4.dp))
                                                                                        Text(
                                                                                            "${quiz.questions.size} MCQs",
                                                                                            style = MaterialTheme.typography.bodySmall,
                                                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                                        )
                                                                                    }
                                                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                                                        Icon(
                                                                                            Icons.Rounded.Timer,
                                                                                            contentDescription = null,
                                                                                            modifier = Modifier.size(14.dp),
                                                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                                                        )
                                                                                        Spacer(modifier = Modifier.width(4.dp))
                                                                                        Text(
                                                                                            "${quiz.durationMinutes} min",
                                                                                            style = MaterialTheme.typography.bodySmall,
                                                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                                        )
                                                                                    }
                                                                                }
                                                                            }
                                                                            Spacer(modifier = Modifier.width(8.dp))
                                                                            Button(
                                                                                onClick = { onStartQuiz(quiz) },
                                                                                shape = RoundedCornerShape(12.dp),
                                                                                modifier = Modifier.testTag("start_quiz_${quiz.id}")
                                                                            ) {
                                                                                Icon(
                                                                                    Icons.Rounded.PlayArrow,
                                                                                    contentDescription = null,
                                                                                    modifier = Modifier.size(18.dp)
                                                                                )
                                                                                Spacer(modifier = Modifier.width(4.dp))
                                                                                Text("Start")
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
                            }
                        }
                    }
                }
            }
        }
    }
}

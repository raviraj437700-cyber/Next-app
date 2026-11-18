package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.LeaderboardEntry
import com.example.data.TestAttempt
import com.example.data.UiState
import com.example.data.UserProfile
import com.example.ui.theme.BronzeMedal
import com.example.ui.theme.GoldMedal
import com.example.ui.theme.HeroVioletEnd
import com.example.ui.theme.HeroVioletStart
import com.example.ui.theme.SilverMedal
import java.util.Locale

@Composable
fun LeaderboardScreen(
    leaderboardState: UiState<List<LeaderboardEntry>>,
    currentUserId: String
) {
    var classFilter by remember { mutableStateOf("All") }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Leaderboard",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Live Bihar Board rankings across objective tests",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("All", "Class 10", "Class 12").forEach { label ->
                            FilterChip(
                                selected = classFilter == label,
                                onClick = { classFilter = label },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            when (leaderboardState) {
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
                            icon = Icons.Rounded.EmojiEvents,
                            title = "Could not load leaderboard",
                            subtitle = leaderboardState.message
                        )
                    }
                }
                is UiState.Success -> {
                    val entries = if (classFilter == "All") leaderboardState.data
                    else leaderboardState.data.filter { it.boardClass == classFilter }

                    if (entries.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Rounded.EmojiEvents,
                                title = "No rankings yet",
                                subtitle = "Be the first student to complete a chapter test and claim #1!"
                            )
                        }
                    } else {
                        // Top 3 Podium matching UI mockup
                        item {
                            val top1 = entries.getOrNull(0)
                            val top2 = entries.getOrNull(1)
                            val top3 = entries.getOrNull(2)
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                                shadowElevation = 4.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 18.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    PodiumColumn(
                                        entry = top2,
                                        rank = 2,
                                        pedestalHeight = 56.dp,
                                        accentColor = SilverMedal,
                                        isCurrentUser = top2?.userId == currentUserId,
                                        modifier = Modifier.weight(1f)
                                    )
                                    PodiumColumn(
                                        entry = top1,
                                        rank = 1,
                                        pedestalHeight = 76.dp,
                                        accentColor = GoldMedal,
                                        isCurrentUser = top1?.userId == currentUserId,
                                        modifier = Modifier.weight(1.1f)
                                    )
                                    PodiumColumn(
                                        entry = top3,
                                        rank = 3,
                                        pedestalHeight = 44.dp,
                                        accentColor = BronzeMedal,
                                        isCurrentUser = top3?.userId == currentUserId,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        // Current user's highlight pill
                        val myIdx = entries.indexOfFirst { it.userId == currentUserId }
                        if (myIdx >= 0) {
                            val me = entries[myIdx]
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(Brush.horizontalGradient(listOf(HeroVioletStart, HeroVioletEnd)))
                                        .padding(14.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = GoldMedal)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Your Rank: #${myIdx + 1} • ${me.totalScore} pts (${String.format(Locale.US, "%.1f%%", me.averagePercent)})",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        // Ranked list from #4 onwards (or all below podium)
                        val remaining = entries.drop(3)
                        itemsIndexed(remaining, key = { _, e -> e.userId }) { idx, entry ->
                            val rank = idx + 4
                            val isMe = entry.userId == currentUserId
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                                shadowElevation = 2.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$rank",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.width(28.dp),
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = entry.displayName.take(1).uppercase(),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isMe) "${entry.displayName} (You)" else entry.displayName,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${entry.boardClass} • ${entry.testsTaken} tests • ${entry.totalScore} pts",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = String.format(Locale.US, "%.1f%%", entry.averagePercent),
                                        style = MaterialTheme.typography.titleSmall,
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

@Composable
private fun PodiumColumn(
    entry: LeaderboardEntry?,
    rank: Int,
    pedestalHeight: androidx.compose.ui.unit.Dp,
    accentColor: Color,
    isCurrentUser: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        if (entry != null) {
            if (rank == 1) {
                Icon(
                    imageVector = Icons.Rounded.WorkspacePremium,
                    contentDescription = "First Place Crown",
                    tint = GoldMedal,
                    modifier = Modifier.size(28.dp)
                )
            }
            Box(contentAlignment = Alignment.BottomCenter) {
                Box(
                    modifier = Modifier
                        .size(if (rank == 1) 66.dp else 54.dp)
                        .clip(CircleShape)
                        .border(3.dp, accentColor, CircleShape)
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
                    color = accentColor,
                    shape = CircleShape,
                    modifier = Modifier.offset(y = 6.dp)
                ) {
                    Text(
                        text = "$rank",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 1.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (isCurrentUser) "${entry.displayName.substringBefore(" ")} (You)" else entry.displayName.substringBefore(" "),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                text = String.format(Locale.US, "%.1f%%", entry.averagePercent),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.86f)
                    .height(pedestalHeight)
                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(accentColor, accentColor.copy(alpha = 0.65f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$rank",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    profileState: UiState<UserProfile?>,
    leaderboardState: UiState<List<LeaderboardEntry>>,
    attempts: List<TestAttempt>,
    currentUserId: String,
    isAdmin: Boolean,
    onUpdateProfile: (String, String, String) -> Unit,
    onOpenAttemptReview: (TestAttempt) -> Unit,
    onOpenStudyGroups: () -> Unit,
    onOpenAdminPanel: () -> Unit,
    onSignOut: () -> Unit
) {
    val profile = (profileState as? UiState.Success)?.data
    val leaderboard = (leaderboardState as? UiState.Success)?.data.orEmpty()
    val myRank = remember(leaderboard, currentUserId) {
        val idx = leaderboard.indexOfFirst { it.userId == currentUserId }
        if (idx >= 0) "#${idx + 1}" else "--"
    }

    var name by remember(profile?.displayName) { mutableStateOf(profile?.displayName ?: "") }
    var boardClass by remember(profile?.boardClass) { mutableStateOf(profile?.boardClass ?: "Class 10") }
    var schoolName by remember(profile?.schoolName) { mutableStateOf(profile?.schoolName ?: "") }
    var classDropdownExpanded by remember { mutableStateOf(false) }

    var showEditProfile by remember { mutableStateOf(false) }
    var showProgressHistory by remember { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Centered Avatar + Name + Subtitle matching mockup
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Box(
                            modifier = Modifier
                                .size(92.dp)
                                .shadow(8.dp, CircleShape)
                                .clip(CircleShape)
                                .border(3.dp, Color.White, CircleShape)
                                .background(Brush.linearGradient(listOf(HeroVioletStart, HeroVioletEnd))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (profile?.displayName?.take(1)?.uppercase() ?: "S"),
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Surface(
                            onClick = { showEditProfile = !showEditProfile },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondary,
                            shadowElevation = 4.dp,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.Edit,
                                    contentDescription = "Edit Profile",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = profile?.displayName ?: "BiharPrep Student",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${profile?.boardClass ?: "Class 10"} • ${profile?.email ?: "Student"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 3 Stat Cards matching mockup (Rank, Total Score, Quizzes)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ProfileStatBox(
                        label = "Rank",
                        value = myRank,
                        modifier = Modifier.weight(1f)
                    )
                    ProfileStatBox(
                        label = "Total Score",
                        value = String.format(Locale.US, "%.1f%%", profile?.averagePercent ?: 0.0),
                        modifier = Modifier.weight(1f)
                    )
                    ProfileStatBox(
                        label = "Quizzes",
                        value = "${profile?.testsTaken ?: 0}",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Expandable Edit Profile Form
            item {
                ProfileMenuRow(
                    icon = Icons.Rounded.Person,
                    iconTint = MaterialTheme.colorScheme.primary,
                    iconBg = MaterialTheme.colorScheme.primaryContainer,
                    title = "Edit Profile & Class",
                    subtitle = if (showEditProfile) "Tap to collapse" else "Update name, Class 10/12 & school",
                    onClick = { showEditProfile = !showEditProfile }
                )
                AnimatedVisibility(visible = showEditProfile) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Student Name") },
                                modifier = Modifier.fillMaxWidth().testTag("profile_name_input"),
                                singleLine = true
                            )
                            ExposedDropdownMenuBox(
                                expanded = classDropdownExpanded,
                                onExpandedChange = { classDropdownExpanded = !classDropdownExpanded }
                            ) {
                                OutlinedTextField(
                                    value = boardClass,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Bihar Board Class") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = classDropdownExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = classDropdownExpanded,
                                    onDismissRequest = { classDropdownExpanded = false }
                                ) {
                                    listOf("Class 10", "Class 12").forEach { cls ->
                                        DropdownMenuItem(
                                            text = { Text(cls) },
                                            onClick = {
                                                boardClass = cls
                                                classDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            OutlinedTextField(
                                value = schoolName,
                                onValueChange = { schoolName = it },
                                label = { Text("School / College Name (Optional)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            GradientPrimaryButton(
                                onClick = {
                                    onUpdateProfile(name, boardClass, schoolName)
                                    showEditProfile = false
                                },
                                modifier = Modifier.fillMaxWidth().testTag("profile_save_button")
                            ) {
                                Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Save Profile", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                ProfileMenuRow(
                    icon = Icons.Rounded.Groups,
                    iconTint = Color(0xFF0284C7),
                    iconBg = Color(0xFFE0F2FE),
                    title = "My Study Groups",
                    subtitle = "Create or join Bihar Board study groups",
                    onClick = onOpenStudyGroups
                )
            }

            if (isAdmin) {
                item {
                    ProfileMenuRow(
                        icon = Icons.Rounded.AdminPanelSettings,
                        iconTint = Color(0xFF7C3AED),
                        iconBg = Color(0xFFEDE9FE),
                        title = "Open Admin Control Panel",
                        subtitle = "Manage folders, upload chapter PDFs & track users",
                        onClick = onOpenAdminPanel,
                        modifier = Modifier.testTag("profile_admin_button")
                    )
                }
            }

            item {
                ProfileMenuRow(
                    icon = Icons.Rounded.BarChart,
                    iconTint = Color(0xFF10B981),
                    iconBg = Color(0xFFD1FAE5),
                    title = "My Progress & Test History (${attempts.size})",
                    subtitle = "Tap any completed test to review wrong answers",
                    onClick = { showProgressHistory = !showProgressHistory }
                )
            }

            if (showProgressHistory) {
                if (attempts.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Rounded.History,
                            title = "No Completed Tests Yet",
                            subtitle = "Complete your first chapter test to track scores and review answers here."
                        )
                    }
                } else {
                    items(attempts, key = { it.id }) { att ->
                        Surface(
                            onClick = { onOpenAttemptReview(att) },
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                            shadowElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(att.quizTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "${att.subjectName} • ${att.correctCount}/${att.totalQuestions} correct",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = String.format(Locale.US, "%.0f%%", att.percentage),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = onSignOut,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("sign_out_button")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Logout", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ProfileStatBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        shadowElevation = 3.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ProfileMenuRow(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        shadowElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

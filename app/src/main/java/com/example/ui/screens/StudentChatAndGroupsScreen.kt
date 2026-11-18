package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.HelpCenter
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.Announcement
import com.example.data.ChatMessage
import com.example.data.GroupMessage
import com.example.data.StudyGroup
import com.example.ui.theme.HeroVioletStart

/**
 * Hub Tabs:
 * 0 = All (Groups + Doubts + Announcements overview)
 * 1 = Study Groups (Create / Join / Open Group Chat)
 * 2 = Doubts Help (1-to-1 Chat with Admin)
 * 3 = Announcements
 */
@Composable
fun StudentHubScreen(
    initialTab: Int,
    messages: List<ChatMessage>,
    announcements: List<Announcement>,
    studyGroups: List<StudyGroup>,
    selectedGroupId: String?,
    groupMessages: List<GroupMessage>,
    currentUserId: String,
    isAdmin: Boolean,
    onSendMessage: (String) -> Unit,
    onSelectGroup: (String?) -> Unit,
    onCreateGroup: (String, String, String, String) -> Unit,
    onJoinGroup: (StudyGroup) -> Unit,
    onLeaveGroup: (StudyGroup) -> Unit,
    onDeleteGroup: (String) -> Unit,
    onSendGroupMessage: (String, String) -> Unit
) {
    var selectedFilter by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 3)) }
    var searchQuery by remember { mutableStateOf("") }
    var showCreateGroupDialog by remember { mutableStateOf(false) }

    val activeGroup = remember(studyGroups, selectedGroupId) {
        studyGroups.firstOrNull { it.id == selectedGroupId }
    }

    // If inside an active Study Group room, render the dedicated Group Chat Room
    if (activeGroup != null) {
        GroupChatRoom(
            group = activeGroup,
            messages = groupMessages,
            currentUserId = currentUserId,
            isAdmin = isAdmin,
            onBack = { onSelectGroup(null) },
            onSendMessage = { text -> onSendGroupMessage(activeGroup.id, text) },
            onLeaveGroup = { onLeaveGroup(activeGroup) },
            onDeleteGroup = { onDeleteGroup(activeGroup.id) }
        )
        return
    }

    val filteredGroups = remember(studyGroups, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) studyGroups
        else studyGroups.filter {
            it.name.lowercase().contains(q) ||
                it.subjectTag.lowercase().contains(q) ||
                it.inviteCode.lowercase().contains(q)
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .widthIn(max = 640.dp)
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header + Create Group CTA matching Chat mockup
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Chat & Groups",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = { showCreateGroupDialog = true },
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("create_study_group_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Group")
                }
            }

            // Segmented Filter Pills matching mockup: All | Groups | Doubts | Notices
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val tabs = listOf(
                    0 to "All",
                    1 to "Study Groups (${studyGroups.size})",
                    2 to "Doubts Help",
                    3 to "Announcements (${announcements.size})"
                )
                items(tabs) { (idx, label) ->
                    FilterChip(
                        selected = selectedFilter == idx,
                        onClick = { selectedFilter = idx },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            when (selectedFilter) {
                0, 1 -> {
                    // Search bar for groups/invite codes
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search study groups or invite code...") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (selectedFilter == 0) {
                            item {
                                Surface(
                                    onClick = { selectedFilter = 2 },
                                    shape = RoundedCornerShape(18.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                                    shadowElevation = 2.dp,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(Color(0xFFDBEAFE)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Rounded.HelpCenter, contentDescription = null, tint = Color(0xFF2563EB))
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Doubts Help (Sir / Admin)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                            Text(
                                                messages.lastOrNull()?.text ?: "Ask your Bihar Board doubts directly here",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }

                            item {
                                Surface(
                                    onClick = { selectedFilter = 3 },
                                    shape = RoundedCornerShape(18.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                                    shadowElevation = 2.dp,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(Color(0xFFEDE9FE)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Rounded.Campaign, contentDescription = null, tint = HeroVioletStart)
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Announcements", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                            Text(
                                                announcements.firstOrNull()?.title ?: "No new announcements yet",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        if (announcements.isNotEmpty()) {
                                            Surface(color = Color(0xFFEF4444), shape = CircleShape) {
                                                Text(
                                                    text = "${announcements.size}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Text(
                                text = "Student Study Groups",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (filteredGroups.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    icon = Icons.Rounded.Groups,
                                    title = "No Study Groups Found",
                                    subtitle = "Tap '+ New Group' above to create a Bihar Board study group for Maths, Science or SST and invite classmates!"
                                )
                            }
                        } else {
                            items(filteredGroups, key = { it.id }) { group ->
                                val isMember = currentUserId in group.memberIds
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                                    shadowElevation = 2.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isMember) onSelectGroup(group.id)
                                            else onJoinGroup(group)
                                        }
                                        .testTag("study_group_card_${group.id}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Rounded.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = group.name,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                Surface(
                                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text(
                                                        text = "${group.memberCount} members",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "${group.boardClass} • ${group.subjectTag} • Code: ${group.inviteCode}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            if (group.lastMessage.isNotBlank()) {
                                                Text(
                                                    text = group.lastMessage,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        if (isMember) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Text(
                                                    text = "Open",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                )
                                            }
                                        } else {
                                            Button(
                                                onClick = { onJoinGroup(group) },
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                modifier = Modifier.testTag("join_group_${group.id}")
                                            ) {
                                                Text("Join")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    AdminDoubtChatView(
                        messages = messages,
                        currentUserId = currentUserId,
                        onSendMessage = onSendMessage,
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    )
                }

                else -> {
                    AnnouncementsListView(
                        announcements = announcements,
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    )
                }
            }
        }
    }

    if (showCreateGroupDialog) {
        CreateStudyGroupDialog(
            onDismiss = { showCreateGroupDialog = false },
            onCreate = { name, desc, cls, tag ->
                onCreateGroup(name, desc, cls, tag)
                showCreateGroupDialog = false
            }
        )
    }
}

@Composable
private fun GroupChatRoom(
    group: StudyGroup,
    messages: List<GroupMessage>,
    currentUserId: String,
    isAdmin: Boolean,
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit,
    onLeaveGroup: () -> Unit,
    onDeleteGroup: () -> Unit
) {
    BackHandler { onBack() }
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .widthIn(max = 640.dp)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to Groups")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(group.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(
                            "${group.memberCount} members • Code: ${group.inviteCode}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (group.createdBy == currentUserId || isAdmin) {
                        IconButton(onClick = onDeleteGroup) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete Group", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    IconButton(onClick = onLeaveGroup) {
                        Icon(Icons.AutoMirrored.Rounded.ExitToApp, contentDescription = "Leave Group")
                    }
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Rounded.Groups,
                            title = "Welcome to ${group.name}",
                            subtitle = "Share invite code ${group.inviteCode} with friends and start discussing chapter doubts!"
                        )
                    }
                } else {
                    items(messages, key = { it.id.ifBlank { "${it.senderId}_${it.text.hashCode()}" } }) { msg ->
                        val isMine = msg.senderId == currentUserId
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
                        ) {
                            Surface(
                                shape = RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isMine) 16.dp else 4.dp,
                                    bottomEnd = if (isMine) 4.dp else 16.dp
                                ),
                                color = if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                shadowElevation = 1.dp,
                                modifier = Modifier.widthIn(max = 290.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = if (isMine) "You" else msg.senderName,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isMine) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = msg.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("Message ${group.name}...") },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f).testTag("group_chat_input"),
                    maxLines = 3
                )
                FilledIconButton(
                    onClick = {
                        if (input.isNotBlank()) {
                            onSendMessage(input)
                            input = ""
                        }
                    },
                    modifier = Modifier.size(48.dp).testTag("group_chat_send")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send")
                }
            }
        }
    }
}

@Composable
private fun AdminDoubtChatView(
    messages: List<ChatMessage>,
    currentUserId: String,
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var messageInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.AutoMirrored.Rounded.Send,
                        title = "Ask Admin Anything",
                        subtitle = "Have a doubt about a Bihar Board chapter or test? Message Admin directly here."
                    )
                }
            } else {
                items(messages, key = { it.id.ifBlank { "${it.senderId}_${it.text.hashCode()}" } }) { msg ->
                    val isMine = msg.senderRole == "student" && msg.senderId == currentUserId
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
                    ) {
                        Surface(
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isMine) 16.dp else 4.dp,
                                bottomEnd = if (isMine) 4.dp else 16.dp
                            ),
                            color = if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            shadowElevation = 1.dp,
                            modifier = Modifier.widthIn(max = 290.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = if (isMine) "You" else msg.senderName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isMine) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = msg.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = messageInput,
                onValueChange = { messageInput = it },
                placeholder = { Text("Type your doubt to Admin...") },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f).testTag("student_chat_input"),
                maxLines = 3
            )
            FilledIconButton(
                onClick = {
                    if (messageInput.isNotBlank()) {
                        onSendMessage(messageInput)
                        messageInput = ""
                    }
                },
                modifier = Modifier.size(48.dp).testTag("student_chat_send")
            ) {
                Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send")
            }
        }
    }
}

@Composable
private fun AnnouncementsListView(
    announcements: List<Announcement>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (announcements.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Rounded.Campaign,
                    title = "No Announcements Yet",
                    subtitle = "Important Bihar Board dates, model papers & personal messages from Admin appear here."
                )
            }
        } else {
            items(announcements, key = { it.id }) { ann ->
                val isPersonal = ann.targetUserId != "ALL"
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (ann.priority == "urgent") {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                    },
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                color = if (isPersonal) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (isPersonal) "Personal Notice" else "All Students",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            if (ann.priority == "urgent") {
                                Surface(
                                    color = MaterialTheme.colorScheme.error,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "URGENT",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(text = ann.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(text = ann.message, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateStudyGroupDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var boardClass by remember { mutableStateOf("Class 10") }
    var subjectTag by remember { mutableStateOf("Maths") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Study Group", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Group Name (e.g. Maths Ch-1 Toppers)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("group_name_input")
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Board Class", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Class 10", "Class 12").forEach { cls ->
                        FilterChip(selected = boardClass == cls, onClick = { boardClass = cls }, label = { Text(cls) })
                    }
                }
                Text("Subject Tag", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("Maths", "Science", "SST", "Hindi", "Physics", "General")) { tag ->
                        FilterChip(selected = subjectTag == tag, onClick = { subjectTag = tag }, label = { Text(tag) })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.trim().length >= 2) {
                        onCreate(name, description, boardClass, subjectTag)
                    }
                },
                enabled = name.trim().length >= 2,
                modifier = Modifier.testTag("confirm_create_group_button")
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

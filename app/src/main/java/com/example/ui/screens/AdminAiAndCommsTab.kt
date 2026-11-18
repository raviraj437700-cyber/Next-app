package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import com.example.data.Announcement
import com.example.data.ChatMessage
import com.example.data.ChatThread
import com.example.data.McqQuestion
import com.example.data.StudyGroup
import com.example.data.UserProfile

@Composable
fun AdminChatInboxTab(
    threads: List<ChatThread>,
    studyGroups: List<StudyGroup>,
    selectedStudentId: String?,
    messages: List<ChatMessage>,
    onSelectThread: (String?) -> Unit,
    onSendReply: (String, String, String) -> Unit,
    onDeleteGroup: (String) -> Unit
) {
    var subTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        if (selectedStudentId == null) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                SegmentedButton(
                    selected = subTab == 0,
                    onClick = { subTab = 0 },
                    shape = SegmentedButtonDefaults.itemShape(0, 2)
                ) {
                    Text("Student Doubts (${threads.size})")
                }
                SegmentedButton(
                    selected = subTab == 1,
                    onClick = { subTab = 1 },
                    shape = SegmentedButtonDefaults.itemShape(1, 2)
                ) {
                    Text("Study Groups (${studyGroups.size})")
                }
            }
        }

        if (subTab == 1 && selectedStudentId == null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (studyGroups.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Rounded.Groups,
                            title = "No Study Groups Created Yet",
                            subtitle = "Peer study groups created by students will appear here for moderation."
                        )
                    }
                } else {
                    items(studyGroups, key = { it.id }) { grp ->
                        Surface(
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
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(grp.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(
                                        "${grp.boardClass} • ${grp.memberCount} members • Code: ${grp.inviteCode}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        "By ${grp.creatorName} • ${grp.lastMessage}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                                IconButton(onClick = { onDeleteGroup(grp.id) }) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Remove Group", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        } else if (selectedStudentId == null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (threads.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.AutoMirrored.Rounded.Chat,
                            title = "No Student Messages Yet",
                            subtitle = "When students ask doubts via Chat, their threads will appear here."
                        )
                    }
                } else {
                    items(threads, key = { it.studentId }) { thread ->
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectThread(thread.studentId) }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = thread.studentName.take(1).uppercase(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(thread.studentName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(
                                        thread.lastMessage,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                                if (thread.unreadByAdmin > 0) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondary,
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            text = "${thread.unreadByAdmin}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            val activeThread = threads.firstOrNull { it.studentId == selectedStudentId }
            var replyText by remember { mutableStateOf("") }
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onSelectThread(null) }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to Inbox")
                    }
                    Text(
                        text = activeThread?.studentName ?: "Student Chat",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages, key = { it.id.ifBlank { "${it.senderId}_${it.text.hashCode()}" } }) { msg ->
                        val isAdminMsg = msg.senderRole == "admin"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isAdminMsg) Arrangement.End else Arrangement.Start
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isAdminMsg) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                shadowElevation = 1.dp
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = msg.senderName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isAdminMsg) Color.White.copy(alpha = 0.85f)
                                        else MaterialTheme.colorScheme.secondary
                                    )
                                    Text(
                                        text = msg.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isAdminMsg) Color.White
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        placeholder = { Text("Reply to student...") },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f),
                        maxLines = 3
                    )
                    FilledIconButton(
                        onClick = {
                            if (replyText.isNotBlank()) {
                                onSendReply(
                                    selectedStudentId,
                                    activeThread?.studentName ?: "Student",
                                    replyText
                                )
                                replyText = ""
                            }
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send Reply")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAnnouncementsTab(
    users: List<UserProfile>,
    announcements: List<Announcement>,
    onSendAnnouncement: (String, String, String, String, String) -> Unit,
    onDeleteAnnouncement: (String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var targetUserId by remember { mutableStateOf("ALL") }
    var targetUserName by remember { mutableStateOf("All Students") }
    var priority by remember { mutableStateOf("normal") }
    var recipientDropdownOpen by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                shadowElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFEE2E2)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Campaign, contentDescription = null, tint = Color(0xFFEF4444))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Send Announcement", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }

                    ExposedDropdownMenuBox(
                        expanded = recipientDropdownOpen,
                        onExpandedChange = { recipientDropdownOpen = !recipientDropdownOpen }
                    ) {
                        OutlinedTextField(
                            value = targetUserName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Recipient (All or Single Student)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(recipientDropdownOpen) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = recipientDropdownOpen,
                            onDismissRequest = { recipientDropdownOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Students (Broadcast)") },
                                onClick = {
                                    targetUserId = "ALL"
                                    targetUserName = "All Students"
                                    recipientDropdownOpen = false
                                }
                            )
                            users.forEach { u ->
                                DropdownMenuItem(
                                    text = { Text("${u.displayName} (${u.email})") },
                                    onClick = {
                                        targetUserId = u.userId
                                        targetUserName = u.displayName
                                        recipientDropdownOpen = false
                                    }
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = priority == "normal",
                            onClick = { priority = "normal" },
                            label = { Text("Normal") }
                        )
                        FilterChip(
                            selected = priority == "urgent",
                            onClick = { priority = "urgent" },
                            label = { Text("Urgent Alert") }
                        )
                    }

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Message Title") },
                        modifier = Modifier.fillMaxWidth().testTag("admin_notice_title"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        label = { Text("Message Content") },
                        modifier = Modifier.fillMaxWidth().testTag("admin_notice_message"),
                        minLines = 3
                    )

                    GradientPrimaryButton(
                        onClick = {
                            if (title.isNotBlank() && message.isNotBlank()) {
                                onSendAnnouncement(title, message, targetUserId, targetUserName, priority)
                                title = ""
                                message = ""
                            }
                        },
                        enabled = title.isNotBlank() && message.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().testTag("admin_send_notice_button")
                    ) {
                        Icon(Icons.Rounded.Campaign, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send Message", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text("Sent Announcements (${announcements.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        items(announcements, key = { it.id }) { ann ->
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${ann.title} • To: ${ann.targetUserName}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = ann.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { onDeleteAnnouncement(ann.id) }) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Delete Notice", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
fun EditMcqDialog(
    initial: McqQuestion,
    onDismiss: () -> Unit,
    onSave: (McqQuestion) -> Unit
) {
    var qText by remember { mutableStateOf(initial.question) }
    var opt0 by remember { mutableStateOf(initial.options.getOrElse(0) { "" }) }
    var opt1 by remember { mutableStateOf(initial.options.getOrElse(1) { "" }) }
    var opt2 by remember { mutableStateOf(initial.options.getOrElse(2) { "" }) }
    var opt3 by remember { mutableStateOf(initial.options.getOrElse(3) { "" }) }
    var correctIdx by remember { mutableIntStateOf(initial.correctIndex.toInt().coerceIn(0, 3)) }
    var explanation by remember { mutableStateOf(initial.explanation) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit MCQ Question", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    OutlinedTextField(
                        value = qText,
                        onValueChange = { qText = it },
                        label = { Text("Question") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item { OutlinedTextField(value = opt0, onValueChange = { opt0 = it }, label = { Text("Option A") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = opt1, onValueChange = { opt1 = it }, label = { Text("Option B") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = opt2, onValueChange = { opt2 = it }, label = { Text("Option C") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = opt3, onValueChange = { opt3 = it }, label = { Text("Option D") }, modifier = Modifier.fillMaxWidth()) }
                item {
                    Text("Correct Answer", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("A", "B", "C", "D").forEachIndexed { i, lbl ->
                            FilterChip(
                                selected = correctIdx == i,
                                onClick = { correctIdx = i },
                                label = { Text(lbl) }
                            )
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = explanation,
                        onValueChange = { explanation = it },
                        label = { Text("Explanation") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (qText.isNotBlank()) {
                    onSave(
                        McqQuestion(
                            question = qText.trim(),
                            options = listOf(opt0.trim(), opt1.trim(), opt2.trim(), opt3.trim()),
                            correctIndex = correctIdx.toLong(),
                            explanation = explanation.trim()
                        )
                    )
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

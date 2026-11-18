package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Biotech
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.GoogleSignInButton
import com.example.ui.theme.HeroVioletEnd
import com.example.ui.theme.HeroVioletMid
import com.example.ui.theme.HeroVioletStart

fun subjectIconForKey(iconKey: String): ImageVector {
    return when (iconKey.lowercase()) {
        "calculate", "math", "maths" -> Icons.Rounded.Calculate
        "science" -> Icons.Rounded.Science
        "public", "sst", "history", "geography" -> Icons.Rounded.Public
        "translate", "hindi", "english" -> Icons.Rounded.Translate
        "bolt", "physics" -> Icons.Rounded.Bolt
        "biology", "bio" -> Icons.Rounded.Biotech
        "computer", "cs" -> Icons.Rounded.Computer
        else -> Icons.Rounded.AutoStories
    }
}

fun subjectGradientColors(iconKey: String, index: Int): List<Color> {
    return when (iconKey.lowercase()) {
        "calculate", "math", "maths" -> listOf(Color(0xFFFF5E6C), Color(0xFFE11D48))
        "science" -> listOf(Color(0xFF4ADE80), Color(0xFF16A34A))
        "bolt", "physics" -> listOf(Color(0xFF38BDF8), Color(0xFF1D4ED8))
        "translate", "hindi", "english" -> listOf(Color(0xFFFB923C), Color(0xFFEA580C))
        "biology", "bio" -> listOf(Color(0xFFC084FC), Color(0xFF7E22CE))
        "public", "sst", "history", "geography" -> listOf(Color(0xFF2DD4BF), Color(0xFF0284C7))
        else -> when (index % 6) {
            0 -> listOf(Color(0xFFFF5E6C), Color(0xFFE11D48))
            1 -> listOf(Color(0xFF4ADE80), Color(0xFF16A34A))
            2 -> listOf(Color(0xFF38BDF8), Color(0xFF1D4ED8))
            3 -> listOf(Color(0xFFFB923C), Color(0xFFEA580C))
            4 -> listOf(Color(0xFFC084FC), Color(0xFF7E22CE))
            else -> listOf(Color(0xFF2DD4BF), Color(0xFF0284C7))
        }
    }
}

@Composable
fun SoftStudyBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val backdropBrush = if (isDark) {
        Brush.linearGradient(
            colors = listOf(Color(0xFF0B1021), Color(0xFF141833), Color(0xFF191538)),
            start = Offset.Zero,
            end = Offset(900f, 1800f)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color(0xFFE9F0FF), Color(0xFFF3EEFF), Color(0xFFE7F3FF)),
            start = Offset.Zero,
            end = Offset(900f, 1800f)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backdropBrush),
        content = content
    )
}

@Composable
fun GradientPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
    content: @Composable RowScope.() -> Unit
) {
    val gradient = if (enabled) {
        Brush.horizontalGradient(listOf(HeroVioletStart, HeroVioletMid, HeroVioletEnd))
    } else {
        Brush.horizontalGradient(listOf(Color(0xFFCBD5E1), Color(0xFF94A3B8)))
    }
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            contentColor = Color.White,
            disabledContentColor = Color.White.copy(alpha = 0.8f)
        ),
        contentPadding = PaddingValues(0.dp),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient, shape = RoundedCornerShape(16.dp))
                .padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                content = content
            )
        }
    }
}

@Composable
fun StudyBottomBar(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    onCenterPracticeClick: () -> Unit,
    firstLabel: String = "Home",
    secondLabel: String = "Leaderboard"
) {
    val surfaceColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .shadow(14.dp, RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)),
            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
            color = surfaceColor
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                BottomNavItem(
                    selected = selectedTab == 0,
                    icon = Icons.Rounded.Home,
                    label = firstLabel,
                    tag = "nav_home",
                    onClick = { onSelectTab(0) },
                    modifier = Modifier.weight(1f)
                )
                BottomNavItem(
                    selected = selectedTab == 1,
                    icon = Icons.Rounded.EmojiEvents,
                    label = secondLabel,
                    tag = "nav_leaderboard",
                    onClick = { onSelectTab(1) },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(64.dp))
                BottomNavItem(
                    selected = selectedTab == 2,
                    icon = Icons.AutoMirrored.Rounded.Chat,
                    label = "Chat",
                    tag = "nav_chat",
                    onClick = { onSelectTab(2) },
                    modifier = Modifier.weight(1f)
                )
                BottomNavItem(
                    selected = selectedTab == 3,
                    icon = Icons.Rounded.Person,
                    label = "Profile",
                    tag = "nav_profile",
                    onClick = { onSelectTab(3) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Elevated Center Purple Practice Book FAB matching UI mockup
        Box(
            modifier = Modifier
                .offset(y = (-22).dp)
                .size(58.dp)
                .shadow(12.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(listOf(HeroVioletStart, HeroVioletEnd))
                )
                .clickable(onClick = onCenterPracticeClick)
                .testTag("nav_center_practice"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                contentDescription = "Quick Practice Subjects",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant
    val tint = if (selected) activeColor else inactiveColor

    Column(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SignInPromptDialog(
    reason: String,
    errorMessage: String?,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(HeroVioletStart, HeroVioletEnd))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.School,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
        },
        title = {
            Text(
                text = stringResource(R.string.sign_in_prompt_title),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = reason,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                if (!errorMessage.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                GoogleSignInButton(
                    onAuthSuccess = onAuthSuccess,
                    onAuthError = onAuthError
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Keep Exploring")
            }
        }
    )
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

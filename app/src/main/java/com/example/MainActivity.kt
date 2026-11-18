package com.example

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.compose.ui.platform.LocalContext
import com.example.data.BiharPrepRepository
import com.example.data.SubjectFolder
import com.example.data.UiState
import com.example.data.attemptAutoSignIn
import com.example.data.authStateFlow
import com.example.data.configureAppCheck
import com.example.data.signOutUser
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.ExploreGuestScreen
import com.example.ui.screens.LeaderboardScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.QuizScreen
import com.example.ui.screens.SoftStudyBackdrop
import com.example.ui.screens.StudentHomeScreen
import com.example.ui.screens.StudentHubScreen
import com.example.ui.screens.StudyBottomBar
import com.example.ui.screens.SubjectDetailScreen
import com.example.ui.screens.TestResultScreen
import com.example.ui.theme.BiharPrepTheme
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.StudentViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mandatory Step 1: Configure Firebase App Check before UI, ViewModels, or Firebase calls
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            BiharPrepTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BiharPrepRoot()
                }
            }
        }
    }
}

@Composable
fun BiharPrepRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val currentUser by Firebase.auth.authStateFlow()
        .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    LaunchedEffect(Unit) {
        if (Firebase.auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {},
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    val user = currentUser
    if (user == null) {
        // Guest exploration mode: zero unauthenticated Firestore queries!
        ExploreGuestScreen(onAuthenticated = {})
    } else {
        key(user.uid) {
            AuthenticatedAppShell(
                currentUserId = user.uid,
                credentialManager = credentialManager
            )
        }
    }
}

@Composable
fun AuthenticatedAppShell(
    currentUserId: String,
    credentialManager: CredentialManager
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember(context) { BiharPrepRepository(context) }
    var isAdmin by remember(currentUserId) { mutableStateOf(repository.isCurrentUserAdmin()) }

    LaunchedEffect(currentUserId) {
        isAdmin = repository.checkAdminPrivilege()
    }

    val studentViewModel: StudentViewModel = viewModel(
        key = "${currentUserId}_$isAdmin",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) as Application
                val repo = BiharPrepRepository(app)
                StudentViewModel(repo, currentUserId, isAdmin)
            }
        }
    )

    val profileState by studentViewModel.userProfile.collectAsStateWithLifecycle()
    val subjectsState by studentViewModel.subjects.collectAsStateWithLifecycle()
    val chaptersState by studentViewModel.chaptersForSubject.collectAsStateWithLifecycle()
    val quizzesState by studentViewModel.quizzesForChapter.collectAsStateWithLifecycle()
    val leaderboardState by studentViewModel.leaderboard.collectAsStateWithLifecycle()
    val myAttempts by studentViewModel.myAttempts.collectAsStateWithLifecycle()
    val announcements by studentViewModel.announcements.collectAsStateWithLifecycle()
    val chatMessages by studentViewModel.chatMessages.collectAsStateWithLifecycle()
    val studyGroups by studentViewModel.studyGroups.collectAsStateWithLifecycle()
    val selectedGroupId by studentViewModel.selectedGroupId.collectAsStateWithLifecycle()
    val groupMessages by studentViewModel.groupMessages.collectAsStateWithLifecycle()
    val activeQuizState by studentViewModel.activeQuizState.collectAsStateWithLifecycle()
    val actionStatus by studentViewModel.actionStatus.collectAsStateWithLifecycle()

    var bottomTab by remember { mutableIntStateOf(0) }
    var hubInitialTab by remember { mutableIntStateOf(0) }
    var openedSubject by remember { mutableStateOf<SubjectFolder?>(null) }
    var selectedChapterId by remember { mutableStateOf<String?>(null) }
    var showAdminDashboard by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(actionStatus) {
        actionStatus?.let {
            snackbarHostState.showSnackbar(it)
            studentViewModel.clearActionStatus()
        }
    }

    // 1. Admin Dashboard Mode (only reachable when isAdmin == true)
    if (showAdminDashboard && isAdmin) {
        val adminViewModel: AdminViewModel = viewModel(
            key = "admin_$currentUserId",
            factory = viewModelFactory {
                initializer {
                    val app = checkNotNull(this[APPLICATION_KEY]) as Application
                    AdminViewModel(BiharPrepRepository(app))
                }
            }
        )
        AdminDashboardScreen(
            adminViewModel = adminViewModel,
            onBackToStudentApp = { showAdminDashboard = false }
        )
        return
    }

    // 2. Active Test Result Report Screen
    val completedAttempt = activeQuizState.completedAttempt
    if (completedAttempt != null) {
        TestResultScreen(
            attempt = completedAttempt,
            quiz = activeQuizState.quiz,
            leaderboardState = leaderboardState,
            currentUserId = currentUserId,
            onDone = { studentViewModel.clearActiveQuiz() }
        )
        return
    }

    // 3. Active Objective Quiz Runner
    if (activeQuizState.quiz != null) {
        QuizScreen(
            state = activeQuizState,
            onSelectOption = studentViewModel::selectQuestionOption,
            onJumpToQuestion = studentViewModel::jumpToQuestion,
            onSubmit = studentViewModel::submitActiveQuiz,
            onExit = studentViewModel::clearActiveQuiz
        )
        return
    }

    // 4. Subject Folder -> Chapter -> Practice Sets Screen
    val subject = openedSubject
    if (subject != null) {
        SubjectDetailScreen(
            subject = subject,
            chaptersState = chaptersState,
            selectedChapterId = selectedChapterId,
            quizzesState = quizzesState,
            onSelectChapter = { chap ->
                selectedChapterId = if (selectedChapterId == chap.id) null else chap.id
                studentViewModel.selectChapter(chap.id)
            },
            onStartQuiz = { quiz ->
                studentViewModel.startQuiz(quiz, subject.name)
            },
            onBack = {
                openedSubject = null
                selectedChapterId = null
            }
        )
        return
    }

    // 5. Main Student Bottom-Bar Shell with luminous backdrop & center Practice Book FAB
    SoftStudyBackdrop {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                StudyBottomBar(
                    selectedTab = bottomTab,
                    onSelectTab = { tab ->
                        if (tab == 2) hubInitialTab = 0
                        bottomTab = tab
                    },
                    onCenterPracticeClick = {
                        val firstSubject = (subjectsState as? UiState.Success)?.data?.firstOrNull()
                        if (firstSubject != null) {
                            openedSubject = firstSubject
                            selectedChapterId = null
                            studentViewModel.selectSubject(firstSubject.id)
                        } else {
                            bottomTab = 0
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (bottomTab) {
                    0 -> StudentHomeScreen(
                        profileState = profileState,
                        subjectsState = subjectsState,
                        leaderboardState = leaderboardState,
                        announcements = announcements,
                        isAdmin = isAdmin,
                        onSubjectClick = { sub ->
                            openedSubject = sub
                            selectedChapterId = null
                            studentViewModel.selectSubject(sub.id)
                        },
                        onOpenAnnouncements = {
                            hubInitialTab = 3
                            bottomTab = 2
                        },
                        onOpenLeaderboard = { bottomTab = 1 },
                        onOpenChat = {
                            hubInitialTab = 2
                            bottomTab = 2
                        },
                        onOpenGroups = {
                            hubInitialTab = 1
                            bottomTab = 2
                        },
                        onOpenAdminPanel = { showAdminDashboard = true }
                    )
                    1 -> LeaderboardScreen(
                        leaderboardState = leaderboardState,
                        currentUserId = currentUserId
                    )
                    2 -> StudentHubScreen(
                        initialTab = hubInitialTab,
                        messages = chatMessages,
                        announcements = announcements,
                        studyGroups = studyGroups,
                        selectedGroupId = selectedGroupId,
                        groupMessages = groupMessages,
                        currentUserId = currentUserId,
                        isAdmin = isAdmin,
                        onSendMessage = studentViewModel::sendChatMessage,
                        onSelectGroup = studentViewModel::selectStudyGroup,
                        onCreateGroup = studentViewModel::createStudyGroup,
                        onJoinGroup = studentViewModel::joinStudyGroup,
                        onLeaveGroup = studentViewModel::leaveStudyGroup,
                        onDeleteGroup = studentViewModel::deleteStudyGroup,
                        onSendGroupMessage = studentViewModel::sendGroupMessage
                    )
                    else -> ProfileScreen(
                        profileState = profileState,
                        leaderboardState = leaderboardState,
                        attempts = myAttempts,
                        currentUserId = currentUserId,
                        isAdmin = isAdmin,
                        onUpdateProfile = studentViewModel::updateProfile,
                        onOpenAttemptReview = { att ->
                            studentViewModel.openPastAttemptReview(att, null)
                        },
                        onOpenStudyGroups = {
                            hubInitialTab = 1
                            bottomTab = 2
                        },
                        onOpenAdminPanel = { showAdminDashboard = true },
                        onSignOut = {
                            signOutUser(credentialManager, scope)
                        }
                    )
                }
            }
        }
    }
}

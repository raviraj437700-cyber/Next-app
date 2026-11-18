package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Announcement
import com.example.data.BiharPrepRepository
import com.example.data.ChapterFolder
import com.example.data.ChatMessage
import com.example.data.GroupMessage
import com.example.data.LeaderboardEntry
import com.example.data.QuizSet
import com.example.data.StudyGroup
import com.example.data.SubjectFolder
import com.example.data.TestAttempt
import com.example.data.UiState
import com.example.data.UserProfile
import com.example.data.unwrapFirestoreErrorMessage
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ActiveQuizUiState(
    val quiz: QuizSet? = null,
    val subjectName: String = "",
    val currentQuestionIndex: Int = 0,
    val selectedAnswers: List<Int> = emptyList(),
    val secondsRemaining: Int = 0,
    val isSubmitting: Boolean = false,
    val completedAttempt: TestAttempt? = null,
    val errorMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class StudentViewModel(
    private val repository: BiharPrepRepository,
    val currentUserId: String,
    val isAdmin: Boolean
) : ViewModel() {

    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            Firebase.auth.currentUser?.let { user ->
                repository.ensureUserProfileExists(user)
            }
        }
    }

    val userProfile: StateFlow<UiState<UserProfile?>> = repository.observeMyProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { error ->
            if (error is CancellationException && error.cause == null) throw error
            emit(UiState.Error(unwrapFirestoreErrorMessage(error, "Failed to load profile")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val subjects: StateFlow<UiState<List<SubjectFolder>>> = repository.observeSubjects(isAdmin)
        .map<List<SubjectFolder>, UiState<List<SubjectFolder>>> { UiState.Success(it) }
        .catch { error ->
            if (error is CancellationException && error.cause == null) throw error
            emit(UiState.Error(unwrapFirestoreErrorMessage(error, "Failed to load subjects")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val _selectedSubjectId = MutableStateFlow<String?>(null)
    val chaptersForSubject: StateFlow<UiState<List<ChapterFolder>>> = _selectedSubjectId
        .flatMapLatest { subId ->
            if (subId.isNullOrBlank()) {
                flowOf(UiState.Success(emptyList()))
            } else {
                repository.observeChapters(subId, isAdmin)
                    .map<List<ChapterFolder>, UiState<List<ChapterFolder>>> { UiState.Success(it) }
                    .catch { error ->
                        if (error is CancellationException && error.cause == null) throw error
                        emit(UiState.Error(unwrapFirestoreErrorMessage(error, "Failed to load chapters")))
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val _selectedChapterId = MutableStateFlow<String?>(null)
    val quizzesForChapter: StateFlow<UiState<List<QuizSet>>> = _selectedChapterId
        .flatMapLatest { chapId ->
            if (chapId.isNullOrBlank()) {
                flowOf(UiState.Success(emptyList()))
            } else {
                repository.observeQuizzesForChapter(chapId, isAdmin)
                    .map<List<QuizSet>, UiState<List<QuizSet>>> { UiState.Success(it) }
                    .catch { error ->
                        if (error is CancellationException && error.cause == null) throw error
                        emit(UiState.Error(unwrapFirestoreErrorMessage(error, "Failed to load tests")))
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val leaderboard: StateFlow<UiState<List<LeaderboardEntry>>> = repository.observeLeaderboard()
        .map<List<LeaderboardEntry>, UiState<List<LeaderboardEntry>>> { UiState.Success(it) }
        .catch { error ->
            if (error is CancellationException && error.cause == null) throw error
            emit(UiState.Error(unwrapFirestoreErrorMessage(error, "Failed to load leaderboard")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val myAttempts: StateFlow<List<TestAttempt>> = repository.observeMyAttempts(currentUserId)
        .catch { error ->
            if (error is CancellationException && error.cause == null) throw error
            Log.w(TAG, "Error observing attempts", error)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val announcements: StateFlow<List<Announcement>> = repository.observeStudentAnnouncements(currentUserId)
        .catch { error ->
            if (error is CancellationException && error.cause == null) throw error
            Log.w(TAG, "Error observing announcements", error)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val chatMessages: StateFlow<List<ChatMessage>> = repository.observeChatMessages(currentUserId)
        .catch { error ->
            if (error is CancellationException && error.cause == null) throw error
            Log.w(TAG, "Error observing chat messages", error)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val studyGroups: StateFlow<List<StudyGroup>> = repository.observeStudyGroups(currentUserId)
        .catch { error ->
            if (error is CancellationException && error.cause == null) throw error
            Log.w(TAG, "Error observing study groups", error)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _selectedGroupId = MutableStateFlow<String?>(null)
    val selectedGroupId: StateFlow<String?> = _selectedGroupId.asStateFlow()

    val groupMessages: StateFlow<List<GroupMessage>> = _selectedGroupId
        .flatMapLatest { groupId ->
            if (groupId.isNullOrBlank()) flowOf(emptyList())
            else repository.observeGroupMessages(groupId)
                .catch { error ->
                    if (error is CancellationException && error.cause == null) throw error
                    Log.w(TAG, "Error observing group messages", error)
                    emit(emptyList())
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _activeQuizState = MutableStateFlow(ActiveQuizUiState())
    val activeQuizState: StateFlow<ActiveQuizUiState> = _activeQuizState.asStateFlow()

    private val _actionStatus = MutableStateFlow<String?>(null)
    val actionStatus: StateFlow<String?> = _actionStatus.asStateFlow()

    fun clearActionStatus() {
        _actionStatus.value = null
    }

    fun selectSubject(subjectId: String) {
        _selectedSubjectId.value = subjectId
        _selectedChapterId.value = null
    }

    fun selectChapter(chapterId: String) {
        _selectedChapterId.value = chapterId
    }

    fun startQuiz(quiz: QuizSet, subjectName: String) {
        timerJob?.cancel()
        val initialAnswers = List(quiz.questions.size) { -1 }
        val totalSeconds = (quiz.durationMinutes.toInt().coerceAtLeast(1)) * 60
        _activeQuizState.value = ActiveQuizUiState(
            quiz = quiz,
            subjectName = subjectName,
            currentQuestionIndex = 0,
            selectedAnswers = initialAnswers,
            secondsRemaining = totalSeconds,
            isSubmitting = false,
            completedAttempt = null,
            errorMessage = null
        )
        timerJob = viewModelScope.launch {
            while (_activeQuizState.value.secondsRemaining > 0 &&
                _activeQuizState.value.completedAttempt == null &&
                _activeQuizState.value.quiz != null
            ) {
                delay(1000L)
                val current = _activeQuizState.value
                if (current.secondsRemaining > 0 && !current.isSubmitting && current.completedAttempt == null) {
                    val next = current.secondsRemaining - 1
                    _activeQuizState.value = current.copy(secondsRemaining = next)
                    if (next == 0) {
                        submitActiveQuiz()
                        break
                    }
                }
            }
        }
    }

    fun selectQuestionOption(questionIdx: Int, optionIdx: Int) {
        val current = _activeQuizState.value
        val updated = current.selectedAnswers.toMutableList()
        if (questionIdx in updated.indices) {
            updated[questionIdx] = optionIdx
            _activeQuizState.value = current.copy(selectedAnswers = updated)
        }
    }

    fun jumpToQuestion(index: Int) {
        val current = _activeQuizState.value
        val size = current.quiz?.questions?.size ?: 0
        if (index in 0 until size) {
            _activeQuizState.value = current.copy(currentQuestionIndex = index)
        }
    }

    fun submitActiveQuiz() {
        val current = _activeQuizState.value
        val quiz = current.quiz ?: return
        if (current.isSubmitting || current.completedAttempt != null) return

        timerJob?.cancel()
        _activeQuizState.value = current.copy(isSubmitting = true, errorMessage = null)

        val profileName = (userProfile.value as? UiState.Success)?.data?.displayName
            ?: Firebase.auth.currentUser?.displayName
            ?: "Bihar Student"

        viewModelScope.launch {
            val result = repository.submitTestAttempt(
                quiz = quiz,
                subjectName = current.subjectName,
                userName = profileName,
                selectedAnswers = current.selectedAnswers
            )
            result.onSuccess { attempt ->
                _activeQuizState.value = _activeQuizState.value.copy(
                    isSubmitting = false,
                    completedAttempt = attempt
                )
            }.onFailure { err ->
                _activeQuizState.value = _activeQuizState.value.copy(
                    isSubmitting = false,
                    errorMessage = err.localizedMessage ?: "Could not submit test."
                )
            }
        }
    }

    fun openPastAttemptReview(attempt: TestAttempt, quiz: QuizSet?) {
        timerJob?.cancel()
        _activeQuizState.value = ActiveQuizUiState(
            quiz = quiz,
            subjectName = attempt.subjectName,
            completedAttempt = attempt
        )
    }

    fun clearActiveQuiz() {
        timerJob?.cancel()
        _activeQuizState.value = ActiveQuizUiState()
    }

    fun updateProfile(displayName: String, boardClass: String, schoolName: String) {
        viewModelScope.launch {
            repository.updateMyProfile(displayName, boardClass, schoolName)
                .onSuccess { _actionStatus.value = "Profile updated successfully!" }
                .onFailure { _actionStatus.value = it.localizedMessage ?: "Failed to update profile." }
        }
    }

    fun sendChatMessage(text: String) {
        val profileName = (userProfile.value as? UiState.Success)?.data?.displayName
            ?: Firebase.auth.currentUser?.displayName
            ?: "Student"
        viewModelScope.launch {
            repository.sendChatMessage(
                studentId = currentUserId,
                studentName = profileName,
                senderName = profileName,
                asAdmin = false,
                text = text
            ).onFailure {
                _actionStatus.value = it.localizedMessage ?: "Failed to send message."
            }
        }
    }

    fun selectStudyGroup(groupId: String?) {
        _selectedGroupId.value = groupId
    }

    fun createStudyGroup(name: String, description: String, boardClass: String, subjectTag: String) {
        val profileName = (userProfile.value as? UiState.Success)?.data?.displayName
            ?: Firebase.auth.currentUser?.displayName
            ?: "Bihar Student"
        viewModelScope.launch {
            repository.createStudyGroup(name, description, boardClass, subjectTag, profileName)
                .onSuccess { newId ->
                    _selectedGroupId.value = newId
                    _actionStatus.value = "Study group created!"
                }
                .onFailure {
                    _actionStatus.value = it.localizedMessage ?: "Failed to create study group."
                }
        }
    }

    fun joinStudyGroup(group: StudyGroup) {
        viewModelScope.launch {
            repository.joinStudyGroup(group)
                .onSuccess {
                    _selectedGroupId.value = group.id
                    _actionStatus.value = "Joined ${group.name}!"
                }
                .onFailure {
                    _actionStatus.value = it.localizedMessage ?: "Failed to join group."
                }
        }
    }

    fun leaveStudyGroup(group: StudyGroup) {
        viewModelScope.launch {
            repository.leaveStudyGroup(group)
                .onSuccess {
                    if (_selectedGroupId.value == group.id) _selectedGroupId.value = null
                    _actionStatus.value = "Left ${group.name}."
                }
                .onFailure {
                    _actionStatus.value = it.localizedMessage ?: "Failed to leave group."
                }
        }
    }

    fun deleteStudyGroup(groupId: String) {
        viewModelScope.launch {
            repository.deleteStudyGroup(groupId)
                .onSuccess {
                    if (_selectedGroupId.value == groupId) _selectedGroupId.value = null
                    _actionStatus.value = "Study group deleted."
                }
                .onFailure {
                    _actionStatus.value = it.localizedMessage ?: "Failed to delete group."
                }
        }
    }

    fun sendGroupMessage(groupId: String, text: String) {
        val profileName = (userProfile.value as? UiState.Success)?.data?.displayName
            ?: Firebase.auth.currentUser?.displayName
            ?: "Student"
        viewModelScope.launch {
            repository.sendGroupMessage(groupId, profileName, text)
                .onFailure {
                    _actionStatus.value = it.localizedMessage ?: "Failed to send message."
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    private companion object {
        const val TAG = "StudentVM"
    }
}

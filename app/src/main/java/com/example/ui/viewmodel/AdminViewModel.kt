package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Announcement
import com.example.data.BiharPrepRepository
import com.example.data.ChapterFolder
import com.example.data.ChatMessage
import com.example.data.ChatThread
import com.example.data.GeneratedQuizDraft
import com.example.data.McqQuestion
import com.example.data.PdfQuizAiRepository
import com.example.data.QuizSet
import com.example.data.StudyGroup
import com.example.data.SubjectFolder
import com.example.data.TestAttempt
import com.example.data.UiState
import com.example.data.UserProfile
import com.example.data.unwrapFirestoreErrorMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

sealed interface AiPdfUiState {
    data object Idle : AiPdfUiState
    data object Analyzing : AiPdfUiState
    data class DraftReady(val draft: GeneratedQuizDraft) : AiPdfUiState
    data class Error(val message: String) : AiPdfUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class AdminViewModel(
    private val repository: BiharPrepRepository,
    private val aiRepository: PdfQuizAiRepository = PdfQuizAiRepository()
) : ViewModel() {

    val allUsers: StateFlow<UiState<List<UserProfile>>> = repository.observeAllUsersAdmin()
        .map<List<UserProfile>, UiState<List<UserProfile>>> { UiState.Success(it) }
        .catch { error ->
            if (error is CancellationException && error.cause == null) throw error
            emit(UiState.Error(unwrapFirestoreErrorMessage(error, "Failed to load students")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val allAttempts: StateFlow<List<TestAttempt>> = repository.observeAllAttemptsAdmin()
        .catch { error ->
            if (error is CancellationException && error.cause == null) throw error
            Log.w(TAG, "Error loading attempts", error)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allSubjects: StateFlow<UiState<List<SubjectFolder>>> = repository.observeSubjects(isAdmin = true)
        .map<List<SubjectFolder>, UiState<List<SubjectFolder>>> { UiState.Success(it) }
        .catch { error ->
            if (error is CancellationException && error.cause == null) throw error
            emit(UiState.Error(unwrapFirestoreErrorMessage(error, "Failed to load subjects")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val allChapters: StateFlow<List<ChapterFolder>> = repository.observeAllChaptersAdmin()
        .catch { error ->
            if (error is CancellationException && error.cause == null) throw error
            Log.w(TAG, "Error loading chapters", error)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allQuizzes: StateFlow<List<QuizSet>> = repository.observeAllQuizzesAdmin()
        .catch { error ->
            if (error is CancellationException && error.cause == null) throw error
            Log.w(TAG, "Error loading quizzes", error)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allChatThreads: StateFlow<List<ChatThread>> = repository.observeAllChatThreadsAdmin()
        .catch { error ->
            if (error is CancellationException && error.cause == null) throw error
            Log.w(TAG, "Error loading chat threads", error)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allAnnouncements: StateFlow<List<Announcement>> = repository.observeAllAnnouncementsAdmin()
        .catch { error ->
            if (error is CancellationException && error.cause == null) throw error
            Log.w(TAG, "Error loading announcements", error)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allStudyGroups: StateFlow<List<StudyGroup>> = repository.observeAllStudyGroupsAdmin()
        .catch { error ->
            if (error is CancellationException && error.cause == null) throw error
            Log.w(TAG, "Error loading study groups", error)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _selectedChatStudentId = MutableStateFlow<String?>(null)
    val selectedChatStudentId: StateFlow<String?> = _selectedChatStudentId.asStateFlow()

    val selectedChatMessages: StateFlow<List<ChatMessage>> = _selectedChatStudentId
        .flatMapLatest { studentId ->
            if (studentId.isNullOrBlank()) flowOf(emptyList())
            else repository.observeChatMessages(studentId)
                .catch { error ->
                    if (error is CancellationException && error.cause == null) throw error
                    emit(emptyList())
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _aiState = MutableStateFlow<AiPdfUiState>(AiPdfUiState.Idle)
    val aiState: StateFlow<AiPdfUiState> = _aiState.asStateFlow()

    private val _adminToast = MutableStateFlow<String?>(null)
    val adminToast: StateFlow<String?> = _adminToast.asStateFlow()

    fun clearToast() {
        _adminToast.value = null
    }

    fun selectChatThread(studentId: String?) {
        _selectedChatStudentId.value = studentId
    }

    fun seedStarterPack() {
        viewModelScope.launch {
            repository.seedBiharBoardStarterContent()
                .onSuccess { _adminToast.value = "Bihar Board starter subjects & tests published!" }
                .onFailure { _adminToast.value = it.localizedMessage ?: "Starter seed failed." }
        }
    }

    fun saveSubject(
        subjectId: String?,
        name: String,
        description: String,
        boardClass: String,
        iconKey: String,
        orderIndex: Long,
        published: Boolean
    ) {
        viewModelScope.launch {
            repository.saveSubject(subjectId, name, description, boardClass, iconKey, orderIndex, published)
                .onSuccess { _adminToast.value = "Subject saved." }
                .onFailure { _adminToast.value = it.localizedMessage ?: "Failed to save subject." }
        }
    }

    fun deleteSubject(subjectId: String) {
        viewModelScope.launch {
            repository.deleteSubject(subjectId)
                .onSuccess { _adminToast.value = "Subject deleted." }
                .onFailure { _adminToast.value = it.localizedMessage ?: "Delete failed." }
        }
    }

    fun saveChapter(
        chapterId: String?,
        subjectId: String,
        title: String,
        summary: String,
        chapterNumber: Long,
        published: Boolean
    ) {
        viewModelScope.launch {
            repository.saveChapter(chapterId, subjectId, title, summary, chapterNumber, published)
                .onSuccess { _adminToast.value = "Chapter saved." }
                .onFailure { _adminToast.value = it.localizedMessage ?: "Failed to save chapter." }
        }
    }

    fun deleteChapter(chapterId: String) {
        viewModelScope.launch {
            repository.deleteChapter(chapterId)
                .onSuccess { _adminToast.value = "Chapter deleted." }
                .onFailure { _adminToast.value = it.localizedMessage ?: "Delete failed." }
        }
    }

    fun saveQuiz(
        quizId: String?,
        subjectId: String,
        chapterId: String,
        title: String,
        durationMinutes: Long,
        sourceType: String,
        published: Boolean,
        questions: List<McqQuestion>
    ) {
        viewModelScope.launch {
            repository.saveQuiz(quizId, subjectId, chapterId, title, durationMinutes, sourceType, published, questions)
                .onSuccess { _adminToast.value = "Quiz saved & synced live!" }
                .onFailure { _adminToast.value = it.localizedMessage ?: "Failed to save quiz." }
        }
    }

    fun deleteQuiz(quizId: String) {
        viewModelScope.launch {
            repository.deleteQuiz(quizId)
                .onSuccess { _adminToast.value = "Quiz deleted." }
                .onFailure { _adminToast.value = it.localizedMessage ?: "Delete failed." }
        }
    }

    fun generateQuizFromPdf(
        context: Context,
        pdfUri: Uri,
        subjectName: String,
        chapterTitle: String,
        questionCount: Int,
        languageMode: String
    ) {
        viewModelScope.launch {
            _aiState.value = AiPdfUiState.Analyzing
            aiRepository.generateQuizFromPdf(
                context = context,
                pdfUri = pdfUri,
                subjectName = subjectName,
                chapterTitle = chapterTitle,
                questionCount = questionCount,
                languageMode = languageMode
            ).onSuccess { draft ->
                _aiState.value = AiPdfUiState.DraftReady(draft)
            }.onFailure { err ->
                _aiState.value = AiPdfUiState.Error(
                    err.localizedMessage ?: "Failed to analyze PDF and generate MCQs."
                )
            }
        }
    }

    fun updateDraftQuestion(index: Int, updated: McqQuestion) {
        val current = _aiState.value as? AiPdfUiState.DraftReady ?: return
        val mutable = current.draft.questions.toMutableList()
        if (index in mutable.indices) {
            mutable[index] = updated
            _aiState.value = AiPdfUiState.DraftReady(current.draft.copy(questions = mutable))
        }
    }

    fun removeDraftQuestion(index: Int) {
        val current = _aiState.value as? AiPdfUiState.DraftReady ?: return
        val mutable = current.draft.questions.toMutableList()
        if (index in mutable.indices && mutable.size > 1) {
            mutable.removeAt(index)
            _aiState.value = AiPdfUiState.DraftReady(current.draft.copy(questions = mutable))
        }
    }

    fun publishAiDraft(
        subjectId: String,
        chapterId: String,
        title: String,
        durationMinutes: Long,
        published: Boolean = true
    ) {
        val current = _aiState.value as? AiPdfUiState.DraftReady ?: return
        viewModelScope.launch {
            repository.saveQuiz(
                quizId = null,
                subjectId = subjectId,
                chapterId = chapterId,
                title = title.ifBlank { current.draft.quizTitle },
                durationMinutes = durationMinutes,
                sourceType = "ai_pdf",
                published = published,
                questions = current.draft.questions
            ).onSuccess {
                _aiState.value = AiPdfUiState.Idle
                _adminToast.value = if (published) "AI Quiz published to chapter!" else "AI Quiz saved as draft."
            }.onFailure {
                _adminToast.value = it.localizedMessage ?: "Failed to publish AI quiz."
            }
        }
    }

    fun resetAiState() {
        _aiState.value = AiPdfUiState.Idle
    }

    fun sendAdminReply(studentId: String, studentName: String, text: String) {
        viewModelScope.launch {
            repository.sendChatMessage(
                studentId = studentId,
                studentName = studentName,
                senderName = "Admin (Sir)",
                asAdmin = true,
                text = text
            ).onFailure {
                _adminToast.value = it.localizedMessage ?: "Failed to send reply."
            }
        }
    }

    fun sendAnnouncement(
        title: String,
        message: String,
        targetUserId: String,
        targetUserName: String,
        priority: String
    ) {
        viewModelScope.launch {
            repository.sendAnnouncement(title, message, targetUserId, targetUserName, priority)
                .onSuccess { _adminToast.value = "Announcement sent to $targetUserName!" }
                .onFailure { _adminToast.value = it.localizedMessage ?: "Failed to send announcement." }
        }
    }

    fun deleteAnnouncement(id: String) {
        viewModelScope.launch {
            repository.deleteAnnouncement(id)
                .onSuccess { _adminToast.value = "Announcement removed." }
                .onFailure { _adminToast.value = it.localizedMessage ?: "Delete failed." }
        }
    }

    fun deleteStudyGroup(groupId: String) {
        viewModelScope.launch {
            repository.deleteStudyGroup(groupId)
                .onSuccess { _adminToast.value = "Study group removed." }
                .onFailure { _adminToast.value = it.localizedMessage ?: "Delete failed." }
        }
    }

    private companion object {
        const val TAG = "AdminVM"
    }
}

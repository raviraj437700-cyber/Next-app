package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

const val ADMIN_EMAIL = "raviraj43770@gmail.com"

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val boardClass: String = "Class 10",
    val schoolName: String = "",
    val totalScore: Long = 0L,
    val testsTaken: Long = 0L,
    val averagePercent: Double = 0.0,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LeaderboardEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val boardClass: String = "Class 10",
    val visibility: String = "public",
    val totalScore: Long = 0L,
    val testsTaken: Long = 0L,
    val averagePercent: Double = 0.0,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SubjectFolder(
    @DocumentId val id: String = "",
    val name: String = "",
    val description: String = "",
    val boardClass: String = "Class 10",
    val iconKey: String = "book",
    val orderIndex: Long = 0L,
    val published: Boolean = true,
    val createdBy: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChapterFolder(
    @DocumentId val id: String = "",
    val subjectId: String = "",
    val title: String = "",
    val summary: String = "",
    val chapterNumber: Long = 1L,
    val published: Boolean = true,
    val createdBy: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class McqQuestion(
    val question: String = "",
    val options: List<String> = listOf("", "", "", ""),
    val correctIndex: Long = 0L,
    val explanation: String = ""
)

@Keep
@IgnoreExtraProperties
data class QuizSet(
    @DocumentId val id: String = "",
    val subjectId: String = "",
    val chapterId: String = "",
    val title: String = "",
    val durationMinutes: Long = 15L,
    val sourceType: String = "manual", // "manual" or "ai_pdf"
    val published: Boolean = true,
    val questions: List<McqQuestion> = emptyList(),
    val createdBy: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class TestAttempt(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val quizId: String = "",
    val chapterId: String = "",
    val subjectId: String = "",
    val quizTitle: String = "",
    val subjectName: String = "",
    val totalQuestions: Long = 0L,
    val correctCount: Long = 0L,
    val wrongCount: Long = 0L,
    val percentage: Double = 0.0,
    val answers: List<Long> = emptyList(),
    val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatThread(
    @DocumentId val id: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val lastMessage: String = "",
    val unreadByAdmin: Long = 0L,
    val unreadByStudent: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val senderRole: String = "student", // "student" or "admin"
    val senderName: String = "",
    val text: String = "",
    val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Announcement(
    @DocumentId val id: String = "",
    val title: String = "",
    val message: String = "",
    val targetUserId: String = "ALL", // "ALL" or specific student UID
    val targetUserName: String = "All Students",
    val priority: String = "normal", // "normal" or "urgent"
    val createdBy: String = "",
    val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StudyGroup(
    @DocumentId val id: String = "",
    val name: String = "",
    val description: String = "",
    val boardClass: String = "Class 10",
    val subjectTag: String = "General",
    val inviteCode: String = "",
    val isPublic: Boolean = true,
    val createdBy: String = "",
    val creatorName: String = "",
    val memberIds: List<String> = emptyList(),
    val memberCount: Long = 1L,
    val lastMessage: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class GroupMessage(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val createdAt: Timestamp? = null
)

/**
 * Local in-memory preview catalog so unauthenticated guests can explore Bihar Board
 * subjects and chapters before signing in to attempt tests (zero unauthenticated Firestore calls).
 */
object GuestPreviewCatalog {
    val previewSubjects = listOf(
        SubjectFolder(
            id = "preview_maths",
            name = "Mathematics (गणित)",
            description = "BSEB Class 10 • Real Numbers, Polynomials, Trigonometry, Coordinate Geometry",
            boardClass = "Class 10",
            iconKey = "calculate",
            orderIndex = 1L,
            published = true
        ),
        SubjectFolder(
            id = "preview_science",
            name = "Science (विज्ञान)",
            description = "BSEB Class 10 • Physics, Chemistry, Biology objective practice sets",
            boardClass = "Class 10",
            iconKey = "science",
            orderIndex = 2L,
            published = true
        ),
        SubjectFolder(
            id = "preview_sst",
            name = "Social Science (सामाजिक विज्ञान)",
            description = "BSEB Class 10 • History, Geography, Civics & Economics MCQs",
            boardClass = "Class 10",
            iconKey = "public",
            orderIndex = 3L,
            published = true
        ),
        SubjectFolder(
            id = "preview_hindi",
            name = "Hindi (हिन्दी - गोधूलि)",
            description = "BSEB Class 10 • Gadya, Padya, Vyakaran objective bank",
            boardClass = "Class 10",
            iconKey = "translate",
            orderIndex = 4L,
            published = true
        ),
        SubjectFolder(
            id = "preview_physics12",
            name = "Physics (भौतिकी)",
            description = "BSEB Class 12 • Electrostatics, Current Electricity, Optics",
            boardClass = "Class 12",
            iconKey = "bolt",
            orderIndex = 5L,
            published = true
        )
    )

    val previewChaptersBySubject = mapOf(
        "preview_maths" to listOf(
            ChapterFolder(id = "pc_m1", subjectId = "preview_maths", title = "Real Numbers (वास्तविक संख्याएँ)", summary = "Euclid's division lemma, HCF, LCM & irrational numbers", chapterNumber = 1L),
            ChapterFolder(id = "pc_m2", subjectId = "preview_maths", title = "Polynomials (बहुपद)", summary = "Zeroes of polynomials & quadratic relations", chapterNumber = 2L),
            ChapterFolder(id = "pc_m3", subjectId = "preview_maths", title = "Trigonometry (त्रिकोणमिति)", summary = "Trigonometric ratios, identities & heights", chapterNumber = 8L)
        ),
        "preview_science" to listOf(
            ChapterFolder(id = "pc_s1", subjectId = "preview_science", title = "Light: Reflection & Refraction", summary = "Mirror & lens formulas, refractive index", chapterNumber = 1L),
            ChapterFolder(id = "pc_s2", subjectId = "preview_science", title = "Chemical Reactions & Equations", summary = "Types of reactions, oxidation & reduction", chapterNumber = 2L),
            ChapterFolder(id = "pc_s3", subjectId = "preview_science", title = "Life Processes (जैव प्रक्रम)", summary = "Nutrition, respiration, circulation & excretion", chapterNumber = 6L)
        ),
        "preview_sst" to listOf(
            ChapterFolder(id = "pc_ss1", subjectId = "preview_sst", title = "Nationalism in Europe & India", summary = "Key dates, movements & freedom fighters", chapterNumber = 1L),
            ChapterFolder(id = "pc_ss2", subjectId = "preview_sst", title = "Resources & Agriculture in Bihar", summary = "Soils, crops, minerals & irrigation", chapterNumber = 2L)
        ),
        "preview_hindi" to listOf(
            ChapterFolder(id = "pc_h1", subjectId = "preview_hindi", title = "श्रम विभाजन और जाति प्रथा", summary = "Dr. Bhimrao Ambedkar — key objective points", chapterNumber = 1L),
            ChapterFolder(id = "pc_h2", subjectId = "preview_hindi", title = "हिन्दी व्याकरण (संधि एवं समास)", summary = "Board exam grammar MCQs", chapterNumber = 2L)
        ),
        "preview_physics12" to listOf(
            ChapterFolder(id = "pc_p1", subjectId = "preview_physics12", title = "Electric Charges & Fields", summary = "Coulomb's law, Gauss's theorem & electric dipole", chapterNumber = 1L)
        )
    )
}

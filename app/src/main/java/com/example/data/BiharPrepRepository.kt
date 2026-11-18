package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class BiharPrepRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ).apply {
            try {
                // Enable disk persistence (25MB cache) for fast offline/low-bandwidth performance
                firestoreSettings = FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(
                        PersistentCacheSettings.newBuilder()
                            .setSizeBytes(25L * 1024L * 1024L)
                            .build()
                    )
                    .build()
            } catch (_: Exception) {
                // Ignore if settings were already locked by an earlier instance
            }
        }
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Please sign in with Google first.")
    }

    fun isCurrentUserAdmin(user: FirebaseUser? = auth.currentUser): Boolean {
        val email = user?.email?.lowercase()?.trim()
        return user != null && user.isEmailVerified && email == ADMIN_EMAIL
    }

    suspend fun checkAdminPrivilege(): Boolean {
        val user = auth.currentUser ?: return false
        if (isCurrentUserAdmin(user)) return true
        return try {
            val doc = db.collection("admins").document(user.uid).get().await()
            doc.exists()
        } catch (_: Exception) {
            false
        }
    }

    // --- USER PROFILE & LEADERBOARD BOOTSTRAP ---

    suspend fun ensureUserProfileExists(user: FirebaseUser): Result<Unit> {
        val uid = user.uid
        var opPath = "users/$uid"
        var opType = OperationType.GET
        return try {
            val userRef = db.collection("users").document(uid)
            val snap = userRef.get().await()
            val safeName = (user.displayName?.takeIf { it.isNotBlank() } ?: "Bihar Student").take(80)
            val safeEmail = (user.email?.takeIf { it.isNotBlank() } ?: "student@biharprep.in").take(160)

            if (!snap.exists()) {
                opType = OperationType.CREATE
                val profilePayload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "email" to safeEmail,
                    "boardClass" to "Class 10",
                    "schoolName" to "",
                    "totalScore" to 0L,
                    "testsTaken" to 0L,
                    "averagePercent" to 0.0,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                userRef.set(profilePayload).await()
            }

            val lbRef = db.collection("leaderboard").document(uid)
            opPath = "leaderboard/$uid"
            opType = OperationType.GET
            val lbSnap = lbRef.get().await()
            if (!lbSnap.exists()) {
                opType = OperationType.CREATE
                val lbPayload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "boardClass" to "Class 10",
                    "visibility" to "public",
                    "totalScore" to 0L,
                    "testsTaken" to 0L,
                    "averagePercent" to 0.0,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                lbRef.set(lbPayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, opType, opPath)
            Result.failure(e)
        }
    }

    fun observeMyProfile(uid: String): Flow<UserProfile?> = flow {
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snap ->
                    snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(fsErr ?: error, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun updateMyProfile(displayName: String, boardClass: String, schoolName: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid"
        return try {
            val cleanName = displayName.trim().take(80)
            require(cleanName.isNotEmpty()) { "Name cannot be empty." }
            val cleanClass = boardClass.trim().ifBlank { "Class 10" }.take(40)
            val cleanSchool = schoolName.trim().take(120)

            db.collection("users").document(uid).update(
                mapOf(
                    "displayName" to cleanName,
                    "boardClass" to cleanClass,
                    "schoolName" to cleanSchool,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            db.collection("leaderboard").document(uid).update(
                mapOf(
                    "displayName" to cleanName,
                    "boardClass" to cleanClass,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- CURRICULUM FOLDERS: SUBJECTS, CHAPTERS, QUIZZES ---

    fun observeSubjects(isAdmin: Boolean): Flow<List<SubjectFolder>> = flow {
        val path = "subjects"
        val query = if (isAdmin) {
            db.collection(path)
        } else {
            db.collection(path).whereEqualTo("published", true)
        }
        emitAll(
            query.snapshots()
                .map { snap ->
                    snap.toObjects(SubjectFolder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.orderIndex }
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser != null) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeChapters(subjectId: String, isAdmin: Boolean): Flow<List<ChapterFolder>> = flow {
        val path = "chapters"
        val query = if (isAdmin) {
            db.collection(path).whereEqualTo("subjectId", subjectId)
        } else {
            db.collection(path)
                .whereEqualTo("subjectId", subjectId)
                .whereEqualTo("published", true)
        }
        emitAll(
            query.snapshots()
                .map { snap ->
                    snap.toObjects(ChapterFolder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.chapterNumber }
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser != null) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeQuizzesForChapter(chapterId: String, isAdmin: Boolean): Flow<List<QuizSet>> = flow {
        val path = "quizzes"
        val query = if (isAdmin) {
            db.collection(path).whereEqualTo("chapterId", chapterId)
        } else {
            db.collection(path)
                .whereEqualTo("chapterId", chapterId)
                .whereEqualTo("published", true)
        }
        emitAll(
            query.snapshots()
                .map { snap ->
                    snap.toObjects(QuizSet::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser != null) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeAllChaptersAdmin(): Flow<List<ChapterFolder>> = flow {
        val path = "chapters"
        emitAll(
            db.collection(path).snapshots()
                .map { snap ->
                    snap.toObjects(ChapterFolder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.chapterNumber }
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser != null) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeAllQuizzesAdmin(): Flow<List<QuizSet>> = flow {
        val path = "quizzes"
        emitAll(
            db.collection(path).snapshots()
                .map { snap ->
                    snap.toObjects(QuizSet::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser != null) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    // --- ADMIN CURRICULUM CRUD ---

    suspend fun saveSubject(
        subjectId: String?,
        name: String,
        description: String,
        boardClass: String,
        iconKey: String,
        orderIndex: Long,
        published: Boolean
    ): Result<String> {
        var docPath = "subjects"
        return try {
            val uid = requireUserId()
            val cleanName = name.trim().take(80)
            require(cleanName.isNotEmpty()) { "Subject name is required." }

            if (subjectId.isNullOrBlank()) {
                val docRef = db.collection("subjects").document()
                docPath = docRef.path
                docRef.set(
                    mapOf(
                        "name" to cleanName,
                        "description" to description.trim().take(300),
                        "boardClass" to boardClass.trim().ifBlank { "Class 10" }.take(40),
                        "iconKey" to iconKey.ifBlank { "book" }.take(40),
                        "orderIndex" to orderIndex.coerceIn(0L, 1000L),
                        "published" to published,
                        "createdBy" to uid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                Result.success(docRef.id)
            } else {
                val docRef = db.collection("subjects").document(subjectId)
                docPath = docRef.path
                docRef.update(
                    mapOf(
                        "name" to cleanName,
                        "description" to description.trim().take(300),
                        "boardClass" to boardClass.trim().ifBlank { "Class 10" }.take(40),
                        "iconKey" to iconKey.ifBlank { "book" }.take(40),
                        "orderIndex" to orderIndex.coerceIn(0L, 1000L),
                        "published" to published,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                Result.success(subjectId)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteSubject(subjectId: String): Result<Unit> {
        val docPath = "subjects/$subjectId"
        return try {
            requireUserId()
            db.collection("subjects").document(subjectId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveChapter(
        chapterId: String?,
        subjectId: String,
        title: String,
        summary: String,
        chapterNumber: Long,
        published: Boolean
    ): Result<String> {
        var docPath = "chapters"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim().take(120)
            require(cleanTitle.isNotEmpty()) { "Chapter title is required." }

            if (chapterId.isNullOrBlank()) {
                val docRef = db.collection("chapters").document()
                docPath = docRef.path
                docRef.set(
                    mapOf(
                        "subjectId" to subjectId,
                        "title" to cleanTitle,
                        "summary" to summary.trim().take(400),
                        "chapterNumber" to chapterNumber.coerceIn(1L, 500L),
                        "published" to published,
                        "createdBy" to uid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                Result.success(docRef.id)
            } else {
                val docRef = db.collection("chapters").document(chapterId)
                docPath = docRef.path
                docRef.update(
                    mapOf(
                        "subjectId" to subjectId,
                        "title" to cleanTitle,
                        "summary" to summary.trim().take(400),
                        "chapterNumber" to chapterNumber.coerceIn(1L, 500L),
                        "published" to published,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                Result.success(chapterId)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteChapter(chapterId: String): Result<Unit> {
        val docPath = "chapters/$chapterId"
        return try {
            requireUserId()
            db.collection("chapters").document(chapterId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveQuiz(
        quizId: String?,
        subjectId: String,
        chapterId: String,
        title: String,
        durationMinutes: Long,
        sourceType: String,
        published: Boolean,
        questions: List<McqQuestion>
    ): Result<String> {
        var docPath = "quizzes"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim().take(120)
            require(cleanTitle.isNotEmpty()) { "Quiz title is required." }
            require(questions.isNotEmpty()) { "Quiz must contain at least 1 question." }

            val questionMaps = questions.take(100).map { q ->
                mapOf(
                    "question" to q.question.trim(),
                    "options" to q.options.take(4),
                    "correctIndex" to q.correctIndex.coerceIn(0L, 3L),
                    "explanation" to q.explanation.trim()
                )
            }
            val safeSource = if (sourceType == "ai_pdf") "ai_pdf" else "manual"

            if (quizId.isNullOrBlank()) {
                val docRef = db.collection("quizzes").document()
                docPath = docRef.path
                docRef.set(
                    mapOf(
                        "subjectId" to subjectId,
                        "chapterId" to chapterId,
                        "title" to cleanTitle,
                        "durationMinutes" to durationMinutes.coerceIn(1L, 180L),
                        "sourceType" to safeSource,
                        "published" to published,
                        "questions" to questionMaps,
                        "createdBy" to uid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                Result.success(docRef.id)
            } else {
                val docRef = db.collection("quizzes").document(quizId)
                docPath = docRef.path
                docRef.update(
                    mapOf(
                        "subjectId" to subjectId,
                        "chapterId" to chapterId,
                        "title" to cleanTitle,
                        "durationMinutes" to durationMinutes.coerceIn(1L, 180L),
                        "sourceType" to safeSource,
                        "published" to published,
                        "questions" to questionMaps,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                Result.success(quizId)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteQuiz(quizId: String): Result<Unit> {
        val docPath = "quizzes/$quizId"
        return try {
            requireUserId()
            db.collection("quizzes").document(quizId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    /**
     * One-tap Bihar Board starter curriculum seed for Admin so subjects, chapters, and objective
     * sets are immediately ready for students.
     */
    suspend fun seedBiharBoardStarterContent(): Result<Unit> {
        return try {
            val uid = requireUserId()
            val existingSubjects = db.collection("subjects").limit(1).get().await()
            if (!existingSubjects.isEmpty) {
                return Result.success(Unit)
            }

            val mathsId = saveSubject(
                null, "Mathematics (गणित)", "BSEB Class 10 Objective Practice Sets",
                "Class 10", "calculate", 1L, true
            ).getOrThrow()
            val scienceId = saveSubject(
                null, "Science (विज्ञान)", "Physics, Chemistry & Biology Chapter MCQs",
                "Class 10", "science", 2L, true
            ).getOrThrow()
            val sstId = saveSubject(
                null, "Social Science (सामाजिक विज्ञान)", "History, Geography, Civics & Economics",
                "Class 10", "public", 3L, true
            ).getOrThrow()

            val chapMath1 = saveChapter(
                null, mathsId, "Ch 1: Real Numbers (वास्तविक संख्याएँ)",
                "Euclid's division lemma, HCF, LCM, rational & irrational numbers", 1L, true
            ).getOrThrow()
            val chapMath8 = saveChapter(
                null, mathsId, "Ch 8: Trigonometry (त्रिकोणमिति)",
                "Trigonometric ratios, standard angles & identities", 2L, true
            ).getOrThrow()
            val chapSci1 = saveChapter(
                null, scienceId, "Ch 1: Light - Reflection & Refraction",
                "Mirror formula, lens power, refractive index", 1L, true
            ).getOrThrow()
            val chapSst1 = saveChapter(
                null, sstId, "Ch 1: Nationalism in Europe & India",
                "Key events, leaders & Bihar movements", 1L, true
            ).getOrThrow()

            saveQuiz(
                null, mathsId, chapMath1, "Real Numbers • BSEB Set 1", 10L, "manual", true,
                listOf(
                    McqQuestion(
                        "दो परिमेय संख्याओं के बीच अधिकतम कितनी परिमेय संख्याएँ हो सकती हैं?",
                        listOf("1", "2", "3", "अनंत (Infinite)"),
                        3L,
                        "दो परिमेय संख्याओं के बीच अनंत (Infinite) परिमेय संख्याएँ होती हैं।"
                    ),
                    McqQuestion(
                        "135 और 225 का म.स. (HCF) यूक्लिड विभाजन एल्गोरिदम से क्या होगा?",
                        listOf("15", "45", "35", "5"),
                        1L,
                        "225 = 135×1 + 90; 135 = 90×1 + 45; 90 = 45×2 + 0, अतः HCF = 45."
                    ),
                    McqQuestion(
                        "निम्नलिखित में कौन अपरिमेय संख्या है?",
                        listOf("√16", "√(9/4)", "√3", "0.25"),
                        2L,
                        "√3 एक अपरिमेय संख्या है क्योंकि 3 पूर्ण वर्ग नहीं है।"
                    ),
                    McqQuestion(
                        "यदि a और b अभाज्य संख्याएँ हैं, तो a और b का ल.स. (LCM) क्या होगा?",
                        listOf("a", "b", "a × b", "a / b"),
                        2L,
                        "दो अभाज्य संख्याओं का LCM हमेशा उनका गुणनफल (a × b) होता है।"
                    ),
                    McqQuestion(
                        "π (पाई) एक कैसी संख्या है?",
                        listOf("परिमेय संख्या", "अपरिमेय संख्या", "पूर्णांक", "प्राकृत संख्या"),
                        1L,
                        "π एक अपरिमेय संख्या है, जबकि 22/7 उसका सन्निकट मान है।"
                    )
                )
            ).getOrThrow()

            saveQuiz(
                null, mathsId, chapMath8, "Trigonometry • Board VVI Set", 10L, "manual", true,
                listOf(
                    McqQuestion(
                        "sin²θ + cos²θ का मान कितना होता है?",
                        listOf("0", "1", "-1", "2"),
                        1L,
                        "त्रिकोणमितीय सर्वसमिका के अनुसार sin²θ + cos²θ = 1 होता है।"
                    ),
                    McqQuestion(
                        "tan 45° का मान क्या होता है?",
                        listOf("0", "1/√3", "1", "√3"),
                        2L,
                        "tan 45° = sin 45° / cos 45° = 1."
                    ),
                    McqQuestion(
                        "sec²θ - tan²θ बराबर होता है:",
                        listOf("0", "1", "2", "-1"),
                        1L,
                        "1 + tan²θ = sec²θ से sec²θ - tan²θ = 1 प्राप्त होता है।"
                    )
                )
            ).getOrThrow()

            saveQuiz(
                null, scienceId, chapSci1, "Light • Objective Practice Set", 10L, "manual", true,
                listOf(
                    McqQuestion(
                        "गोलीय दर्पण की फोकस दूरी (f) और वक्रता त्रिज्या (R) में क्या संबंध है?",
                        listOf("f = R/2", "f = 2R", "f = R", "f = 3R"),
                        0L,
                        "गोलीय दर्पण की फोकस दूरी वक्रता त्रिज्या की आधी (f = R/2) होती है।"
                    ),
                    McqQuestion(
                        "लेंस की क्षमता (Power) का SI मात्रक क्या है?",
                        listOf("मीटर", "डायोप्टर (Dioptre)", "वाट", "न्यूटन"),
                        1L,
                        "लेंस की क्षमता का SI मात्रक डायोप्टर (D = 1/f मीटर में) होता है।"
                    ),
                    McqQuestion(
                        "दाढ़ी बनाने (Shaving) के लिए किस दर्पण का उपयोग होता है?",
                        listOf("उत्तल दर्पण", "अवतल दर्पण", "समतल दर्पण", "इनमें से कोई नहीं"),
                        1L,
                        "अवतल दर्पण बड़ा और सीधा प्रतिबिंब बनाता है, इसलिए इसका उपयोग होता है।"
                    )
                )
            ).getOrThrow()

            saveQuiz(
                null, sstId, chapSst1, "History • Ch 1 Important MCQs", 10L, "manual", true,
                listOf(
                    McqQuestion(
                        "चंपारण सत्याग्रह (बिहार) किस वर्ष हुआ था?",
                        listOf("1915", "1917", "1919", "1922"),
                        1L,
                        "महात्मा गांधी के नेतृत्व में चंपारण सत्याग्रह 1917 ई. में हुआ था।"
                    ),
                    McqQuestion(
                        "यंग इटली (Young Italy) की स्थापना किसने की थी?",
                        listOf("कावूर", "गैरीबाल्डी", "मेजिनी (Mazzini)", "बिस्मार्क"),
                        2L,
                        "जोसेफ मेजिनी ने 1831 ई. में यंग इटली की स्थापना की थी।"
                    )
                )
            ).getOrThrow()

            // Initial welcome announcement
            val annRef = db.collection("announcements").document()
            annRef.set(
                mapOf(
                    "title" to "Welcome to BiharPrep!",
                    "message" to "Bihar Board Class 10 chapter-wise objective sets are now live. Attempt tests and climb the leaderboard!",
                    "targetUserId" to "ALL",
                    "targetUserName" to "All Students",
                    "priority" to "normal",
                    "createdBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "subjects")
            Result.failure(e)
        }
    }

    // --- TEST SUBMISSION, ATTEMPTS & LEADERBOARD ---

    suspend fun submitTestAttempt(
        quiz: QuizSet,
        subjectName: String,
        userName: String,
        selectedAnswers: List<Int>
    ): Result<TestAttempt> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Please sign in to submit test."))
        val attemptRef = db.collection("attempts").document()
        var opPath = attemptRef.path
        var opType = OperationType.CREATE
        return try {
            val total = quiz.questions.size
            var correct = 0
            var wrong = 0
            quiz.questions.forEachIndexed { idx, q ->
                val picked = selectedAnswers.getOrNull(idx) ?: -1
                if (picked == q.correctIndex.toInt()) {
                    correct++
                } else {
                    wrong++
                }
            }
            val pct = if (total > 0) (correct * 100.0) / total else 0.0
            val answerLongs = selectedAnswers.map { it.toLong() }

            val attemptPayload = mapOf(
                "userId" to uid,
                "userName" to userName.trim().ifBlank { "Student" }.take(80),
                "quizId" to quiz.id,
                "chapterId" to quiz.chapterId,
                "subjectId" to quiz.subjectId,
                "quizTitle" to quiz.title.take(120),
                "subjectName" to subjectName.take(80),
                "totalQuestions" to total.toLong(),
                "correctCount" to correct.toLong(),
                "wrongCount" to wrong.toLong(),
                "percentage" to pct,
                "answers" to answerLongs,
                "createdAt" to FieldValue.serverTimestamp()
            )
            attemptRef.set(attemptPayload).await()

            // Recompute student stats cleanly from profile
            val currentUser = auth.currentUser
            if (currentUser != null) {
                ensureUserProfileExists(currentUser).getOrThrow()
            }
            val userRef = db.collection("users").document(uid)
            opPath = userRef.path
            opType = OperationType.GET
            val userSnap = userRef.get().await()
            val prevScore = (userSnap.getLong("totalScore") ?: 0L)
            val prevTests = (userSnap.getLong("testsTaken") ?: 0L)
            val prevAvg = (userSnap.getDouble("averagePercent") ?: 0.0)
            val boardClass = userSnap.getString("boardClass") ?: "Class 10"
            val currentName = userSnap.getString("displayName") ?: userName

            val newTests = prevTests + 1L
            val newScore = prevScore + correct.toLong()
            val newAvg = ((prevAvg * prevTests) + pct) / newTests

            opType = OperationType.UPDATE
            userRef.update(
                mapOf(
                    "totalScore" to newScore,
                    "testsTaken" to newTests,
                    "averagePercent" to newAvg,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val lbRef = db.collection("leaderboard").document(uid)
            opPath = lbRef.path
            lbRef.update(
                mapOf(
                    "displayName" to currentName.take(80),
                    "boardClass" to boardClass,
                    "totalScore" to newScore,
                    "testsTaken" to newTests,
                    "averagePercent" to newAvg,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(
                TestAttempt(
                    id = attemptRef.id,
                    userId = uid,
                    userName = userName,
                    quizId = quiz.id,
                    chapterId = quiz.chapterId,
                    subjectId = quiz.subjectId,
                    quizTitle = quiz.title,
                    subjectName = subjectName,
                    totalQuestions = total.toLong(),
                    correctCount = correct.toLong(),
                    wrongCount = wrong.toLong(),
                    percentage = pct,
                    answers = answerLongs
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, opType, opPath)
            Result.failure(e)
        }
    }

    fun observeMyAttempts(uid: String): Flow<List<TestAttempt>> = flow {
        val path = "attempts"
        emitAll(
            db.collection(path).whereEqualTo("userId", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(TestAttempt::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeLeaderboard(): Flow<List<LeaderboardEntry>> = flow {
        val path = "leaderboard"
        emitAll(
            db.collection(path).whereEqualTo("visibility", "public").limit(100)
                .snapshots()
                .map { snap ->
                    snap.toObjects(LeaderboardEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(
                            compareByDescending<LeaderboardEntry> { it.totalScore }
                                .thenByDescending { it.averagePercent }
                        )
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser != null) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    // --- ADMIN PERFORMANCE TRACKING ---

    fun observeAllUsersAdmin(): Flow<List<UserProfile>> = flow {
        val path = "users"
        emitAll(
            db.collection(path).snapshots()
                .map { snap ->
                    snap.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.totalScore }
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser != null) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeAllAttemptsAdmin(): Flow<List<TestAttempt>> = flow {
        val path = "attempts"
        emitAll(
            db.collection(path).limit(150).snapshots()
                .map { snap ->
                    snap.toObjects(TestAttempt::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser != null) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    // --- DIRECT STUDENT <-> ADMIN CHAT ---

    fun observeChatMessages(studentId: String): Flow<List<ChatMessage>> = flow {
        val path = "chats/$studentId/messages"
        emitAll(
            db.collection("chats").document(studentId).collection("messages")
                .snapshots()
                .map { snap ->
                    snap.toObjects(ChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt?.seconds ?: Long.MAX_VALUE }
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser != null) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeAllChatThreadsAdmin(): Flow<List<ChatThread>> = flow {
        val path = "chats"
        emitAll(
            db.collection(path).snapshots()
                .map { snap ->
                    snap.toObjects(ChatThread::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser != null) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun sendChatMessage(
        studentId: String,
        studentName: String,
        senderName: String,
        asAdmin: Boolean,
        text: String
    ): Result<Unit> {
        val cleanText = text.trim().take(2000)
        if (cleanText.isEmpty()) return Result.failure(IllegalArgumentException("Message cannot be empty."))
        val threadRef = db.collection("chats").document(studentId)
        return try {
            val uid = requireUserId()
            val threadSnap = threadRef.get().await()
            if (!threadSnap.exists()) {
                threadRef.set(
                    mapOf(
                        "studentId" to studentId,
                        "studentName" to studentName.trim().ifBlank { "Student" }.take(80),
                        "lastMessage" to cleanText.take(1000),
                        "unreadByAdmin" to if (asAdmin) 0L else 1L,
                        "unreadByStudent" to if (asAdmin) 1L else 0L,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                threadRef.update(
                    mapOf(
                        "studentName" to studentName.trim().ifBlank { "Student" }.take(80),
                        "lastMessage" to cleanText.take(1000),
                        "unreadByAdmin" to if (asAdmin) 0L else FieldValue.increment(1L),
                        "unreadByStudent" to if (asAdmin) FieldValue.increment(1L) else 0L,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            val msgRef = threadRef.collection("messages").document()
            msgRef.set(
                mapOf(
                    "senderId" to uid,
                    "senderRole" to if (asAdmin) "admin" else "student",
                    "senderName" to senderName.trim().ifBlank { if (asAdmin) "Admin" else "Student" }.take(80),
                    "text" to cleanText,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, threadRef.path)
            Result.failure(e)
        }
    }

    // --- ANNOUNCEMENTS (BROADCAST OR SINGLE STUDENT) ---

    fun observeStudentAnnouncements(uid: String): Flow<List<Announcement>> {
        val broadcastFlow = db.collection("announcements")
            .whereEqualTo("targetUserId", "ALL")
            .snapshots()
            .map { it.toObjects(Announcement::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }

        val personalFlow = db.collection("announcements")
            .whereEqualTo("targetUserId", uid)
            .snapshots()
            .map { it.toObjects(Announcement::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }

        return combine(broadcastFlow, personalFlow) { allList, personalList ->
            (allList + personalList)
                .distinctBy { it.id }
                .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }.catch { error ->
            val fsErr = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>().firstOrNull()
            if (fsErr == null && error is CancellationException) throw error
            if (auth.currentUser?.uid == uid) {
                handleFirestoreError(fsErr ?: error, OperationType.LIST, "announcements")
            }
            throw error
        }
    }

    fun observeAllAnnouncementsAdmin(): Flow<List<Announcement>> = flow {
        val path = "announcements"
        emitAll(
            db.collection(path).snapshots()
                .map { snap ->
                    snap.toObjects(Announcement::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser != null) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun sendAnnouncement(
        title: String,
        message: String,
        targetUserId: String,
        targetUserName: String,
        priority: String
    ): Result<Unit> {
        val docRef = db.collection("announcements").document()
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim().take(120)
            val cleanMsg = message.trim().take(2000)
            require(cleanTitle.isNotEmpty() && cleanMsg.isNotEmpty()) {
                "Announcement title and message are required."
            }
            docRef.set(
                mapOf(
                    "title" to cleanTitle,
                    "message" to cleanMsg,
                    "targetUserId" to targetUserId.ifBlank { "ALL" }.take(128),
                    "targetUserName" to targetUserName.ifBlank { "All Students" }.take(80),
                    "priority" to if (priority == "urgent") "urgent" else "normal",
                    "createdBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteAnnouncement(announcementId: String): Result<Unit> {
        val docPath = "announcements/$announcementId"
        return try {
            requireUserId()
            db.collection("announcements").document(announcementId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- STUDY GROUPS (CREATE, JOIN, LEAVE & GROUP CHAT) ---

    fun observeStudyGroups(uid: String): Flow<List<StudyGroup>> {
        val publicFlow = db.collection("study_groups")
            .whereEqualTo("isPublic", true)
            .snapshots()
            .map { it.toObjects(StudyGroup::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }

        val mineFlow = db.collection("study_groups")
            .whereArrayContains("memberIds", uid)
            .snapshots()
            .map { it.toObjects(StudyGroup::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }

        return combine(publicFlow, mineFlow) { pub, mine ->
            (mine + pub)
                .distinctBy { it.id }
                .sortedByDescending { it.updatedAt?.seconds ?: 0L }
        }.catch { error ->
            val fsErr = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>().firstOrNull()
            if (fsErr == null && error is CancellationException) throw error
            if (auth.currentUser?.uid == uid) {
                handleFirestoreError(fsErr ?: error, OperationType.LIST, "study_groups")
            }
            throw error
        }
    }

    fun observeAllStudyGroupsAdmin(): Flow<List<StudyGroup>> = flow {
        val path = "study_groups"
        emitAll(
            db.collection(path).snapshots()
                .map { snap ->
                    snap.toObjects(StudyGroup::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser != null) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createStudyGroup(
        name: String,
        description: String,
        boardClass: String,
        subjectTag: String,
        creatorName: String
    ): Result<String> {
        val docRef = db.collection("study_groups").document()
        return try {
            val uid = requireUserId()
            val cleanName = name.trim().take(80)
            require(cleanName.length >= 2) { "Group name must be at least 2 characters." }
            val codeChars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
            val inviteCode = buildString {
                repeat(6) {
                    append(codeChars[(docRef.id.hashCode().ushr(it * 4) + it * 7) and 0x1F])
                }
            }
            docRef.set(
                mapOf(
                    "name" to cleanName,
                    "description" to description.trim().take(300),
                    "boardClass" to boardClass.trim().ifBlank { "Class 10" }.take(40),
                    "subjectTag" to subjectTag.trim().ifBlank { "General" }.take(40),
                    "inviteCode" to inviteCode,
                    "isPublic" to true,
                    "createdBy" to uid,
                    "creatorName" to creatorName.trim().ifBlank { "Bihar Student" }.take(80),
                    "memberIds" to listOf(uid),
                    "memberCount" to 1L,
                    "lastMessage" to "Group created! Say hello 👋",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun joinStudyGroup(group: StudyGroup): Result<Unit> {
        val docPath = "study_groups/${group.id}"
        return try {
            val uid = requireUserId()
            if (uid in group.memberIds) return Result.success(Unit)
            val updatedMembers = (group.memberIds + uid).distinct()
            db.collection("study_groups").document(group.id).update(
                mapOf(
                    "memberIds" to updatedMembers,
                    "memberCount" to updatedMembers.size.toLong(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun leaveStudyGroup(group: StudyGroup): Result<Unit> {
        val docPath = "study_groups/${group.id}"
        return try {
            val uid = requireUserId()
            val remaining = group.memberIds.filter { it != uid }
            if (remaining.isEmpty()) {
                db.collection("study_groups").document(group.id).delete().await()
            } else {
                db.collection("study_groups").document(group.id).update(
                    mapOf(
                        "memberIds" to remaining,
                        "memberCount" to remaining.size.toLong(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteStudyGroup(groupId: String): Result<Unit> {
        val docPath = "study_groups/$groupId"
        return try {
            requireUserId()
            db.collection("study_groups").document(groupId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    fun observeGroupMessages(groupId: String): Flow<List<GroupMessage>> = flow {
        val path = "study_groups/$groupId/messages"
        emitAll(
            db.collection("study_groups").document(groupId).collection("messages")
                .snapshots()
                .map { snap ->
                    snap.toObjects(GroupMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt?.seconds ?: Long.MAX_VALUE }
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser != null) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun sendGroupMessage(groupId: String, senderName: String, text: String): Result<Unit> {
        val cleanText = text.trim().take(2000)
        if (cleanText.isEmpty()) return Result.failure(IllegalArgumentException("Message cannot be empty."))
        val groupRef = db.collection("study_groups").document(groupId)
        var opPath = "$groupRef/messages"
        var opType = OperationType.CREATE
        return try {
            val uid = requireUserId()
            val msgRef = groupRef.collection("messages").document()
            opPath = msgRef.path
            msgRef.set(
                mapOf(
                    "senderId" to uid,
                    "senderName" to senderName.trim().ifBlank { "Student" }.take(80),
                    "text" to cleanText,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()

            opPath = groupRef.path
            opType = OperationType.UPDATE
            groupRef.update(
                mapOf(
                    "lastMessage" to "${senderName.substringBefore(" ")}: ${cleanText.take(800)}",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, opType, opPath)
            Result.failure(e)
        }
    }
}

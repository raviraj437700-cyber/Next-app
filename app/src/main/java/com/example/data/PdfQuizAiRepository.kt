package com.example.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val AI_TAG = "PdfQuizAiRepo"
private const val MAX_PDF_BYTES = 18 * 1024 * 1024 // Stay safely below 20MB inline limit

data class GeneratedQuizDraft(
    val quizTitle: String,
    val questions: List<McqQuestion>
)

class PdfQuizAiRepository {

    private val quizSchema = Schema.obj(
        properties = mapOf(
            "quizTitle" to Schema.string(
                description = "Short descriptive title for the Bihar Board chapter quiz"
            ),
            "questions" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "question" to Schema.string(
                            description = "Clear board-exam level objective MCQ question"
                        ),
                        "options" to Schema.array(
                            items = Schema.string(),
                            description = "Exactly 4 answer choices"
                        ),
                        "correctIndex" to Schema.integer(
                            description = "0-based index (0, 1, 2, or 3) of the single correct option"
                        ),
                        "explanation" to Schema.string(
                            description = "Concise 1-2 line explanation of why the answer is correct"
                        )
                    )
                ),
                description = "List of generated multiple-choice questions"
            )
        )
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = quizSchema
                temperature = 0.3f
            }
        )
    }

    suspend fun generateQuizFromPdf(
        context: Context,
        pdfUri: Uri,
        subjectName: String,
        chapterTitle: String,
        questionCount: Int,
        languageMode: String
    ): Result<GeneratedQuizDraft> {
        return try {
            val pdfBytes = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(pdfUri)?.use { stream ->
                    stream.readBytes()
                } ?: throw IllegalStateException("Unable to read the selected PDF file.")
            }

            if (pdfBytes.isEmpty()) {
                return Result.failure(IllegalArgumentException("The selected PDF file is empty."))
            }
            if (pdfBytes.size > MAX_PDF_BYTES) {
                return Result.failure(
                    IllegalArgumentException("PDF exceeds 18 MB limit. Please upload a smaller chapter PDF.")
                )
            }

            val promptText = """
                You are an expert Bihar School Examination Board (BSEB) question setter.
                Analyze the attached chapter PDF carefully and generate $questionCount high-quality,
                exam-oriented objective MCQ questions for Subject: "$subjectName", Chapter: "$chapterTitle".
                Language preference: $languageMode.
                Rules:
                1. Every question must be directly grounded in the PDF concepts, formulas, dates, or definitions.
                2. Provide exactly 4 distinct options per question.
                3. Set correctIndex strictly between 0 and 3.
                4. Provide a helpful short explanation for students reviewing wrong answers.
            """.trimIndent()

            val response = withTimeout(60_000L) {
                model.generateContent(
                    content {
                        inlineData(pdfBytes, "application/pdf")
                        text(promptText)
                    }
                )
            }

            val jsonString = response.text
                ?: return Result.failure(IllegalStateException("AI returned an empty response."))

            val root = JSONObject(jsonString)
            val title = root.optString("quizTitle", "$chapterTitle - AI Practice Set").trim()
                .ifBlank { "$chapterTitle - AI Practice Set" }
            val jsonQuestions = root.getJSONArray("questions")
            val parsedQuestions = buildList {
                for (i in 0 until jsonQuestions.length()) {
                    val obj = jsonQuestions.getJSONObject(i)
                    val qText = obj.optString("question", "").trim()
                    val optsJson = obj.optJSONArray("options")
                    val options = mutableListOf<String>()
                    if (optsJson != null) {
                        for (j in 0 until optsJson.length()) {
                            options.add(optsJson.optString(j, "").trim())
                        }
                    }
                    while (options.size < 4) options.add("Option ${options.size + 1}")
                    val trimmedOptions = options.take(4)
                    val correctIdx = obj.optInt("correctIndex", 0).coerceIn(0, 3).toLong()
                    val explanation = obj.optString("explanation", "").trim()
                    if (qText.isNotBlank()) {
                        add(
                            McqQuestion(
                                question = qText,
                                options = trimmedOptions,
                                correctIndex = correctIdx,
                                explanation = explanation
                            )
                        )
                    }
                }
            }

            if (parsedQuestions.isEmpty()) {
                Result.failure(IllegalStateException("Could not extract valid MCQs from this PDF."))
            } else {
                Result.success(GeneratedQuizDraft(quizTitle = title, questions = parsedQuestions))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(AI_TAG, "PDF AI quiz generation timed out", e)
            Result.failure(IllegalStateException("PDF analysis timed out after 60 seconds. Try fewer pages or fewer questions."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(AI_TAG, "PDF AI quiz generation failed", e)
            Result.failure(e)
        }
    }
}

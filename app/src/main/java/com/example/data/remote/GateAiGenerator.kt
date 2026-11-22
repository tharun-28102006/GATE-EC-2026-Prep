package com.example.data.remote

import android.util.Log
import com.example.data.local.CachedQuestionEntity
import com.example.domain.GateEcSyllabus
import com.example.domain.GateSubtopic
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.UUID

private const val TAG = "GateAiGenerator"

class GateAiGenerator {

    private val singleQuestionSchema = Schema.obj(
        properties = mapOf(
            "questionText" to Schema.string(description = "Authentic GATE 2026 EC multiple-choice question statement with clear numerical values and units"),
            "options" to Schema.array(
                items = Schema.string(),
                description = "Exactly 4 distinct multiple-choice answer options (A, B, C, D) without letter prefixes"
            ),
            "correctIndex" to Schema.integer(description = "Zero-based index (0, 1, 2, or 3) of the single correct option"),
            "marks" to Schema.integer(description = "Official GATE marks weight: either 1 or 2"),
            "difficulty" to Schema.enumeration(
                values = listOf("Easy", "Medium", "Hard"),
                description = "Difficulty level matching GATE EC exam standards"
            ),
            "keyFormula" to Schema.string(description = "Concise core equation, theorem, or law in plain Unicode math notation"),
            "explanation" to Schema.string(description = "Detailed step-by-step GATE solution derivation clearly justifying the correct option and noting any common trap")
        )
    )

    private val batchSchema = Schema.obj(
        properties = mapOf(
            "questions" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "subtopicId" to Schema.string(description = "Exact subtopicId provided in the prompt"),
                        "questionText" to Schema.string(description = "Authentic GATE 2026 EC question text"),
                        "options" to Schema.array(
                            items = Schema.string(),
                            description = "Exactly 4 distinct options without A/B/C/D prefixes"
                        ),
                        "correctIndex" to Schema.integer(description = "Zero-based index 0-3 of correct option"),
                        "marks" to Schema.integer(description = "1 or 2"),
                        "difficulty" to Schema.enumeration(values = listOf("Easy", "Medium", "Hard")),
                        "keyFormula" to Schema.string(description = "Concise key formula in Unicode math notation"),
                        "explanation" to Schema.string(description = "Detailed step-by-step explanation")
                    )
                ),
                description = "List of generated GATE 2026 EC questions, one per requested subtopicId"
            )
        )
    )

    private val singleModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = singleQuestionSchema
                temperature = 0.7f
            }
        )
    }

    private val batchModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = batchSchema
                temperature = 0.75f
            }
        )
    }

    suspend fun generateQuestionForSubtopic(
        subtopic: GateSubtopic,
        recentStems: List<String> = emptyList()
    ): Result<CachedQuestionEntity> {
        val section = GateEcSyllabus.sectionById(subtopic.sectionId)
        val avoidClause = if (recentStems.isNotEmpty()) {
            "\nAvoid repeating concepts or exact numbers from these recent questions:\n" +
                recentStems.take(5).joinToString("\n") { "- ${it.take(90)}" }
        } else {
            ""
        }

        val prompt = """
            You are an official IIT professor authoring a fresh, original GATE 2026 Electronics and Communication Engineering (EC) exam question.
            Section: ${section.name}
            Subtopic: ${subtopic.name}
            Syllabus Scope: ${subtopic.keyConcepts}
            $avoidClause

            Requirements:
            1. Write ONE crisp, mathematically verified GATE 2026 EC MCQ solvable cleanly on mobile without external figures.
            2. Provide exactly 4 distinct plausible options (no "A)" prefixes).
            3. Verify every calculation carefully so correctIndex matches the exact mathematically correct option.
            4. Use clean plain Unicode math notation (e.g., Ω, μ, π, √, ², ³, λ, ∠, ω) — do NOT use LaTeX backslashes like \frac or \omega.
            5. Include a clear step-by-step explanation and a concise keyFormula.
        """.trimIndent()

        return try {
            val response = withTimeout(60_000L) {
                singleModel.generateContent(prompt)
            }
            val jsonStr = response.text
                ?: return Result.failure(IllegalStateException("AI returned an empty response."))

            val parsed = parseSingleQuestion(jsonStr, subtopic)
            Result.success(parsed)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Question generation timed out for ${subtopic.id}", e)
            Result.failure(Exception("AI question generation timed out. Check connection or retry.", e))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Question generation failed for ${subtopic.id}", e)
            Result.failure(e)
        }
    }

    suspend fun generateBatchForSubtopics(
        subtopics: List<GateSubtopic>
    ): Result<List<CachedQuestionEntity>> {
        if (subtopics.isEmpty()) return Result.success(emptyList())

        val topicSpec = subtopics.joinToString("\n") { sub ->
            val sec = GateEcSyllabus.sectionById(sub.sectionId)
            "- subtopicId=\"${sub.id}\" | Section: ${sec.name} | Subtopic: ${sub.name} | Scope: ${sub.keyConcepts}"
        }

        val prompt = """
            You are an IIT professor authoring original GATE 2026 Electronics and Communication Engineering (EC) MCQ questions for an offline study cache.
            Generate EXACTLY ONE original, mathematically verified question for EACH subtopic listed below (total ${subtopics.size} questions):

            $topicSpec

            Rules:
            - Copy the exact subtopicId string into each JSON item.
            - Provide exactly 4 distinct options per question (no "A)" prefix).
            - Double-check all calculations so correctIndex (0..3) is 100% accurate.
            - Use clean Unicode math symbols (Ω, μ, π, √, ², λ, ω) — NO LaTeX backslashes.
            - Include a concise keyFormula and clear step-by-step explanation for every question.
        """.trimIndent()

        return try {
            val response = withTimeout(60_000L) {
                batchModel.generateContent(prompt)
            }
            val jsonStr = response.text
                ?: return Result.failure(IllegalStateException("AI returned an empty batch response."))

            val root = JSONObject(jsonStr)
            val arr = root.getJSONArray("questions")
            val subtopicMap = subtopics.associateBy { it.id }
            val result = mutableListOf<CachedQuestionEntity>()

            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val subId = obj.optString("subtopicId", "")
                val sub = subtopicMap[subId] ?: subtopics.getOrNull(i) ?: continue
                runCatching { parseQuestionObject(obj, sub) }.getOrNull()?.let { result.add(it) }
            }

            if (result.isEmpty()) {
                Result.failure(IllegalStateException("No valid GATE questions parsed from AI batch."))
            } else {
                Result.success(result)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Batch generation timed out", e)
            Result.failure(Exception("Offline AI pool refill timed out. Please try again.", e))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Batch generation failed", e)
            Result.failure(e)
        }
    }

    private fun parseSingleQuestion(jsonStr: String, subtopic: GateSubtopic): CachedQuestionEntity {
        val obj = JSONObject(jsonStr)
        return parseQuestionObject(obj, subtopic)
    }

    private fun parseQuestionObject(obj: JSONObject, subtopic: GateSubtopic): CachedQuestionEntity {
        val questionText = obj.getString("questionText").trim().take(2900)
        val optionsArr = obj.getJSONArray("options")
        require(optionsArr.length() == 4) { "Expected exactly 4 options" }
        val options = (0 until 4).map { idx ->
            optionsArr.getString(idx)
                .trim()
                .replace(Regex("^[A-Da-d][).:]\\s*"), "")
                .ifBlank { "Option ${'A' + idx}" }
        }
        val correctIndex = obj.getInt("correctIndex").coerceIn(0, 3)
        val marks = if (obj.optInt("marks", 1) == 2) 2 else 1
        val negativeMarks = if (marks == 2) 0.67 else 0.33
        val rawDiff = obj.optString("difficulty", "Medium")
        val difficulty = if (rawDiff in listOf("Easy", "Medium", "Hard")) rawDiff else "Medium"
        val keyFormula = obj.optString("keyFormula", "").trim().take(750)
        val explanation = obj.getString("explanation").trim().take(5900)

        val uniqueSuffix = UUID.randomUUID().toString().replace("-", "").take(12)
        val sanitizedId = "ai_${subtopic.id.take(24)}_$uniqueSuffix"

        return CachedQuestionEntity(
            id = sanitizedId,
            sectionId = subtopic.sectionId,
            subtopicId = subtopic.id,
            questionText = questionText,
            optionA = options[0],
            optionB = options[1],
            optionC = options[2],
            optionD = options[3],
            correctIndex = correctIndex,
            marks = marks,
            negativeMarks = negativeMarks,
            difficulty = difficulty,
            explanation = explanation,
            keyFormula = keyFormula,
            source = "AI",
            savedOffline = false,
            attempted = false,
            createdAtMs = System.currentTimeMillis()
        )
    }
}

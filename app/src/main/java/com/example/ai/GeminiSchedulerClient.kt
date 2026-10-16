package com.example.ai

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.TaskEntity
import com.example.data.model.TaskPriority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class OptimizedTimeBlock(
    val taskId: Long,
    val taskTitle: String,
    val startTime: String, // "HH:mm"
    val endTime: String,   // "HH:mm"
    val priority: TaskPriority,
    val rationale: String,
    val suggestedTimestamp: Long = 0L
)

data class ScheduleOptimizationResult(
    val summary: String,
    val timeBlocks: List<OptimizedTimeBlock>,
    val tips: List<String>,
    val isAiGenerated: Boolean = true
)

class GeminiSchedulerClient {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    suspend fun optimizeSchedule(tasks: List<TaskEntity>): Result<ScheduleOptimizationResult> =
        withContext(Dispatchers.IO) {
            if (tasks.isEmpty()) {
                return@withContext Result.success(
                    ScheduleOptimizationResult(
                        summary = "No tasks found for today. Create some tasks or routines to optimize your day!",
                        timeBlocks = emptyList(),
                        tips = listOf("Add daily routines to automate your habits.", "Prioritize high-impact tasks early in the day."),
                        isAiGenerated = false
                    )
                )
            }

            val apiKey = try {
                BuildConfig.GEMINI_API_KEY
            } catch (_: Exception) {
                ""
            }

            if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                Log.w("GeminiSchedulerClient", "API key not configured. Using smart algorithmic optimizer.")
                return@withContext Result.success(generateAlgorithmicOptimization(tasks, "Smart Local Optimizer (Offline Mode)"))
            }

            try {
                val tasksJson = JSONArray().apply {
                    tasks.forEach { task ->
                        put(JSONObject().apply {
                            put("id", task.id)
                            put("title", task.title)
                            put("priority", task.priority.name)
                            put("notes", task.notes ?: "")
                            put("isCompleted", task.isCompleted)
                            put("currentScheduledTime", if (task.scheduledTime > 0) timeFormat.format(Date(task.scheduledTime)) else "Unscheduled")
                        })
                    }
                }

                val systemPrompt = "Given this list of tasks [tasks], suggest an optimized, realistic time-blocked schedule for today accounting for task priority. Return valid JSON only with structure: {\"summary\": \"...\", \"schedule\": [{\"taskId\": number, \"startTime\": \"HH:mm\", \"endTime\": \"HH:mm\", \"rationale\": \"...\"}], \"tips\": [\"...\"]}. Focus HIGH priority tasks during morning peak energy, MEDIUM in early afternoon, and LOW later."

                val requestPayload = JSONObject().apply {
                    val contentsArray = JSONArray().apply {
                        put(JSONObject().apply {
                            val partsArray = JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", "Tasks: $tasksJson\n\nPrompt: $systemPrompt")
                                })
                            }
                            put("parts", partsArray)
                        })
                    }
                    put("contents", contentsArray)

                    put("generationConfig", JSONObject().apply {
                        put("temperature", 0.3)
                        put("responseMimeType", "application/json")
                    })
                }

                // Use gemini-3.5-flash as per skill mandates for basic text tasks
                val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
                val requestBody = requestPayload.toString().toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()

                val response = okHttpClient.newCall(request).execute()
                val responseBody = response.body?.string()

                if (!response.isSuccessful || responseBody.isNullOrBlank()) {
                    Log.e("GeminiSchedulerClient", "Gemini API error: ${response.code} $responseBody")
                    return@withContext Result.success(
                        generateAlgorithmicOptimization(tasks, "AI request fallback (Smart Algorithmic Scheduler)")
                    )
                }

                val parsed = parseGeminiResponse(responseBody, tasks)
                Result.success(parsed)
            } catch (e: Exception) {
                Log.e("GeminiSchedulerClient", "Error calling Gemini API", e)
                Result.success(generateAlgorithmicOptimization(tasks, "AI unavailable (Local Schedule Optimizer applied)"))
            }
        }

    private fun parseGeminiResponse(responseJson: String, originalTasks: List<TaskEntity>): ScheduleOptimizationResult {
        val root = JSONObject(responseJson)
        val candidates = root.optJSONArray("candidates")
        val firstCandidate = candidates?.optJSONObject(0)
        val content = firstCandidate?.optJSONObject("content")
        val parts = content?.optJSONArray("parts")
        val rawText = parts?.optJSONObject(0)?.optString("text") ?: ""

        val taskMap = originalTasks.associateBy { it.id }

        try {
            val cleanJson = if (rawText.startsWith("```json")) {
                rawText.removePrefix("```json").substringBeforeLast("```").trim()
            } else if (rawText.startsWith("```")) {
                rawText.removePrefix("```").substringBeforeLast("```").trim()
            } else {
                rawText.trim()
            }

            val jsonObject = JSONObject(cleanJson)
            val summary = jsonObject.optString("summary", "Optimized daily timetable tailored for focus and energy alignment.")
            val scheduleArray = jsonObject.optJSONArray("schedule") ?: JSONArray()
            val tipsArray = jsonObject.optJSONArray("tips") ?: JSONArray()

            val tips = mutableListOf<String>()
            for (i in 0 until tipsArray.length()) {
                tips.add(tipsArray.optString(i))
            }
            if (tips.isEmpty()) {
                tips.add("Tackle high-focus work before lunch when cognitive reserves peak.")
                tips.add("Take a 5-minute stretch break between consecutive time blocks.")
            }

            val blocks = mutableListOf<OptimizedTimeBlock>()
            for (i in 0 until scheduleArray.length()) {
                val blockObj = scheduleArray.optJSONObject(i) ?: continue
                val taskId = blockObj.optLong("taskId")
                val task = taskMap[taskId]
                val title = task?.title ?: "Task #$taskId"
                val priority = task?.priority ?: TaskPriority.MEDIUM
                val startTime = blockObj.optString("startTime", "09:00")
                val endTime = blockObj.optString("endTime", "10:00")
                val rationale = blockObj.optString("rationale", "Scheduled during your optimal productivity window.")

                val timestamp = calculateTimestampForTime(startTime)

                blocks.add(
                    OptimizedTimeBlock(
                        taskId = taskId,
                        taskTitle = title,
                        startTime = startTime,
                        endTime = endTime,
                        priority = priority,
                        rationale = rationale,
                        suggestedTimestamp = timestamp
                    )
                )
            }

            // Include any task that was not in the AI output
            val scheduledTaskIds = blocks.map { it.taskId }.toSet()
            val remainingTasks = originalTasks.filter { it.id !in scheduledTaskIds }
            var currentHour = 16
            for (task in remainingTasks) {
                val startTime = String.format(Locale.getDefault(), "%02d:00", currentHour)
                val endTime = String.format(Locale.getDefault(), "%02d:45", currentHour)
                blocks.add(
                    OptimizedTimeBlock(
                        taskId = task.id,
                        taskTitle = task.title,
                        startTime = startTime,
                        endTime = endTime,
                        priority = task.priority,
                        rationale = "Scheduled in remaining afternoon time block.",
                        suggestedTimestamp = calculateTimestampForTime(startTime)
                    )
                )
                currentHour += 1
            }

            return ScheduleOptimizationResult(
                summary = summary,
                timeBlocks = blocks.sortedBy { it.startTime },
                tips = tips,
                isAiGenerated = true
            )
        } catch (e: Exception) {
            Log.e("GeminiSchedulerClient", "Failed to parse AI JSON, falling back to algorithmic schedule", e)
            return generateAlgorithmicOptimization(originalTasks, "Gemini parsing fallback")
        }
    }

    private fun generateAlgorithmicOptimization(tasks: List<TaskEntity>, reason: String): ScheduleOptimizationResult {
        // High priority first, then Medium, then Low
        val sorted = tasks.sortedWith(
            compareBy<TaskEntity> {
                when (it.priority) {
                    TaskPriority.HIGH -> 0
                    TaskPriority.MEDIUM -> 1
                    TaskPriority.LOW -> 2
                }
            }.thenBy { it.id }
        )

        var startHour = 8
        var startMin = 30

        val blocks = mutableListOf<OptimizedTimeBlock>()

        for (task in sorted) {
            val durationMin = when (task.priority) {
                TaskPriority.HIGH -> 60
                TaskPriority.MEDIUM -> 45
                TaskPriority.LOW -> 30
            }

            val startTimeStr = String.format(Locale.getDefault(), "%02d:%02d", startHour, startMin)

            // Add duration
            var endHour = startHour
            var endMin = startMin + durationMin
            while (endMin >= 60) {
                endHour += 1
                endMin -= 60
            }
            val endTimeStr = String.format(Locale.getDefault(), "%02d:%02d", endHour, endMin)

            val rationale = when (task.priority) {
                TaskPriority.HIGH -> "Allocated early morning high-focus slot for maximum cognitive capacity."
                TaskPriority.MEDIUM -> "Structured in the core working session with steady energy."
                TaskPriority.LOW -> "Placed in flexible afternoon time block to avoid mental fatigue."
            }

            blocks.add(
                OptimizedTimeBlock(
                    taskId = task.id,
                    taskTitle = task.title,
                    startTime = startTimeStr,
                    endTime = endTimeStr,
                    priority = task.priority,
                    rationale = rationale,
                    suggestedTimestamp = calculateTimestampForTime(startTimeStr)
                )
            )

            // Add 15m buffer
            startMin = endMin + 15
            while (startMin >= 60) {
                startHour += 1
                startMin -= 60
            }
            if (startHour >= 21) {
                startHour = 21
                startMin = 0
            }
        }

        return ScheduleOptimizationResult(
            summary = "Your day is structured with high-impact priorities front-loaded into morning deep-work blocks, followed by steady routine maintenance.",
            timeBlocks = blocks,
            tips = listOf(
                "Protect your 8:30 AM - 11:00 AM window from notifications and distractions.",
                "Take intentional 10-minute movement breaks between work blocks.",
                "Review tasks at the start of each block to prime focus."
            ),
            isAiGenerated = false
        )
    }

    private fun calculateTimestampForTime(timeStr: String): Long {
        val calendar = Calendar.getInstance()
        try {
            val parts = timeStr.split(":")
            if (parts.size >= 2) {
                calendar.set(Calendar.HOUR_OF_DAY, parts[0].toIntOrNull() ?: 8)
                calendar.set(Calendar.MINUTE, parts[1].toIntOrNull() ?: 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
            }
        } catch (_: Exception) {}
        return calendar.timeInMillis
    }
}

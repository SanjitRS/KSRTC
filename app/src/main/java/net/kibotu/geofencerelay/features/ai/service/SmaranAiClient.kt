package net.kibotu.geofencerelay.features.ai.service

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class SmaranAiDifficultyResponse(
    val recommendedLevel: String, // "Easy", "Medium", "Hard"
    val patientMessage: String,
    val caregiverSummary: String
)

data class SmaranAiSessionAnalysisResponse(
    val recommendedLevel: String,
    val patientMessage: String,
    val caregiverSummary: String,
    val cpsScore: Double,
    val memoryRetentionIndex: Double,
    val reactionLatencyScore: Double,
    val executiveFunctionIndex: Double,
    val anomalyDetected: Boolean,
    val anomalyMessage: String,
    val riskLevel: String
)

object SmaranAiClient {

    private const val TAG = "SmaranAiClient"
    private const val DEFAULT_BASE_URL = "http://10.0.2.2:8000"
    private const val TIMEOUT_MS = 3000

    fun getBaseUrl(context: Context): String {
        val prefs = context.getSharedPreferences("ai_backend_prefs", Context.MODE_PRIVATE)
        return prefs.getString("backend_url", DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
    }

    fun setBaseUrl(context: Context, url: String) {
        val prefs = context.getSharedPreferences("ai_backend_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("backend_url", url.trimEnd('/')).apply()
    }

    suspend fun predictDifficulty(
        context: Context,
        gameType: String,
        currentDifficulty: String,
        accuracy: Double,
        completionRate: Double,
        responseTimeMs: Long,
        errors: Int,
        hintsUsed: Int
    ): SmaranAiDifficultyResponse? = withContext(Dispatchers.IO) {
        val baseUrl = getBaseUrl(context)
        val endpoint = "$baseUrl/predict-difficulty"

        try {
            val jsonPayload = JSONObject().apply {
                put("game_type", if (gameType.contains("pattern", ignoreCase = true)) "pattern_recognition" else "memory_matching")
                put("current_difficulty", currentDifficulty.lowercase())
                put("accuracy", accuracy.coerceIn(0.0, 1.0))
                put("completion_rate", completionRate.coerceIn(0.0, 1.0))
                put("response_time_ms", responseTimeMs.coerceAtLeast(100L))
                put("errors", errors.coerceAtLeast(0))
                put("hints_used", hintsUsed.coerceAtLeast(0))
            }

            val url = URL(endpoint)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                doInput = true
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
            }

            OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
                writer.write(jsonPayload.toString())
                writer.flush()
            }

            val responseCode = conn.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8"))
                val responseText = reader.use { it.readText() }
                val respJson = JSONObject(responseText)

                val recLevel = respJson.optString("recommended_level", "Medium")
                val patMsg = respJson.optString("patient_message", "")
                val cgSummary = respJson.optString("caregiver_summary", "")

                Log.i(TAG, "[+] SMARAN AI Backend Difficulty Response: $recLevel | $patMsg")
                return@withContext SmaranAiDifficultyResponse(
                    recommendedLevel = recLevel,
                    patientMessage = patMsg,
                    caregiverSummary = cgSummary
                )
            } else {
                Log.w(TAG, "[-] SMARAN AI API returned HTTP $responseCode")
                return@withContext null
            }
        } catch (e: Exception) {
            Log.d(TAG, "[-] SMARAN AI backend unreachable (offline mode active): ${e.message}")
            return@withContext null
        }
    }

    suspend fun analyzeSession(
        context: Context,
        gameType: String,
        currentDifficulty: String,
        accuracy: Double,
        completionRate: Double,
        responseTimeMs: Long,
        errors: Int,
        hintsUsed: Int
    ): SmaranAiSessionAnalysisResponse? = withContext(Dispatchers.IO) {
        val baseUrl = getBaseUrl(context)
        val endpoint = "$baseUrl/analyze-session"

        try {
            val jsonPayload = JSONObject().apply {
                put("game_type", if (gameType.contains("pattern", ignoreCase = true)) "pattern_recognition" else "memory_matching")
                put("current_difficulty", currentDifficulty.lowercase())
                put("accuracy", accuracy.coerceIn(0.0, 1.0))
                put("completion_rate", completionRate.coerceIn(0.0, 1.0))
                put("response_time_ms", responseTimeMs.coerceAtLeast(100L))
                put("errors", errors.coerceAtLeast(0))
                put("hints_used", hintsUsed.coerceAtLeast(0))
            }

            val url = URL(endpoint)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                doInput = true
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
            }

            OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
                writer.write(jsonPayload.toString())
                writer.flush()
            }

            val responseCode = conn.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8"))
                val responseText = reader.use { it.readText() }
                val respJson = JSONObject(responseText)

                val recLevel = respJson.optString("recommended_level", "Medium")
                val patMsg = respJson.optString("patient_message", "")
                val cgSummary = respJson.optString("caregiver_summary", "")
                val cpsScore = respJson.optDouble("cps_score", 70.0)

                val subScores = respJson.optJSONObject("cognitive_sub_scores")
                val memIdx = subScores?.optDouble("memory_retention_index", 75.0) ?: 75.0
                val speedIdx = subScores?.optDouble("reaction_latency_score", 75.0) ?: 75.0
                val execIdx = subScores?.optDouble("executive_function_index", 75.0) ?: 75.0

                val anomalyObj = respJson.optJSONObject("anomaly_alert")
                val anomalyDetected = anomalyObj?.optBoolean("detected", false) ?: false
                val alertMsg = anomalyObj?.optString("alert_message", "Normal bounds") ?: "Normal bounds"
                val riskLvl = anomalyObj?.optString("risk_level", "Low") ?: "Low"

                return@withContext SmaranAiSessionAnalysisResponse(
                    recommendedLevel = recLevel,
                    patientMessage = patMsg,
                    caregiverSummary = cgSummary,
                    cpsScore = cpsScore,
                    memoryRetentionIndex = memIdx,
                    reactionLatencyScore = speedIdx,
                    executiveFunctionIndex = execIdx,
                    anomalyDetected = anomalyDetected,
                    anomalyMessage = alertMsg,
                    riskLevel = riskLvl
                )
            } else {
                return@withContext null
            }
        } catch (e: Exception) {
            Log.d(TAG, "[-] SMARAN AI backend unreachable for analyze-session: ${e.message}")
            return@withContext null
        }
    }
}
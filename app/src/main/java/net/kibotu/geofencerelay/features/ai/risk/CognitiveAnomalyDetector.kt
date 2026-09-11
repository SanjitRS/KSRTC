package net.kibotu.geofencerelay.features.ai.risk

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class AnomalyAlert(
    val type: String, // "ACUTE_ACCURACY_DROP", "LATENCY_SURGE", "ERROR_SPIKE"
    val severity: String, // "HIGH", "MEDIUM"
    val message: String
)

data class AnomalyReport(
    val anomalyDetected: Boolean,
    val riskLevel: String, // "High", "Moderate", "Normal"
    val alerts: List<AnomalyAlert>,
    val caregiverActionRequired: Boolean
)

data class SessionBaselineItem(
    val accuracy: Double,
    val responseTimeMs: Long,
    val errors: Int
)

/**
 * Replicates `anomaly_detector.py` from SMARAN-AI-Backend:
 * Detects acute cognitive drops (delirium onset, extreme fatigue, or sudden impairment)
 * by benchmarking live session telemetry against rolling historical baselines.
 */
object CognitiveAnomalyDetector {

    private const val ACCURACY_DROP_THRESHOLD = 0.25
    private const val TIME_SURGE_FACTOR = 1.8
    private const val ERROR_SURGE_THRESHOLD = 4

    fun detectAnomalies(
        currentAccuracy: Double,
        currentResponseTimeMs: Long,
        currentErrors: Int,
        baselineHistory: List<SessionBaselineItem>
    ): AnomalyReport {
        if (baselineHistory.isEmpty()) {
            return AnomalyReport(
                anomalyDetected = false,
                riskLevel = "Normal",
                alerts = emptyList(),
                caregiverActionRequired = false
            )
        }

        val avgAcc = baselineHistory.map { it.accuracy }.average()
        val avgTime = baselineHistory.map { it.responseTimeMs.toDouble() }.average()
        val avgErrors = baselineHistory.map { it.errors.toDouble() }.average()

        val alerts = mutableListOf<AnomalyAlert>()

        // 1. Acute Accuracy Drop
        val accDiff = avgAcc - currentAccuracy
        if (accDiff >= ACCURACY_DROP_THRESHOLD) {
            alerts.add(
                AnomalyAlert(
                    type = "ACUTE_ACCURACY_DROP",
                    severity = "HIGH",
                    message = String.format("Accuracy dropped by %.1f%% compared to baseline average (%.1f%%).", accDiff * 100.0, avgAcc * 100.0)
                )
            )
        }

        // 2. Reaction Latency Surge
        if (avgTime > 0 && currentResponseTimeMs.toDouble() >= (avgTime * TIME_SURGE_FACTOR)) {
            alerts.add(
                AnomalyAlert(
                    type = "LATENCY_SURGE",
                    severity = "MEDIUM",
                    message = String.format("Response time surged to %.1fs vs baseline average %.1fs.", currentResponseTimeMs / 1000.0, avgTime / 1000.0)
                )
            )
        }

        // 3. Error Spike
        if (currentErrors >= avgErrors + ERROR_SURGE_THRESHOLD) {
            alerts.add(
                AnomalyAlert(
                    type = "ERROR_SPIKE",
                    severity = "MEDIUM",
                    message = String.format("High error count (%d errors vs baseline average %.1f).", currentErrors, avgErrors)
                )
            )
        }

        val anomalyDetected = alerts.isNotEmpty()
        val riskLevel = when {
            alerts.any { it.severity == "HIGH" } -> "High"
            anomalyDetected -> "Moderate"
            else -> "Normal"
        }

        return AnomalyReport(
            anomalyDetected = anomalyDetected,
            riskLevel = riskLevel,
            alerts = alerts,
            caregiverActionRequired = riskLevel == "High" || riskLevel == "Moderate"
        )
    }

    /**
     * Records a completed session into SharedPreferences rolling baseline (keeps last 10 sessions).
     */
    fun recordSessionToHistory(context: Context, accuracy: Double, responseTimeMs: Long, errors: Int) {
        val prefs = context.getSharedPreferences("cognitive_baseline_prefs", Context.MODE_PRIVATE)
        val rawJson = prefs.getString("session_history_json", "[]") ?: "[]"
        try {
            val jsonArray = JSONArray(rawJson)
            val newItem = JSONObject().apply {
                put("accuracy", accuracy)
                put("response_time_ms", responseTimeMs)
                put("errors", errors)
            }
            jsonArray.put(newItem)

            // Keep only latest 10 sessions
            val trimmedArray = JSONArray()
            val startIdx = (jsonArray.length() - 10).coerceAtLeast(0)
            for (i in startIdx until jsonArray.length()) {
                trimmedArray.put(jsonArray.get(i))
            }
            prefs.edit().putString("session_history_json", trimmedArray.toString()).apply()
        } catch (_: Exception) {}
    }

    fun getSessionHistory(context: Context): List<SessionBaselineItem> {
        val prefs = context.getSharedPreferences("cognitive_baseline_prefs", Context.MODE_PRIVATE)
        val rawJson = prefs.getString("session_history_json", "[]") ?: "[]"
        val list = mutableListOf<SessionBaselineItem>()
        try {
            val jsonArray = JSONArray(rawJson)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    SessionBaselineItem(
                        accuracy = obj.optDouble("accuracy", 0.8),
                        responseTimeMs = obj.optLong("response_time_ms", 25000L),
                        errors = obj.optInt("errors", 1)
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }
}
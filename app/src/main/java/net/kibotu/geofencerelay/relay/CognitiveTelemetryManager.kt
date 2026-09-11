package net.kibotu.geofencerelay.relay

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.kibotu.geofencerelay.features.ai.model.CpsAssessmentResult
import net.kibotu.geofencerelay.model.GameSessionRecord
import net.kibotu.geofencerelay.model.PatientCognitiveTelemetry
import net.kibotu.geofencerelay.service.TrackerForegroundService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CognitiveTelemetryManager {

    private const val TAG = "CognitiveTelemetry"
    private const val PREFS_NAME = "cognitive_telemetry_prefs"
    private const val KEY_SESSIONS = "recent_game_sessions"
    private const val KEY_LATEST_TELEMETRY = "latest_telemetry_json"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    fun recordGameAndBroadcast(
        context: Context,
        userEmail: String,
        assessment: CpsAssessmentResult,
        sessionRecord: GameSessionRecord
    ) {
        val effectiveEmail = resolveEmail(context, userEmail)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Load existing sessions
        val existingSessions = getRecentSessions(context).toMutableList()
        existingSessions.add(0, sessionRecord)
        val trimmedSessions = existingSessions.take(20)

        // Persist updated sessions
        try {
            val sessionsJson = json.encodeToString(trimmedSessions)
            prefs.edit().putString(KEY_SESSIONS, sessionsJson).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving sessions: ${e.message}")
        }

        // Count sessions played today
        val todayStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val countToday = trimmedSessions.count { session ->
            SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(session.timestamp)) == todayStr
        }

        val telemetry = PatientCognitiveTelemetry(
            patientEmail = effectiveEmail,
            patientName = "Smaran Patient Device",
            timestamp = System.currentTimeMillis(),
            compositeCps = assessment.cpsScore,
            functionalCognitiveAge = assessment.functionalCognitiveAge,
            biologicalAge = assessment.biologicalAge,
            memoryRetentionIndex = assessment.subScores.memoryRetentionIndex,
            executiveFunctionIndex = assessment.subScores.executiveFunctionIndex,
            reactionLatencyScore = assessment.subScores.reactionLatencyScore,
            errorRecoveryRate = assessment.subScores.errorRecoveryRate,
            trajectoryStatus = assessment.trajectoryStatus,
            circadianRisk = assessment.circadianRisk,
            fatigueIndex = assessment.fatigueIndex,
            totalGamesPlayedToday = countToday,
            recentGameSessions = trimmedSessions,
            alertMessage = if (assessment.circadianRisk == "High") "Elevated cognitive fatigue detected" else null
        )

        // Save latest telemetry
        try {
            val telJson = json.encodeToString(telemetry)
            prefs.edit().putString(KEY_LATEST_TELEMETRY, telJson).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving telemetry: ${e.message}")
        }

        // Broadcast over MQTT to all authorized and cross-pairing targets
        val targets = mutableSetOf<String>()
        if (effectiveEmail.isNotBlank()) targets.add(effectiveEmail)
        try {
            targets.addAll(TrackerForegroundService.getAuthorizedEmails(context))
        } catch (_: Exception) {}
        targets.add("patient.device@smaran.local")
        targets.add("guardian.device@smaran.local")
        targets.add("smaran_shared")

        for (target in targets) {
            broadcastTelemetry(target, telemetry)
        }
    }

    fun broadcastLatest(context: Context, userEmail: String = "") {
        val effectiveEmail = resolveEmail(context, userEmail)
        val latest = getLatestTelemetry(context, effectiveEmail) ?: return
        val targets = mutableSetOf<String>()
        if (effectiveEmail.isNotBlank()) targets.add(effectiveEmail)
        try {
            targets.addAll(TrackerForegroundService.getAuthorizedEmails(context))
        } catch (_: Exception) {}
        targets.add("patient.device@smaran.local")
        targets.add("guardian.device@smaran.local")
        targets.add("smaran_shared")

        for (target in targets) {
            broadcastTelemetry(target, latest)
        }
    }

    private fun broadcastTelemetry(email: String, telemetry: PatientCognitiveTelemetry) {
        if (email.isBlank()) {
            Log.w(TAG, "Cannot broadcast cognitive telemetry: email is blank")
            return
        }
        scope.launch {
            try {
                Log.d(TAG, "Broadcasting cognitive telemetry for $email (CPS=${telemetry.compositeCps})...")
                val success = MqttRelayClient.shared.publishCognitiveTelemetry(email, telemetry)
                if (success) {
                    Log.i(TAG, "Successfully published cognitive telemetry to MQTT for $email!")
                } else {
                    Log.w(TAG, "Failed to publish cognitive telemetry to MQTT for $email")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during telemetry broadcast: ${e.message}", e)
            }
        }
    }

    fun getLatestTelemetry(context: Context, fallbackEmail: String = ""): PatientCognitiveTelemetry? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_LATEST_TELEMETRY, null) ?: return null
        return try {
            json.decodeFromString<PatientCognitiveTelemetry>(raw)
        } catch (e: Exception) {
            null
        }
    }

    fun getRecentSessions(context: Context): List<GameSessionRecord> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_SESSIONS, null) ?: return emptyList()
        return try {
            json.decodeFromString<List<GameSessionRecord>>(raw)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun resolveEmail(context: Context, explicitEmail: String): String {
        if (explicitEmail.isNotBlank()) return explicitEmail
        val appPrefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val authPrefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        return appPrefs.getString("user_google_email", null)
            ?: authPrefs.getString("user_google_email", null)
            ?: MqttRelayClient.shared.userEmail.takeIf { it.isNotBlank() }
            ?: ""
    }
}

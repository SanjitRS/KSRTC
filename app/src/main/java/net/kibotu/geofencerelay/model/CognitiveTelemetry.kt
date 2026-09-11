package net.kibotu.geofencerelay.model

import kotlinx.serialization.Serializable

@Serializable
data class GameSessionRecord(
    val gameId: String = "MEMORY_MATCH", // MEMORY_MATCH, PATTERN_RECALL, STROOP, TRAIL_MAKING
    val gameName: String = "Jumbo Memory Match",
    val score: Int = 85,
    val roundsCompleted: Int = 2,
    val accuracyPercent: Double = 90.0,
    val averageLatencyMs: Long = 1450L,
    val errors: Int = 1,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class PatientCognitiveTelemetry(
    val patientEmail: String = "",
    val patientName: String = "Senior Patient",
    val timestamp: Long = System.currentTimeMillis(),
    val compositeCps: Double = 82.5,
    val functionalCognitiveAge: Double = 68.0,
    val biologicalAge: Int = 74,
    val memoryRetentionIndex: Double = 84.0,
    val executiveFunctionIndex: Double = 79.5,
    val reactionLatencyScore: Double = 81.0,
    val errorRecoveryRate: Double = 88.0,
    val trajectoryStatus: String = "Stable / Preserved",
    val circadianRisk: String = "Low",
    val fatigueIndex: Double = 0.18,
    val totalGamesPlayedToday: Int = 4,
    val recentGameSessions: List<GameSessionRecord> = emptyList(),
    val alertMessage: String? = null
)

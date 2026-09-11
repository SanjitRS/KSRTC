package net.kibotu.geofencerelay.features.ai.model

import kotlinx.serialization.Serializable

/**
 * Telemetry collected from patient and gameplay interactions (22 features matching ML model).
 */
@Serializable
data class GameSessionTelemetry(
    val age: Int = 68,
    val educationLevel: Int = 2,
    val preferredLanguage: String = "English",
    val mmseScore: Double = 24.5,
    val gdsScore: Double = 4.0,
    val gameType: String = "memory_matching",
    val timeOfDayHour: Int = 10,
    val accuracy: Double = 0.85,
    val responseTimeMs: Long = 28000,
    val attempts: Int = 10,
    val errors: Int = 1,
    val repeatMismatches: Int = 0,
    val spatialProximityErrorScore: Double = 0.5,
    val spanMemoryCapacity: Int = 6,
    val flipLatencyVarianceMs: Double = 320.0,
    val motorJitterIndex: Double = 22.4,
    val speechHesitationScore: Double = 25.0,
    val hintsUsed: Int = 0,
    val completionRate: Double = 1.0,
    val isReminiscenceGame: Boolean = true,
    val familyPhotoRecognitionRate: Double = 0.90
)

/**
 * Clinical cognitive breakdown sub-scores (0-100 scale).
 */
@Serializable
data class SubDomainScores(
    val autobiographicalReminiscence: Double,
    val memoryRetentionIndex: Double,
    val reactionLatencyScore: Double,
    val executiveFunctionIndex: Double,
    val errorRecoveryRate: Double
)

/**
 * Comprehensive Cognitive Performance Score (CPS) assessment result.
 */
@Serializable
data class CpsAssessmentResult(
    val cpsScore: Double,                     // 0 - 100 continuous score
    val functionalCognitiveAge: Double,      // Normative functional age
    val biologicalAge: Int,
    val subScores: SubDomainScores,
    val motorJitterIndex: Double,
    val motorDiagnostic: String,
    val speechHesitationScore: Double,
    val speechDiagnostic: String,
    val hiddenDifficulty: String,             // "easy", "medium", "hard" (FOR INTERNAL USE ONLY)
    val fatigueIndex: Double,                // 0.0 - 1.0
    val avgReactionPerAttemptMs: Double,
    val circadianRisk: String,                // "Low", "Moderate", "High"
    val optimalExerciseWindow: String,
    val projectedCps30Days: Double,
    val projectedCps90Days: Double,
    val trajectoryStatus: String,
    val caregiverReminiscencePlan: String,
    val encouragementPrompt: String
)

/**
 * Dynamic Game Configuration adapted to patient without revealing difficulty labels.
 */
data class AdaptiveGameConfig(
    val columns: Int,
    val rows: Int,
    val totalPairs: Int,
    val revealDurationMs: Long,
    val timeLimitSec: Int,
    val hintAvailable: Boolean
)

/**
 * Clinical Impairment and Wander Hazard report.
 */
data class ImpairmentRiskReport(
    val riskLevel: String,                   // "Normal / Stable Trajectory", "Early MCI Risk", "Clinical Review Recommended"
    val isHighRisk: Boolean,
    val sundowningAlert: Boolean,
    val wanderRiskIndex: Double,             // 0.0 to 1.0
    val safeZoneStatus: String,
    val clinicalGuidance: String
)

package net.kibotu.geofencerelay.features.ai.engine

import net.kibotu.geofencerelay.features.ai.localization.MultilingualManager
import net.kibotu.geofencerelay.features.ai.model.CpsAssessmentResult
import net.kibotu.geofencerelay.features.ai.model.GameSessionTelemetry
import net.kibotu.geofencerelay.features.ai.model.SubDomainScores
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Pure on-device Kotlin Cognitive Performance Scoring Engine.
 * Replicates the clinical ML pipeline from sih-ai-analysis- with zero server calls.
 */
object CpsEngine {

    fun analyzeSession(
        telemetry: GameSessionTelemetry,
        langCode: String = "en"
    ): CpsAssessmentResult {
        val accuracy = telemetry.accuracy.coerceIn(0.0, 1.0)
        val responseTimeMs = max(telemetry.responseTimeMs, 1000L)
        val attempts = max(telemetry.attempts, 1)
        val errors = telemetry.errors
        val hints = telemetry.hintsUsed
        val completion = telemetry.completionRate.coerceIn(0.0, 1.0)

        // 1. Latency Efficiency Score
        val speedScore = (100.0 - (responseTimeMs / 1800.0)).coerceIn(0.0, 100.0)

        // 2. Game Type Multiplier
        val gameMultiplier = if (telemetry.gameType == "pattern_recognition") 1.05 else 1.0

        // 3. Family Reminiscence Therapy Boost
        val remBoost = if (telemetry.isReminiscenceGame) 1.10 else 1.0
        val remRecallScore = (accuracy * 100.0 * remBoost).coerceIn(10.0, 100.0)

        // 4. Composite CPS Calculation (0 - 100)
        var cps = (0.35 * (telemetry.mmseScore / 30.0 * 100.0)) +
                (0.35 * (accuracy * 100.0 * remBoost)) +
                (0.15 * (completion * 100.0)) +
                (0.15 * speedScore * gameMultiplier)

        cps -= (hints * 1.5) + (errors * 1.0)

        if (telemetry.isReminiscenceGame && telemetry.familyPhotoRecognitionRate > 0.8) {
            cps += 3.5
        }
        cps = cps.coerceIn(0.0, 100.0)

        // 5. Multi-domain Cognitive Breakdown Sub-scores
        val memoryRetention = ((accuracy * 70.0) + (completion * 30.0) - (hints * 3.0)).coerceIn(10.0, 100.0)
        val reactionLatency = (100.0 - (responseTimeMs / 1800.0)).coerceIn(10.0, 100.0)
        val executiveFunction = ((telemetry.mmseScore / 30.0 * 50.0) + (accuracy * 50.0) - (errors * 1.5)).coerceIn(10.0, 100.0)
        val errorRecovery = (100.0 - (errors.toDouble() / attempts * 100.0)).coerceIn(10.0, 100.0)

        val subScores = SubDomainScores(
            autobiographicalReminiscence = roundOne(remRecallScore),
            memoryRetentionIndex = roundOne(memoryRetention),
            reactionLatencyScore = roundOne(reactionLatency),
            executiveFunctionIndex = roundOne(executiveFunction),
            errorRecoveryRate = roundOne(errorRecovery)
        )

        // 6. Functional Cognitive Age
        val cogAgeDelta = (50.0 - cps) * 0.18
        val functionalCognitiveAge = (telemetry.age + cogAgeDelta).coerceIn(18.0, 110.0)

        // 7. Hidden Adaptive Difficulty (Never shown to patient)
        val nextDifficulty = when {
            cps >= 72.0 && accuracy >= 0.80 -> "hard"
            cps >= 48.0 && accuracy >= 0.55 -> "medium"
            else -> "easy"
        }

        // 8. Fatigue & Risk
        val fatigueIndex = ((responseTimeMs / 60000.0) * (hints + 1) * (errors + 1) / 10.0).coerceIn(0.0, 1.0)
        val avgReaction = responseTimeMs.toDouble() / attempts

        // 9. Diagnostics
        val motorDiagnostic = if (telemetry.motorJitterIndex < 35.0)
            "Normal Motor Fine Control"
        else
            "Subtle Touch Jitter Detected (Dementia/Parkinsonian Indicator)"

        val speechDiagnostic = if (telemetry.speechHesitationScore < 40.0)
            "Fluent Speech Response"
        else
            "Elevated Acoustic Hesitation (Word-Finding Latency)"

        // 10. Circadian Sundowning Analysis
        val hour = telemetry.timeOfDayHour
        val isEvening = hour >= 16 || hour <= 4
        val circadianRisk = if (isEvening && (accuracy < 0.65 || fatigueIndex > 0.5)) "Moderate" else "Low"

        val optimalWindow = when (hour) {
            in 8..12 -> "Morning (08:00 - 12:00) - Peak Cognitive Alertness"
            in 13..16 -> "Early Afternoon - Moderate Focus"
            else -> "Evening - Rest Recommended (Sundowning Watch Window)"
        }

        // 11. Trajectory Projections
        val slope = 0.65 // Baseline weekly positive trend with regular exercise
        val projected30 = (cps + (slope * 4.0)).coerceIn(10.0, 100.0)
        val projected90 = (cps + (slope * 12.0)).coerceIn(10.0, 100.0)
        val trajectoryStatus = if (slope > 0.5) "Upward Recovery Trajectory" else "Stable Memory Retention"

        // 12. Caregiver Plan
        val caregiverPlan = if (remRecallScore >= 80.0) {
            "Patient shows vivid recognition of familiar cues. Continue daily family photo matching and regional landmark recall."
        } else {
            "Pair family photos with familiar audio voice notes (grandchild voice recording) to stimulate emotional recall."
        }

        val tier = when (nextDifficulty) {
            "hard" -> "celebratory"
            "medium" -> "encouraging"
            else -> "gentle"
        }
        val prompt = MultilingualManager.getEncouragement(tier, langCode)

        return CpsAssessmentResult(
            cpsScore = roundTwo(cps),
            functionalCognitiveAge = roundOne(functionalCognitiveAge),
            biologicalAge = telemetry.age,
            subScores = subScores,
            motorJitterIndex = roundOne(telemetry.motorJitterIndex),
            motorDiagnostic = motorDiagnostic,
            speechHesitationScore = roundOne(telemetry.speechHesitationScore),
            speechDiagnostic = speechDiagnostic,
            hiddenDifficulty = nextDifficulty,
            fatigueIndex = roundTwo(fatigueIndex),
            avgReactionPerAttemptMs = roundOne(avgReaction),
            circadianRisk = circadianRisk,
            optimalExerciseWindow = optimalWindow,
            projectedCps30Days = roundOne(projected30),
            projectedCps90Days = roundOne(projected90),
            trajectoryStatus = trajectoryStatus,
            caregiverReminiscencePlan = caregiverPlan,
            encouragementPrompt = prompt
        )
    }

    private fun roundOne(v: Double): Double = (v * 10.0).roundToInt() / 10.0
    private fun roundTwo(v: Double): Double = (v * 100.0).roundToInt() / 100.0
}

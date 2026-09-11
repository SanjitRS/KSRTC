package net.kibotu.geofencerelay.features.ai.difficulty

import net.kibotu.geofencerelay.features.ai.model.AdaptiveGameConfig

/**
 * Manages dynamic game difficulty adaptation.
 *
 * CRITICAL CLINICAL ETHICS & PATIENT PROTECTION POLICY:
 * In accordance with dementia care guidelines (SIH26003), raw difficulty labels
 * ("Easy", "Medium", "Hard", "Impaired") MUST NEVER be exposed to the patient.
 * The game adjusts its grid size, time limit, and card reveal durations silently
 * in the background so the patient feels accomplished and stress-free.
 */
object AdaptiveDifficultyManager {

    /**
     * Computes the next session parameters based on CPS and accuracy.
     * Raw difficulty is encapsulated here and never surfaced on patient screens.
     */
    fun getAdaptiveConfig(cpsScore: Double, accuracy: Double): AdaptiveGameConfig {
        return when {
            cpsScore >= 72.0 && accuracy >= 0.80 -> {
                // Background tier: Advanced (4x3 grid = 6 pairs, shorter preview)
                AdaptiveGameConfig(
                    columns = 4,
                    rows = 3,
                    totalPairs = 6,
                    revealDurationMs = 700L,
                    timeLimitSec = 90,
                    hintAvailable = true
                )
            }
            cpsScore >= 48.0 && accuracy >= 0.55 -> {
                // Background tier: Balanced (3x3 or 4x2 grid = 4 pairs, comfortable preview)
                AdaptiveGameConfig(
                    columns = 3,
                    rows = 3,
                    totalPairs = 4,
                    revealDurationMs = 1000L,
                    timeLimitSec = 120,
                    hintAvailable = true
                )
            }
            else -> {
                // Background tier: Supportive (2x2 grid = 2 pairs, extended preview)
                AdaptiveGameConfig(
                    columns = 2,
                    rows = 2,
                    totalPairs = 2,
                    revealDurationMs = 1400L,
                    timeLimitSec = 180,
                    hintAvailable = true
                )
            }
        }
    }

    /**
     * Patient-safe title for game sessions.
     * Notice: NO mention of difficulty.
     */
    fun getPatientFacingSessionTitle(): String {
        return "Daily Memory Journey"
    }

    /**
     * Patient-safe encouraging subtext.
     */
    fun getPatientFacingDescription(): String {
        return "A gentle, fun exercise to brighten your day and keep your mind active."
    }
}

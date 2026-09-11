package net.kibotu.geofencerelay

import net.kibotu.geofencerelay.features.ai.difficulty.AdaptiveDifficultyManager
import net.kibotu.geofencerelay.features.ai.engine.CpsEngine
import net.kibotu.geofencerelay.features.ai.localization.MultilingualManager
import net.kibotu.geofencerelay.features.ai.model.GameSessionTelemetry
import net.kibotu.geofencerelay.features.ai.risk.ImpairmentRiskMonitor
import org.junit.Assert.*
import org.junit.Test

class CpsEngineTest {

    @Test
    fun testCpsScoreCalculation_MemoryMatching() {
        val telemetry = GameSessionTelemetry(
            gameType = "memory_matching",
            age = 74,
            mmseScore = 25.0,
            accuracy = 0.90,
            responseTimeMs = 28000,
            attempts = 10,
            errors = 1,
            hintsUsed = 0,
            completionRate = 1.0,
            isReminiscenceGame = true,
            familyPhotoRecognitionRate = 0.90
        )

        val result = CpsEngine.analyzeSession(telemetry, "en")

        assertTrue("CPS score should be between 0 and 100", result.cpsScore in 0.0..100.0)
        assertTrue("Functional age should be clinical range", result.functionalCognitiveAge in 45.0..98.0)
        assertTrue("Memory retention index should be valid", result.subScores.memoryRetentionIndex in 10.0..100.0)
        assertTrue("Executive function index should be valid", result.subScores.executiveFunctionIndex in 10.0..100.0)
        assertTrue("Autobiographical recall should reflect family photo rate", result.subScores.autobiographicalReminiscence >= 80.0)
    }

    @Test
    fun testCpsScoreCalculation_PatternRecognition() {
        val telemetry = GameSessionTelemetry(
            gameType = "pattern_recognition",
            age = 70,
            mmseScore = 26.0,
            accuracy = 1.0,
            responseTimeMs = 15000,
            attempts = 3,
            errors = 0,
            hintsUsed = 0,
            completionRate = 1.0
        )

        val result = CpsEngine.analyzeSession(telemetry, "hi")

        assertTrue("CPS score should be between 0 and 100", result.cpsScore in 0.0..100.0)
        assertTrue("Accuracy should reflect high score", result.cpsScore >= 70.0)
    }

    @Test
    fun testPatientProtectionPolicy_ZeroRawDifficultyLabels() {
        val safeTitle = AdaptiveDifficultyManager.getPatientFacingSessionTitle()
        val safeDesc = AdaptiveDifficultyManager.getPatientFacingDescription()

        assertFalse("Must never mention 'Easy'", safeTitle.contains("Easy", ignoreCase = true))
        assertFalse("Must never mention 'Medium'", safeTitle.contains("Medium", ignoreCase = true))
        assertFalse("Must never mention 'Hard'", safeTitle.contains("Hard", ignoreCase = true))
        assertFalse("Must never mention 'Impaired'", safeTitle.contains("Impaired", ignoreCase = true))

        assertFalse("Description must never mention 'Easy'", safeDesc.contains("Easy", ignoreCase = true))
        assertFalse("Description must never mention 'Hard'", safeDesc.contains("Hard", ignoreCase = true))
    }

    @Test
    fun testMultilingualSupport_Translations() {
        val enTitle = MultilingualManager.tr("tile_gps_title", "en")
        val hiTitle = MultilingualManager.tr("tile_gps_title", "hi")
        val asTitle = MultilingualManager.tr("tile_gps_title", "as")
        val lusTitle = MultilingualManager.tr("tile_gps_title", "lus")
        val mniTitle = MultilingualManager.tr("tile_gps_title", "mni")

        assertEquals("GPS Beacon", enTitle)
        assertEquals("जीपीएस बीकन", hiTitle)
        assertEquals("জি.পি.এছ. বিকন", asTitle)
        assertEquals("GPS Hmun Zawnna", lusTitle)
        assertEquals("GPS মফম তাকপা", mniTitle)

        val hiGames = MultilingualManager.tr("tile_games_title", "hi")
        assertEquals("मस्तिष्क खेल", hiGames)

        val asScore = MultilingualManager.tr("tile_score_title", "as")
        assertEquals("মগজুৰ স্বাস্থ্য", asScore)
    }

    @Test
    fun testCircadianSundowningRisk() {
        val morningTelemetry = GameSessionTelemetry(timeOfDayHour = 10, accuracy = 0.90)
        val morningResult = CpsEngine.analyzeSession(morningTelemetry)
        assertEquals("Low", morningResult.circadianRisk)

        val eveningFatiguedTelemetry = GameSessionTelemetry(
            timeOfDayHour = 19,
            accuracy = 0.40,
            responseTimeMs = 65000,
            hintsUsed = 3,
            errors = 4
        )
        val eveningResult = CpsEngine.analyzeSession(eveningFatiguedTelemetry)
        val report = ImpairmentRiskMonitor.evaluateRisk(eveningResult, currentHour = 19)

        assertTrue("Evening high fatigue should trigger sundowning alert", report.sundowningAlert)
    }
}

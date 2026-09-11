package net.kibotu.geofencerelay.features.ai.risk

import net.kibotu.geofencerelay.features.ai.model.CpsAssessmentResult
import net.kibotu.geofencerelay.features.ai.model.ImpairmentRiskReport
import java.util.Calendar

/**
 * Real-time clinical impairment & wander hazard monitor.
 * Links on-device telemetry, live circadian clock hours, and GPS geofence states.
 */
object ImpairmentRiskMonitor {

    fun evaluateRisk(
        cpsResult: CpsAssessmentResult?,
        currentHour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
        distanceToSafeZoneMeters: Double = 0.0,
        isOutsideGeofence: Boolean = false
    ): ImpairmentRiskReport {
        val hasAssessment = cpsResult != null
        val cps = cpsResult?.cpsScore ?: 70.0
        val fatigue = cpsResult?.fatigueIndex ?: 0.2

        // Circadian evening sundowning hours: 4:00 PM (16:00) to 5:00 AM
        val isEvening = currentHour >= 16 || currentHour <= 5
        val sundowningAlert = isEvening

        // Wander risk calculation combining cognitive latency + physical location
        var wanderRisk = 0.10
        if (isOutsideGeofence) wanderRisk += 0.50
        if (distanceToSafeZoneMeters > 500) wanderRisk += 0.20
        if (sundowningAlert) wanderRisk += 0.20
        if (hasAssessment && cps < 50.0) wanderRisk += 0.15
        wanderRisk = wanderRisk.coerceIn(0.0, 1.0)

        val isHighRisk = (hasAssessment && cps < 50.0) || (isOutsideGeofence && sundowningAlert) || wanderRisk > 0.65

        val riskLevel = when {
            wanderRisk > 0.70 || (isOutsideGeofence && sundowningAlert) ->
                "Critical Intervention (Immediate Caregiver Attention)"
            isOutsideGeofence ->
                "Perimeter Breach Alert (Device Outside Safe Zone)"
            sundowningAlert ->
                "Evening Sundowning Watch Window (Fatigue Monitoring Active)"
            hasAssessment && cps < 65.0 ->
                "Mild Cognitive Impairment (MCI) Active Monitoring"
            hasAssessment ->
                "Cognitive Status: Stable"
            else ->
                "Baseline Surveillance (No Game Session Recorded Yet)"
        }

        val safeZoneStatus = when {
            isOutsideGeofence -> "BREACH: Outside designated safe perimeter (${distanceToSafeZoneMeters.toInt()}m)"
            distanceToSafeZoneMeters > 300 -> "WARNING: Approaching geofence boundary"
            else -> "SECURE: Inside designated safe area"
        }

        val guidance = when {
            isOutsideGeofence ->
                "Geofence Breach Alert: Device is outside safe boundary. Check live GPS Sentinel beacon immediately or call patient."
            sundowningAlert ->
                "Evening Sundowning Period Active: Patient may experience evening confusion, sensory fatigue, or restlessness. Keep room lighting bright and warm, minimize jarring noises, and maintain comforting family presence."
            hasAssessment && cps < 60.0 ->
                "Encourage daytime hydration, gentle 15-minute family reminiscence card recall, and quiet walking routines."
            hasAssessment ->
                "Cognitive alertness is high. Positive reinforcement and regular cognitive exercises recommended."
            else ->
                "No game sessions completed yet today. Have the patient complete a daily brain exercise to calibrate fine motor tremor and speech reaction latency."
        }

        return ImpairmentRiskReport(
            riskLevel = riskLevel,
            isHighRisk = isHighRisk,
            sundowningAlert = sundowningAlert,
            wanderRiskIndex = (wanderRisk * 100.0).toInt() / 100.0,
            safeZoneStatus = safeZoneStatus,
            clinicalGuidance = guidance
        )
    }
}


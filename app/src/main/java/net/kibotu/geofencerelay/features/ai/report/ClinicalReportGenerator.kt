package net.kibotu.geofencerelay.features.ai.report

import android.content.Context
import android.content.Intent
import net.kibotu.geofencerelay.features.ai.model.CpsAssessmentResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Replicates `clinical_report_generator.py` from SMARAN-AI-Backend:
 * Generates comprehensive clinical diagnostic reports in Markdown with 1-tap sharing.
 */
object ClinicalReportGenerator {

    fun generateMarkdownReport(
        assessment: CpsAssessmentResult,
        patientName: String = "Senior Participant",
        biologicalAge: Int = 74
    ): String {
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val cps = assessment.cpsScore
        val funcAge = assessment.functionalCognitiveAge
        val sub = assessment.subScores
        val proj30 = assessment.projectedCps30Days
        val proj90 = assessment.projectedCps90Days
        val trajStatus = assessment.trajectoryStatus

        return """
# ðŸ§  SMARAN AI: Executive Cognitive Performance & Clinical Report

**Generated On:** $dateStr  
**Patient Name:** $patientName | **Biological Age:** $biologicalAge years | **Functional Cognitive Age:** ${String.format(Locale.US, "%.1f", funcAge)} years  
**SIH Problem Statement:** SIH26003 (Cognitive Care & Reminiscence Suite)

---

## 1. Executive Cognitive Summary
* **Composite CPS Score:** **`${String.format(Locale.US, "%.1f", cps)} / 100.0`**
* **Cognitive Trajectory Status:** **$trajStatus**
* **Circadian Sundowning Risk:** **${assessment.circadianRisk}**
* **Session Fatigue Index:** `${String.format(Locale.US, "%.2f", assessment.fatigueIndex)}`

---

## 2. Cognitive Sub-Domain Breakdown (0 - 100 Scale)
| Cognitive Sub-Domain | Score | Clinical Evaluation Focus |
| :--- | :---: | :--- |
| **Memory Retention Index** | `${String.format(Locale.US, "%.1f", sub.memoryRetentionIndex)}` | Spatial & Visual Pair Memory |
| **Reaction Latency Score** | `${String.format(Locale.US, "%.1f", sub.reactionLatencyScore)}` | Processing Speed & Choice Latency |
| **Executive Function Index** | `${String.format(Locale.US, "%.1f", sub.executiveFunctionIndex)}` | Sequence Planning & Rule Inhibition |
| **Reminiscence Recall** | `${String.format(Locale.US, "%.1f", sub.autobiographicalReminiscence)}` | Familial & Cultural Cue Recall |
| **Error Recovery Rate** | `${String.format(Locale.US, "%.1f", sub.errorRecoveryRate)}` | Post-Error Strategy Adaptation |

---

## 3. Biomotor & Speech Acoustics Diagnostics
* **Motor Control Status:** ${assessment.motorDiagnostic}
* **Speech Latency Status:** ${assessment.speechDiagnostic}

---

## 4. AI Predictive Trajectory Forecast (30 & 90 Days)
* **30-Day Projected CPS Score:** **`${String.format(Locale.US, "%.1f", proj30)} / 100`**
* **90-Day Projected CPS Score:** **`${String.format(Locale.US, "%.1f", proj90)} / 100`**
* **Forecasted Trend:** $trajStatus

---

## 5. Active Patient Guidance & Reminiscence Therapy Plan
* **Active Patient Encouragement:** *"${assessment.encouragementPrompt}"*
* **Caregiver Action Plan:** ${assessment.caregiverReminiscencePlan}
* **Optimal Exercise Window:** ${assessment.optimalExerciseWindow}

---
*Report automatically compiled by SMARAN AI Multi-Feature Engine v4.0. Confidentially exported for caregiver and physician review.*
""".trimIndent()
    }

    fun shareClinicalReport(context: Context, reportMarkdown: String, patientName: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, reportMarkdown)
            putExtra(Intent.EXTRA_SUBJECT, "SMARAN AI Clinical Cognitive Report - $patientName")
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Clinical Cognitive Report")
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    }
}
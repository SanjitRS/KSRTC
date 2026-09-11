package net.kibotu.geofencerelay.features.ai.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.kibotu.geofencerelay.features.ai.localization.MultilingualManager
import net.kibotu.geofencerelay.features.ai.model.CpsAssessmentResult
import net.kibotu.geofencerelay.features.ai.report.ClinicalReportGenerator
import net.kibotu.geofencerelay.features.ai.risk.CognitiveAnomalyDetector
import net.kibotu.geofencerelay.features.ai.ui.components.IosBackPillButton
import net.kibotu.geofencerelay.relay.CognitiveTelemetryManager
import net.kibotu.geofencerelay.ui.theme.*

/**
 * Vibrant Cognitive Assessment Dashboard.
 * Adheres directly to the reference design kit:
 * - Warm porcelain canvas and authentic woven ribbon banner
 * - Tactile 24dp white cards and Atkinson Hyperlegible typography
 * - Multi-tier progress bars for sub-domains (Orange, Green, Blue, Plum)
 */
@Composable
fun CognitiveHealthPanel(
    assessment: CpsAssessmentResult?,
    selectedLanguageCode: String,
    onLaunchGame: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val baselineHistory = remember { CognitiveAnomalyDetector.getSessionHistory(context) }
    val latestSession = baselineHistory.lastOrNull()
    val anomalyReport = remember(latestSession) {
        if (latestSession != null && baselineHistory.size > 1) {
            CognitiveAnomalyDetector.detectAnomalies(
                latestSession.accuracy,
                latestSession.responseTimeMs,
                latestSession.errors,
                baselineHistory.dropLast(1)
            )
        } else {
            null
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NerColors.CanvasWarm)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // Top Authentic Woven Textile Ribbon
        NerWovenRibbon(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            height = 14.dp,
            primaryColor = NerColors.Tertiary,
            secondaryColor = NerColors.Primary,
            accentColor = NerColors.Marigold
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = MultilingualManager.tr("tile_score_title", selectedLanguageCode),
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = NerColors.Charcoal
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = MultilingualManager.tr("health_subtitle", selectedLanguageCode),
                fontSize = 13.sp,
                color = NerColors.NeutralMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (anomalyReport != null && anomalyReport.anomalyDetected) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = NerColors.CrimsonTint),
                    border = BorderStroke(1.2.dp, NerColors.Crimson)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = NerColors.Crimson,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Acute Performance Drop Alert (${anomalyReport.riskLevel})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = NerColors.Crimson
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            anomalyReport.alerts.forEach { alert ->
                                Text(
                                    text = "• ${alert.message}",
                                    fontSize = 12.sp,
                                    color = NerColors.Charcoal
                                )
                            }
                        }
                    }
                }
            }

            if (assessment == null) {
                // Unassessed State Card (Warm 24dp Card)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = NerColors.SurfaceWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    border = BorderStroke(1.dp, NerColors.NeutralBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(26.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(NerColors.TertiaryTint),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = NerColors.Tertiary,
                                modifier = Modifier.size(46.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = MultilingualManager.tr("lbl_untested", selectedLanguageCode),
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = NerColors.Charcoal
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = MultilingualManager.tr("lbl_untested_desc", selectedLanguageCode),
                            fontSize = 13.sp,
                            color = NerColors.NeutralMedium,
                            textAlign = TextAlign.Center,
                            lineHeight = 19.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        NerPillButton(
                            text = MultilingualManager.tr("btn_start_test", selectedLanguageCode),
                            icon = Icons.Default.SportsEsports,
                            hierarchy = NerButtonHierarchy.Primary,
                            containerColor = NerColors.Primary,
                            onClick = onLaunchGame,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                // Calculated CPS Score Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = NerColors.SurfaceWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    border = BorderStroke(1.dp, NerColors.NeutralBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(130.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(NerColors.TertiaryTint, Color.Transparent)
                                    )
                                )
                                .border(6.dp, NerColors.Tertiary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${assessment.cpsScore.toInt()}",
                                    fontSize = 42.sp,
                                    fontWeight = FontWeight.Black,
                                    color = NerColors.Tertiary
                                )
                                Text(
                                    text = MultilingualManager.tr("health_cps_score", selectedLanguageCode),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NerColors.NeutralMedium,
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${assessment.functionalCognitiveAge.toInt()} yrs",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NerColors.Tertiary
                                )
                                Text(
                                    MultilingualManager.tr("health_cog_age", selectedLanguageCode),
                                    fontSize = 12.sp,
                                    color = NerColors.NeutralMedium
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(32.dp)
                                    .background(NerColors.NeutralBorder)
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${assessment.biologicalAge} yrs",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NerColors.Charcoal
                                )
                                Text(
                                    MultilingualManager.tr("health_bio_age", selectedLanguageCode),
                                    fontSize = 12.sp,
                                    color = NerColors.NeutralMedium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sub-domain Breakdown Card with UI Kit Progress Bars
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = NerColors.SurfaceWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    border = BorderStroke(1.dp, NerColors.NeutralBorder)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = MultilingualManager.tr("health_subdomains", selectedLanguageCode),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = NerColors.Charcoal
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        SubScoreRow(MultilingualManager.tr("sub_memory", selectedLanguageCode), assessment.subScores.memoryRetentionIndex, NerColors.Tertiary)
                        SubScoreRow(MultilingualManager.tr("sub_executive", selectedLanguageCode), assessment.subScores.executiveFunctionIndex, NerColors.Primary)
                        SubScoreRow(MultilingualManager.tr("sub_reaction", selectedLanguageCode), assessment.subScores.reactionLatencyScore, NerColors.Secondary)
                        SubScoreRow(MultilingualManager.tr("sub_autobio", selectedLanguageCode), assessment.subScores.autobiographicalReminiscence, NerColors.Marigold)
                        SubScoreRow(MultilingualManager.tr("sub_recovery", selectedLanguageCode), assessment.subScores.errorRecoveryRate, NerColors.PlumMaroon)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 30 & 90 Days Projections Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = NerColors.SurfaceWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    border = BorderStroke(1.dp, NerColors.NeutralBorder)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = NerColors.Secondary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${MultilingualManager.tr("health_forecast", selectedLanguageCode)}: ${assessment.trajectoryStatus}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = NerColors.Charcoal
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(MultilingualManager.tr("health_30days", selectedLanguageCode), fontSize = 12.sp, color = NerColors.NeutralMedium)
                                Text("${assessment.projectedCps30Days} CPS", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NerColors.Secondary)
                            }
                            Column {
                                Text(MultilingualManager.tr("health_90days", selectedLanguageCode), fontSize = 12.sp, color = NerColors.NeutralMedium)
                                Text("${assessment.projectedCps90Days} CPS", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NerColors.Tertiary)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = assessment.caregiverReminiscencePlan,
                            fontSize = 12.sp,
                            color = NerColors.NeutralMedium,
                            lineHeight = 16.sp
                        )
                    }
                }

                // Instant Sync CPS Telemetry to Caregiver Pill Button
                NerPillButton(
                    text = "Sync CPS to Caregiver Now",
                    onClick = {
                        CognitiveTelemetryManager.broadcastLatest(context)
                        android.widget.Toast.makeText(context, "CPS Telemetry synced to Caregiver over MQTT!", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    containerColor = NerColors.Secondary,
                    icon = Icons.Default.Sync,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Share Clinical Diagnostic Report Pill Button
                NerPillButton(
                    text = "Share Clinical Diagnostic Report",
                    onClick = {
                        val reportMd = ClinicalReportGenerator.generateMarkdownReport(assessment)
                        ClinicalReportGenerator.shareClinicalReport(context, reportMd, "Senior Participant")
                    },
                    containerColor = NerColors.Tertiary,
                    icon = Icons.Default.Share,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Accessible Bottom Back Pill
        IosBackPillButton(
            label = MultilingualManager.tr("btn_back", selectedLanguageCode),
            onClick = onBack
        )
    }
}

@Composable
private fun SubScoreRow(
    label: String,
    score: Double,
    tintColor: Color
) {
    val progress = (score / 100.0).coerceIn(0.0, 1.0).toFloat()
    Column(modifier = Modifier.padding(vertical = 5.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NerColors.Charcoal)
            Text("${score.toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = tintColor)
        }
        Spacer(modifier = Modifier.height(5.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(NerColors.NeutralSoft)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = progress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(percent = 50))
                    .background(tintColor)
            )
        }
    }
}
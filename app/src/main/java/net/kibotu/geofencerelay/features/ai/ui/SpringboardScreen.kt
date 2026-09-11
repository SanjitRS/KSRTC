package net.kibotu.geofencerelay.features.ai.ui

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.kibotu.geofencerelay.R
import net.kibotu.geofencerelay.features.ai.localization.MultilingualManager
import net.kibotu.geofencerelay.features.ai.model.CpsAssessmentResult
import net.kibotu.geofencerelay.features.ai.model.SubDomainScores
import net.kibotu.geofencerelay.features.ai.reminder.GameReminderManager
import net.kibotu.geofencerelay.features.ai.ui.components.IosSpringboardCard
import net.kibotu.geofencerelay.features.ai.ui.dialogs.*
import net.kibotu.geofencerelay.relay.CognitiveTelemetryManager
import net.kibotu.geofencerelay.relay.MqttRelayClient
import net.kibotu.geofencerelay.service.TrackerForegroundService
import net.kibotu.geofencerelay.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

sealed class SpringboardDestination {
    object Home : SpringboardDestination()
    object Beacon : SpringboardDestination()
    object Exercises : SpringboardDestination()
    object Health : SpringboardDestination()
    object Safety : SpringboardDestination()
    object Voice : SpringboardDestination()
    object Memory : SpringboardDestination()
}

/**
 * Vibrant North-East Cultural & High-Aesthetic Assistive Springboard.
 * Directly adheres to the reference design kit:
 * - Warm porcelain canvas with authentic procedural woven textile ribbon
 * - High-contrast Atkinson Hyperlegible typography
 * - Tactile 24dp rounded cards and floating pill dock navigation
 * - Fully localized across all 7 regional languages
 */
@Composable
fun SpringboardScreen(
    userEmail: String,
    initialDestination: SpringboardDestination = SpringboardDestination.Home,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current

    // Initialize Multilingual & TTS support
    LaunchedEffect(Unit) {
        MultilingualManager.initTts(context)
    }

    DisposableEffect(Unit) {
        onDispose {
            MultilingualManager.shutdown()
        }
    }

    val prefs = remember { context.getSharedPreferences("app_settings", Context.MODE_PRIVATE) }
    var activeDestination by remember { mutableStateOf<SpringboardDestination>(initialDestination) }
    var selectedLanguageCode by remember {
        mutableStateOf(prefs.getString("selected_language", "en") ?: "en")
    }

    // Load previous assessment if available from local disk cache
    val cachedTel = remember { CognitiveTelemetryManager.getLatestTelemetry(context, userEmail) }
    var currentAssessment by remember {
        mutableStateOf<CpsAssessmentResult?>(
            cachedTel?.let { t ->
                CpsAssessmentResult(
                    cpsScore = t.compositeCps.toDouble(),
                    functionalCognitiveAge = t.functionalCognitiveAge.toDouble(),
                    biologicalAge = t.biologicalAge,
                    subScores = SubDomainScores(
                        autobiographicalReminiscence = 85.0,
                        memoryRetentionIndex = t.memoryRetentionIndex,
                        reactionLatencyScore = t.reactionLatencyScore,
                        executiveFunctionIndex = t.executiveFunctionIndex,
                        errorRecoveryRate = t.errorRecoveryRate
                    ),
                    motorJitterIndex = 0.05,
                    motorDiagnostic = "Smooth steady gestures",
                    speechHesitationScore = 0.08,
                    speechDiagnostic = "Fluent prosody",
                    hiddenDifficulty = "Adaptive",
                    fatigueIndex = t.fatigueIndex / 100.0,
                    avgReactionPerAttemptMs = t.reactionLatencyScore.toDouble() * 7.0,
                    circadianRisk = t.circadianRisk,
                    optimalExerciseWindow = "Morning 9-11 AM",
                    projectedCps30Days = (t.compositeCps + 1).toDouble(),
                    projectedCps90Days = (t.compositeCps + 3).toDouble(),
                    trajectoryStatus = t.trajectoryStatus,
                    caregiverReminiscencePlan = "Review family photos",
                    encouragementPrompt = "Great effort! Keep up your daily cognitive exercises."
                )
            }
        )
    }

    LaunchedEffect(userEmail) {
        val effectiveEmail = CognitiveTelemetryManager.resolveEmail(context, userEmail)
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            MqttRelayClient.shared.connect(effectiveEmail)
        }
        CognitiveTelemetryManager.broadcastLatest(context, userEmail)

        // Listen for incoming remote commands from Caregiver (FETCH_COGNITIVE_DATA / SYNC)
        MqttRelayClient.shared.incomingCommand.collect { cmd ->
            if (cmd.command.contains("COGNITIVE", ignoreCase = true) || cmd.command.contains("SYNC", ignoreCase = true)) {
                CognitiveTelemetryManager.broadcastLatest(context, userEmail)
            }
        }
    }

    var isAlarmPopping by remember { mutableStateOf(GameReminderManager.isAlarmFiring(context)) }

    LaunchedEffect(Unit) {
        while (true) {
            isAlarmPopping = GameReminderManager.isAlarmFiring(context)
            kotlinx.coroutines.delay(1200)
        }
    }

    val isServiceRunning by TrackerForegroundService.serviceRunning.collectAsState()
    val isBroadcasting = isServiceRunning || TrackerForegroundService.isRunning(context)

    val currentTimeStr = remember {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        sdf.format(Date())
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NerColors.CanvasWarm)
    ) {
        // Delicate Folk Mandala Watermark
        NerMandalaWatermark(
            modifier = Modifier.fillMaxSize(),
            baseColor = NerColors.Primary,
            alpha = 0.035f
        )

        AnimatedContent(
            targetState = activeDestination,
            transitionSpec = {
                if (targetState == SpringboardDestination.Home) {
                    slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                } else {
                    slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                }
            },
            label = "springboardNav"
        ) { dest ->
            when (dest) {
                SpringboardDestination.Home -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Top Authentic Woven Textile Ribbon
                            NerWovenRibbon(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp),
                                height = 14.dp,
                                primaryColor = NerColors.Primary,
                                secondaryColor = NerColors.Secondary,
                                accentColor = NerColors.Marigold
                            )

                            // Status Header Bar (Clock & Live Satellite Telemetry status)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = currentTimeStr,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NerColors.Charcoal
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(percent = 50))
                                        .background(if (isBroadcasting) NerColors.SecondaryTint else NerColors.NeutralSoft)
                                        .padding(horizontal = 10.dp, vertical = 3.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (isBroadcasting) NerColors.Secondary else NerColors.NeutralMedium)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isBroadcasting)
                                            MultilingualManager.tr("beacon_live", selectedLanguageCode)
                                        else
                                            MultilingualManager.tr("beacon_standby", selectedLanguageCode),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isBroadcasting) NerColors.SecondaryDark else NerColors.NeutralMedium
                                    )
                                }
                            }

                            // App Title & Patient Info Card (Soft 20dp Card adhering to kit)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = NerColors.SurfaceWhite),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                border = BorderStroke(1.dp, NerColors.NeutralBorder)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Image(
                                            painter = painterResource(id = R.drawable.smaran_logo),
                                            contentDescription = "Smaran Logo",
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(NerColors.PrimaryTint)
                                                .border(1.dp, NerColors.Primary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = MultilingualManager.tr("app_title", selectedLanguageCode),
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 17.sp,
                                                color = NerColors.Charcoal,
                                                letterSpacing = 0.3.sp
                                            )
                                            Text(
                                                userEmail,
                                                fontSize = 12.sp,
                                                color = NerColors.NeutralMedium
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = onSignOut,
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(NerColors.NeutralSoft)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                            contentDescription = "Sign Out",
                                            tint = NerColors.Charcoal,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // 2-Column Responsive Assistive Grid with Colorful Theme Badges
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                contentPadding = PaddingValues(top = 4.dp, bottom = 8.dp)
                            ) {
                                // 1. GPS Beacon Tile (Forest Green Theme)
                                item {
                                    IosSpringboardCard(
                                        title = MultilingualManager.tr("tile_gps_title", selectedLanguageCode),
                                        icon = Icons.Default.LocationOn,
                                        iconBgColor = NerColors.Secondary,
                                        statusSubtitle = if (isBroadcasting)
                                            MultilingualManager.tr("tile_gps_sub_broadcasting", selectedLanguageCode)
                                        else
                                            MultilingualManager.tr("tile_gps_sub_standby", selectedLanguageCode),
                                        badgeText = if (isBroadcasting) "Live" else null,
                                        badgeIcon = Icons.Default.Sensors,
                                        badgeColor = NerColors.Secondary,
                                        onClick = { activeDestination = SpringboardDestination.Beacon }
                                    )
                                }

                                // 2. Brain Games Hub Tile (Vibrant Terracotta Orange Theme)
                                item {
                                    IosSpringboardCard(
                                        title = MultilingualManager.tr("tile_games_title", selectedLanguageCode),
                                        icon = Icons.Default.SportsEsports,
                                        iconBgColor = NerColors.Primary,
                                        badgeText = "Daily",
                                        badgeColor = NerColors.Primary,
                                        statusSubtitle = MultilingualManager.tr("tile_games_sub", selectedLanguageCode),
                                        onClick = { activeDestination = SpringboardDestination.Exercises }
                                    )
                                }

                                // 3. Cognitive Health Tile (Royal Cobalt Blue Theme with CPS Score)
                                item {
                                    val scoreText = currentAssessment?.cpsScore?.toInt()?.let { "$it CPS" }
                                    IosSpringboardCard(
                                        title = MultilingualManager.tr("tile_score_title", selectedLanguageCode),
                                        icon = Icons.Default.Psychology,
                                        iconBgColor = NerColors.Tertiary,
                                        badgeText = scoreText ?: "--",
                                        badgeColor = if (currentAssessment != null) NerColors.Tertiary else NerColors.NeutralMedium,
                                        statusSubtitle = if (currentAssessment != null)
                                            MultilingualManager.tr("tile_score_sub_tested", selectedLanguageCode)
                                        else
                                            MultilingualManager.tr("tile_score_sub_untested", selectedLanguageCode),
                                        onClick = { activeDestination = SpringboardDestination.Health }
                                    )
                                }

                                // 4. Safety & Hazard Alerts Tile (Crimson Red Theme)
                                item {
                                    IosSpringboardCard(
                                        title = MultilingualManager.tr("tile_safety_title", selectedLanguageCode),
                                        icon = Icons.Default.Shield,
                                        iconBgColor = NerColors.Crimson,
                                        badgeText = "Safe",
                                        badgeIcon = Icons.Default.Security,
                                        badgeColor = NerColors.Crimson,
                                        statusSubtitle = MultilingualManager.tr("tile_safety_sub", selectedLanguageCode),
                                        onClick = { activeDestination = SpringboardDestination.Safety }
                                    )
                                }

                                // 5. Languages & Voice Guidance Tile (Marigold Gold Theme)
                                item {
                                    IosSpringboardCard(
                                        title = MultilingualManager.tr("tile_voice_title", selectedLanguageCode),
                                        icon = Icons.Default.Translate,
                                        iconBgColor = NerColors.Marigold,
                                        statusSubtitle = MultilingualManager.tr("tile_voice_sub", selectedLanguageCode),
                                        onClick = { activeDestination = SpringboardDestination.Voice }
                                    )
                                }

                                // 6. Memory Vault Tile (Deep Plum Maroon Theme)
                                item {
                                    IosSpringboardCard(
                                        title = MultilingualManager.tr("tile_memory_title", selectedLanguageCode),
                                        icon = Icons.Default.CollectionsBookmark,
                                        iconBgColor = NerColors.PlumMaroon,
                                        statusSubtitle = MultilingualManager.tr("tile_memory_sub", selectedLanguageCode),
                                        onClick = { activeDestination = SpringboardDestination.Memory }
                                    )
                                }
                            }
                        }

                        // Floating Bottom Navigation Dock from Reference Kit
                        NerBottomDock(
                            selectedIndex = 0,
                            onTabSelected = { index ->
                                when (index) {
                                    0 -> activeDestination = SpringboardDestination.Home
                                    1 -> activeDestination = SpringboardDestination.Exercises
                                    2 -> activeDestination = SpringboardDestination.Health
                                    3 -> activeDestination = SpringboardDestination.Safety
                                }
                            }
                        )
                    }
                }

                SpringboardDestination.Beacon -> {
                    BeaconTrackerPanel(
                        userEmail = userEmail,
                        selectedLanguageCode = selectedLanguageCode,
                        onBack = { activeDestination = SpringboardDestination.Home }
                    )
                }

                SpringboardDestination.Exercises -> {
                    BrainExerciseGamePanel(
                        userEmail = userEmail,
                        selectedLanguageCode = selectedLanguageCode,
                        onAssessmentUpdated = { updated -> currentAssessment = updated },
                        onBack = { activeDestination = SpringboardDestination.Home }
                    )
                }

                SpringboardDestination.Health -> {
                    CognitiveHealthPanel(
                        assessment = currentAssessment,
                        selectedLanguageCode = selectedLanguageCode,
                        onLaunchGame = { activeDestination = SpringboardDestination.Exercises },
                        onBack = { activeDestination = SpringboardDestination.Home }
                    )
                }

                SpringboardDestination.Safety -> {
                    SafetyAlertsPanel(
                        assessment = currentAssessment,
                        selectedLanguageCode = selectedLanguageCode,
                        onBack = { activeDestination = SpringboardDestination.Home }
                    )
                }

                SpringboardDestination.Voice -> {
                    VoiceLanguagePanel(
                        selectedLanguageCode = selectedLanguageCode,
                        onLanguageSelected = { code ->
                            selectedLanguageCode = code
                            prefs.edit().putString("selected_language", code).commit()
                        },
                        onBack = { activeDestination = SpringboardDestination.Home }
                    )
                }

                SpringboardDestination.Memory -> {
                    MemoryVaultPanel(
                        selectedLanguageCode = selectedLanguageCode,
                        onBack = { activeDestination = SpringboardDestination.Home }
                    )
                }
            }
        }

        // Vibrant Popping Colors Alarm Reminder Dialog adhering to design kit
        if (isAlarmPopping) {
            AlertDialog(
                onDismissRequest = {
                    // Do NOT dismiss alarm on outside touch
                },
                properties = androidx.compose.ui.window.DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                ),
                title = {
                    Column {
                        NerWovenRibbon(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            height = 10.dp,
                            primaryColor = NerColors.Primary,
                            secondaryColor = NerColors.Secondary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🌟 ", fontSize = 24.sp)
                            Text(
                                text = MultilingualManager.tr("alarm_title", selectedLanguageCode),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 19.sp,
                                color = NerColors.Primary
                            )
                        }
                    }
                },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            listOf(
                                NerColors.Primary,
                                NerColors.Secondary,
                                NerColors.Tertiary,
                                NerColors.Marigold
                            ).forEach { col ->
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 5.dp)
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(col)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = MultilingualManager.tr("alarm_desc", selectedLanguageCode),
                            fontSize = 14.sp,
                            color = NerColors.Charcoal,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                },
                confirmButton = {
                    NerPillButton(
                        text = MultilingualManager.tr("alarm_btn_play", selectedLanguageCode),
                        icon = Icons.Default.SportsEsports,
                        hierarchy = NerButtonHierarchy.Primary,
                        containerColor = NerColors.Primary,
                        onClick = {
                            GameReminderManager.dismissAlarm(context)
                            isAlarmPopping = false
                            activeDestination = SpringboardDestination.Exercises
                        }
                    )
                },
                dismissButton = {
                    NerPillButton(
                        text = MultilingualManager.tr("alarm_btn_snooze", selectedLanguageCode),
                        hierarchy = NerButtonHierarchy.Secondary,
                        onClick = {
                            GameReminderManager.dismissAlarm(context)
                            GameReminderManager.scheduleNextAlarm(context, 10L)
                            isAlarmPopping = false
                        }
                    )
                },
                containerColor = NerColors.SurfaceWhite,
                shape = RoundedCornerShape(24.dp)
            )
        }
    }
}
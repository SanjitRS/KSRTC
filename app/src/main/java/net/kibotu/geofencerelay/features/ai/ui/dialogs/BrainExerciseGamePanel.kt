package net.kibotu.geofencerelay.features.ai.ui.dialogs

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.kibotu.geofencerelay.features.ai.engine.CpsEngine
import net.kibotu.geofencerelay.features.ai.localization.MultilingualManager
import net.kibotu.geofencerelay.features.ai.model.CpsAssessmentResult
import net.kibotu.geofencerelay.features.ai.model.GameSessionTelemetry
import net.kibotu.geofencerelay.features.ai.reminder.GameReminderManager
import net.kibotu.geofencerelay.features.ai.risk.CognitiveAnomalyDetector
import net.kibotu.geofencerelay.features.ai.service.SmaranAiClient
import net.kibotu.geofencerelay.features.ai.ui.components.IosBackPillButton
import net.kibotu.geofencerelay.features.ai.ui.theme.GoogleColors
import net.kibotu.geofencerelay.model.GameSessionRecord
import net.kibotu.geofencerelay.relay.CognitiveTelemetryManager
import net.kibotu.geofencerelay.features.ai.ui.theme.IosColors
import net.kibotu.geofencerelay.ui.theme.*
import net.kibotu.geofencerelay.features.ai.ui.theme.IosDimensions

enum class ActiveGameMode {
    HUB,
    MEMORY_MATCHING,
    PATTERN_RECOGNITION,
    STROOP_CHALLENGE,
    TRAIL_MAKING
}

data class MatchSymbol(
    val id: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val tint: Color,
    val name: String
)

data class CardItem(
    val id: Int,
    val symbolItem: MatchSymbol,
    var isFlipped: Boolean = false,
    var isMatched: Boolean = false
)

/**
 * Full-Screen Cognitive Games Hub supporting 4 rich multi-round clinical games:
 * 1. Jumbo Memory Match (Multi-round: Round 1 (8 cards) -> Round 2 (12 cards))
 * 2. Cultural Pattern & Sequence Recall (Levels 1-3 with instant touch glow and audio feedback)
 * 3. Color-Word Stroop Inhibition Challenge (10 rounds with jumbo conflict text)
 * 4. Ascending Number Trail Making (Rounds 1-2 with pulsing target locator)
 */
@Composable
fun BrainExerciseGamePanel(
    userEmail: String = "",
    selectedLanguageCode: String,
    onAssessmentUpdated: (CpsAssessmentResult) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var activeMode by remember { mutableStateOf(ActiveGameMode.HUB) }

    when (activeMode) {
        ActiveGameMode.HUB -> {
            GameHubSelectionView(
                selectedLanguageCode = selectedLanguageCode,
                onSelectGame = { mode -> activeMode = mode },
                onBack = onBack
            )
        }
        ActiveGameMode.MEMORY_MATCHING -> {
            FullScreenMemoryMatchingGameView(
                userEmail = userEmail,
                selectedLanguageCode = selectedLanguageCode,
                onAssessmentUpdated = { res ->
                    GameReminderManager.recordGamePlayed(context)
                    onAssessmentUpdated(res)
                },
                onBack = { activeMode = ActiveGameMode.HUB }
            )
        }
        ActiveGameMode.PATTERN_RECOGNITION -> {
            FullScreenPatternSequenceGameView(
                userEmail = userEmail,
                selectedLanguageCode = selectedLanguageCode,
                onAssessmentUpdated = { res ->
                    GameReminderManager.recordGamePlayed(context)
                    onAssessmentUpdated(res)
                },
                onBack = { activeMode = ActiveGameMode.HUB }
            )
        }
        ActiveGameMode.STROOP_CHALLENGE -> {
            ColorStroopChallengeGameView(
                userEmail = userEmail,
                selectedLanguageCode = selectedLanguageCode,
                onAssessmentUpdated = { res ->
                    GameReminderManager.recordGamePlayed(context)
                    onAssessmentUpdated(res)
                },
                onBack = { activeMode = ActiveGameMode.HUB }
            )
        }
        ActiveGameMode.TRAIL_MAKING -> {
            AscendingTrailMakingGameView(
                userEmail = userEmail,
                selectedLanguageCode = selectedLanguageCode,
                onAssessmentUpdated = { res ->
                    GameReminderManager.recordGamePlayed(context)
                    onAssessmentUpdated(res)
                },
                onBack = { activeMode = ActiveGameMode.HUB }
            )
        }
    }
}

/**
 * Hub Screen with 4 Game Options & Reminder Interval Configuration.
 */
@Composable
private fun GameHubSelectionView(
    selectedLanguageCode: String,
    onSelectGame: (ActiveGameMode) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var reminderInterval by remember { mutableStateOf(GameReminderManager.getReminderInterval(context)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NerColors.CanvasWarm)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        // Top Authentic Woven Textile Ribbon
        NerWovenRibbon(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            height = 14.dp,
            primaryColor = NerColors.Primary,
            secondaryColor = NerColors.Secondary,
            accentColor = NerColors.Marigold
        )
            Text(
                text = MultilingualManager.tr("games_hub_title", selectedLanguageCode),
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = NerColors.Charcoal
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = MultilingualManager.tr("games_hub_sub", selectedLanguageCode),
                fontSize = 12.sp,
                color = NerColors.NeutralMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Game 1: Memory Match
            GameSelectionCard(
                title = MultilingualManager.tr("game1_name", selectedLanguageCode),
                desc = MultilingualManager.tr("game1_desc", selectedLanguageCode),
                icon = Icons.Default.Style,
                color = GoogleColors.Blue,
                onClick = { onSelectGame(ActiveGameMode.MEMORY_MATCHING) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Game 2: 6-Pad Pattern & Sequence Logic
            GameSelectionCard(
                title = MultilingualManager.tr("game2_name", selectedLanguageCode),
                desc = MultilingualManager.tr("game2_desc", selectedLanguageCode),
                icon = Icons.Default.Extension,
                color = GoogleColors.Green,
                onClick = { onSelectGame(ActiveGameMode.PATTERN_RECOGNITION) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Game 3: Color-Word Stroop Challenge
            GameSelectionCard(
                title = MultilingualManager.tr("game3_title", selectedLanguageCode),
                desc = MultilingualManager.tr("game3_desc", selectedLanguageCode),
                icon = Icons.Default.ColorLens,
                color = GoogleColors.Red,
                onClick = { onSelectGame(ActiveGameMode.STROOP_CHALLENGE) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Game 4: Ascending Number Trail Making
            GameSelectionCard(
                title = MultilingualManager.tr("game4_title", selectedLanguageCode),
                desc = MultilingualManager.tr("game4_desc", selectedLanguageCode),
                icon = Icons.Default.Pin,
                color = GoogleColors.Yellow,
                onClick = { onSelectGame(ActiveGameMode.TRAIL_MAKING) }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Game Reminder Interval Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = NerColors.SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(NerColors.NeutralBorder),
                    width = 1.dp
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Alarm, contentDescription = null, tint = GoogleColors.Red, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(MultilingualManager.tr("lbl_reminder_interval", selectedLanguageCode), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = NerColors.Charcoal)
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(MultilingualManager.tr("lbl_reminder_interval_desc", selectedLanguageCode), fontSize = 11.sp, color = NerColors.NeutralMedium)

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(
                            Pair(1L, "1 min (Test)"),
                            Pair(60L, "1 hr"),
                            Pair(120L, "2 hrs"),
                            Pair(240L, "4 hrs")
                        ).forEach { pair ->
                            val isSel = reminderInterval == pair.first
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) GoogleColors.Blue else NerColors.NeutralSoft)
                                    .clickable {
                                        reminderInterval = pair.first
                                        GameReminderManager.setReminderInterval(context, pair.first)
                                        android.widget.Toast.makeText(
                                            context,
                                            "⏰ Reminder interval set to ${pair.second}. Alarm is armed!",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = pair.second,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSel) Color.White else NerColors.Charcoal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Instant Test Button (3 seconds)
                    Button(
                        onClick = {
                            GameReminderManager.triggerTestAlarmInSeconds(context, 3)
                            android.widget.Toast.makeText(
                                context,
                                "🔔 Alarm will trigger in 3 seconds! Turn screen OFF or close app now.",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoogleColors.Red),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = MultilingualManager.tr("btn_test_alarm", selectedLanguageCode),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        IosBackPillButton(
            label = MultilingualManager.tr("btn_back", selectedLanguageCode),
            onClick = onBack
        )
    }
}

@Composable
private fun GameSelectionCard(
    title: String,
    desc: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = NerColors.SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(NerColors.NeutralBorder),
            width = 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NerColors.Charcoal)
                Spacer(modifier = Modifier.height(2.dp))
                Text(desc, fontSize = 11.sp, color = NerColors.NeutralMedium, lineHeight = 15.sp)
            }

            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = NerColors.NeutralMedium)
        }
    }
}

/**
 * Game 1: Multi-Round Jumbo Memory Matching Game (Round 1: 4 pairs, Round 2: 6 pairs)
 */
@Composable
private fun FullScreenMemoryMatchingGameView(
    userEmail: String = "",
    selectedLanguageCode: String,
    onAssessmentUpdated: (CpsAssessmentResult) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val symbolsPool = remember {
        listOf(
            MatchSymbol(1, Icons.Default.Favorite, GoogleColors.Red, "Heart"),
            MatchSymbol(2, Icons.Default.Star, GoogleColors.Yellow, "Star"),
            MatchSymbol(3, Icons.Default.WbSunny, GoogleColors.Blue, "Sun"),
            MatchSymbol(4, Icons.Default.Pets, GoogleColors.Green, "Pet"),
            MatchSymbol(5, Icons.Default.LocalFlorist, Color(0xFFFF4081), "Flower"),
            MatchSymbol(6, Icons.Default.DirectionsCar, Color(0xFF00BCD4), "Car"),
            MatchSymbol(7, Icons.Default.MusicNote, Color(0xFFAB47BC), "Music"),
            MatchSymbol(8, Icons.Default.Park, Color(0xFF4CAF50), "Tree")
        )
    }
    var currentRound by remember { mutableStateOf(1) }
    val maxRounds = 2
    val pairsForRound = if (currentRound == 1) 4 else 6

    var cards by remember(currentRound) {
        val picked = symbolsPool.shuffled().take(pairsForRound)
        val deck = (picked + picked).shuffled().mapIndexed { idx, s ->
            CardItem(id = idx, symbolItem = s)
        }
        mutableStateOf(deck)
    }

    var flippedIndices by remember { mutableStateOf<List<Int>>(emptyList()) }
    var totalAttempts by remember { mutableStateOf(0) }
    var totalErrors by remember { mutableStateOf(0) }
    var isGameFinished by remember { mutableStateOf(false) }
    val startTime = remember { System.currentTimeMillis() }
    var completionMessage by remember { mutableStateOf("") }
    var isBusyChecking by remember { mutableStateOf(false) }

    fun resetWholeGame() {
        currentRound = 1
        val picked = symbolsPool.shuffled().take(4)
        cards = (picked + picked).shuffled().mapIndexed { idx, s -> CardItem(id = idx, symbolItem = s) }
        flippedIndices = emptyList()
        totalAttempts = 0
        totalErrors = 0
        isGameFinished = false
        completionMessage = ""
        isBusyChecking = false
    }

    LaunchedEffect(flippedIndices) {
        if (flippedIndices.size == 2) {
            isBusyChecking = true
            totalAttempts++
            val firstIdx = flippedIndices[0]
            val secondIdx = flippedIndices[1]

            if (cards[firstIdx].symbolItem.id == cards[secondIdx].symbolItem.id) {
                delay(200)
                val updatedCards = cards.mapIndexed { idx, card ->
                    if (idx == firstIdx || idx == secondIdx) card.copy(isMatched = true, isFlipped = true)
                    else card
                }
                cards = updatedCards

                val allMatched = updatedCards.all { it.isMatched }
                if (allMatched) {
                    if (currentRound < maxRounds) {
                        delay(500)
                        val nextRnd = currentRound + 1
                        val nextPairs = if (nextRnd == 1) 4 else 6
                        val nextPicked = symbolsPool.shuffled().take(nextPairs)
                        cards = (nextPicked + nextPicked).shuffled().mapIndexed { idx, s ->
                            CardItem(id = idx, symbolItem = s)
                        }
                        currentRound = nextRnd
                        flippedIndices = emptyList()
                        isBusyChecking = false
                    } else {
                        val durationMs = System.currentTimeMillis() - startTime
                        val accuracy = if (totalAttempts > 0) ((4 + 6).toDouble() / totalAttempts).coerceIn(0.0, 1.0) else 1.0
                        val telemetry = GameSessionTelemetry(
                            gameType = "memory_matching",
                            accuracy = accuracy,
                            responseTimeMs = durationMs,
                            attempts = totalAttempts,
                            errors = totalErrors,
                            completionRate = 1.0
                        )
                        val result = CpsEngine.analyzeSession(telemetry, selectedLanguageCode)
                        completionMessage = result.encouragementPrompt
                        CognitiveAnomalyDetector.recordSessionToHistory(context, telemetry.accuracy, telemetry.responseTimeMs, telemetry.errors)
                        scope.launch {
                            SmaranAiClient.predictDifficulty(
                                context = context,
                                gameType = telemetry.gameType,
                                currentDifficulty = result.hiddenDifficulty,
                                accuracy = telemetry.accuracy,
                                completionRate = telemetry.completionRate,
                                responseTimeMs = telemetry.responseTimeMs,
                                errors = telemetry.errors,
                                hintsUsed = telemetry.hintsUsed
                            )
                        }
                        val session = GameSessionRecord(
                            gameId = "MEMORY_MATCH",
                            gameName = "Jumbo Memory Match",
                            score = (accuracy * 100).toInt(),
                            roundsCompleted = 2,
                            accuracyPercent = accuracy * 100.0,
                            averageLatencyMs = durationMs,
                            errors = totalErrors,
                            timestamp = System.currentTimeMillis()
                        )
                        CognitiveTelemetryManager.recordGameAndBroadcast(context, userEmail, result, session)
                        Toast.makeText(context, "✅ CPS Synced to Caregiver! (${result.cpsScore.toInt()} pts)", Toast.LENGTH_SHORT).show()
                        onAssessmentUpdated(result)
                        isGameFinished = true
                        flippedIndices = emptyList()
                        isBusyChecking = false
                        MultilingualManager.speak(result.encouragementPrompt, selectedLanguageCode)
                    }
                } else {
                    flippedIndices = emptyList()
                    isBusyChecking = false
                }
            } else {
                totalErrors++
                delay(700)
                cards = cards.mapIndexed { idx, card ->
                    if (idx == firstIdx || idx == secondIdx) card.copy(isFlipped = false)
                    else card
                }
                flippedIndices = emptyList()
                isBusyChecking = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NerColors.CanvasWarm)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = MultilingualManager.tr("game1_name", selectedLanguageCode),
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = NerColors.Charcoal
            )
            Text(
                text = "${MultilingualManager.tr("lbl_round", selectedLanguageCode)} $currentRound ${MultilingualManager.tr("lbl_of", selectedLanguageCode)} $maxRounds • ${cards.count { it.isMatched } / 2} / $pairsForRound ${MultilingualManager.tr("game1_matched_status", selectedLanguageCode)}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = GoogleColors.Blue
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (isGameFinished) {
                VictoryCard(
                    message = completionMessage,
                    selectedLanguageCode = selectedLanguageCode,
                    onPlayAgain = { resetWholeGame() }
                )
            } else {
                val cols = if (currentRound == 1) 2 else 3
                LazyVerticalGrid(
                    columns = GridCells.Fixed(cols),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(cards) { idx, card ->
                        val cardBg by animateColorAsState(
                            targetValue = if (card.isMatched) GoogleColors.Green.copy(alpha = 0.25f)
                            else if (card.isFlipped) NerColors.NeutralSoft
                            else NerColors.SurfaceWhite,
                            animationSpec = tween(200),
                            label = "cardBg"
                        )

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (currentRound == 1) 120.dp else 96.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .clickable(enabled = !card.isFlipped && !card.isMatched && !isBusyChecking) {
                                    if (flippedIndices.size < 2) {
                                        cards = cards.mapIndexed { i, c ->
                                            if (i == idx) c.copy(isFlipped = true) else c
                                        }
                                        flippedIndices = flippedIndices + idx
                                    }
                                },
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(
                                    if (card.isMatched) GoogleColors.Green else NerColors.NeutralBorder
                                ),
                                width = 1.5.dp
                            )
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (card.isFlipped || card.isMatched) {
                                    Icon(
                                        imageVector = card.symbolItem.icon,
                                        contentDescription = card.symbolItem.name,
                                        tint = card.symbolItem.tint,
                                        modifier = Modifier.size(if (currentRound == 1) 56.dp else 44.dp)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(GoogleColors.Blue.copy(alpha = 0.16f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = GoogleColors.Blue,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        IosBackPillButton(
            label = MultilingualManager.tr("btn_back", selectedLanguageCode),
            onClick = onBack
        )
    }
}

/**
 * Game 2: Multi-Round Cultural Pattern & Sequence Recall with instant tap glow feedback
 */
data class PatternPadItem(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color,
    val label: String
)

@Composable
private fun FullScreenPatternSequenceGameView(
    userEmail: String = "",
    selectedLanguageCode: String,
    onAssessmentUpdated: (CpsAssessmentResult) -> Unit,
    onBack: () -> Unit
) {
    val items = remember {
        listOf(
            PatternPadItem(Icons.Default.Star, GoogleColors.Yellow, "Star"),
            PatternPadItem(Icons.Default.Favorite, GoogleColors.Red, "Heart"),
            PatternPadItem(Icons.Default.Eco, GoogleColors.Green, "Nature"),
            PatternPadItem(Icons.Default.WbSunny, GoogleColors.Blue, "Sun"),
            PatternPadItem(Icons.Default.MusicNote, Color(0xFFAB47BC), "Music"),
            PatternPadItem(Icons.Default.DirectionsCar, Color(0xFF00BCD4), "Car")
        )
    }

    var currentLevel by remember { mutableStateOf(1) }
    val maxLevels = 3
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Level 1: 3 steps; Level 2: 4 steps; Level 3: 5 steps
    fun generateSequence(level: Int): List<Int> {
        val len = when (level) {
            1 -> 3
            2 -> 4
            else -> 5
        }
        return (1..len).map { (0..5).random() }
    }

    var sequence by remember { mutableStateOf(generateSequence(1)) }
    var highlightedIndex by remember { mutableStateOf<Int?>(null) }
    var userTappedHighlightIdx by remember { mutableStateOf<Int?>(null) }
    var isShowingSequence by remember { mutableStateOf(true) }
    var userTappedSteps by remember { mutableStateOf<List<Int>>(emptyList()) }
    var isFinished by remember { mutableStateOf(false) }
    var completionMessage by remember { mutableStateOf("") }
    var feedbackText by remember { mutableStateOf(MultilingualManager.tr("game2_watch_glow", selectedLanguageCode)) }
    val startTime = remember { System.currentTimeMillis() }

    fun playSequenceForLevel(lvl: Int) {
        sequence = generateSequence(lvl)
        highlightedIndex = null
        userTappedHighlightIdx = null
        userTappedSteps = emptyList()
        feedbackText = MultilingualManager.tr("game2_watch_glow", selectedLanguageCode)
        isShowingSequence = true
    }

    LaunchedEffect(isShowingSequence, sequence) {
        if (isShowingSequence) {
            delay(500)
            for (step in sequence) {
                highlightedIndex = step
                delay(550)
                highlightedIndex = null
                delay(200)
            }
            isShowingSequence = false
            feedbackText = MultilingualManager.tr("game2_your_turn_prompt", selectedLanguageCode)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NerColors.CanvasWarm)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = MultilingualManager.tr("game2_name", selectedLanguageCode),
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = NerColors.Charcoal
            )
            Text(
                text = "${MultilingualManager.tr("lbl_level", selectedLanguageCode)} $currentLevel ${MultilingualManager.tr("lbl_of", selectedLanguageCode)} $maxLevels • $feedbackText",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isShowingSequence) GoogleColors.Red else GoogleColors.Blue,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Step Progress Dots
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                sequence.indices.forEach { stepIdx ->
                    val isDone = stepIdx < userTappedSteps.size
                    val isCurrent = stepIdx == userTappedSteps.size && !isShowingSequence
                    val dotColor = when {
                        isDone -> GoogleColors.Green
                        isCurrent -> GoogleColors.Blue
                        else -> Color.LightGray
                    }
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (isCurrent) 12.dp else 10.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isFinished) {
                VictoryCard(
                    message = completionMessage,
                    selectedLanguageCode = selectedLanguageCode,
                    onPlayAgain = {
                        currentLevel = 1
                        isFinished = false
                        playSequenceForLevel(1)
                    }
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(items) { idx, item ->
                        val isLitBySystem = highlightedIndex == idx
                        val isLitByUser = userTappedHighlightIdx == idx
                        val isLit = isLitBySystem || isLitByUser

                        val cardBg by animateColorAsState(
                            targetValue = if (isLit) item.color else NerColors.SurfaceWhite,
                            animationSpec = tween(150),
                            label = "padBg"
                        )

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .clickable(enabled = !isShowingSequence) {
                                    // User tap feedback!
                                    userTappedHighlightIdx = idx
                                    coroutineScope.launch {
                                        delay(220)
                                        userTappedHighlightIdx = null
                                    }

                                    val nextExpected = sequence[userTappedSteps.size]
                                    if (idx == nextExpected) {
                                        val newSteps = userTappedSteps + idx
                                        userTappedSteps = newSteps
                                        if (newSteps.size == sequence.size) {
                                            if (currentLevel < maxLevels) {
                                                coroutineScope.launch {
                                                    feedbackText = "${MultilingualManager.tr("lbl_level", selectedLanguageCode)} $currentLevel: ${MultilingualManager.tr("game2_level_cleared_msg", selectedLanguageCode)}"
                                                    delay(700)
                                                    currentLevel++
                                                    playSequenceForLevel(currentLevel)
                                                }
                                            } else {
                                                val elapsed = System.currentTimeMillis() - startTime
                                                val telemetry = GameSessionTelemetry(
                                                    gameType = "pattern_recognition",
                                                    accuracy = 1.0,
                                                    responseTimeMs = elapsed,
                                                    attempts = sequence.size,
                                                    errors = 0,
                                                    completionRate = 1.0
                                                )
                                                val result = CpsEngine.analyzeSession(telemetry, selectedLanguageCode)
                                                completionMessage = result.encouragementPrompt
                                                CognitiveAnomalyDetector.recordSessionToHistory(context, telemetry.accuracy, telemetry.responseTimeMs, telemetry.errors)
                                                coroutineScope.launch {
                                                    SmaranAiClient.predictDifficulty(
                                                        context = context,
                                                        gameType = telemetry.gameType,
                                                        currentDifficulty = result.hiddenDifficulty,
                                                        accuracy = telemetry.accuracy,
                                                        completionRate = telemetry.completionRate,
                                                        responseTimeMs = telemetry.responseTimeMs,
                                                        errors = telemetry.errors,
                                                        hintsUsed = telemetry.hintsUsed
                                                    )
                                                }
                                                val session = GameSessionRecord(
                                                    gameId = "PATTERN_RECALL",
                                                    gameName = "Pattern Sequence Recall",
                                                    score = 100,
                                                    roundsCompleted = currentLevel,
                                                    accuracyPercent = 100.0,
                                                    averageLatencyMs = elapsed,
                                                    errors = 0,
                                                    timestamp = System.currentTimeMillis()
                                                )
                                                CognitiveTelemetryManager.recordGameAndBroadcast(context, userEmail, result, session)
                                                Toast.makeText(context, "✅ CPS Synced to Caregiver! (${result.cpsScore.toInt()} pts)", Toast.LENGTH_SHORT).show()
                                                onAssessmentUpdated(result)
                                                isFinished = true
                                                MultilingualManager.speak(result.encouragementPrompt, selectedLanguageCode)
                                            }
                                        }
                                    } else {
                                        // User missed step - replay sequence cleanly!
                                        coroutineScope.launch {
                                            feedbackText = MultilingualManager.tr("game2_missed_msg", selectedLanguageCode)
                                            delay(700)
                                            userTappedSteps = emptyList()
                                            isShowingSequence = true
                                        }
                                    }
                                },
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isLit) 8.dp else 2.dp),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(
                                    if (isLit) item.color else NerColors.NeutralBorder
                                ),
                                width = if (isLit) 3.dp else 1.dp
                            )
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = if (isLit) Color.White else item.color,
                                    modifier = Modifier.size(54.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        IosBackPillButton(
            label = MultilingualManager.tr("btn_back", selectedLanguageCode),
            onClick = onBack
        )
    }
}

/**
 * Game 3: Color-Word Stroop Cognitive Inhibition Challenge (10 Full Rounds with Jumbo Text)
 */
@Composable
private fun ColorStroopChallengeGameView(
    userEmail: String = "",
    selectedLanguageCode: String,
    onAssessmentUpdated: (CpsAssessmentResult) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val colorOptions = listOf(
        Triple("BLUE", "Blue / नीला", GoogleColors.Blue),
        Triple("RED", "Red / लाल", GoogleColors.Red),
        Triple("GREEN", "Green / हरा", GoogleColors.Green),
        Triple("YELLOW", "Yellow / पीला", GoogleColors.Yellow)
    )

    var currentWordIndex by remember { mutableStateOf(0) }
    var currentInkColorIndex by remember { mutableStateOf(1) } // Conflict!
    var scoreCount by remember { mutableStateOf(0) }
    val totalRounds = 10
    var currentRound by remember { mutableStateOf(1) }
    var isFinished by remember { mutableStateOf(false) }
    var completionMessage by remember { mutableStateOf("") }
    val startTime = remember { System.currentTimeMillis() }

    fun nextStroopQuestion() {
        if (currentRound >= totalRounds) {
            val elapsed = System.currentTimeMillis() - startTime
            val accuracy = (scoreCount.toDouble() / totalRounds).coerceIn(0.0, 1.0)
            val telemetry = GameSessionTelemetry(
                gameType = "stroop_test",
                accuracy = accuracy,
                responseTimeMs = elapsed,
                attempts = totalRounds,
                errors = totalRounds - scoreCount,
                completionRate = 1.0
            )
            val result = CpsEngine.analyzeSession(telemetry, selectedLanguageCode)
            completionMessage = result.encouragementPrompt
            CognitiveAnomalyDetector.recordSessionToHistory(context, telemetry.accuracy, telemetry.responseTimeMs, telemetry.errors)
            coroutineScope.launch {
                SmaranAiClient.predictDifficulty(
                    context = context,
                    gameType = telemetry.gameType,
                    currentDifficulty = result.hiddenDifficulty,
                    accuracy = telemetry.accuracy,
                    completionRate = telemetry.completionRate,
                    responseTimeMs = telemetry.responseTimeMs,
                    errors = telemetry.errors,
                    hintsUsed = telemetry.hintsUsed
                )
            }
            val session = GameSessionRecord(
                gameId = "STROOP",
                gameName = "Color-Word Stroop Challenge",
                score = scoreCount * 10,
                roundsCompleted = totalRounds,
                accuracyPercent = accuracy * 100.0,
                averageLatencyMs = elapsed,
                errors = totalRounds - scoreCount,
                timestamp = System.currentTimeMillis()
            )
            CognitiveTelemetryManager.recordGameAndBroadcast(context, userEmail, result, session)
            Toast.makeText(context, "✅ CPS Synced to Caregiver! (${result.cpsScore.toInt()} pts)", Toast.LENGTH_SHORT).show()
            onAssessmentUpdated(result)
            isFinished = true
            MultilingualManager.speak(result.encouragementPrompt, selectedLanguageCode)
        } else {
            currentRound++
            currentWordIndex = (0..3).random()
            var ink = (0..3).random()
            while (ink == currentWordIndex) {
                ink = (0..3).random()
            }
            currentInkColorIndex = ink
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
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = MultilingualManager.tr("game3_title", selectedLanguageCode),
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = NerColors.Charcoal
            )
            Text(
                text = "${MultilingualManager.tr("game3_tap_ink", selectedLanguageCode)} • ${MultilingualManager.tr("lbl_round", selectedLanguageCode)} $currentRound ${MultilingualManager.tr("lbl_of", selectedLanguageCode)} $totalRounds (${MultilingualManager.tr("lbl_score", selectedLanguageCode)}: $scoreCount)",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = GoogleColors.Blue,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            if (isFinished) {
                VictoryCard(
                    message = completionMessage,
                    selectedLanguageCode = selectedLanguageCode,
                    onPlayAgain = {
                        scoreCount = 0
                        currentRound = 1
                        isFinished = false
                    }
                )
            } else {
                // Word Display Box with Jumbo text and intentional color conflict!
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = NerColors.SurfaceWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(NerColors.NeutralBorder),
                        width = 1.5.dp
                    )
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = colorOptions[currentWordIndex].first,
                            color = colorOptions[currentInkColorIndex].third, // Intentional conflict!
                            fontSize = 54.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                    }
                }

            Spacer(modifier = Modifier.height(24.dp))

            Text(MultilingualManager.tr("game3_select_prompt", selectedLanguageCode), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NerColors.Charcoal)

                Spacer(modifier = Modifier.height(14.dp))

                // 4 Large Color Choice Buttons
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    itemsIndexed(colorOptions) { idx, opt ->
                        Button(
                            onClick = {
                                if (idx == currentInkColorIndex) {
                                    scoreCount++
                                }
                                nextStroopQuestion()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = opt.third),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(68.dp)
                        ) {
                            Text(opt.first, fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color.White)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        IosBackPillButton(
            label = MultilingualManager.tr("btn_back", selectedLanguageCode),
            onClick = onBack
        )
    }
}

/**
 * Game 4: Multi-Round Ascending Number Trail Making (Round 1: 1 to 8, Round 2: 1 to 10)
 */
@Composable
private fun AscendingTrailMakingGameView(
    userEmail: String = "",
    selectedLanguageCode: String,
    onAssessmentUpdated: (CpsAssessmentResult) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentRound by remember { mutableStateOf(1) }
    val maxRound = 2
    val targetMax = if (currentRound == 1) 8 else 10

    var nextExpectedNumber by remember(currentRound) { mutableStateOf(1) }
    var numbersPool by remember(currentRound) { mutableStateOf((1..targetMax).toList().shuffled()) }
    var isFinished by remember { mutableStateOf(false) }
    var completionMessage by remember { mutableStateOf("") }
    val startTime = remember { System.currentTimeMillis() }

    fun resetWholeGame() {
        currentRound = 1
        nextExpectedNumber = 1
        numbersPool = (1..8).toList().shuffled()
        isFinished = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NerColors.CanvasWarm)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = MultilingualManager.tr("game4_title", selectedLanguageCode),
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = NerColors.Charcoal
            )
            Text(
                text = "${MultilingualManager.tr("lbl_round", selectedLanguageCode)} $currentRound ${MultilingualManager.tr("lbl_of", selectedLanguageCode)} $maxRound • ${MultilingualManager.tr("game4_next_prompt", selectedLanguageCode)}: [$nextExpectedNumber]",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = GoogleColors.Blue
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isFinished) {
                VictoryCard(
                    message = completionMessage,
                    selectedLanguageCode = selectedLanguageCode,
                    onPlayAgain = { resetWholeGame() }
                )
            } else {
                val infiniteTransition = rememberInfiniteTransition(label = "targetPulse")
                val targetPulseScale by infiniteTransition.animateFloat(
                    initialValue = 1.0f,
                    targetValue = 1.10f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(500, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulse"
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(if (targetMax <= 8) 3 else 3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    itemsIndexed(numbersPool) { _, num ->
                        val isCompleted = num < nextExpectedNumber
                        val isTarget = num == nextExpectedNumber

                        val btnBg = when {
                            isCompleted -> GoogleColors.Green
                            isTarget -> GoogleColors.Blue
                            else -> NerColors.SurfaceWhite
                        }

                        val textColor = if (isCompleted || isTarget) Color.White else NerColors.Charcoal

                        Card(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .scale(if (isTarget) targetPulseScale else 1.0f)
                                .clip(CircleShape)
                                .clickable {
                                    if (num == nextExpectedNumber) {
                                        nextExpectedNumber++
                                        if (nextExpectedNumber > targetMax) {
                                            if (currentRound < maxRound) {
                                                currentRound++
                                            } else {
                                                val elapsed = System.currentTimeMillis() - startTime
                                                val telemetry = GameSessionTelemetry(
                                                    gameType = "trail_making",
                                                    accuracy = 1.0,
                                                    responseTimeMs = elapsed,
                                                    attempts = targetMax,
                                                    errors = 0,
                                                    completionRate = 1.0
                                                )
                                                val result = CpsEngine.analyzeSession(telemetry, selectedLanguageCode)
                                                completionMessage = result.encouragementPrompt
                                                CognitiveAnomalyDetector.recordSessionToHistory(context, telemetry.accuracy, telemetry.responseTimeMs, telemetry.errors)
                                                coroutineScope.launch {
                                                    SmaranAiClient.predictDifficulty(
                                                        context = context,
                                                        gameType = telemetry.gameType,
                                                        currentDifficulty = result.hiddenDifficulty,
                                                        accuracy = telemetry.accuracy,
                                                        completionRate = telemetry.completionRate,
                                                        responseTimeMs = telemetry.responseTimeMs,
                                                        errors = telemetry.errors,
                                                        hintsUsed = telemetry.hintsUsed
                                                    )
                                                }
                                                val session = GameSessionRecord(
                                                    gameId = "TRAIL_MAKING",
                                                    gameName = "Ascending Trail Making",
                                                    score = 95,
                                                    roundsCompleted = targetMax,
                                                    accuracyPercent = 100.0,
                                                    averageLatencyMs = elapsed,
                                                    errors = 0,
                                                    timestamp = System.currentTimeMillis()
                                                )
                                                CognitiveTelemetryManager.recordGameAndBroadcast(context, userEmail, result, session)
                                                Toast.makeText(context, "✅ CPS Synced to Caregiver! (${result.cpsScore.toInt()} pts)", Toast.LENGTH_SHORT).show()
                                                onAssessmentUpdated(result)
                                                isFinished = true
                                                MultilingualManager.speak(result.encouragementPrompt, selectedLanguageCode)
                                            }
                                        }
                                    }
                                },
                            shape = CircleShape,
                            colors = CardDefaults.cardColors(containerColor = btnBg),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isTarget) 6.dp else 2.dp),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(
                                    if (isTarget) GoogleColors.Yellow else NerColors.NeutralBorder
                                ),
                                width = if (isTarget) 3.dp else 1.dp
                            )
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "$num",
                                    fontSize = 34.sp,
                                    fontWeight = FontWeight.Black,
                                    color = textColor
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        IosBackPillButton(
            label = MultilingualManager.tr("btn_back", selectedLanguageCode),
            onClick = onBack
        )
    }
}

@Composable
private fun VictoryCard(
    message: String,
    selectedLanguageCode: String,
    onPlayAgain: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = NerColors.SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape)
                    .background(GoogleColors.Green.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Star, contentDescription = null, tint = GoogleColors.Green, modifier = Modifier.size(38.dp))
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = MultilingualManager.tr("app_title", selectedLanguageCode),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = NerColors.Charcoal
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                fontSize = 14.sp,
                color = NerColors.NeutralMedium,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onPlayAgain,
                colors = ButtonDefaults.buttonColors(containerColor = GoogleColors.Blue),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = MultilingualManager.tr("btn_play_again", selectedLanguageCode),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
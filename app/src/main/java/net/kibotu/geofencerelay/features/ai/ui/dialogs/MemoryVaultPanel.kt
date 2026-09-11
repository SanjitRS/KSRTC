package net.kibotu.geofencerelay.features.ai.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.kibotu.geofencerelay.features.ai.localization.MultilingualManager
import net.kibotu.geofencerelay.features.ai.reminiscence.MemoryCard
import net.kibotu.geofencerelay.features.ai.reminiscence.ReminiscenceManager
import net.kibotu.geofencerelay.features.ai.ui.components.IosBackPillButton
import net.kibotu.geofencerelay.ui.theme.*

/**
 * Reminiscence Memory Vault.
 * Adheres to the reference design kit:
 * - Warm porcelain canvas and authentic woven ribbon banner
 * - Tactile 24dp white cards and high-contrast Atkinson Hyperlegible typography
 * - Autobiographical memory recall cards with family question uploads.
 */
@Composable
fun MemoryVaultPanel(
    selectedLanguageCode: String = "en",
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var cards by remember { mutableStateOf(ReminiscenceManager.loadAllCards(context)) }
    var currentCardIndex by remember { mutableStateOf(0) }
    var selectedOptionIndex by remember { mutableStateOf<Int?>(null) }
    var showAffirmation by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    val currentCard = if (cards.isNotEmpty()) cards[currentCardIndex.coerceIn(0, cards.size - 1)] else null

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
            primaryColor = NerColors.PlumMaroon,
            secondaryColor = NerColors.Primary,
            accentColor = NerColors.Marigold
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = MultilingualManager.tr("memory_title", selectedLanguageCode),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NerColors.Charcoal
                    )
                    Text(
                        text = MultilingualManager.tr("memory_subtitle", selectedLanguageCode),
                        fontSize = 12.sp,
                        color = NerColors.NeutralMedium
                    )
                }

                // Family Upload Question Button
                NerPillButton(
                    text = MultilingualManager.tr("btn_add_memory", selectedLanguageCode),
                    icon = Icons.Default.Add,
                    hierarchy = NerButtonHierarchy.Primary,
                    containerColor = NerColors.Primary,
                    fontSize = 13.sp,
                    onClick = { showAddDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (currentCard != null) {
                // Main Memory Card
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
                        // Category Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(percent = 50))
                                .background(NerColors.PlumTint)
                                .padding(horizontal = 14.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "${currentCard.iconEmoji} ${currentCard.category}",
                                color = NerColors.PlumMaroon,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = currentCard.iconEmoji,
                            fontSize = 48.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = currentCard.title,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = NerColors.Charcoal
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = currentCard.question,
                            fontSize = 15.sp,
                            color = NerColors.Charcoal,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )

                        if (currentCard.cueText.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(NerColors.NeutralSoft)
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = "ðŸ’¡ Clue: ${currentCard.cueText}",
                                    fontSize = 13.sp,
                                    color = NerColors.NeutralMedium,
                                    lineHeight = 18.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // 4 Answer Options
                        currentCard.options.forEachIndexed { optIdx, optText ->
                            val isSelected = selectedOptionIndex == optIdx
                            val isCorrect = optIdx == currentCard.correctIndex
                            val showFeedback = showAffirmation

                            val optBgColor = when {
                                showFeedback && isCorrect -> NerColors.SecondaryTint
                                showFeedback && isSelected && !isCorrect -> NerColors.CrimsonTint
                                isSelected -> NerColors.PrimaryTint
                                else -> NerColors.NeutralSoft
                            }

                            val optBorderColor = when {
                                showFeedback && isCorrect -> NerColors.Secondary
                                showFeedback && isSelected && !isCorrect -> NerColors.Crimson
                                isSelected -> NerColors.Primary
                                else -> NerColors.NeutralBorder
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp)
                                    .clickable(enabled = !showAffirmation) {
                                        selectedOptionIndex = optIdx
                                        showAffirmation = true
                                    },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = optBgColor),
                                border = BorderStroke(1.dp, optBorderColor)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${('A' + optIdx)}.",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = NerColors.Charcoal
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = optText,
                                        fontSize = 14.sp,
                                        color = NerColors.Charcoal,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (showFeedback && isCorrect) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NerColors.Secondary, modifier = Modifier.size(22.dp))
                                    }
                                }
                            }
                        }

                        // Affirmation & Next Button
                        if (showAffirmation) {
                            Spacer(modifier = Modifier.height(14.dp))
                            val wasCorrect = selectedOptionIndex == currentCard.correctIndex
                            Text(
                                text = if (wasCorrect) "ðŸŽ‰ Wonderful memory! You remembered correctly."
                                else "â¤ï¸ Beautiful memory. That's always close to our hearts.",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (wasCorrect) NerColors.SecondaryDark else NerColors.PrimaryDark,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            NerPillButton(
                                text = MultilingualManager.tr("mv_next_memory", selectedLanguageCode),
                                hierarchy = NerButtonHierarchy.Primary,
                                containerColor = NerColors.Primary,
                                onClick = {
                                    selectedOptionIndex = null
                                    showAffirmation = false
                                    currentCardIndex = (currentCardIndex + 1) % cards.size
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            } else {
                // Empty State
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
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(NerColors.PrimaryTint),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("ðŸ‘¨â€ðŸ‘©â€ðŸ‘§â€ðŸ‘¦", fontSize = 40.sp)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = MultilingualManager.tr("lbl_no_memories", selectedLanguageCode),
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = NerColors.Charcoal
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = MultilingualManager.tr("lbl_no_memories_desc", selectedLanguageCode),
                            fontSize = 13.sp,
                            color = NerColors.NeutralMedium,
                            textAlign = TextAlign.Center,
                            lineHeight = 19.sp
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = MultilingualManager.tr("mv_default_themes", selectedLanguageCode),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NerColors.Charcoal
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        ReminiscenceManager.defaultThemes.forEach { theme ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(theme.second, fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(theme.first, fontSize = 13.sp, color = NerColors.Charcoal, fontWeight = FontWeight.Medium)
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        NerPillButton(
                            text = MultilingualManager.tr("mv_add_first", selectedLanguageCode),
                            icon = Icons.Default.Add,
                            hierarchy = NerButtonHierarchy.Primary,
                            containerColor = NerColors.Primary,
                            onClick = { showAddDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Accessible Bottom Back Pill
        IosBackPillButton(
            label = MultilingualManager.tr("btn_back", selectedLanguageCode),
            onClick = onBack
        )

        // Family Custom Memory Upload Dialog
        if (showAddDialog) {
            FamilyAddMemoryDialog(
                selectedLanguageCode = selectedLanguageCode,
                onDismiss = { showAddDialog = false },
                onSave = { newCard ->
                    ReminiscenceManager.saveCustomCard(context, newCard)
                    cards = ReminiscenceManager.loadAllCards(context)
                    currentCardIndex = cards.size - 1
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
private fun FamilyAddMemoryDialog(
    selectedLanguageCode: String = "en",
    onDismiss: () -> Unit,
    onSave: (MemoryCard) -> Unit
) {
    val themes = ReminiscenceManager.defaultThemes
    var selectedThemeIdx by remember { mutableStateOf(0) }
    var title by remember { mutableStateOf("") }
    var question by remember { mutableStateOf("") }
    var hint by remember { mutableStateOf("") }
    var opt1 by remember { mutableStateOf("") }
    var opt2 by remember { mutableStateOf("") }
    var opt3 by remember { mutableStateOf("") }
    var opt4 by remember { mutableStateOf("") }
    var correctOptIdx by remember { mutableStateOf(0) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NerColors.SurfaceWhite,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                MultilingualManager.tr("mv_upload_title", selectedLanguageCode),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = NerColors.Charcoal
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    MultilingualManager.tr("mv_select_theme", selectedLanguageCode),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = NerColors.Charcoal
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Theme Chips
                Column {
                    themes.forEachIndexed { idx, pair ->
                        val isSel = selectedThemeIdx == idx
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSel) NerColors.PrimaryTint else Color.Transparent)
                                .clickable { selectedThemeIdx = idx }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(pair.second, fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = pair.first,
                                fontSize = 13.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) NerColors.PrimaryDark else NerColors.Charcoal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(MultilingualManager.tr("mv_title_label", selectedLanguageCode)) },
                    placeholder = { Text(MultilingualManager.tr("mv_title_hint", selectedLanguageCode), color = Color.Gray) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    label = { Text(MultilingualManager.tr("mv_question_label", selectedLanguageCode)) },
                    placeholder = { Text(MultilingualManager.tr("mv_question_hint", selectedLanguageCode), color = Color.Gray) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = hint,
                    onValueChange = { hint = it },
                    label = { Text(MultilingualManager.tr("mv_cue_label", selectedLanguageCode)) },
                    placeholder = { Text(MultilingualManager.tr("mv_cue_hint", selectedLanguageCode), color = Color.Gray) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    MultilingualManager.tr("mv_options_title", selectedLanguageCode),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = NerColors.Charcoal
                )
                Spacer(modifier = Modifier.height(6.dp))

                listOf(
                    Pair(opt1, { s: String -> opt1 = s }),
                    Pair(opt2, { s: String -> opt2 = s }),
                    Pair(opt3, { s: String -> opt3 = s }),
                    Pair(opt4, { s: String -> opt4 = s })
                ).forEachIndexed { i, pair ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                        RadioButton(
                            selected = correctOptIdx == i,
                            onClick = { correctOptIdx = i }
                        )
                        OutlinedTextField(
                            value = pair.first,
                            onValueChange = pair.second,
                            placeholder = { Text("Option ${i + 1}", color = Color.Gray) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (errorMsg != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorMsg!!, color = NerColors.Crimson, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            NerPillButton(
                text = MultilingualManager.tr("mv_save", selectedLanguageCode),
                hierarchy = NerButtonHierarchy.Primary,
                containerColor = NerColors.Primary,
                onClick = {
                    if (title.isBlank() || question.isBlank() || opt1.isBlank() || opt2.isBlank()) {
                        errorMsg = "Please fill in title, question, and at least 2 options."
                    } else {
                        val chosenTheme = themes[selectedThemeIdx]
                        val options = listOf(opt1, opt2, opt3, opt4).filter { it.isNotBlank() }
                        val card = MemoryCard(
                            id = "custom_${System.currentTimeMillis()}",
                            title = title.trim(),
                            category = chosenTheme.first,
                            question = question.trim(),
                            cueText = hint.trim(),
                            iconEmoji = chosenTheme.second,
                            options = options,
                            correctIndex = correctOptIdx.coerceIn(0, options.size - 1),
                            isCustom = true
                        )
                        onSave(card)
                    }
                }
            )
        },
        dismissButton = {
            NerPillButton(
                text = MultilingualManager.tr("mv_cancel", selectedLanguageCode),
                hierarchy = NerButtonHierarchy.Secondary,
                onClick = onDismiss
            )
        }
    )
}
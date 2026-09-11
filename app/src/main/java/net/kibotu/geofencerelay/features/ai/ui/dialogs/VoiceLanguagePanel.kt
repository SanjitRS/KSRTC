package net.kibotu.geofencerelay.features.ai.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.kibotu.geofencerelay.features.ai.localization.MultilingualManager
import net.kibotu.geofencerelay.features.ai.ui.components.IosBackPillButton
import net.kibotu.geofencerelay.ui.theme.*

/**
 * Vibrant Language & Voice Guidance Panel.
 * Directly adheres to the reference design kit:
 * - Warm porcelain canvas and authentic woven ribbon banner
 * - Tactile 24dp white cards and high-contrast Atkinson Hyperlegible typography
 * - Supports English, Hindi, Assamese, Mizo, Khasi, Manipuri, and Nagamese.
 */
@Composable
fun VoiceLanguagePanel(
    selectedLanguageCode: String,
    onLanguageSelected: (String) -> Unit,
    onBack: () -> Unit
) {
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
            primaryColor = NerColors.Marigold,
            secondaryColor = NerColors.Secondary,
            accentColor = NerColors.Primary
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = MultilingualManager.tr("tile_voice_title", selectedLanguageCode),
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = NerColors.Charcoal
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = MultilingualManager.tr("tile_voice_sub", selectedLanguageCode),
                fontSize = 13.sp,
                color = NerColors.NeutralMedium
            )

            Spacer(modifier = Modifier.height(18.dp))

            // White 24dp Card of Supported Languages
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = NerColors.SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                border = BorderStroke(1.dp, NerColors.NeutralBorder)
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    MultilingualManager.supportedLanguages.forEachIndexed { index, lang ->
                        val isSelected = lang.code == selectedLanguageCode

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isSelected) NerColors.PrimaryTint.copy(alpha = 0.5f) else Color.Transparent)
                                .clickable {
                                    onLanguageSelected(lang.code)
                                    val confirmMsg = MultilingualManager.getVoiceConfirmation(lang.code)
                                    MultilingualManager.speak(confirmMsg, lang.code)
                                }
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(lang.flagEmoji, fontSize = 22.sp)
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = lang.nativeName,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) NerColors.PrimaryDark else NerColors.Charcoal
                                    )
                                    Text(
                                        text = lang.displayName,
                                        fontSize = 12.sp,
                                        color = NerColors.NeutralMedium
                                    )
                                }
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = NerColors.Primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        if (index < MultilingualManager.supportedLanguages.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 18.dp),
                                color = NerColors.NeutralBorder
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Voice Test Card
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
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = MultilingualManager.tr("voice_preview_title", selectedLanguageCode),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = NerColors.Charcoal
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = MultilingualManager.getEncouragement("encouraging", selectedLanguageCode),
                        fontSize = 13.sp,
                        color = NerColors.NeutralMedium,
                        lineHeight = 18.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    NerPillButton(
                        text = MultilingualManager.tr("voice_preview_btn", selectedLanguageCode),
                        icon = Icons.Default.VolumeUp,
                        hierarchy = NerButtonHierarchy.Primary,
                        containerColor = NerColors.Primary,
                        onClick = {
                            val msg = MultilingualManager.getEncouragement("celebratory", selectedLanguageCode)
                            MultilingualManager.speak(msg, selectedLanguageCode)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Accessible Bottom Back Pill
        IosBackPillButton(
            label = MultilingualManager.tr("btn_back", selectedLanguageCode),
            onClick = onBack
        )
    }
}
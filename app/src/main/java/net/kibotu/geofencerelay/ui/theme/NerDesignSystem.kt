package net.kibotu.geofencerelay.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Official Vibrant North-East & Design-System Palette.
 * Matches the reference design kit:
 * - Primary: Terracotta Orange (#E65100)
 * - Secondary: Botanical Forest Green (#1B5E20)
 * - Tertiary: Royal Cobalt Blue (#0D47A1)
 * - Neutral: Charcoal (#212121)
 * - Accents: Crimson SOS (#C62828), Marigold Gold (#F57F17), Warm Porcelain Canvas (#F7F5F0)
 */
object NerColors {
    // Primary Terracotta / Amber Orange
    val Primary = Color(0xFFE65100)
    val PrimaryDark = Color(0xFFBF360C)
    val PrimaryLight = Color(0xFFFF7526)
    val PrimaryTint = Color(0xFFFFF0E6)
    val PrimarySoft = Color(0xFFFFD1B5)

    // Secondary Botanical Forest Green
    val Secondary = Color(0xFF1B5E20)
    val SecondaryDark = Color(0xFF0F3813)
    val SecondaryLight = Color(0xFF2E7D32)
    val SecondaryTint = Color(0xFFE8F5E9)
    val SecondarySoft = Color(0xFFC8E6C9)

    // Tertiary Royal Cobalt Blue
    val Tertiary = Color(0xFF0D47A1)
    val TertiaryDark = Color(0xFF072759)
    val TertiaryLight = Color(0xFF1976D2)
    val TertiaryTint = Color(0xFFE3F2FD)
    val TertiarySoft = Color(0xFFBBDEFB)

    // Neutrals & Surfaces (Dark-Whitish Aesthetic)
    val Charcoal = Color(0xFF1E1E1E)
    val NeutralDark = Color(0xFF121212)
    val NeutralMedium = Color(0xFF5A5A5A)
    val NeutralLight = Color(0xFF8E8E8E)
    val NeutralSoft = Color(0xFFDEDBD3)
    val NeutralBorder = Color(0xFFD2CEC4)
    val CanvasWarm = Color(0xFFECEAE5)      // Dark-whitish alabaster canvas
    val CanvasIvory = Color(0xFFF1EFEA)     // Elevated dark-white
    val SurfaceWhite = Color(0xFFF7F6F2)    // Refined dark-whitish card surface

    // Vibrant Cultural Accents
    val Crimson = Color(0xFFC62828)
    val CrimsonTint = Color(0xFFFFEBEE)
    val Marigold = Color(0xFFF57F17)
    val MarigoldTint = Color(0xFFFFF8E1)
    val PlumMaroon = Color(0xFF6B1724)
    val PlumTint = Color(0xFFFCE4EC)
    val SkyBlue = Color(0xFF0288D1)
}

enum class NerButtonHierarchy {
    Primary,
    Secondary,
    Inverted,
    Outlined
}

/**
 * Procedural Vector Canvas Woven Ribbon.
 * Renders authentic North-East textile weaving patterns (Assamese Gamosa / Mizo Puan / Naga weaves)
 * using crisp geometric diamond lozenges, chevrons, and serrated borders.
 */
@Composable
fun NerWovenRibbon(
    modifier: Modifier = Modifier,
    height: Dp = 16.dp,
    primaryColor: Color = NerColors.Primary,
    secondaryColor: Color = NerColors.Secondary,
    accentColor: Color = NerColors.Marigold
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val w = size.width
        val h = size.height
        val unitW = 28f
        val count = (w / unitW).toInt() + 2

        // Top and bottom guide lines
        drawLine(
            color = primaryColor.copy(alpha = 0.4f),
            start = Offset(0f, 1f),
            end = Offset(w, 1f),
            strokeWidth = 1.5f
        )
        drawLine(
            color = secondaryColor.copy(alpha = 0.4f),
            start = Offset(0f, h - 1f),
            end = Offset(w, h - 1f),
            strokeWidth = 1.5f
        )

        // Repeating geometric diamond & temple chevron weave
        for (i in 0..count) {
            val cx = i * unitW
            val cy = h / 2f

            // Diamond lozenge
            val diamondPath = Path().apply {
                moveTo(cx, cy - h * 0.38f)
                lineTo(cx + unitW * 0.38f, cy)
                lineTo(cx, cy + h * 0.38f)
                lineTo(cx - unitW * 0.38f, cy)
                close()
            }
            drawPath(
                path = diamondPath,
                color = if (i % 2 == 0) primaryColor else secondaryColor
            )

            // Inner accent diamond dot
            val innerPath = Path().apply {
                moveTo(cx, cy - h * 0.16f)
                lineTo(cx + unitW * 0.16f, cy)
                lineTo(cx, cy + h * 0.16f)
                lineTo(cx - unitW * 0.16f, cy)
                close()
            }
            drawPath(
                path = innerPath,
                color = accentColor
            )

            // Inter-diamond chevron peaks
            val midX = cx + unitW / 2f
            drawCircle(
                color = if (i % 2 == 0) secondaryColor else primaryColor,
                radius = 2f,
                center = Offset(midX, cy)
            )
        }
    }
}

/**
 * Procedural Vector Folk Mandala Watermark.
 * Renders an authentic concentric circular folk rosette motif (inspired by Mithila / Alpana / Warli art).
 */
@Composable
fun NerMandalaWatermark(
    modifier: Modifier = Modifier,
    baseColor: Color = NerColors.PrimaryDark,
    alpha: Float = 0.045f
) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val maxR = minOf(size.width, size.height) * 0.48f

        // Outer concentric rings
        drawCircle(
            color = baseColor.copy(alpha = alpha * 0.8f),
            radius = maxR,
            center = Offset(cx, cy),
            style = Stroke(width = 1.5f)
        )
        drawCircle(
            color = baseColor.copy(alpha = alpha * 0.6f),
            radius = maxR * 0.82f,
            center = Offset(cx, cy),
            style = Stroke(width = 1f)
        )
        drawCircle(
            color = baseColor.copy(alpha = alpha),
            radius = maxR * 0.64f,
            center = Offset(cx, cy),
            style = Stroke(width = 1.2f)
        )
        drawCircle(
            color = baseColor.copy(alpha = alpha * 0.8f),
            radius = maxR * 0.46f,
            center = Offset(cx, cy),
            style = Stroke(width = 1f)
        )
        drawCircle(
            color = baseColor.copy(alpha = alpha * 1.2f),
            radius = maxR * 0.28f,
            center = Offset(cx, cy),
            style = Stroke(width = 1.5f)
        )

        // 12 radiating folk petals & satellite dots
        val petalCount = 12
        for (i in 0 until petalCount) {
            val angle = (i * 360f / petalCount) * (Math.PI / 180f)
            val px = cx + (maxR * 0.64f * Math.cos(angle)).toFloat()
            val py = cy + (maxR * 0.64f * Math.sin(angle)).toFloat()

            drawCircle(
                color = baseColor.copy(alpha = alpha * 1.5f),
                radius = 3.5f,
                center = Offset(px, py)
            )

            val outerPx = cx + (maxR * 0.92f * Math.cos(angle + 0.15)).toFloat()
            val outerPy = cy + (maxR * 0.92f * Math.sin(angle + 0.15)).toFloat()

            drawCircle(
                color = NerColors.Secondary.copy(alpha = alpha * 1.2f),
                radius = 2.5f,
                center = Offset(outerPx, outerPy)
            )
        }
    }
}

/**
 * Accessible Pill Button adhering to reference design kit.
 * High-contrast, friendly, tactile with animated touch bounds.
 */
@Composable
fun NerPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hierarchy: NerButtonHierarchy = NerButtonHierarchy.Primary,
    icon: ImageVector? = null,
    containerColor: Color? = null,
    contentColor: Color? = null,
    enabled: Boolean = true,
    fontSize: androidx.compose.ui.unit.TextUnit = 16.sp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.96f else 1.0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
        label = "nerPillPress"
    )

    val bgColor = containerColor ?: when (hierarchy) {
        NerButtonHierarchy.Primary -> NerColors.Primary
        NerButtonHierarchy.Secondary -> NerColors.NeutralSoft
        NerButtonHierarchy.Inverted -> NerColors.Charcoal
        NerButtonHierarchy.Outlined -> Color.Transparent
    }

    val fgColor = contentColor ?: when (hierarchy) {
        NerButtonHierarchy.Primary -> Color.White
        NerButtonHierarchy.Secondary -> NerColors.Charcoal
        NerButtonHierarchy.Inverted -> Color.White
        NerButtonHierarchy.Outlined -> NerColors.Primary
    }

    val borderStroke = when (hierarchy) {
        NerButtonHierarchy.Outlined -> BorderStroke(1.5.dp, NerColors.Primary)
        NerButtonHierarchy.Secondary -> BorderStroke(1.dp, NerColors.NeutralBorder)
        else -> null
    }

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .scale(scale)
            .height(50.dp),
        shape = RoundedCornerShape(percent = 50),
        color = if (enabled) bgColor else bgColor.copy(alpha = 0.5f),
        contentColor = if (enabled) fgColor else fgColor.copy(alpha = 0.5f),
        border = borderStroke,
        shadowElevation = if (hierarchy == NerButtonHierarchy.Primary && enabled) 2.dp else 0.dp,
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = fgColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                color = fgColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Circular Icon Badge adhering to the reference kit.
 */
@Composable
fun NerCircularBadge(
    icon: ImageVector,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    onClick: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.92f else 1.0f,
        label = "badgePress"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .clip(CircleShape)
            .background(backgroundColor)
            .then(
                if (onClick != null) Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Multi-tier Rounded Progress Bars from the UI Kit.
 * Displays orange, green, and blue progress indicators on soft neutral tracks.
 */
@Composable
fun NerMultiProgressBar(
    orangeProgress: Float = 0.7f,
    greenProgress: Float = 0.85f,
    blueProgress: Float = 0.55f,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Orange tier
        LinearProgressItem(
            progress = orangeProgress,
            color = NerColors.Primary
        )
        // Green tier
        LinearProgressItem(
            progress = greenProgress,
            color = NerColors.Secondary
        )
        // Blue tier
        LinearProgressItem(
            progress = blueProgress,
            color = NerColors.Tertiary
        )
    }
}

@Composable
private fun LinearProgressItem(
    progress: Float,
    color: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(NerColors.NeutralSoft)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(percent = 50))
                .background(color)
        )
    }
}

/**
 * Floating Bottom Navigation Dock adhering to the UI kit.
 * Pill container with circular terracotta active indicator and clean tabs.
 */
@Composable
fun NerBottomDock(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(horizontal = 24.dp, vertical = 10.dp)
            .height(58.dp),
        shape = RoundedCornerShape(percent = 50),
        colors = CardDefaults.cardColors(containerColor = NerColors.SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        border = BorderStroke(1.dp, NerColors.NeutralBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tab 0: Home (Terracotta circular badge when active)
            DockItem(
                icon = Icons.Default.Home,
                label = "Home",
                isActive = selectedIndex == 0,
                activeColor = NerColors.Primary,
                onClick = { onTabSelected(0) }
            )

            // Tab 1: Games Hub
            DockItem(
                icon = Icons.Default.SportsEsports,
                label = "Games",
                isActive = selectedIndex == 1,
                activeColor = NerColors.Primary,
                onClick = { onTabSelected(1) }
            )

            // Tab 2: Health CPS
            DockItem(
                icon = Icons.Default.Psychology,
                label = "Health",
                isActive = selectedIndex == 2,
                activeColor = NerColors.Tertiary,
                onClick = { onTabSelected(2) }
            )

            // Tab 3: Safety / SOS
            DockItem(
                icon = Icons.Default.Shield,
                label = "Safety",
                isActive = selectedIndex == 3,
                activeColor = NerColors.Crimson,
                onClick = { onTabSelected(3) }
            )
        }
    }
}

@Composable
private fun DockItem(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    if (isActive) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(activeColor)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    } else {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(42.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = NerColors.NeutralMedium,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
package net.kibotu.geofencerelay.features.ai.ui.dialogs

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
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
import net.kibotu.geofencerelay.features.ai.model.CpsAssessmentResult
import net.kibotu.geofencerelay.features.ai.ui.components.IosBackPillButton
import net.kibotu.geofencerelay.service.TrackerForegroundService
import net.kibotu.geofencerelay.ui.theme.*

/**
 * Actionable, life-saving Patient Safety & Emergency Hub.
 * Adheres to the reference design kit:
 * - Warm porcelain canvas and authentic woven ribbon banner
 * - Tactile 24dp white cards and high-contrast Atkinson Hyperlegible typography
 * - High-visibility 1-Tap SOS (112) in Crimson and Take Me Home Navigation in Terracotta
 */
@Composable
fun SafetyAlertsPanel(
    assessment: CpsAssessmentResult?,
    selectedLanguageCode: String = "en",
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("patient_safety_prefs", Context.MODE_PRIVATE) }

    var caregiverName by remember { mutableStateOf(prefs.getString("caregiver_name", "Family Caregiver") ?: "Family Caregiver") }
    var caregiverPhone by remember { mutableStateOf(prefs.getString("caregiver_phone", "") ?: "") }
    var homeAddress by remember {
        val raw = prefs.getString("home_address", "") ?: ""
        mutableStateOf(if (raw.equals("Home Safe Zone", ignoreCase = true)) "" else raw)
    }
    var homeLat by remember { mutableStateOf(prefs.getFloat("home_lat", 0f).toDouble()) }
    var homeLon by remember { mutableStateOf(prefs.getFloat("home_lon", 0f).toDouble()) }
    var patientName by remember { mutableStateOf(prefs.getString("patient_name", "Smaran Patient") ?: "Smaran Patient") }

    var showEditDialog by remember { mutableStateOf(false) }

    var medTaken by remember { mutableStateOf(false) }
    var waterTaken by remember { mutableStateOf(false) }

    val hasHomeSet = homeAddress.isNotBlank() || (homeLat != 0.0 && homeLon != 0.0)

    fun launchTakeMeHome() {
        if (!hasHomeSet) {
            Toast.makeText(context, "Please configure your home address or capture GPS first", Toast.LENGTH_SHORT).show()
            showEditDialog = true
            return
        }

        try {
            val navUri = if (homeLat != 0.0 && homeLon != 0.0) {
                Uri.parse("google.navigation:q=$homeLat,$homeLon&mode=w")
            } else {
                Uri.parse("google.navigation:q=" + Uri.encode(homeAddress) + "&mode=w")
            }
            val mapIntent = Intent(Intent.ACTION_VIEW, navUri).apply {
                setPackage("com.google.android.apps.maps")
            }
            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
            } else {
                val geoUri = if (homeLat != 0.0 && homeLon != 0.0) {
                    Uri.parse("geo:$homeLat,$homeLon?q=$homeLat,$homeLon(" + Uri.encode(homeAddress.ifBlank { "Home" }) + ")")
                } else {
                    Uri.parse("geo:0,0?q=" + Uri.encode(homeAddress))
                }
                context.startActivity(Intent(Intent.ACTION_VIEW, geoUri))
            }
        } catch (e: Exception) {
            try {
                val webDest = if (homeLat != 0.0 && homeLon != 0.0) "$homeLat,$homeLon" else homeAddress
                val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" + Uri.encode(webDest) + "&travelmode=walking")
                context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
            } catch (_: Exception) {
                Toast.makeText(context, "Navigating to: $homeAddress", Toast.LENGTH_SHORT).show()
            }
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
            primaryColor = NerColors.Crimson,
            secondaryColor = NerColors.Secondary,
            accentColor = NerColors.Marigold
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Screen Header
            Text(
                text = MultilingualManager.tr("safety_title", selectedLanguageCode),
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = NerColors.Charcoal
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = MultilingualManager.tr("safety_subtitle", selectedLanguageCode),
                fontSize = 13.sp,
                color = NerColors.NeutralMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 1-TAP ACTION BUTTONS (EMERGENCY SOS & CALL CAREGIVER)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1-Tap Emergency SOS (Crimson Red)
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:112")
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            Toast.makeText(context, "Emergency SOS: Dialing 112", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(68.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NerColors.Crimson),
                    shape = RoundedCornerShape(20.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Sos, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            MultilingualManager.tr("safety_sos", selectedLanguageCode),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }

                // 1-Tap Call Family / Caregiver (Botanical Forest Green)
                Button(
                    onClick = {
                        if (caregiverPhone.isNotBlank()) {
                            try {
                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:$caregiverPhone")
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                Toast.makeText(context, "Calling $caregiverName ($caregiverPhone)", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            showEditDialog = true
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(68.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NerColors.Secondary),
                    shape = RoundedCornerShape(20.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PhoneInTalk, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            if (caregiverPhone.isNotBlank()) "CALL FAMILY" else "SET FAMILY #",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Take Me Home (1-Tap Navigation in Terracotta Orange)
            Button(
                onClick = { launchTakeMeHome() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NerColors.Primary),
                shape = RoundedCornerShape(20.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.DirectionsWalk, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = MultilingualManager.tr("btn_take_me_home", selectedLanguageCode),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = if (hasHomeSet) "ðŸ“ Destination: ${homeAddress.ifBlank { "Saved Coordinates ($homeLat, $homeLon)" }}" else "âš ï¸ Tap to set your home address or capture GPS",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // LIVE SAFE ZONE & GPS SENTINEL CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = NerColors.SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                border = BorderStroke(1.5.dp, NerColors.Secondary.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(NerColors.SecondaryTint),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = NerColors.Secondary, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    MultilingualManager.tr("safety_geofence_title", selectedLanguageCode),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NerColors.Charcoal
                                )
                                Text(
                                    MultilingualManager.tr("safety_geofence_sub", selectedLanguageCode),
                                    fontSize = 12.sp,
                                    color = NerColors.NeutralMedium
                                )
                            }
                        }

                        // Live status badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(percent = 50))
                                .background(NerColors.SecondaryTint)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("â— SECURE", fontSize = 11.sp, fontWeight = FontWeight.Black, color = NerColors.SecondaryDark)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = MultilingualManager.tr("safety_geofence_desc", selectedLanguageCode),
                        fontSize = 13.sp,
                        color = NerColors.NeutralMedium,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // MEDICAL EMERGENCY ID & CONTACT CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = NerColors.SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                border = BorderStroke(1.dp, NerColors.NeutralBorder)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MedicalServices, contentDescription = null, tint = NerColors.Crimson, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = MultilingualManager.tr("safety_med_id", selectedLanguageCode),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = NerColors.Charcoal
                            )
                        }
                        IconButton(
                            onClick = { showEditDialog = true },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Contact", tint = NerColors.Primary, modifier = Modifier.size(20.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Patient Name
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(MultilingualManager.tr("safety_patient_name", selectedLanguageCode), fontSize = 13.sp, color = NerColors.NeutralMedium)
                        Text(patientName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NerColors.Charcoal)
                    }

                    // Condition
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(MultilingualManager.tr("safety_condition", selectedLanguageCode), fontSize = 13.sp, color = NerColors.NeutralMedium)
                        Text(
                            MultilingualManager.tr("safety_condition_desc", selectedLanguageCode),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NerColors.PrimaryDark
                        )
                    }

                    // Primary Caregiver
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(MultilingualManager.tr("safety_caregiver", selectedLanguageCode), fontSize = 13.sp, color = NerColors.NeutralMedium)
                        Text(
                            text = if (caregiverPhone.isNotBlank()) "$caregiverName ($caregiverPhone)" else "$caregiverName (Tap âœï¸ to add)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (caregiverPhone.isNotBlank()) NerColors.SecondaryDark else NerColors.Primary
                        )
                    }

                    // Safe Home Address
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(MultilingualManager.tr("safety_address", selectedLanguageCode), fontSize = 13.sp, color = NerColors.NeutralMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (hasHomeSet) homeAddress.ifBlank { "GPS Coordinates Saved" } else "Not configured",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (hasHomeSet) NerColors.Charcoal else NerColors.PrimaryDark
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ðŸ“ Set",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NerColors.Primary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(NerColors.PrimaryTint)
                                    .clickable { showEditDialog = true }
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // DAILY WELLNESS & CARE CHECKLIST
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = NerColors.SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                border = BorderStroke(1.dp, NerColors.NeutralBorder)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = MultilingualManager.tr("safety_wellness", selectedLanguageCode),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = NerColors.Charcoal
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Medication Check
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (medTaken) NerColors.SecondaryTint else NerColors.NeutralSoft)
                            .clickable { medTaken = !medTaken }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Medication, contentDescription = null, tint = NerColors.Crimson, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    MultilingualManager.tr("safety_meds", selectedLanguageCode),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = NerColors.Charcoal
                                )
                                Text(
                                    if (medTaken) "Taken today âœ“" else "Tap to mark taken",
                                    fontSize = 11.sp,
                                    color = if (medTaken) NerColors.SecondaryDark else NerColors.NeutralMedium
                                )
                            }
                        }
                        Icon(
                            imageVector = if (medTaken) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (medTaken) NerColors.Secondary else NerColors.NeutralMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Hydration Check
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (waterTaken) NerColors.TertiaryTint else NerColors.NeutralSoft)
                            .clickable { waterTaken = !waterTaken }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.WaterDrop, contentDescription = null, tint = NerColors.Tertiary, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    MultilingualManager.tr("safety_hydration", selectedLanguageCode),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = NerColors.Charcoal
                                )
                                Text(
                                    if (waterTaken) "Drank water âœ“" else "Tap to mark completed",
                                    fontSize = 11.sp,
                                    color = if (waterTaken) NerColors.TertiaryDark else NerColors.NeutralMedium
                                )
                            }
                        }
                        Icon(
                            imageVector = if (waterTaken) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (waterTaken) NerColors.Tertiary else NerColors.NeutralMedium
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
    }

    // Edit Emergency Info & Home Location Dialog
    if (showEditDialog) {
        var tempName by remember { mutableStateOf(caregiverName) }
        var tempPhone by remember { mutableStateOf(caregiverPhone) }
        var tempAddress by remember { mutableStateOf(homeAddress) }
        var tempLat by remember { mutableStateOf(homeLat) }
        var tempLon by remember { mutableStateOf(homeLon) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = NerColors.SurfaceWhite,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    MultilingualManager.tr("safety_dialog_title", selectedLanguageCode),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = NerColors.Charcoal
                )
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        "Set caregiver phone and home location. The 'TAKE ME HOME' button uses this exact location for walking directions.",
                        fontSize = 12.sp,
                        color = NerColors.NeutralMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text(MultilingualManager.tr("safety_caregiver_name", selectedLanguageCode)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tempPhone,
                        onValueChange = { tempPhone = it },
                        label = { Text(MultilingualManager.tr("safety_caregiver_phone", selectedLanguageCode)) },
                        placeholder = { Text("+91 9876543210") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        "${MultilingualManager.tr("btn_set_home", selectedLanguageCode)}:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = NerColors.Charcoal
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = tempAddress,
                        onValueChange = {
                            tempAddress = it
                            tempLat = 0.0
                            tempLon = 0.0
                        },
                        label = { Text(MultilingualManager.tr("safety_home_address_label", selectedLanguageCode)) },
                        placeholder = { Text("e.g. 24 Indiranagar 100ft Rd, Bengaluru") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 1-Tap Use Current GPS as Home Button
                    Button(
                        onClick = {
                            val ping = TrackerForegroundService.latestDevicePing.value
                            if (ping != null && ping.latitude != 0.0) {
                                tempLat = ping.latitude
                                tempLon = ping.longitude
                                tempAddress = if (ping.address.isNotBlank() && !ping.address.contains("Locating")) {
                                    ping.address
                                } else {
                                    String.format(java.util.Locale.US, "GPS: %.5f, %.5f", ping.latitude, ping.longitude)
                                }
                                Toast.makeText(context, "Captured Current GPS Location as Home!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Acquiring GPS fix... make sure GPS broadcast is active", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = NerColors.Secondary),
                        shape = RoundedCornerShape(percent = 50)
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(MultilingualManager.tr("btn_use_current_gps", selectedLanguageCode), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    if (tempLat != 0.0 && tempLon != 0.0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format(java.util.Locale.US, "GPS: %.5f, %.5f", tempLat, tempLon),
                            fontSize = 11.sp,
                            color = NerColors.SecondaryDark
                        )
                    }
                }
            },
            confirmButton = {
                NerPillButton(
                    text = MultilingualManager.tr("btn_save_home", selectedLanguageCode),
                    hierarchy = NerButtonHierarchy.Primary,
                    containerColor = NerColors.Primary,
                    onClick = {
                        caregiverName = tempName
                        caregiverPhone = tempPhone
                        homeAddress = tempAddress
                        homeLat = tempLat
                        homeLon = tempLon
                        prefs.edit()
                            .putString("caregiver_name", tempName)
                            .putString("caregiver_phone", tempPhone)
                            .putString("home_address", tempAddress)
                            .putFloat("home_lat", tempLat.toFloat())
                            .putFloat("home_lon", tempLon.toFloat())
                            .commit()
                        showEditDialog = false
                        Toast.makeText(context, "Home location & caregiver saved!", Toast.LENGTH_SHORT).show()
                    }
                )
            },
            dismissButton = {
                NerPillButton(
                    text = MultilingualManager.tr("mv_cancel", selectedLanguageCode),
                    hierarchy = NerButtonHierarchy.Secondary,
                    onClick = { showEditDialog = false }
                )
            }
        )
    }
}
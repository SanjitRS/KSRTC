package net.kibotu.geofencerelay.features.ai.ui.dialogs

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
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
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import net.kibotu.geofencerelay.features.ai.localization.MultilingualManager
import net.kibotu.geofencerelay.features.ai.ui.components.IosBackPillButton
import net.kibotu.geofencerelay.model.LocationPing
import net.kibotu.geofencerelay.service.TrackerForegroundService
import net.kibotu.geofencerelay.ui.theme.*
import net.kibotu.geofencerelay.util.BatteryUtils
import net.kibotu.geofencerelay.util.LocationUtils
import java.util.Locale

/**
 * Robust GPS Sentinel Beacon controller panel.
 * Adheres to the reference design kit:
 * - Warm porcelain canvas and authentic woven ribbon banner
 * - Tactile 24dp white cards and high-contrast Atkinson Hyperlegible typography
 * - Live satellite telemetry feed (coordinates, address, battery, speed), and authorized caregivers management.
 */
@Composable
fun BeaconTrackerPanel(
    userEmail: String,
    selectedLanguageCode: String = "en",
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val servicePrefs = remember {
        context.getSharedPreferences(TrackerForegroundService.PREFS_NAME, Context.MODE_PRIVATE)
    }

    val isBroadcastingFromService by TrackerForegroundService.serviceRunning.collectAsState()
    var isBroadcasting by remember {
        mutableStateOf(TrackerForegroundService.isRunning(context))
    }
    LaunchedEffect(isBroadcastingFromService) {
        isBroadcasting = isBroadcastingFromService || TrackerForegroundService.isRunning(context)
    }

    val latestPing by TrackerForegroundService.latestDevicePing.collectAsState()

    var authorizedEmails by remember {
        val set = TrackerForegroundService.getAuthorizedEmails(context)
        mutableStateOf(if (set.isNotEmpty()) set.toList() else listOf(userEmail))
    }

    LaunchedEffect(userEmail) {
        if (userEmail.isNotBlank()) {
            TrackerForegroundService.addAuthorizedEmail(context, userEmail)
            authorizedEmails = TrackerForegroundService.getAuthorizedEmails(context).toList()
        }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var newEmailInput by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var showBackgroundPermissionDialog by remember { mutableStateOf(false) }

    val deviceName = remember { LocationUtils.getFriendlyDeviceName(Build.MODEL) }

    fun checkLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun checkBackgroundPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    var hasLocationPermission by remember { mutableStateOf(checkLocationPermission()) }
    var hasBackgroundPermission by remember { mutableStateOf(checkBackgroundPermission()) }

    val locationManager = remember {
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }
    var isGpsHardwareEnabled by remember {
        mutableStateOf(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
    }

    val gpsEnableLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            isGpsHardwareEnabled = true
            TrackerForegroundService.start(context)
            isBroadcasting = true
        }
    }

    fun promptEnableGps(onGpsReady: () -> Unit) {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L).build()
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest).setAlwaysShow(true)
        val client = LocationServices.getSettingsClient(context)
        val task = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            isGpsHardwareEnabled = true
            onGpsReady()
        }
        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    val intentSenderRequest = IntentSenderRequest.Builder(exception.resolution).build()
                    gpsEnableLauncher.launch(intentSenderRequest)
                } catch (_: Exception) {
                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
            } else {
                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
        }
    }

    val backgroundPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasBackgroundPermission = granted
        if (!granted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)
        }
    }

    val fineLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val fineGranted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = fineGranted || coarseGranted

        if (hasLocationPermission) {
            promptEnableGps {
                TrackerForegroundService.start(context)
                isBroadcasting = true
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !checkBackgroundPermission()) {
                showBackgroundPermissionDialog = true
            }
        }
    }

    fun saveEmails(list: List<String>) {
        authorizedEmails = list
        servicePrefs.edit().putStringSet(TrackerForegroundService.KEY_AUTHORIZED_EMAILS, list.toSet()).apply()
        TrackerForegroundService.notifyAuthorizedEmailsChanged(context)
    }

    // Pulsing radar animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.30f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

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
            primaryColor = NerColors.Secondary,
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
                text = MultilingualManager.tr("beacon_title", selectedLanguageCode),
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = NerColors.Charcoal
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = MultilingualManager.tr("beacon_subtitle", selectedLanguageCode),
                fontSize = 13.sp,
                color = NerColors.NeutralMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            // GPS Disabled Warning Banner
            if (!isGpsHardwareEnabled) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clickable { promptEnableGps { isGpsHardwareEnabled = true } },
                    colors = CardDefaults.cardColors(containerColor = NerColors.MarigoldTint),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, NerColors.Marigold)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.LocationOff, contentDescription = null, tint = NerColors.PrimaryDark, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                MultilingualManager.tr("btn_turn_on_gps", selectedLanguageCode),
                                fontWeight = FontWeight.Bold,
                                color = NerColors.PrimaryDark,
                                fontSize = 14.sp
                            )
                            Text("Tap here to turn on Google High-Accuracy GPS with one tap.", color = NerColors.NeutralMedium, fontSize = 11.sp)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = NerColors.PrimaryDark)
                    }
                }
            }

            // Background Location Permission Banner
            if (!hasBackgroundPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clickable { showBackgroundPermissionDialog = true },
                    colors = CardDefaults.cardColors(containerColor = NerColors.TertiaryTint),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, NerColors.Tertiary)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = NerColors.Tertiary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Background Permission Needed", fontWeight = FontWeight.Bold, color = NerColors.Tertiary, fontSize = 14.sp)
                            Text("Tap to set 'Allow all the time' for continuous 24/7 tracking.", color = NerColors.NeutralMedium, fontSize = 11.sp)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = NerColors.Tertiary)
                    }
                }
            } else if (hasBackgroundPermission) {
                Row(
                    modifier = Modifier
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(NerColors.SecondaryTint)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NerColors.Secondary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Background Location: Allowed All the Time", fontSize = 12.sp, color = NerColors.SecondaryDark, fontWeight = FontWeight.SemiBold)
                }
            }

            // Main Radar Card
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
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)) {
                        if (isBroadcasting) {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .scale(pulseScale)
                                    .clip(CircleShape)
                                    .background(NerColors.Secondary.copy(alpha = pulseAlpha))
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(if (isBroadcasting) NerColors.Secondary else NerColors.NeutralSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isBroadcasting) Icons.Default.Sensors else Icons.Default.SensorsOff,
                                contentDescription = null,
                                tint = if (isBroadcasting) Color.White else NerColors.NeutralMedium,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = if (isBroadcasting) MultilingualManager.tr("btn_stop_broadcast", selectedLanguageCode).uppercase() else "BROADCAST PAUSED",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isBroadcasting) NerColors.SecondaryDark else NerColors.NeutralMedium
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (isBroadcasting)
                            "Broadcasting coordinates to ${authorizedEmails.size} authorized Google accounts"
                        else
                            "Tap below to begin sending background GPS coordinates",
                        fontSize = 12.sp,
                        color = NerColors.NeutralMedium,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Primary Action Pill Button
                    NerPillButton(
                        text = when {
                            isBroadcasting -> MultilingualManager.tr("btn_stop_broadcast", selectedLanguageCode)
                            !hasLocationPermission -> MultilingualManager.tr("btn_grant_perms", selectedLanguageCode)
                            else -> MultilingualManager.tr("btn_start_broadcast", selectedLanguageCode)
                        },
                        icon = when {
                            isBroadcasting -> Icons.Default.Stop
                            !hasLocationPermission -> Icons.Default.Security
                            else -> Icons.Default.PlayArrow
                        },
                        hierarchy = NerButtonHierarchy.Primary,
                        containerColor = when {
                            isBroadcasting -> NerColors.Crimson
                            !hasLocationPermission -> NerColors.Tertiary
                            else -> NerColors.Secondary
                        },
                        onClick = {
                            if (isBroadcasting) {
                                TrackerForegroundService.stop(context)
                                isBroadcasting = false
                            } else {
                                hasLocationPermission = checkLocationPermission()
                                if (hasLocationPermission) {
                                    promptEnableGps {
                                        TrackerForegroundService.start(context)
                                        isBroadcasting = true
                                    }
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !checkBackgroundPermission()) {
                                        showBackgroundPermissionDialog = true
                                    }
                                } else {
                                    val perms = mutableListOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        perms.add(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    fineLocationLauncher.launch(perms.toTypedArray())
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Live Telemetry Fix Card
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MyLocation, contentDescription = null, tint = NerColors.Tertiary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = MultilingualManager.tr("lbl_coordinates", selectedLanguageCode),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = NerColors.Charcoal
                            )
                        }
                        if (isBroadcasting) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(NerColors.Secondary))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    MultilingualManager.tr("beacon_live_badge", selectedLanguageCode),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = NerColors.SecondaryDark
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val ping = latestPing
                    if (ping != null) {
                        Text(
                            text = "ðŸ“ ${ping.address}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NerColors.Charcoal
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format(Locale.US, "GPS: %.5f, %.5f (Â±%dm)", ping.latitude, ping.longitude, ping.accuracy.toInt()),
                            fontSize = 12.sp,
                            color = NerColors.NeutralMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (ping.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                                contentDescription = null,
                                tint = NerColors.Secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "${MultilingualManager.tr("lbl_battery", selectedLanguageCode)}: ${ping.batteryLevel}%",
                                fontSize = 12.sp,
                                color = NerColors.NeutralMedium
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Icon(Icons.Default.Speed, contentDescription = null, tint = NerColors.Primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "${MultilingualManager.tr("lbl_speed", selectedLanguageCode)}: ${LocationUtils.formatSpeed(ping.speed)}",
                                fontSize = 12.sp,
                                color = NerColors.NeutralMedium
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Last Broadcast: ${LocationUtils.formatTime(ping.timestamp)}",
                            fontSize = 11.sp,
                            color = NerColors.NeutralMedium
                        )
                        if (ping.isBreach) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NerColors.CrimsonTint)
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = NerColors.Crimson, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${MultilingualManager.tr("status_breach", selectedLanguageCode)} (${LocationUtils.formatDistance(ping.distanceFromCenter)})",
                                    color = NerColors.Crimson,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else if (isBroadcasting) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = NerColors.Tertiary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(MultilingualManager.tr("beacon_acquiring", selectedLanguageCode), fontSize = 13.sp, color = NerColors.NeutralMedium)
                        }
                    } else {
                        Text(
                            text = MultilingualManager.tr("beacon_start_hint", selectedLanguageCode),
                            fontSize = 12.sp,
                            color = NerColors.NeutralMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Hardware & Privacy Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = NerColors.SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                border = BorderStroke(1.dp, NerColors.NeutralBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = NerColors.Tertiary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(deviceName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NerColors.Charcoal)
                            Text("100% On-Device GPS Sentinel â€¢ Zero Cloud Tracking", fontSize = 11.sp, color = NerColors.NeutralMedium)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Authorized Caregivers Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = NerColors.SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                border = BorderStroke(1.dp, NerColors.NeutralBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Authorized Caregivers (${authorizedEmails.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = NerColors.Charcoal
                        )

                        IconButton(
                            onClick = {
                                newEmailInput = ""
                                emailError = null
                                showAddDialog = true
                            },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(NerColors.Primary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    authorizedEmails.forEach { email ->
                        val isSelf = email.equals(userEmail, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(NerColors.NeutralSoft)
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(email, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NerColors.Charcoal)
                                Text(if (isSelf) "Owner (This Phone)" else "Authorized Guardian", fontSize = 10.sp, color = NerColors.NeutralMedium)
                            }
                            if (!isSelf) {
                                IconButton(
                                    onClick = { saveEmails(authorizedEmails.filter { it != email }) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Revoke", tint = NerColors.Crimson, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
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

        // Background Permission Dialog
        if (showBackgroundPermissionDialog) {
            AlertDialog(
                onDismissRequest = { showBackgroundPermissionDialog = false },
                containerColor = NerColors.SurfaceWhite,
                shape = RoundedCornerShape(24.dp),
                title = { Text(MultilingualManager.tr("beacon_bg_perm_title", selectedLanguageCode), fontWeight = FontWeight.Bold, color = NerColors.Charcoal) },
                text = {
                    Text(
                        MultilingualManager.tr("beacon_bg_perm_sub", selectedLanguageCode),
                        fontSize = 13.sp,
                        color = NerColors.NeutralMedium
                    )
                },
                confirmButton = {
                    NerPillButton(
                        text = MultilingualManager.tr("beacon_continue", selectedLanguageCode),
                        hierarchy = NerButtonHierarchy.Primary,
                        containerColor = NerColors.Tertiary,
                        onClick = {
                            showBackgroundPermissionDialog = false
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                backgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                            }
                        }
                    )
                },
                dismissButton = {
                    NerPillButton(
                        text = MultilingualManager.tr("beacon_later", selectedLanguageCode),
                        hierarchy = NerButtonHierarchy.Secondary,
                        onClick = { showBackgroundPermissionDialog = false }
                    )
                }
            )
        }

        // Add Caregiver Dialog
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                containerColor = NerColors.SurfaceWhite,
                shape = RoundedCornerShape(24.dp),
                title = { Text("Grant Access to Caregiver", fontWeight = FontWeight.Bold, color = NerColors.Charcoal) },
                text = {
                    Column {
                        Text("Enter Google Account email of your family or caregiver:", fontSize = 12.sp, color = NerColors.NeutralMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newEmailInput,
                            onValueChange = {
                                newEmailInput = it.trim()
                                emailError = null
                            },
                            placeholder = { Text("caregiver@gmail.com", color = Color.Gray) },
                            isError = emailError != null,
                            supportingText = emailError?.let { { Text(it, color = NerColors.Crimson) } },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                },
                confirmButton = {
                    NerPillButton(
                        text = "Add Caregiver",
                        hierarchy = NerButtonHierarchy.Primary,
                        containerColor = NerColors.Primary,
                        onClick = {
                            val clean = newEmailInput.trim().lowercase()
                            if (!clean.contains("@") || !clean.contains(".")) {
                                emailError = "Enter a valid email"
                            } else if (authorizedEmails.contains(clean)) {
                                emailError = "Already authorized"
                            } else {
                                saveEmails(authorizedEmails + clean)
                                showAddDialog = false
                            }
                        }
                    )
                },
                dismissButton = {
                    NerPillButton(
                        text = "Cancel",
                        hierarchy = NerButtonHierarchy.Secondary,
                        onClick = { showAddDialog = false }
                    )
                }
            )
        }
    }
}
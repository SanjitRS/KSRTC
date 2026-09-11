package net.kibotu.geofencerelay.ui.guardian

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import net.kibotu.geofencerelay.model.GameSessionRecord
import net.kibotu.geofencerelay.model.PatientCognitiveTelemetry
import net.kibotu.geofencerelay.ui.theme.*
import net.kibotu.geofencerelay.util.LocationUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuardianScreen(
    googleAccountEmail: String,
    onBack: () -> Unit,
    onSignOut: () -> Unit = onBack,
    vm: GuardianViewModel = viewModel()
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: GPS Map, 1: Cognitive Score, 2: Games Played

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(googleAccountEmail) {
        vm.init(googleAccountEmail)
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab > 0) {
            vm.refreshPatientData()
        }
    }

    val isConnected by vm.isConnected.collectAsState()
    val zone by vm.zone.collectAsState()
    val targetPing by vm.targetPing.collectAsState()
    val latestAlert by vm.latestAlert.collectAsState()
    val isBreached by vm.isBreached.collectAsState()
    val broadcastSuccess by vm.broadcastSuccess.collectAsState()
    val recenterTrigger by vm.recenterTrigger.collectAsState()
    val isPlayingSound by vm.isPlayingSound.collectAsState()
    val patientTelemetry by vm.patientTelemetry.collectAsState()
    val targetPatientEmail by vm.targetPatientEmail.collectAsState()

    var showTargetPatientDialog by remember { mutableStateOf(false) }
    var editingPatientEmail by remember(targetPatientEmail) { mutableStateOf(targetPatientEmail) }
    var editingPatientAge by remember(patientTelemetry) {
        mutableStateOf((patientTelemetry?.biologicalAge ?: 68).toString())
    }

    var showZoneEditor by remember { mutableStateOf(false) }
    var sliderRadius by remember(zone.radiusMeters) {
        mutableFloatStateOf(zone.radiusMeters.toFloat().coerceIn(50f, 2000f))
    }

    if (showTargetPatientDialog) {
        AlertDialog(
            onDismissRequest = { showTargetPatientDialog = false },
            title = { Text("Pair Patient Device", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Enter the email or Google account configured in the Smaran Patient app:",
                        fontSize = 13.sp,
                        color = NerColors.Charcoal
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = editingPatientEmail,
                        onValueChange = { editingPatientEmail = it },
                        label = { Text("Patient Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = editingPatientAge,
                        onValueChange = { if (it.all { ch -> ch.isDigit() } && it.length <= 3) editingPatientAge = it },
                        label = { Text("Patient Biological Age (years)") },
                        placeholder = { Text("68") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Default: patient.device@smaran.local\nBoth phones auto-bridge via cross-pairing channels.",
                        fontSize = 11.sp,
                        color = NerColors.NeutralMedium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.setTargetPatientEmail(editingPatientEmail)
                        val ageNum = editingPatientAge.toIntOrNull()
                        if (ageNum != null && ageNum in 18..110) {
                            vm.updatePatientAge(ageNum)
                        }
                        showTargetPatientDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NerColors.Primary)
                ) {
                    Text("Save & Connect", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTargetPatientDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(NerColors.Primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "S",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "Smaran Caregiver Console",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                googleAccountEmail,
                                fontSize = 11.sp,
                                color = NerColors.Marigold
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    // Pair / Change Target Patient Device Button
                    IconButton(onClick = {
                        editingPatientEmail = targetPatientEmail
                        showTargetPatientDialog = true
                    }) {
                        Icon(Icons.Default.Settings, contentDescription = "Pair Patient Device", tint = Color.White)
                    }

                    // Sync / Refresh Patient Stats Button
                    IconButton(onClick = { vm.refreshPatientData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Fetch Patient Data", tint = Color.White)
                    }

                    // Online / Connecting Indicator
                    Box(
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isConnected) NerColors.Secondary else NerColors.Crimson)
                            .clickable { vm.reconnect() }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = if (isConnected) "LIVE" else "OFFLINE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Sign Out Button
                    IconButton(onClick = {
                        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE).edit().clear().commit()
                        onSignOut()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Sign Out", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NerColors.PrimaryDark)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = NerColors.SurfaceWhite, tonalElevation = 6.dp) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    label = { Text("GPS Radar") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NerColors.Primary,
                        selectedTextColor = NerColors.Primary,
                        indicatorColor = NerColors.PrimaryTint
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Psychology, contentDescription = null) },
                    label = { Text("Scores & CPS") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NerColors.Tertiary,
                        selectedTextColor = NerColors.Tertiary,
                        indicatorColor = NerColors.Tertiary.copy(alpha = 0.15f)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.SportsEsports, contentDescription = null) },
                    label = { Text("Games Log") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NerColors.Secondary,
                        selectedTextColor = NerColors.Secondary,
                        indicatorColor = NerColors.Secondary.copy(alpha = 0.15f)
                    )
                )
            }
        }
    ) { padding ->
        when (selectedTab) {
            // TAB 0: Interactive OpenStreetMap Live GPS Radar & Geofence
            0 -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(NerColors.CanvasWarm)
                ) {
                    // Top Woven Ribbon
                    NerWovenRibbon(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter),
                        height = 12.dp,
                        primaryColor = NerColors.Primary,
                        secondaryColor = NerColors.Secondary,
                        accentColor = NerColors.Marigold
                    )
                    // Interactive Map View
                    OsmMapView(
                        modifier = Modifier.fillMaxSize(),
                        zone = zone,
                        targetPing = targetPing,
                        isBreached = isBreached,
                        recenterTrigger = recenterTrigger,
                        onMapTapped = { lat, lon ->
                            if (showZoneEditor) {
                                vm.updateCenter(lat, lon)
                            }
                        }
                    )

                    // Safe Zone Breach Banner
                    AnimatedVisibility(
                        visible = isBreached || latestAlert != null,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(12.dp),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = NerColors.Crimson),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("SAFE ZONE BREACH DETECTED!", fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 14.sp)
                                    Text(
                                        "Target is outside '${zone.name}' (${LocationUtils.formatDistance(targetPing?.distanceFromCenter ?: 0.0)} from center).",
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    // Floating Recenter Button
                    FloatingActionButton(
                        onClick = { vm.triggerRecenter() },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 270.dp, end = 16.dp),
                        containerColor = NerColors.SurfaceWhite,
                        contentColor = NerColors.Tertiary,
                        shape = CircleShape,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                    ) {
                        Icon(Icons.Default.GpsFixed, contentDescription = "Recenter on Device")
                    }

                    // Bottom Control Panel
                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = NerColors.SurfaceWhite),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 14.dp),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NerColors.NeutralBorder))
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 20.dp, vertical = 14.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Drawer Handle
                            Box(
                                modifier = Modifier
                                    .size(36.dp, 4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(NerColors.NeutralBorder)
                                    .align(Alignment.CenterHorizontally)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Device Telemetry Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = targetPing?.deviceName ?: "Locating Beacon...",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NerColors.Charcoal
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = targetPing?.address ?: "Acquiring live GPS fix...",
                                        fontSize = 12.sp,
                                        color = NerColors.NeutralMedium,
                                        maxLines = 1
                                    )
                                }

                                // Status Badge
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isBreached) NerColors.Crimson else NerColors.Secondary
                                        )
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = if (isBreached) "BREACH" else "IN ZONE",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Distance, Speed, Last Ping row
                            if (targetPing != null) {
                                val lastSeen = LocationUtils.formatTime(targetPing!!.timestamp)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(NerColors.CanvasWarm)
                                        .border(1.dp, NerColors.NeutralBorder, RoundedCornerShape(10.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "Dist: ${LocationUtils.formatDistance(targetPing!!.distanceFromCenter)}",
                                        fontSize = 12.sp,
                                        color = NerColors.Tertiary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "Speed: ${LocationUtils.formatSpeed(targetPing!!.speed)}",
                                        fontSize = 12.sp,
                                        color = NerColors.NeutralMedium
                                    )
                                    Text(
                                        if (lastSeen == "Just now") "Live Ping" else "Seen: $lastSeen",
                                        fontSize = 12.sp,
                                        color = if (lastSeen == "Just now") NerColors.Secondary else NerColors.NeutralMedium,
                                        fontWeight = if (lastSeen == "Just now") FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // 4 Interactive Action Tiles
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // 1. Play Sound / Alarm
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            if (isPlayingSound) vm.stopSound() else vm.playSound()
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(CircleShape)
                                            .background(if (isPlayingSound) NerColors.Crimson else NerColors.Tertiary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Play Sound", tint = Color.White)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        if (isPlayingSound) "Stop Sound" else "Play Sound",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = NerColors.Charcoal
                                    )
                                }

                                // 2. Directions in Google Maps
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            val ping = targetPing
                                            val lat = ping?.latitude ?: zone.latitude
                                            val lon = ping?.longitude ?: zone.longitude
                                            if (lat != 0.0 && lon != 0.0) {
                                                val uri = "google.navigation:q=$lat,$lon"
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
                                                    setPackage("com.google.android.apps.maps")
                                                }
                                                try {
                                                    context.startActivity(intent)
                                                } catch (_: Exception) {
                                                    val webUri = "https://www.google.com/maps/dir/?api=1&destination=$lat,$lon"
                                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webUri)))
                                                }
                                            }
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(CircleShape)
                                            .background(NerColors.TertiaryLight),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Directions, contentDescription = "Directions", tint = Color.White)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Directions", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = NerColors.Charcoal)
                                }

                                // 3. Safe Zone Setup
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            showZoneEditor = !showZoneEditor
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(CircleShape)
                                            .background(if (showZoneEditor) NerColors.Primary else NerColors.PrimaryDark),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Security, contentDescription = "Safe Zone", tint = Color.White)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Safe Zone", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = NerColors.Charcoal)
                                }

                                // 4. Recenter Radar
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            vm.triggerRecenter()
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(CircleShape)
                                            .background(NerColors.Tertiary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.MyLocation, contentDescription = "Recenter Radar", tint = Color.White)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Center Radar", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = NerColors.Charcoal)
                                }
                            }

                            // Collapsible Safe Geofence Editor
                            AnimatedVisibility(visible = showZoneEditor) {
                                Column(
                                    modifier = Modifier
                                        .padding(top = 16.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(NerColors.CanvasWarm)
                                        .border(1.dp, NerColors.NeutralBorder, RoundedCornerShape(16.dp))
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Geofence Configuration",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = NerColors.Charcoal
                                        )
                                        Text(
                                            "${sliderRadius.toInt()} m radius",
                                            color = NerColors.Tertiary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        "Tap anywhere on the map to place center coordinate. Adjust boundary radius with the slider below.",
                                        fontSize = 11.sp,
                                        color = NerColors.NeutralMedium,
                                        lineHeight = 16.sp
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Slider(
                                        value = sliderRadius,
                                        onValueChange = {
                                            sliderRadius = it
                                            vm.updateRadius(it.toDouble())
                                        },
                                        valueRange = 50f..2000f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = NerColors.Primary,
                                            activeTrackColor = NerColors.Primary,
                                            inactiveTrackColor = NerColors.NeutralBorder
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = zone.name,
                                        onValueChange = { vm.updateName(it) },
                                        label = { Text("Zone Name (e.g., Home, Campus, Work)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Button(
                                        onClick = { vm.broadcastZone() },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(46.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = NerColors.Secondary),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            if (broadcastSuccess) "Safe Zone Synced to Device! ✓" else "Broadcast Safe Zone to Tracker",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // TAB 1: Patient Cognitive Scores & Clinical CPS Metrics
            1 -> {
                PatientCognitiveScoresTab(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(NerColors.CanvasWarm),
                    telemetry = patientTelemetry,
                    targetEmail = targetPatientEmail,
                    onSyncNow = { vm.refreshPatientData() },
                    onChangePatient = {
                        editingPatientEmail = targetPatientEmail
                        showTargetPatientDialog = true
                    }
                )
            }

            // TAB 2: Games Played Activity Log & Session History
            2 -> {
                GamesPlayedHistoryTab(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(NerColors.CanvasWarm),
                    sessions = patientTelemetry?.recentGameSessions ?: emptyList(),
                    totalToday = patientTelemetry?.totalGamesPlayedToday ?: 0,
                    targetEmail = targetPatientEmail,
                    onSyncNow = { vm.refreshPatientData() }
                )
            }
        }
    }
}

@Composable
fun PatientCognitiveScoresTab(
    modifier: Modifier = Modifier,
    telemetry: PatientCognitiveTelemetry?,
    targetEmail: String,
    onSyncNow: () -> Unit,
    onChangePatient: () -> Unit
) {
    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Pairing Status Bar
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = NerColors.PrimaryTint.copy(alpha = 0.45f)),
                border = BorderStroke(1.dp, NerColors.Primary.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onChangePatient() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null, tint = NerColors.Primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "Monitored Patient Device",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NerColors.NeutralMedium
                            )
                            Text(
                                targetEmail,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = NerColors.PrimaryDark
                            )
                        }
                    }
                    Button(
                        onClick = onSyncNow,
                        colors = ButtonDefaults.buttonColors(containerColor = NerColors.Primary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sync", fontSize = 12.sp, color = Color.White)
                    }
                }
            }
        }

        if (telemetry == null) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = NerColors.SurfaceWhite),
                    elevation = CardDefaults.cardElevation(3.dp),
                    border = BorderStroke(1.dp, NerColors.NeutralBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(NerColors.Tertiary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Psychology,
                                contentDescription = null,
                                tint = NerColors.Tertiary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            "Waiting for Cognitive Telemetry",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = NerColors.Charcoal
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "No scores received yet for $targetEmail.\nWhen the patient plays any brain exercise game on the Patient device, clinical CPS metrics and logs will automatically stream here!",
                            fontSize = 13.sp,
                            color = NerColors.NeutralMedium,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = onChangePatient,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Change Email", fontSize = 12.sp)
                            }
                            Button(
                                onClick = onSyncNow,
                                colors = ButtonDefaults.buttonColors(containerColor = NerColors.Primary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sync Now", fontSize = 12.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        } else {
            // 1. Executive Summary Card with Composite Score
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = NerColors.SurfaceWhite),
                    elevation = CardDefaults.cardElevation(3.dp),
                    border = BorderStroke(1.dp, NerColors.NeutralBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(if (telemetry.compositeCps >= 75.0) NerColors.Secondary else NerColors.Primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${telemetry.compositeCps.toInt()}",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                Text("CPS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.85f))
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Composite CPS Score",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = NerColors.Charcoal
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "Trajectory: ${telemetry.trajectoryStatus}",
                                fontSize = 13.sp,
                                color = NerColors.Secondary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Cognitive Age: ${telemetry.functionalCognitiveAge.toInt()} yrs (Bio: ${telemetry.biologicalAge} yrs)",
                                    fontSize = 12.sp,
                                    color = NerColors.NeutralMedium
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "✏️ Edit",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NerColors.Primary,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(NerColors.PrimaryTint)
                                        .clickable { onChangePatient() }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Cognitive Sub-Domain Radar Breakdown
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = NerColors.SurfaceWhite),
                    elevation = CardDefaults.cardElevation(2.dp),
                    border = BorderStroke(1.dp, NerColors.NeutralBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Clinical Sub-Domain Breakdown",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = NerColors.Charcoal
                            )
                            Icon(Icons.Default.Insights, contentDescription = null, tint = NerColors.Tertiary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        ScoreProgressBar("Memory Retention Index", telemetry.memoryRetentionIndex, NerColors.Tertiary)
                        ScoreProgressBar("Executive Function Index", telemetry.executiveFunctionIndex, NerColors.PlumMaroon)
                        ScoreProgressBar("Reaction Latency Score", telemetry.reactionLatencyScore, NerColors.Marigold)
                        ScoreProgressBar("Error Recovery Rate", telemetry.errorRecoveryRate, NerColors.Secondary)
                    }
                }
            }

            // 3. Circadian & Fatigue Risk Card
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = NerColors.SurfaceWhite),
                    elevation = CardDefaults.cardElevation(2.dp),
                    border = BorderStroke(1.dp, NerColors.NeutralBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (telemetry.circadianRisk == "Low") NerColors.Secondary.copy(alpha = 0.15f)
                                    else NerColors.Crimson.copy(alpha = 0.15f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Bedtime,
                                contentDescription = null,
                                tint = if (telemetry.circadianRisk == "Low") NerColors.Secondary else NerColors.Crimson,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Sundowning & Fatigue Status", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NerColors.Charcoal)
                            Text(
                                "Circadian Risk: ${telemetry.circadianRisk} • Fatigue Index: ${String.format(Locale.US, "%.2f", telemetry.fatigueIndex)}",
                                fontSize = 12.sp,
                                color = NerColors.NeutralMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScoreProgressBar(label: String, score: Double, color: Color) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = NerColors.Charcoal)
            Text("${score.toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
        }
        Spacer(modifier = Modifier.height(5.dp))
        LinearProgressIndicator(
            progress = { (score / 100f).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = NerColors.NeutralSoft
        )
    }
}

@Composable
fun GamesPlayedHistoryTab(
    modifier: Modifier = Modifier,
    sessions: List<GameSessionRecord>,
    totalToday: Int,
    targetEmail: String,
    onSyncNow: () -> Unit
) {
    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = NerColors.SurfaceWhite),
                border = BorderStroke(1.dp, NerColors.NeutralBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Games Played Today: $totalToday sessions",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = NerColors.Charcoal
                        )
                        Text(
                            "Device: $targetEmail",
                            fontSize = 11.sp,
                            color = NerColors.NeutralMedium
                        )
                    }
                    Button(
                        onClick = onSyncNow,
                        colors = ButtonDefaults.buttonColors(containerColor = NerColors.Secondary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sync", fontSize = 12.sp, color = Color.White)
                    }
                }
            }
        }

        if (sessions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.SportsEsports,
                            contentDescription = null,
                            tint = NerColors.NeutralMedium.copy(alpha = 0.5f),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No games recorded yet today.", fontWeight = FontWeight.SemiBold, color = NerColors.NeutralMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Play any brain game on the patient app to log activity here.", fontSize = 12.sp, color = NerColors.NeutralMedium)
                    }
                }
            }
        } else {
            items(sessions) { session ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = NerColors.SurfaceWhite),
                    elevation = CardDefaults.cardElevation(2.dp),
                    border = BorderStroke(1.dp, NerColors.NeutralBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(NerColors.PrimaryTint),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.SportsEsports, contentDescription = null, tint = NerColors.Primary, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(session.gameName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = NerColors.Charcoal)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "Accuracy: ${session.accuracyPercent.toInt()}% • Latency: ${session.averageLatencyMs}ms",
                                fontSize = 12.sp,
                                color = NerColors.NeutralMedium
                            )
                            Text(
                                "Rounds: ${session.roundsCompleted} • Errors: ${session.errors}",
                                fontSize = 11.sp,
                                color = NerColors.NeutralMedium
                            )
                            if (session.timestamp > 0) {
                                val timeFormatted = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(session.timestamp))
                                Text(
                                    "Played at $timeFormatted",
                                    fontSize = 10.sp,
                                    color = NerColors.NeutralMedium.copy(alpha = 0.8f)
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(NerColors.Secondary.copy(alpha = 0.15f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                "${session.score} pts",
                                fontWeight = FontWeight.Bold,
                                color = NerColors.Secondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

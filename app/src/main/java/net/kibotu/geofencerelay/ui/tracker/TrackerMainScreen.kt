package net.kibotu.geofencerelay.ui.tracker

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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
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
import net.kibotu.geofencerelay.service.TrackerForegroundService
import net.kibotu.geofencerelay.ui.theme.*
import net.kibotu.geofencerelay.util.BatteryUtils
import net.kibotu.geofencerelay.util.LocationUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerMainScreen(
    userEmail: String,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val servicePrefs = remember {
        context.getSharedPreferences(TrackerForegroundService.PREFS_NAME, Context.MODE_PRIVATE)
    }

    var isBroadcasting by remember {
        mutableStateOf(TrackerForegroundService.isRunning(context))
    }

    var authorizedEmails by remember {
        val currentSet = TrackerForegroundService.getAuthorizedEmails(context)
        mutableStateOf(if (currentSet.isNotEmpty()) currentSet.toList() else listOf(userEmail))
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

    val battery = remember { BatteryUtils.getBatteryStatus(context) }
    val deviceName = remember { LocationUtils.getFriendlyDeviceName(Build.MODEL) }

    fun checkBackgroundPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    var hasBackgroundPermission by remember {
        mutableStateOf(checkBackgroundPermission())
    }

    // Check if system location/GPS hardware is turned ON
    val locationManager = remember { context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager }
    var isGpsHardwareEnabled by remember {
        mutableStateOf(locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
                locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true)
    }

    // Google Play Services Dialog Launcher to turn on GPS with 1 tap
    val gpsResolutionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            isGpsHardwareEnabled = true
            TrackerForegroundService.start(context)
            isBroadcasting = true
        } else {
            // Even if dismissed, check state and start if permissions exist
            isGpsHardwareEnabled = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
                    locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
            TrackerForegroundService.start(context)
            isBroadcasting = true
        }
    }

    fun promptEnableGps(onGpsReady: () -> Unit) {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L).build()
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(true)
        val client = LocationServices.getSettingsClient(context)
        val task = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            isGpsHardwareEnabled = true
            onGpsReady()
        }
        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    val intentSenderRequest = IntentSenderRequest.Builder(exception.resolution.intentSender).build()
                    gpsResolutionLauncher.launch(intentSenderRequest)
                } catch (_: Exception) {
                    onGpsReady()
                }
            } else {
                onGpsReady()
            }
        }
    }

    val backgroundPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasBackgroundPermission = granted || checkBackgroundPermission()
        promptEnableGps {
            TrackerForegroundService.start(context)
            isBroadcasting = true
        }
    }

    val fineLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (granted) {
            promptEnableGps {
                TrackerForegroundService.start(context)
                isBroadcasting = true
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !checkBackgroundPermission()) {
                showBackgroundPermissionDialog = true
            }
        }
    }

    // Animated pulsing wave for transmission radar
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    fun saveEmails(list: List<String>) {
        authorizedEmails = list
        servicePrefs.edit().putStringSet(TrackerForegroundService.KEY_AUTHORIZED_EMAILS, list.toSet()).apply()
        TrackerForegroundService.notifyAuthorizedEmailsChanged(context)
    }

    // Auto-save initial user email
    LaunchedEffect(userEmail) {
        if (!authorizedEmails.contains(userEmail.lowercase())) {
            saveEmails(authorizedEmails + userEmail.lowercase())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(IrctcSaffron),
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
                                "Smaran Sentinel Beacon",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color.White
                            )
                            Text(
                                userEmail,
                                fontSize = 12.sp,
                                color = IrctcGold
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE).edit().clear().apply()
                        onSignOut()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Sign Out",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = IrctcNavy)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(IrctcCanvas)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // GPS Hardware Turned Off Warning Banner
            if (!isGpsHardwareEnabled) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clickable { promptEnableGps { isBroadcasting = true } },
                    colors = CardDefaults.cardColors(containerColor = IrctcSaffronLight),
                    shape = RoundedCornerShape(14.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(IrctcSaffron))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.LocationOff, contentDescription = null, tint = IrctcSaffron, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Device GPS is Turned OFF", fontWeight = FontWeight.Bold, color = IrctcOrangeDark, fontSize = 13.sp)
                            Text("Tap here to turn on Google High-Accuracy GPS with one tap.", color = IrctcTextSecondary, fontSize = 11.sp)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = IrctcSaffron)
                    }
                }
            }

            // Background Permission Banner
            if (!hasBackgroundPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clickable { showBackgroundPermissionDialog = true },
                    colors = CardDefaults.cardColors(containerColor = IrctcGoldLight),
                    shape = RoundedCornerShape(14.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(IrctcGold))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = IrctcOrangeDark, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Background Permission Needed", fontWeight = FontWeight.Bold, color = IrctcTextPrimary, fontSize = 13.sp)
                            Text("Tap to set 'Allow all the time' for continuous 24/7 background tracking.", color = IrctcTextSecondary, fontSize = 11.sp)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = IrctcOrangeDark)
                    }
                }
            } else if (hasBackgroundPermission) {
                Row(
                    modifier = Modifier
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(IrctcGreenLight)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = IrctcGreen, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Background Location: Allowed All the Time", fontSize = 11.sp, color = IrctcGreen, fontWeight = FontWeight.SemiBold)
                }
            }

            // 1. Live Transmission Radar Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = IrctcCardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                shape = RoundedCornerShape(20.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(IrctcCardBorder))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Pulsing animated icon
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(110.dp)) {
                        if (isBroadcasting) {
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .scale(pulseScale)
                                    .clip(CircleShape)
                                    .background(IrctcGreen.copy(alpha = pulseAlpha))
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(if (isBroadcasting) IrctcGreen else IrctcRoyal),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isBroadcasting) Icons.Default.Sensors else Icons.Default.SensorsOff,
                                contentDescription = "Status",
                                tint = Color.White,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = if (isBroadcasting) "LIVE LOCATION BROADCASTING" else "BROADCASTING PAUSED",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isBroadcasting) IrctcGreen else IrctcTextSecondary,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (isBroadcasting)
                            "Broadcasting live high-accuracy GPS fixes to ${authorizedEmails.size} authorized Google account(s)"
                        else
                            "Tap button below to activate live background satellite transmission",
                        fontSize = 12.sp,
                        color = IrctcTextSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Broadcast Switch Button
                    Button(
                        onClick = {
                            if (isBroadcasting) {
                                TrackerForegroundService.stop(context)
                                isBroadcasting = false
                            } else {
                                val hasFine = ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.ACCESS_FINE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED

                                if (hasFine) {
                                    promptEnableGps {
                                        TrackerForegroundService.start(context)
                                        isBroadcasting = true
                                    }
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !checkBackgroundPermission()) {
                                        showBackgroundPermissionDialog = true
                                    }
                                } else {
                                    val perms = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        perms.add(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    fineLocationLauncher.launch(perms.toTypedArray())
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isBroadcasting) IrctcRed else IrctcSaffron
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isBroadcasting) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (isBroadcasting) "Stop Broadcasting" else "Start Live Broadcasting",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Hardware Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = IrctcCardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(IrctcCardBorder))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(deviceName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = IrctcTextPrimary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("High-Accuracy Hardware GPS • Free Zero Cloud Cost", fontSize = 11.sp, color = IrctcTextSecondary)
                        }


                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Authorized Viewers Section (Who Can See This Phone)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = IrctcCardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(IrctcCardBorder))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Who Can View My Location",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = IrctcTextPrimary
                            )
                            Text(
                                "Authorized Google Accounts (${authorizedEmails.size})",
                                fontSize = 11.sp,
                                color = IrctcTextSecondary
                            )
                        }

                        IconButton(
                            onClick = {
                                newEmailInput = ""
                                emailError = null
                                showAddDialog = true
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(IrctcRoyal)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Grant Access", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // List of authorized emails
                    authorizedEmails.forEach { email ->
                        val isSelf = email.equals(userEmail, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(IrctcCanvas)
                                .border(1.dp, IrctcCardBorder, RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelf) IrctcRoyal else IrctcSaffron),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        email.take(1).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        email,
                                        fontSize = 13.sp,
                                        color = IrctcTextPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        if (isSelf) "Owner (This Phone)" else "Authorized Guardian",
                                        fontSize = 10.sp,
                                        color = if (isSelf) IrctcGreen else IrctcTextSecondary
                                    )
                                }
                            }

                            if (!isSelf) {
                                IconButton(
                                    onClick = {
                                        saveEmails(authorizedEmails.filter { it != email })
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Revoke", tint = IrctcRed, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Privacy Guarantee Notice
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = IrctcCardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(IrctcCardBorder))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = IrctcRoyal, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Battery & Privacy Shield: Map rendering is disabled on this beacon device to conserve battery life. Only authorized guardians can inspect your location.",
                        fontSize = 11.sp,
                        color = IrctcTextSecondary,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // Add Authorized Email Dialog
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Grant Access to Google Account", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = IrctcTextPrimary) },
                text = {
                    Column {
                        Text(
                            "Enter the Google Account email of the guardian you want to allow to track this phone:",
                            fontSize = 12.sp,
                            color = IrctcTextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = newEmailInput,
                            onValueChange = {
                                newEmailInput = it.trim()
                                emailError = null
                            },
                            label = { Text("Google Account Email") },
                            placeholder = { Text("guardian@gmail.com") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = IrctcRoyal) },
                            isError = emailError != null,
                            supportingText = emailError?.let { { Text(it, color = IrctcRed) } },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val clean = newEmailInput.trim().lowercase()
                            if (!clean.contains("@") || !clean.contains(".")) {
                                emailError = "Please enter a valid Google email address"
                            } else if (authorizedEmails.contains(clean)) {
                                emailError = "This account is already authorized"
                            } else {
                                saveEmails(authorizedEmails + clean)
                                showAddDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IrctcSaffron),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Grant Access", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text("Cancel", color = IrctcTextSecondary)
                    }
                },
                containerColor = IrctcCardBg,
                shape = RoundedCornerShape(18.dp)
            )
        }

        // Background Location Guidance Dialog ("Allow all the time")
        if (showBackgroundPermissionDialog) {
            AlertDialog(
                onDismissRequest = { showBackgroundPermissionDialog = false },
                icon = {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(IrctcGoldLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = IrctcOrangeDark, modifier = Modifier.size(32.dp))
                    }
                },
                title = {
                    Text(
                        "Set Location to 'Allow all the time'",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = IrctcTextPrimary,
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "To ensure your device can be found even when the screen is off or when you are using other apps, Android requires you to select:\n\n🔘 Allow all the time\n\nin the permission settings.",
                            fontSize = 13.sp,
                            color = IrctcTextSecondary,
                            lineHeight = 18.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showBackgroundPermissionDialog = false
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                backgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                            } else {
                                promptEnableGps {
                                    TrackerForegroundService.start(context)
                                    isBroadcasting = true
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IrctcSaffron),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Open Permission Setting", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showBackgroundPermissionDialog = false
                        promptEnableGps {
                            TrackerForegroundService.start(context)
                            isBroadcasting = true
                        }
                    }) {
                        Text("Continue Anyway", color = IrctcTextSecondary)
                    }
                },
                containerColor = IrctcCardBg,
                shape = RoundedCornerShape(18.dp)
            )
        }
    }
}

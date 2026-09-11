package net.kibotu.geofencerelay.ui.tracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import net.kibotu.geofencerelay.ui.theme.Blue500
import net.kibotu.geofencerelay.ui.theme.Emerald500
import net.kibotu.geofencerelay.ui.theme.Red500
import net.kibotu.geofencerelay.ui.theme.Slate700
import net.kibotu.geofencerelay.ui.theme.Slate800
import net.kibotu.geofencerelay.ui.theme.Slate900
import net.kibotu.geofencerelay.util.LocationUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerScreen(
    onBack: () -> Unit,
    vm: TrackerViewModel = viewModel()
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        vm.loadAuthorizedEmails(context)
    }

    val isConnected by vm.isConnected.collectAsState()
    val activeZone by vm.activeZone.collectAsState()
    val isRunning by vm.isServiceRunning.collectAsState()
    val latestPing by vm.latestPing.collectAsState()
    val isBreached by vm.isBreached.collectAsState()
    val authorizedEmails by vm.authorizedEmails.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var newEmailInput by remember { mutableStateOf("") }

    var hasPermissions by remember {
        val fineLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        mutableStateOf(fineLocation)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasPermissions = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true
    }

    fun requestRequiredPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Location Sentinel", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "${authorizedEmails.size} Google Accounts Authorized",
                            fontSize = 12.sp,
                            color = Color(0xFFA5B4FC)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isConnected) Emerald500 else Red500)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isConnected) "ONLINE" else "DISCONNECTED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Slate900)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Slate900)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Main Status Banner Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        !isRunning -> Slate800
                        isBreached -> Red500
                        else -> Emerald500
                    }
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                !isRunning -> Icons.Default.Security
                                isBreached -> Icons.Default.Warning
                                else -> Icons.Default.CheckCircle
                            },
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = when {
                            !isRunning -> "TRACKING STANDBY"
                            isBreached -> "?? BREACH DETECTED!"
                            else -> "??? SECURE (INSIDE SAFE ZONE)"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = when {
                            !isRunning -> "Tap 'Start Location Sharing' below to begin."
                            isBreached -> "Device has crossed boundary of '${activeZone?.name ?: "Safe Zone"}'. High-rate GPS streaming is active."
                            else -> "Device is safely inside '${activeZone?.name ?: "Safe Zone"}'. Operating in battery-saver mode."
                        },
                        fontSize = 12.sp,
                        color = Color(0xEEFFFFFF),
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Authorized Google Accounts Section (Find My Sharing List)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Slate800),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Blue500, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Who Can See This Phone",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                        }

                        OutlinedButton(
                            onClick = { showAddDialog = true },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (authorizedEmails.isEmpty()) {
                        Text(
                            "No Google Accounts added yet. Click '+ Share' above to allow your guardian to track this phone.",
                            fontSize = 12.sp,
                            color = Color.LightGray,
                            lineHeight = 16.sp
                        )
                    } else {
                        authorizedEmails.forEach { email ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Slate700)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(email, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                                IconButton(
                                    onClick = { vm.removeAuthorizedEmail(context, email) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Revoke", tint = Red500, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Live Telemetry Details
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Slate800),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Live GPS Fix & Address",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (latestPing != null) {
                        val ping = latestPing!!
                        Text("Address: ${ping.address}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text("GPS: %.5f, %.5f (±%dm)".format(ping.latitude, ping.longitude, ping.accuracy.toInt()), color = Color.Gray, fontSize = 12.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.BatteryFull, contentDescription = null, tint = Emerald500, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Battery: ${ping.batteryLevel}%", color = Color.Gray, fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Speed: ${LocationUtils.formatSpeed(ping.speed)}", color = Color.Gray, fontSize = 12.sp)
                        }
                        Text("Last Broadcast: ${LocationUtils.formatTime(ping.timestamp)}", color = Color.Gray, fontSize = 11.sp)
                    } else {
                        Text(
                            "Acquiring exact GPS satellite fix...",
                            color = Color.LightGray,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Start / Stop Sentinel Button
            if (!hasPermissions) {
                Button(
                    onClick = { requestRequiredPermissions() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Grant Location Permissions", fontWeight = FontWeight.Bold)
                }
            } else {
                if (!isRunning) {
                    Button(
                        onClick = { vm.startTracking(context) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Location Sharing", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                } else {
                    Button(
                        onClick = { vm.stopTracking(context) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Red500),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Stop Location Sharing", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }

    // Dialog to add an authorized Google email
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Authorize Google Account") },
            text = {
                Column {
                    Text(
                        "Enter the Google Account email of the person who is allowed to track this device:",
                        fontSize = 13.sp,
                        color = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newEmailInput,
                        onValueChange = { newEmailInput = it.trim() },
                        label = { Text("e.g. guardian@gmail.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newEmailInput.isNotBlank() && newEmailInput.contains("@")) {
                            vm.addAuthorizedEmail(context, newEmailInput)
                            newEmailInput = ""
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Blue500)
                ) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

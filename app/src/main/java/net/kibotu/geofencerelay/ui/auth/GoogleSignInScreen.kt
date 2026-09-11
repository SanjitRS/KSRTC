package net.kibotu.geofencerelay.ui.auth

import android.accounts.AccountManager
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import net.kibotu.geofencerelay.R
import net.kibotu.geofencerelay.features.ai.localization.MultilingualManager
import net.kibotu.geofencerelay.features.ai.ui.theme.GoogleColors
import net.kibotu.geofencerelay.ui.theme.*
import net.kibotu.geofencerelay.features.ai.ui.theme.IosColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleSignInScreen(
    appTitle: String = "Smaran",
    appSubtitle: String = "Assistive Sentinel & Cognitive AI",
    isTrackerMode: Boolean = false,
    onSignInSuccess: (email: String) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE) }
    val settingsPrefs = remember { context.getSharedPreferences("app_settings", Context.MODE_PRIVATE) }
    var selectedLanguageCode by remember {
        mutableStateOf(settingsPrefs.getString("selected_language", "en") ?: "en")
    }

    var rememberDevice by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var detectedAccounts by remember { mutableStateOf<List<String>>(emptyList()) }

    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
    }
    val googleSignInClient: GoogleSignInClient = remember {
        GoogleSignIn.getClient(context, gso)
    }

    fun saveAuthAndProceed(email: String) {
        val clean = email.trim().lowercase()
        prefs.edit()
            .putBoolean("is_device_remembered", rememberDevice)
            .putBoolean("is_device_authenticated", true)
            .putString("user_google_email", clean)
            .putString("user_role", if (isTrackerMode) "tracker" else "guardian")
            .commit()
        onSignInSuccess(clean)
    }

    fun refreshDetectedAccounts() {
        try {
            val am = AccountManager.get(context)
            val googleAccounts = am.getAccountsByType("com.google")
            detectedAccounts = googleAccounts.map { it.name.lowercase() }
        } catch (_: Exception) {}
    }

    LaunchedEffect(Unit) {
        // Auto-login if device was remembered
        val isRemembered = prefs.getBoolean("is_device_remembered", false) || prefs.getBoolean("is_device_authenticated", false)
        val savedEmail = prefs.getString("user_google_email", null)
        if (isRemembered && !savedEmail.isNullOrBlank()) {
            onSignInSuccess(savedEmail)
            return@LaunchedEffect
        }
        val lastAccount = GoogleSignIn.getLastSignedInAccount(context)
        if (lastAccount != null && !lastAccount.email.isNullOrBlank()) {
            saveAuthAndProceed(lastAccount.email!!)
            return@LaunchedEffect
        }
        refreshDetectedAccounts()
    }

    val accountChooserLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isLoading = false
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val accountName = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            if (!accountName.isNullOrBlank()) {
                saveAuthAndProceed(accountName)
            }
        }
        refreshDetectedAccounts()
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isLoading = false
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account: GoogleSignInAccount = task.getResult(ApiException::class.java)
            val email = account.email
            if (!email.isNullOrBlank()) {
                saveAuthAndProceed(email)
            } else {
                errorMessage = "Google Account did not return an email address."
            }
        } catch (e: ApiException) {
            Log.e("GoogleSignIn", "Play Services sign-in code: ${e.statusCode}", e)
            try {
                val intent = AccountManager.newChooseAccountIntent(
                    null, null, arrayOf("com.google"), null, null, null, null
                )
                accountChooserLauncher.launch(intent)
            } catch (ex: Exception) {
                errorMessage = "Google Sign-In was cancelled or not configured."
            }
        }
    }

    fun launchDirectGoogleSignIn() {
        isLoading = true
        errorMessage = null
        try {
            val signInIntent: Intent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        } catch (e: Exception) {
            isLoading = false
            try {
                val intent = AccountManager.newChooseAccountIntent(
                    null, null, arrayOf("com.google"), null, null, null, null
                )
                accountChooserLauncher.launch(intent)
            } catch (ex: Exception) {
                errorMessage = "Unable to start sign in: ${e.message}"
            }
        }
    }

    // Regional Artistic Palette
    val saffronGold = Color(0xFFFFB300)
    val warmTerracotta = Color(0xFFFF7043)
    val deepEmerald = Color(0xFF00C853)
    val richIndigo = Color(0xFF3D5AFE)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NerColors.CanvasWarm)
    ) {
        // Subtle Folk Mandala Background Motif
        NerMandalaWatermark(
            modifier = Modifier.fillMaxSize(),
            baseColor = NerColors.Primary,
            alpha = 0.04f
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                // Language Switcher Row at Top of Login Screen
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    MultilingualManager.supportedLanguages.forEach { lang ->
                        val isSelected = lang.code == selectedLanguageCode
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) saffronGold.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f))
                                .border(
                                    1.dp,
                                    if (isSelected) saffronGold else Color.White.copy(alpha = 0.15f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    selectedLanguageCode = lang.code
                                    settingsPrefs.edit().putString("selected_language", lang.code).commit()
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(lang.flagEmoji, fontSize = 13.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = lang.nativeName,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) saffronGold else Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // App Logo
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(
                                listOf(saffronGold, warmTerracotta, deepEmerald, richIndigo, saffronGold)
                            )
                        )
                        .padding(3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(NerColors.SurfaceWhite)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.smaran_logo),
                            contentDescription = "Smaran Logo",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // App Name Only
                Text(
                    text = "SMARAN",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = NerColors.Charcoal,
                    letterSpacing = 1.5.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Exact Tagline Required by User
                Text(
                    text = "Guiding minds. Supporting families",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = NerColors.NeutralMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))
            }

            // Central Sign-In Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NerColors.SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, NerColors.NeutralBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Detected accounts 1-tap select list
                    if (detectedAccounts.isNotEmpty()) {
                        detectedAccounts.take(2).forEach { acc ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(NerColors.CanvasWarm)
                                    .border(1.dp, NerColors.NeutralBorder, RoundedCornerShape(14.dp))
                                    .clickable {
                                        saveAuthAndProceed(acc)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(richIndigo, saffronGold)
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        acc.take(1).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 15.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(acc, fontSize = 13.sp, color = NerColors.Charcoal, fontWeight = FontWeight.SemiBold)
                                    Text("1-Tap Sign In", fontSize = 10.sp, color = deepEmerald, fontWeight = FontWeight.Medium)
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = NerColors.NeutralMedium)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Google Sign-In Button
                    Button(
                        onClick = { launchDirectGoogleSignIn() },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NerColors.Primary
                        ),
                        shape = RoundedCornerShape(14.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                MultilingualManager.tr("auth_signing_in", selectedLanguageCode),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("G", fontWeight = FontWeight.Black, color = GoogleColors.Blue, fontSize = 15.sp)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    if (detectedAccounts.isNotEmpty())
                                        MultilingualManager.tr("auth_choose_another", selectedLanguageCode)
                                    else
                                        MultilingualManager.tr("auth_continue_google", selectedLanguageCode),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // âš¡ Quick Device Access (Continue as This Device)
                    OutlinedButton(
                        onClick = {
                            val devEmail = if (isTrackerMode) "patient.device@smaran.local" else "guardian.device@smaran.local"
                            saveAuthAndProceed(devEmail)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, saffronGold.copy(alpha = 0.7f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = saffronGold)
                    ) {
                        Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = saffronGold, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = MultilingualManager.tr("auth_quick_access", selectedLanguageCode),
                            color = saffronGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Remember This Device Checkbox
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { rememberDevice = !rememberDevice }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Checkbox(
                            checked = rememberDevice,
                            onCheckedChange = { rememberDevice = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = deepEmerald,
                                checkmarkColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = MultilingualManager.tr("auth_remember_device", selectedLanguageCode),
                            fontSize = 11.sp,
                            color = NerColors.Charcoal,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    AnimatedVisibility(visible = errorMessage != null) {
                        errorMessage?.let { msg ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                color = IosColors.SystemRed.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, IosColors.SystemRed)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = IosColors.SystemRed, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(msg, color = IosColors.SystemRed, fontSize = 11.sp, lineHeight = 15.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}
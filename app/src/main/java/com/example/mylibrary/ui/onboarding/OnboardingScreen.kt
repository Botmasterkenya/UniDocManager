package com.example.mylibrary.ui.onboarding

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Colors ────────────────────────────────────────────────────────────────────
private val DarkBg        = Color(0xFF141414)
private val CardBg        = Color(0xFF1F1F1F)
private val CardBorder    = Color(0xFF2A2A2A)
private val NetflixRed    = Color(0xFFE50914)
private val TextPrimary   = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFF999999)

// ── Prefs helper ──────────────────────────────────────────────────────────────
const val PREFS_NAME    = "tees_library_prefs"
const val KEY_NAME      = "user_name"
const val KEY_REG_NO    = "user_reg_no"
const val KEY_PROGRAMME = "user_programme"
const val KEY_ONBOARDED = "onboarded"

fun saveUserProfile(context: Context, name: String, regNo: String, programme: String) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        .putString(KEY_NAME, name)
        .putString(KEY_REG_NO, regNo)
        .putString(KEY_PROGRAMME, programme)
        .putBoolean(KEY_ONBOARDED, true)
        .apply()
}

fun isOnboarded(context: Context): Boolean =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getBoolean(KEY_ONBOARDED, false)

fun getUserName(context: Context): String =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(KEY_NAME, "Student") ?: "Student"

// ── OnboardingScreen ──────────────────────────────────────────────────────────
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val context      = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scrollState  = rememberScrollState()

    var name      by remember { mutableStateOf("") }
    var regNo     by remember { mutableStateOf("") }
    var programme by remember { mutableStateOf("") }

    // Animate logo in
    var startAnim by remember { mutableStateOf(false) }
    val logoAlpha by animateFloatAsState(
        targetValue   = if (startAnim) 1f else 0f,
        animationSpec = tween(800),
        label         = "logoAlpha"
    )
    val slideUp by animateFloatAsState(
        targetValue   = if (startAnim) 0f else 40f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label         = "slideUp"
    )
    LaunchedEffect(Unit) { startAnim = true }
    val animModifier = Modifier
        .alpha(logoAlpha)
        .offset(y = slideUp.dp)

    val canProceed = name.isNotBlank() && regNo.isNotBlank() && programme.isNotBlank()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // Top gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(NetflixRed.copy(alpha = 0.15f), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // Logo / header
            Box(
                modifier         = animModifier,
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📚", fontSize = 56.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text          = "Tee's Library",
                        color         = NetflixRed,
                        fontSize      = 32.sp,
                        fontWeight    = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text     = "Let's set up your profile",
                        color    = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Form card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardBg)
                    .padding(24.dp)
            ) {
                Column {
                    Text(
                        text       = "Your Details",
                        color      = TextPrimary,
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text     = "This will appear on your profile",
                        color    = TextSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Full Name
                    OnboardingField(
                        value         = name,
                        onValueChange = { name = it },
                        label         = "Full Name",
                        placeholder   = "e.g. Mucho Key",
                        icon          = Icons.Default.Person,
                        imeAction     = ImeAction.Next,
                        onNext        = { focusManager.moveFocus(FocusDirection.Down) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Reg No
                    OnboardingField(
                        value         = regNo,
                        onValueChange = { regNo = it },
                        label         = "Registration Number",
                        placeholder   = "e.g. CT300-0001/2022",
                        icon          = Icons.Default.Badge,
                        imeAction     = ImeAction.Next,
                        onNext        = { focusManager.moveFocus(FocusDirection.Down) },
                        caps          = KeyboardCapitalization.Characters
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Programme
                    OnboardingField(
                        value         = programme,
                        onValueChange = { programme = it },
                        label         = "Programme / Course",
                        placeholder   = "e.g. BBIT Year 3",
                        icon          = Icons.Default.School,
                        imeAction     = ImeAction.Done,
                        onNext        = { focusManager.clearFocus() }
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // Get Started button
                    Button(
                        onClick = {
                            if (canProceed) {
                                saveUserProfile(context, name.trim(), regNo.trim(), programme.trim())
                                onFinished()
                            }
                        },
                        enabled  = canProceed,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor         = NetflixRed,
                            disabledContainerColor = NetflixRed.copy(alpha = 0.3f)
                        )
                    ) {
                        Text(
                            text       = "Get Started  →",
                            color      = Color.White,
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text     = "Your details are stored only on this device",
                color    = TextSecondary,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ── Reusable field ────────────────────────────────────────────────────────────
@Composable
fun OnboardingField(
    value:         String,
    onValueChange: (String) -> Unit,
    label:         String,
    placeholder:   String,
    icon:          ImageVector,
    imeAction:     ImeAction,
    onNext:        () -> Unit,
    caps:          KeyboardCapitalization = KeyboardCapitalization.Words
) {
    Text(label, color = TextSecondary, fontSize = 12.sp, letterSpacing = 0.5.sp)
    Spacer(modifier = Modifier.height(6.dp))
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        placeholder   = { Text(placeholder, color = TextSecondary, fontSize = 14.sp) },
        leadingIcon   = { Icon(icon, contentDescription = null, tint = if (value.isNotBlank()) NetflixRed else TextSecondary, modifier = Modifier.size(20.dp)) },
        singleLine    = true,
        modifier      = Modifier.fillMaxWidth(),
        shape         = RoundedCornerShape(10.dp),
        colors        = OutlinedTextFieldDefaults.colors(
            focusedBorderColor    = NetflixRed,
            unfocusedBorderColor  = Color(0xFF2A2A2A),
            focusedTextColor      = TextPrimary,
            unfocusedTextColor    = TextPrimary,
            cursorColor           = NetflixRed
        ),
        keyboardOptions = KeyboardOptions(
            capitalization = caps,
            imeAction      = imeAction
        ),
        keyboardActions = KeyboardActions(
            onNext = { onNext() },
            onDone = { onNext() }
        )
    )
}
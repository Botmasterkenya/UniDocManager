package com.example.mylibrary.ui.navigation

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.mylibrary.data.LibraryRepository
import com.example.mylibrary.ui.onboarding.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val DarkBg        = Color(0xFF141414)
private val CardBg        = Color(0xFF1F1F1F)
private val CardBorder    = Color(0xFF2A2A2A)
private val NetflixRed    = Color(0xFFE50914)
private val NoteBlue      = Color(0xFF4A90D9)
private val FolderAmber   = Color(0xFFE8A838)
private val TextPrimary   = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFF999999)
private val InputBg       = Color(0xFF2A2A2A)
private val GreenBg       = Color(0xFF1A2E1A)
private val GreenText     = Color(0xFF6BCB77)

// ── SharedPrefs key for avatar URI ────────────────────────────────────────────
const val KEY_AVATAR_URI = "user_avatar_uri"

fun saveAvatarUri(context: Context, uri: String) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit().putString(KEY_AVATAR_URI, uri).apply()
}

fun getAvatarUri(context: Context): String? =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(KEY_AVATAR_URI, null)

@Composable
fun ProfileTab() {
    val context      = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope        = rememberCoroutineScope()
    val scrollState  = rememberScrollState()

    val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)

    var name      by remember { mutableStateOf(prefs.getString(KEY_NAME,      "") ?: "") }
    var regNo     by remember { mutableStateOf(prefs.getString(KEY_REG_NO,    "") ?: "") }
    var programme by remember { mutableStateOf(prefs.getString(KEY_PROGRAMME, "") ?: "") }
    var avatarUri by remember { mutableStateOf(getAvatarUri(context)) }

    var isEditing   by remember { mutableStateOf(false) }
    var savedBanner by remember { mutableStateOf(false) }

    var editName      by remember { mutableStateOf(name) }
    var editRegNo     by remember { mutableStateOf(regNo) }
    var editProgramme by remember { mutableStateOf(programme) }

    var unitCount  by remember { mutableStateOf(0) }
    var noteCount  by remember { mutableStateOf(0) }
    var paperCount by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        val repo  = LibraryRepository(context)
        val units = repo.allUnits.first()
        unitCount = units.size
        var nc = 0; var pc = 0
        units.forEach { unit ->
            nc += repo.notesFor(unit.id).first().size
            pc += repo.papersFor(unit.id).first().size
        }
        noteCount  = nc
        paperCount = pc
    }

    // ── Image picker ──────────────────────────────────────────────────────────
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            // Persist permission so image loads after app restart
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            val uriString = uri.toString()
            saveAvatarUri(context, uriString)
            avatarUri = uriString
        }
    }

    fun saveProfile() {
        if (editName.isBlank()) return
        name      = editName.trim()
        regNo     = editRegNo.trim()
        programme = editProgramme.trim()
        saveUserProfile(context, name, regNo, programme)
        isEditing   = false
        savedBanner = true
        scope.launch {
            kotlinx.coroutines.delay(2500L)
            savedBanner = false
        }
    }

    fun cancelEdit() {
        editName      = name
        editRegNo     = regNo
        editProgramme = programme
        isEditing     = false
        focusManager.clearFocus()
    }

    Column(
        modifier            = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // ── Top bar ───────────────────────────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                text          = "Profile",
                color         = NetflixRed,
                fontSize      = 22.sp,
                fontWeight    = FontWeight.Black,
                letterSpacing = 1.sp
            )
            if (!isEditing) {
                IconButton(onClick = {
                    editName      = name
                    editRegNo     = regNo
                    editProgramme = programme
                    isEditing     = true
                }) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Edit profile", tint = TextSecondary)
                }
            } else {
                TextButton(onClick = { cancelEdit() }) {
                    Text("Cancel", color = TextSecondary, fontSize = 14.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Avatar ────────────────────────────────────────────────────────────
        Box(contentAlignment = Alignment.BottomEnd) {

            // Avatar circle
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(48.dp))
                    .background(NetflixRed.copy(alpha = 0.15f))
                    .border(
                        width = if (isEditing) 2.dp else 1.dp,
                        color = if (isEditing) NetflixRed else CardBorder,
                        shape = RoundedCornerShape(48.dp)
                    )
                    .clickable(enabled = isEditing) {
                        imagePicker.launch(arrayOf("image/*"))
                    },
                contentAlignment = Alignment.Center
            ) {
                if (avatarUri != null) {
                    // Load saved image using Coil
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(Uri.parse(avatarUri))
                            .crossfade(true)
                            .build(),
                        contentDescription = "Profile picture",
                        contentScale       = ContentScale.Crop,
                        modifier           = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(48.dp))
                    )
                } else {
                    // Fallback — first letter or emoji
                    Text(
                        text       = name.firstOrNull()?.uppercase() ?: "👤",
                        fontSize   = if (name.isNotBlank()) 36.sp else 44.sp,
                        color      = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Camera badge — only visible in edit mode
            if (isEditing) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(NetflixRed)
                        .border(2.dp, DarkBg, RoundedCornerShape(15.dp))
                        .clickable { imagePicker.launch(arrayOf("image/*")) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = Icons.Filled.CameraAlt,
                        contentDescription = "Change photo",
                        tint               = Color.White,
                        modifier           = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Tap to change hint shown in edit mode
        if (isEditing) {
            Text(
                text     = "Tap photo to change",
                color    = TextSecondary,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (!isEditing) {
            Text(
                text       = name.ifBlank { "Student" },
                color      = TextPrimary,
                fontSize   = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text     = programme.ifBlank { "No programme set" },
                color    = TextSecondary,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Stats row ─────────────────────────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProfileStat("$unitCount",  "Units",  FolderAmber, Modifier.weight(1f))
            ProfileStat("$noteCount",  "Notes",  NoteBlue,    Modifier.weight(1f))
            ProfileStat("$paperCount", "Papers", NetflixRed,  Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Saved banner ──────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = savedBanner,
            enter   = fadeIn(tween(300)),
            exit    = fadeOut(tween(300))
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(GreenBg)
                        .border(0.5.dp, GreenText.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = GreenText, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Profile saved successfully!", color = GreenText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // ── Edit form / Info cards ────────────────────────────────────────────
        if (isEditing) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardBg)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Edit Profile", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)

                ProfileEditField(
                    value         = editName,
                    onValueChange = { editName = it },
                    label         = "Full Name",
                    placeholder   = "e.g. Mucho Key",
                    icon          = Icons.Outlined.Person,
                    imeAction     = ImeAction.Next,
                    onNext        = { focusManager.moveFocus(FocusDirection.Down) }
                )
                ProfileEditField(
                    value         = editRegNo,
                    onValueChange = { editRegNo = it },
                    label         = "Registration Number",
                    placeholder   = "e.g. CT300-0001/2022",
                    icon          = Icons.Outlined.Badge,
                    imeAction     = ImeAction.Next,
                    onNext        = { focusManager.moveFocus(FocusDirection.Down) },
                    caps          = KeyboardCapitalization.Characters
                )
                ProfileEditField(
                    value         = editProgramme,
                    onValueChange = { editProgramme = it },
                    label         = "Programme / Course",
                    placeholder   = "e.g. BBIT Year 3",
                    icon          = Icons.Outlined.School,
                    imeAction     = ImeAction.Done,
                    onNext        = { focusManager.clearFocus(); saveProfile() }
                )

                Button(
                    onClick  = { saveProfile() },
                    enabled  = editName.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor         = NetflixRed,
                        disabledContainerColor = NetflixRed.copy(alpha = 0.3f)
                    )
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Changes", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }

        } else {
            Column(
                modifier            = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ProfileInfoCard(icon = Icons.Outlined.Person, label = "Full Name",            value = name.ifBlank      { "Not set" })
                ProfileInfoCard(icon = Icons.Outlined.Badge,  label = "Registration Number",  value = regNo.ifBlank     { "Not set" })
                ProfileInfoCard(icon = Icons.Outlined.School, label = "Programme",            value = programme.ifBlank { "Not set" })
                ProfileInfoCard(icon = Icons.Outlined.Info,   label = "App Version",          value = "Tee's Library v1.0")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ── Reusable edit field ───────────────────────────────────────────────────────
@Composable
fun ProfileEditField(
    value:         String,
    onValueChange: (String) -> Unit,
    label:         String,
    placeholder:   String,
    icon:          ImageVector,
    imeAction:     ImeAction,
    onNext:        () -> Unit,
    caps:          KeyboardCapitalization = KeyboardCapitalization.Words
) {
    Column {
        Text(label, color = TextSecondary, fontSize = 12.sp, letterSpacing = 0.5.sp)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value         = value,
            onValueChange = onValueChange,
            placeholder   = { Text(placeholder, color = Color(0xFF555555), fontSize = 14.sp) },
            leadingIcon   = {
                Icon(
                    icon,
                    contentDescription = null,
                    tint     = if (value.isNotBlank()) NetflixRed else TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            },
            singleLine    = true,
            modifier      = Modifier.fillMaxWidth(),
            shape         = RoundedCornerShape(10.dp),
            colors        = OutlinedTextFieldDefaults.colors(
                focusedBorderColor      = NetflixRed,
                unfocusedBorderColor    = CardBorder,
                focusedTextColor        = TextPrimary,
                unfocusedTextColor      = TextPrimary,
                cursorColor             = NetflixRed,
                focusedContainerColor   = InputBg,
                unfocusedContainerColor = InputBg
            ),
            keyboardOptions = KeyboardOptions(capitalization = caps, imeAction = imeAction),
            keyboardActions = KeyboardActions(onNext = { onNext() }, onDone = { onNext() })
        )
    }
}

// ── Stat chip ─────────────────────────────────────────────────────────────────
@Composable
fun ProfileStat(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier            = modifier.clip(RoundedCornerShape(12.dp)).background(CardBg).padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = color,         fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(label, color = TextSecondary, fontSize = 12.sp)
    }
}

// ── Info card ─────────────────────────────────────────────────────────────────
@Composable
fun ProfileInfoCard(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBg)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(NetflixRed.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = NetflixRed, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(label, color = TextSecondary, fontSize = 11.sp, letterSpacing = 0.5.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, color = TextPrimary,   fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}
package com.example.mylibrary.ui.paper

import android.app.Application
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mylibrary.data.PaperEntity
import com.example.mylibrary.data.LibraryRepository
import com.example.mylibrary.viewmodel.UnitViewModelFactory
import kotlinx.coroutines.launch
import java.util.UUID

private val DarkBg        = Color(0xFF141414)
private val CardBg        = Color(0xFF1F1F1F)
private val NetflixRed    = Color(0xFFE50914)
private val FolderAmber   = Color(0xFFE8A838)
private val TextPrimary   = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFF999999)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaperPickerScreen(
    unitId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var selectedUri  by remember { mutableStateOf<Uri?>(null) }
    var selectedName by remember { mutableStateOf("") }
    var customTitle  by remember { mutableStateOf("") }
    var saving       by remember { mutableStateOf(false) }

    // Use GET_CONTENT so we get a content:// URI with read permission
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            // Persist the permission so we can reopen it later
            // Persist READ permission so the URI stays accessible after app restarts
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: SecurityException) {
                // Some providers don't support persistable permissions — that's ok
            }
            selectedUri  = uri
            val fileName = uri.lastPathSegment?.substringAfterLast("/")
                ?.substringAfterLast("%2F") ?: "document"
            selectedName = fileName
            if (customTitle.isBlank()) {
                customTitle = fileName
                    .substringBeforeLast(".")
                    .replace("%20", " ")
                    .replace("_", " ")
            }
        }
    }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Add Paper",
                        color      = TextPrimary,
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        }
    ) { innerPadding ->
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(24.dp))

            // Pick file area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardBg),
                contentAlignment = Alignment.Center
            ) {
                if (selectedUri == null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.AttachFile,
                            contentDescription = null,
                            tint     = FolderAmber,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Tap to pick any document, PDF or image", color = TextSecondary, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = { picker.launch(arrayOf("*/*")) },
                            colors  = ButtonDefaults.outlinedButtonColors(contentColor = FolderAmber),
                            border  = androidx.compose.foundation.BorderStroke(1.dp, FolderAmber)
                        ) {
                            Text("Browse Files")
                        }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📄", fontSize = 40.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            selectedName.replace("%20", " "),
                            color    = TextPrimary,
                            fontSize = 13.sp,
                            maxLines = 2
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { picker.launch(arrayOf("*/*")) }) {
                            Text("Change file", color = FolderAmber, fontSize = 13.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Title field
            Text(
                "Paper Title",
                color    = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value         = customTitle,
                onValueChange = { customTitle = it },
                placeholder   = { Text("e.g. Past Paper 2023", color = TextSecondary) },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(10.dp),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor    = NetflixRed,
                    unfocusedBorderColor  = Color(0xFF2A2A2A),
                    focusedTextColor      = TextPrimary,
                    unfocusedTextColor    = TextPrimary,
                    cursorColor           = NetflixRed
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (selectedUri != null && customTitle.isNotBlank()) {
                        saving = true
                        scope.launch {
                            val repo = LibraryRepository(context)
                            repo.savePaper(
                                PaperEntity(
                                    id       = UUID.randomUUID().toString(),
                                    unitId   = unitId,
                                    title    = customTitle.trim(),
                                    // Store URI string — permission is already persisted
                                    filePath = selectedUri.toString(),
                                    mimeType = context.contentResolver.getType(selectedUri!!)
                                        ?: "application/pdf"
                                )
                            )
                            onBack()
                        }
                    }
                },
                enabled  = selectedUri != null && customTitle.isNotBlank() && !saving,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = NetflixRed,
                    disabledContainerColor = NetflixRed.copy(alpha = 0.3f)
                )
            ) {
                if (saving) {
                    CircularProgressIndicator(
                        color       = Color.White,
                        modifier    = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        "Save Paper",
                        color      = Color.White,
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
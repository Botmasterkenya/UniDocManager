package com.example.mylibrary.ui.note

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mylibrary.viewmodel.NoteViewModel
import com.example.mylibrary.viewmodel.NoteViewModelFactory
import kotlinx.coroutines.launch

private val DarkBg        = Color(0xFF141414)
private val NetflixRed    = Color(0xFFE50914)
private val TextPrimary   = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFF999999)
private val DividerColor  = Color(0xFF2A2A2A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    unitId:  String,
    noteId:  String?,        // null = new note
    onBack:  () -> Unit
) {
    val context = LocalContext.current
    val vm: NoteViewModel = viewModel(
        factory = NoteViewModelFactory(context.applicationContext as Application, unitId)
    )

    var title   by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var loaded  by remember { mutableStateOf(false) }
    val scope   = rememberCoroutineScope()

    // Load existing note if editing
    LaunchedEffect(noteId) {
        if (noteId != null && !loaded) {
            val existing = vm.loadNote(noteId)
            if (existing != null) {
                title   = existing.title
                content = existing.content
            }
            loaded = true
        }
    }

    val canSave = title.isNotBlank()

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text       = if (noteId == null) "New Note" else "Edit Note",
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
                actions = {
                    IconButton(
                        onClick = {
                            if (canSave) {
                                scope.launch {
                                    vm.saveNote(noteId, title.trim(), content.trim())
                                    onBack()
                                }
                            }
                        },
                        enabled = canSave
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Save note",
                            tint = if (canSave) NetflixRed else TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {

            // Title field
            BasicTextField(
                value         = title,
                onValueChange = { title = it },
                textStyle     = TextStyle(
                    color      = TextPrimary,
                    fontSize   = 24.sp,
                    fontWeight = FontWeight.Bold
                ),
                cursorBrush   = SolidColor(NetflixRed),
                modifier      = Modifier.fillMaxWidth().padding(top = 16.dp),
                decorationBox = { inner ->
                    Box {
                        if (title.isEmpty()) {
                            Text("Note title", color = TextSecondary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                        inner()
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Content field
            BasicTextField(
                value         = content,
                onValueChange = { content = it },
                textStyle     = TextStyle(
                    color      = TextPrimary,
                    fontSize   = 16.sp,
                    lineHeight = 26.sp
                ),
                cursorBrush   = SolidColor(NetflixRed),
                modifier      = Modifier.fillMaxSize(),
                decorationBox = { inner ->
                    Box {
                        if (content.isEmpty()) {
                            Text(
                                "Start writing your note here...",
                                color    = TextSecondary,
                                fontSize = 16.sp,
                                lineHeight = 26.sp
                            )
                        }
                        inner()
                    }
                }
            )
        }
    }
}
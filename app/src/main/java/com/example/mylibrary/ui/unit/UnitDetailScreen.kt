package com.example.mylibrary.ui.unit

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mylibrary.data.NoteEntity
import com.example.mylibrary.data.PaperEntity
import com.example.mylibrary.viewmodel.UnitViewModel
import com.example.mylibrary.viewmodel.UnitViewModelFactory
import java.text.SimpleDateFormat
import java.util.*

private val DarkBg        = Color(0xFF141414)
private val CardBg        = Color(0xFF1F1F1F)
private val NetflixRed    = Color(0xFFE50914)
private val FolderAmber   = Color(0xFFE8A838)
private val NoteBlue      = Color(0xFF4A90D9)
private val TextPrimary   = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFF999999)

enum class UnitTab(val label: String, val icon: ImageVector) {
    NOTES("Notes", Icons.Outlined.Edit),
    PAPERS("Papers", Icons.Outlined.AttachFile)
}

fun Long.toDateString(): String =
    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(this))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitDetailScreen(
    unitId:      String,
    unitName:    String,
    unitCode:    String,
    onBack:      () -> Unit,
    onOpenNote:  (NoteEntity) -> Unit = {},
    onAddNote:   () -> Unit = {},
    onAddPaper:  () -> Unit = {}
) {
    val context = LocalContext.current
    val vm: UnitViewModel = viewModel(
        factory = UnitViewModelFactory(context.applicationContext as Application, unitId)
    )
    val notes  by vm.notes.collectAsState()
    val papers by vm.papers.collectAsState()

    var selectedTab by remember { mutableStateOf(UnitTab.NOTES) }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(unitName, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (unitCode.isNotBlank())
                            Text(unitCode.uppercase(), color = NetflixRed, fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick        = { if (selectedTab == UnitTab.NOTES) onAddNote() else onAddPaper() },
                containerColor = NetflixRed,
                contentColor   = Color.White,
                shape          = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {

            // Stats row
            Row(
                modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatChip("${notes.size} Notes",   NoteBlue,    Modifier.weight(1f))
                StatChip("${papers.size} Papers", FolderAmber, Modifier.weight(1f))
            }

            // Tab switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth().padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CardBg).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                UnitTab.entries.forEach { tab ->
                    val selected = selectedTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) NetflixRed else Color.Transparent)
                            .clickable { selectedTab = tab }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(tab.icon, contentDescription = null, tint = if (selected) Color.White else TextSecondary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(tab.label, color = if (selected) Color.White else TextSecondary, fontSize = 14.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (selectedTab) {
                UnitTab.NOTES  -> NotesTab(notes,  onOpenNote, onAddNote,  { vm.deleteNote(it) })
                UnitTab.PAPERS -> PapersTab(papers, onAddPaper, { vm.deletePaper(it) })
            }
        }
    }
}

@Composable
fun NotesTab(notes: List<NoteEntity>, onOpen: (NoteEntity) -> Unit, onAdd: () -> Unit, onDelete: (NoteEntity) -> Unit) {
    if (notes.isEmpty()) {
        EmptyState("📝", "No notes yet", "Tap + to write your first note")
    } else {
        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(notes, key = { it.id }) { note ->
                NoteCard(note = note, onClick = { onOpen(note) }, onDelete = { onDelete(note) })
            }
        }
    }
}

@Composable
fun PapersTab(papers: List<PaperEntity>, onAdd: () -> Unit, onDelete: (PaperEntity) -> Unit) {
    if (papers.isEmpty()) {
        EmptyState("📄", "No papers yet", "Tap + to attach a paper or PDF")
    } else {
        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(papers, key = { it.id }) { paper ->
                PaperCard(paper = paper, onDelete = { onDelete(paper) })
            }
        }
    }
}

@Composable
fun NoteCard(note: NoteEntity, onClick: () -> Unit, onDelete: () -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBg)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)).background(NoteBlue.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
            Text("📝", fontSize = 20.sp)
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(note.title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(3.dp))
            Text(note.content.take(60), color = TextSecondary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(note.updatedAt.toDateString(), color = TextSecondary, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(4.dp))
            IconButton(onClick = { showConfirm = true }, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete note", tint = TextSecondary, modifier = Modifier.size(16.dp))
            }
        }
    }
    if (showConfirm) {
        DeleteDialog(title = "Delete note?", text = "This note will be permanently deleted.", onConfirm = { onDelete(); showConfirm = false }, onDismiss = { showConfirm = false })
    }
}

@Composable
fun PaperCard(paper: PaperEntity, onDelete: () -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBg)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)).background(FolderAmber.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
            Text("📄", fontSize = 20.sp)
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(paper.title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(3.dp))
            Text("PDF · ${paper.addedAt.toDateString()}", color = TextSecondary, fontSize = 12.sp)
        }
        IconButton(onClick = { showConfirm = true }, modifier = Modifier.size(20.dp)) {
            Icon(Icons.Default.Delete, contentDescription = "Delete paper", tint = TextSecondary, modifier = Modifier.size(16.dp))
        }
    }
    if (showConfirm) {
        DeleteDialog(title = "Delete paper?", text = "This paper will be permanently deleted.", onConfirm = { onDelete(); showConfirm = false }, onDismiss = { showConfirm = false })
    }
}

@Composable
fun StatChip(label: String, color: Color, modifier: Modifier = Modifier) {
    Box(modifier = modifier.clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = 0.12f)).padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
        Text(label, color = color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun EmptyState(emoji: String, title: String, subtitle: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 52.sp)
            Spacer(modifier = Modifier.height(14.dp))
            Text(title,    color = TextPrimary,   fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(subtitle, color = TextSecondary, fontSize = 13.sp)
        }
    }
}

@Composable
fun DeleteDialog(title: String, text: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val CardBg = Color(0xFF1F1F1F)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = CardBg,
        title            = { Text(title, color = Color.White) },
        text             = { Text(text,  color = Color(0xFF999999)) },
        confirmButton    = { TextButton(onClick = onConfirm) { Text("Delete", color = Color(0xFFE50914)) } },
        dismissButton    = { TextButton(onClick = onDismiss) { Text("Cancel", color = Color(0xFF999999)) } }
    )
}
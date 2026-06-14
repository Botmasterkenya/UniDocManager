package com.example.mylibrary.ui.home

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mylibrary.data.CourseUnitEntity
import com.example.mylibrary.viewmodel.HomeViewModel
import com.example.mylibrary.viewmodel.HomeViewModelFactory

private val DarkBg        = Color(0xFF141414)
private val CardBg        = Color(0xFF1F1F1F)
private val CardBorder    = Color(0xFF2A2A2A)
private val NetflixRed    = Color(0xFFE50914)
private val FolderAmber   = Color(0xFFE8A838)
private val TextPrimary   = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFF999999)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onOpenUnit: (CourseUnitEntity) -> Unit = {}) {

    val context = LocalContext.current
    val vm: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(context.applicationContext as Application)
    )
    val units by vm.units.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text          = "Tee's Library",
                        color         = NetflixRed,
                        fontSize      = 22.sp,
                        fontWeight    = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick        = { showDialog = true },
                containerColor = NetflixRed,
                contentColor   = Color.White,
                shape          = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add course unit", modifier = Modifier.size(28.dp))
            }
        }
    ) { innerPadding ->

        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (units.isEmpty()) {
                Column(
                    modifier            = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("📚", fontSize = 56.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No course units yet", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Tap + to add your first unit", color = TextSecondary, fontSize = 14.sp)
                }
            } else {
                LazyVerticalGrid(
                    columns               = GridCells.Fixed(2),
                    contentPadding        = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement   = Arrangement.spacedBy(12.dp),
                    modifier              = Modifier.fillMaxSize()
                ) {
                    items(units) { unit ->
                        FolderCard(
                            unit      = unit,
                            onClick   = { onOpenUnit(unit) },
                            onDelete  = { vm.deleteUnit(unit) }
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        AddUnitDialog(
            onDismiss = { showDialog = false },
            onConfirm = { name, code ->
                vm.addUnit(name, code)
                showDialog = false
            }
        )
    }
}

@Composable
fun FolderCard(unit: CourseUnitEntity, onClick: () -> Unit, onDelete: () -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CardBg)
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Box(
                modifier = Modifier
                    .width(40.dp).height(8.dp)
                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                    .background(FolderAmber.copy(alpha = 0.85f))
            )
            IconButton(onClick = { showConfirm = true }, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextSecondary, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth().height(80.dp)
                .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp, topEnd = 8.dp))
                .background(FolderAmber.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Text("📂", fontSize = 36.sp)
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (unit.code.isNotBlank()) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(NetflixRed.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(unit.code.uppercase(), color = NetflixRed, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))
        }
        Text(unit.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Notes & Papers", color = TextSecondary, fontSize = 11.sp)
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            containerColor   = CardBg,
            title            = { Text("Delete unit?", color = TextPrimary) },
            text             = { Text("All notes and papers inside will be deleted.", color = TextSecondary) },
            confirmButton    = {
                TextButton(onClick = { onDelete(); showConfirm = false }) {
                    Text("Delete", color = NetflixRed)
                }
            },
            dismissButton    = {
                TextButton(onClick = { showConfirm = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun AddUnitDialog(onDismiss: () -> Unit, onConfirm: (name: String, code: String) -> Unit) {
    var unitName by remember { mutableStateOf("") }
    var unitCode by remember { mutableStateOf("") }
    val isValid  = unitName.isNotBlank()

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardBg)
                .padding(24.dp)
        ) {
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Add Course Unit", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text("Unit Code (optional)", color = TextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = unitCode, onValueChange = { unitCode = it },
                    placeholder   = { Text("e.g. CIT 3154", color = TextSecondary, fontSize = 14.sp) },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = RoundedCornerShape(10.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = NetflixRed, unfocusedBorderColor = CardBorder,
                        focusedTextColor     = TextPrimary, unfocusedTextColor  = TextPrimary,
                        cursorColor          = NetflixRed
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Unit Name", color = TextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = unitName, onValueChange = { unitName = it },
                    placeholder   = { Text("e.g. Systems Analysis & Design", color = TextSecondary, fontSize = 14.sp) },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = RoundedCornerShape(10.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = NetflixRed, unfocusedBorderColor = CardBorder,
                        focusedTextColor     = TextPrimary, unfocusedTextColor  = TextPrimary,
                        cursorColor          = NetflixRed
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (isValid) onConfirm(unitName.trim(), unitCode.trim()) })
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick  = { if (isValid) onConfirm(unitName.trim(), unitCode.trim()) },
                    enabled  = isValid,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape    = RoundedCornerShape(10.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor         = NetflixRed,
                        disabledContainerColor = NetflixRed.copy(alpha = 0.3f)
                    )
                ) {
                    Text("Create Folder", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
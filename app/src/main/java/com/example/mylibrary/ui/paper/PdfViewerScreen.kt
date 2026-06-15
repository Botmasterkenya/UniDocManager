package com.example.mylibrary.ui.paper

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private val DarkBg        = Color(0xFF141414)
private val NetflixRed    = Color(0xFFE50914)
private val TextPrimary   = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFF999999)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    title:    String,
    filePath: String,
    onBack:   () -> Unit
) {
    val context = LocalContext.current
    val uri     = remember(filePath) { Uri.parse(filePath) }

    var pages   by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error   by remember { mutableStateOf<String?>(null) }

    // Pinch-to-zoom state
    var scale       by remember { mutableStateOf(1f) }
    var offsetX     by remember { mutableStateOf(0f) }
    var offsetY     by remember { mutableStateOf(0f) }

    // Render PDF pages using PdfRenderer
    LaunchedEffect(filePath) {
        withContext(Dispatchers.IO) {
            try {
                val cacheFile = copyUriToCache(context, uri)
                if (cacheFile == null) {
                    error = "Could not read the file."
                    loading = false
                    return@withContext
                }

                val pfd      = ParcelFileDescriptor.open(cacheFile, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(pfd)
                val bitmaps  = mutableListOf<Bitmap>()

                for (i in 0 until renderer.pageCount) {
                    val page   = renderer.openPage(i)
                    val scale  = 2f  // render at 2x for sharpness
                    val width  = (page.width  * scale).toInt()
                    val height = (page.height * scale).toInt()
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    // White background
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    bitmaps.add(bitmap)
                }

                renderer.close()
                pfd.close()

                pages   = bitmaps
                loading = false
            } catch (e: Exception) {
                error   = "Failed to open PDF: ${e.message}"
                loading = false
            }
        }
    }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text       = title,
                        color      = TextPrimary,
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { openExternally(context, uri) }) {
                        Icon(Icons.Default.OpenInNew, contentDescription = "Open externally", tint = NetflixRed)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF1A1A1A))
        ) {
            when {
                loading -> {
                    // Loading state
                    Column(
                        modifier            = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = NetflixRed, strokeWidth = 3.dp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Rendering PDF...", color = TextSecondary, fontSize = 13.sp)
                    }
                }

                error != null -> {
                    // Error state — always show open externally option
                    Column(
                        modifier            = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("📄", fontSize = 56.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Cannot preview this file",
                            color      = TextPrimary,
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            error ?: "",
                            color    = TextSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(28.dp))
                        Button(
                            onClick  = { openExternally(context, uri) },
                            colors   = ButtonDefaults.buttonColors(containerColor = NetflixRed),
                            shape    = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Icon(
                                Icons.Default.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Open in another app",
                                color      = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                pages.isNotEmpty() -> {
                    // PDF rendered — show pages with pinch-to-zoom
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale   = (scale * zoom).coerceIn(1f, 5f)
                                    offsetX += pan.x
                                    offsetY += pan.y
                                }
                            }
                            .graphicsLayer {
                                scaleX        = scale
                                scaleY        = scale
                                translationX  = offsetX
                                translationY  = offsetY
                            },
                        contentPadding      = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(pages) { index, bitmap ->
                            Column {
                                Image(
                                    bitmap      = bitmap.asImageBitmap(),
                                    contentDescription = "Page ${index + 1}",
                                    contentScale = ContentScale.FillWidth,
                                    modifier    = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White)
                                )
                                // Page number badge
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text     = "${index + 1} / ${pages.size}",
                                        color    = TextSecondary,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Copy content:// URI to cache file ────────────────────────────────────────
private fun copyUriToCache(context: Context, uri: Uri): File? {
    return try {
        val cacheFile = File(context.cacheDir, "pdf_${uri.hashCode()}.pdf")
        if (cacheFile.exists()) return cacheFile
        context.contentResolver.openInputStream(uri)?.use { input ->
            cacheFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        if (cacheFile.exists() && cacheFile.length() > 0) cacheFile else null
    } catch (e: Exception) {
        null
    }
}

// ── Open in external app ──────────────────────────────────────────────────────
fun openExternally(context: Context, uri: Uri) {
    try {
        val mime   = context.contentResolver.getType(uri) ?: "application/pdf"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Open with"))
    } catch (e: Exception) {
        // Last resort — just fire the URI
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, uri).apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        } catch (e2: Exception) {
            e2.printStackTrace()
        }
    }
}
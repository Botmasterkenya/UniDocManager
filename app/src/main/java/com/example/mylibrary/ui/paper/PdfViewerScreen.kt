package com.example.mylibrary.ui.paper

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

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
    // Ensure we handle both raw paths and proper URIs
    val uri = remember(filePath) {
        if (filePath.startsWith("content://") || filePath.startsWith("file://")) {
            Uri.parse(filePath)
        } else {
            Uri.fromFile(File(filePath))
        }
    }

    var pages   by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error   by remember { mutableStateOf<String?>(null) }

    var scale       by remember { mutableStateOf(1f) }
    var offsetX     by remember { mutableStateOf(0f) }
    var offsetY     by remember { mutableStateOf(0f) }

    LaunchedEffect(uri) {
        loading = true
        error = null
        withContext(Dispatchers.IO) {
            var renderer: PdfRenderer? = null
            var pfd: ParcelFileDescriptor? = null

            try {
                val cacheFile = copyUriToCache(context, uri)
                if (cacheFile == null || !cacheFile.exists()) {
                    throw Exception("File could not be loaded into cache.")
                }

                pfd = ParcelFileDescriptor.open(cacheFile, ParcelFileDescriptor.MODE_READ_ONLY)
                renderer = PdfRenderer(pfd)
                val bitmaps = mutableListOf<Bitmap>()

                // Limit rendering to avoid OutOfMemory if PDF is huge
                val pageCount = renderer.pageCount
                for (i in 0 until pageCount) {
                    val page = renderer.openPage(i)

                    // Dynamic scaling: 1.5f is a good balance between quality and memory
                    val renderScale = 1.5f
                    val width = (page.width * renderScale).toInt()
                    val height = (page.height * renderScale).toInt()

                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmaps.add(bitmap)
                    page.close()
                }

                withContext(Dispatchers.Main) {
                    pages = bitmaps
                    loading = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    error = e.localizedMessage ?: "Unknown error"
                    loading = false
                }
            } finally {
                renderer?.close()
                pfd?.close()
            }
        }
    }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding).background(Color(0xFF1A1A1A))) {
            when {
                loading -> {
                    Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = NetflixRed)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Preparing Document...", color = TextSecondary, fontSize = 13.sp)
                    }
                }
                error != null -> {
                    ErrorView(error!!, context, uri)
                }
                pages.isNotEmpty() -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(1f, 5f)
                                    offsetX += pan.x
                                    offsetY += pan.y
                                }
                            }
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offsetX
                                translationY = offsetY
                            },
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(pages) { index, bitmap ->
                            PdfPageItem(bitmap, index, pages.size)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PdfPageItem(bitmap: Bitmap, index: Int, total: Int) {
    Column {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Page ${index + 1}",
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth().background(Color.White)
        )
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("${index + 1} / $total", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(vertical = 4.dp))
        }
    }
}

@Composable
fun BoxScope.ErrorView(error: String, context: Context, uri: Uri) {
    Column(modifier = Modifier.align(Alignment.Center).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("📄", fontSize = 56.sp)
        Text("Cannot preview this file", color = TextPrimary, fontWeight = FontWeight.Bold)
        Text(error, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { openExternally(context, uri) },
            colors = ButtonDefaults.buttonColors(containerColor = NetflixRed),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Open in another app")
        }
    }
}

private fun copyUriToCache(context: Context, uri: Uri): File? {
    return try {
        val fileName = "temp_doc_${uri.lastPathSegment?.hashCode() ?: "file"}.pdf"
        val cacheFile = File(context.cacheDir, fileName)

        // If file exists and is not empty, use it
        if (cacheFile.exists() && cacheFile.length() > 0) return cacheFile

        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(cacheFile).use { output ->
                input.copyTo(output)
            }
        }
        if (cacheFile.exists() && cacheFile.length() > 0) cacheFile else null
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun openExternally(context: Context, uri: Uri) {
    try {
        val authority = "${context.packageName}.provider"
        val finalUri = if (uri.scheme == "content") {
            uri
        } else {
            val file = File(uri.path ?: "")
            FileProvider.getUriForFile(context, authority, file)
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(finalUri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Open PDF with"))
    } catch (e: Exception) {
        Toast.makeText(context, "No app found to open PDF", Toast.LENGTH_SHORT).show()
    }
}
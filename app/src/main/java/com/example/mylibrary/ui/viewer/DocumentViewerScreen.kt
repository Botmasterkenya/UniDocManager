package com.example.mylibrary.ui.viewer

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

// ── Colors ────────────────────────────────────────────────────────────────────
private val DarkBg        = Color(0xFF141414)
private val CardBg        = Color(0xFF1F1F1F)
private val NetflixRed    = Color(0xFFE50914)
private val TextPrimary   = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFF999999)
private val CodeBg        = Color(0xFF0D0D0D)

// ── Document types ────────────────────────────────────────────────────────────
sealed class DocType {
    object PDF      : DocType()
    object Image    : DocType()
    object Text     : DocType()
    object Code     : DocType()
    object Unknown  : DocType()
}

fun resolveDocType(context: Context, uri: Uri): DocType {
    val mime = context.contentResolver.getType(uri) ?: ""
    val path = uri.lastPathSegment?.lowercase() ?: ""
    return when {
        mime == "application/pdf" || path.endsWith(".pdf")
            -> DocType.PDF
        mime.startsWith("image/") || path.endsWith(".jpg") || path.endsWith(".jpeg")
                || path.endsWith(".png") || path.endsWith(".gif") || path.endsWith(".webp")
            -> DocType.Image
        mime.startsWith("text/") || path.endsWith(".txt") || path.endsWith(".md")
                || path.endsWith(".csv")
            -> DocType.Text
        path.endsWith(".kt") || path.endsWith(".java") || path.endsWith(".py")
                || path.endsWith(".js") || path.endsWith(".ts") || path.endsWith(".html")
                || path.endsWith(".xml") || path.endsWith(".json") || path.endsWith(".yaml")
                || path.endsWith(".yml") || path.endsWith(".c") || path.endsWith(".cpp")
                || path.endsWith(".sh")
            -> DocType.Code
        else -> DocType.Unknown
    }
}

// ── Viewer state ──────────────────────────────────────────────────────────────
sealed class ViewerState {
    object Loading                        : ViewerState()
    data class PdfReady(val pages: List<Bitmap>) : ViewerState()
    data class ImageReady(val uri: Uri)   : ViewerState()
    data class TextReady(val content: String) : ViewerState()
    data class Error(val message: String) : ViewerState()
    object NeedsExternalApp               : ViewerState()
}

// ── Main screen ───────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentViewerScreen(
    title:    String,
    filePath: String,
    onBack:   () -> Unit
) {
    val context = LocalContext.current
    val uri     = remember(filePath) { Uri.parse(filePath) }
    val docType = remember(filePath) { resolveDocType(context, uri) }

    var state by remember { mutableStateOf<ViewerState>(ViewerState.Loading) }

    LaunchedEffect(filePath) {
        withContext(Dispatchers.IO) {
            try {
                val cacheFile = copyToCache(context, uri)

                state = when (docType) {
                    DocType.PDF -> {
                        if (cacheFile == null) {
                            ViewerState.Error("Could not read the file.")
                        } else {
                            val pages = renderPdfPages(cacheFile)
                            if (pages.isEmpty()) ViewerState.Error("PDF has no pages.")
                            else ViewerState.PdfReady(pages)
                        }
                    }
                    DocType.Image -> ViewerState.ImageReady(uri)
                    DocType.Text, DocType.Code -> {
                        val text = readTextFile(context, uri)
                        if (text != null) ViewerState.TextReady(text)
                        else ViewerState.Error("Could not read file content.")
                    }
                    DocType.Unknown -> ViewerState.NeedsExternalApp
                }
            } catch (e: Exception) {
                state = ViewerState.Error(e.message ?: "Unknown error")
            }
        }
    }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text       = title,
                            color      = TextPrimary,
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines   = 1,
                            overflow   = TextOverflow.Ellipsis
                        )
                        Text(
                            text     = docTypeLabel(docType),
                            color    = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CardBg)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkBg)
        ) {
            when (val s = state) {
                is ViewerState.Loading        -> LoadingView()
                is ViewerState.PdfReady       -> PdfView(pages = s.pages)
                is ViewerState.ImageReady     -> ImageView(uri = s.uri)
                is ViewerState.TextReady      -> TextView(content = s.content, isCode = docType is DocType.Code)
                is ViewerState.Error          -> ErrorView(message = s.message) { openExternally(context, uri) }
                is ViewerState.NeedsExternalApp -> UnsupportedView { openExternally(context, uri) }
            }
        }
    }
}

// ── PDF view ──────────────────────────────────────────────────────────────────
@Composable
fun PdfView(pages: List<Bitmap>) {
    var scale   by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

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
                scaleX       = scale
                scaleY       = scale
                translationX = offsetX
                translationY = offsetY
            },
        contentPadding      = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        itemsIndexed(pages) { index, bitmap ->
            Column {
                Image(
                    bitmap             = bitmap.asImageBitmap(),
                    contentDescription = "Page ${index + 1}",
                    contentScale       = ContentScale.FillWidth,
                    modifier           = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                )
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        "${index + 1} / ${pages.size}",
                        color    = TextSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

// ── Image view ────────────────────────────────────────────────────────────────
@Composable
fun ImageView(uri: Uri) {
    var scale   by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale   = (scale * zoom).coerceIn(1f, 8f)
                    offsetX += pan.x
                    offsetY += pan.y
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(uri)
                .crossfade(true)
                .build(),
            contentDescription = "Image",
            contentScale       = ContentScale.Fit,
            modifier           = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX       = scale
                    scaleY       = scale
                    translationX = offsetX
                    translationY = offsetY
                }
        )
    }
}

// ── Text / Code view ──────────────────────────────────────────────────────────
@Composable
fun TextView(content: String, isCode: Boolean) {
    val scrollState = rememberScrollState()
    val bg          = if (isCode) CodeBg else DarkBg

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            if (isCode) {
                // Code badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(NetflixRed.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("CODE", color = NetflixRed, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Text(
                text  = content,
                style = TextStyle(
                    fontSize   = if (isCode) 13.sp else 15.sp,
                    color      = if (isCode) Color(0xFF00FF88) else TextPrimary,
                    fontFamily = if (isCode) FontFamily.Monospace else FontFamily.Default,
                    lineHeight = if (isCode) 20.sp else 24.sp
                )
            )
        }
    }
}

// ── Loading view ──────────────────────────────────────────────────────────────
@Composable
fun LoadingView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = NetflixRed, strokeWidth = 3.dp)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Opening document...", color = TextSecondary, fontSize = 13.sp)
        }
    }
}

// ── Error view ────────────────────────────────────────────────────────────────
@Composable
fun ErrorView(message: String, onOpenExternal: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.padding(32.dp)
        ) {
            Text("⚠️", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Could not open file", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(message, color = TextSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(24.dp))
            ExternalOpenButton(onOpenExternal)
        }
    }
}

// ── Unsupported view ──────────────────────────────────────────────────────────
@Composable
fun UnsupportedView(onOpenExternal: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.padding(32.dp)
        ) {
            Text("📎", fontSize = 56.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Format not supported", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "This file type can't be previewed in‑app.\nOpen it in another app on your device.",
                color    = TextSecondary,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Supported formats hint
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CardBg)
                    .padding(14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                SupportedBadge("PDF")
                SupportedBadge("PNG")
                SupportedBadge("JPG")
                SupportedBadge("TXT")
                SupportedBadge("MD")
                SupportedBadge("CODE")
            }

            Spacer(modifier = Modifier.height(24.dp))
            ExternalOpenButton(onOpenExternal)
        }
    }
}

@Composable
fun SupportedBadge(label: String) {
    Box(
        modifier = Modifier
            .padding(3.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(NetflixRed.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(label, color = NetflixRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ExternalOpenButton(onClick: () -> Unit) {
    Button(
        onClick  = onClick,
        colors   = ButtonDefaults.buttonColors(containerColor = NetflixRed),
        shape    = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().height(50.dp)
    ) {
        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Open in another app", color = Color.White, fontWeight = FontWeight.Bold)
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

fun docTypeLabel(type: DocType) = when (type) {
    DocType.PDF     -> "PDF Document"
    DocType.Image   -> "Image"
    DocType.Text    -> "Text File"
    DocType.Code    -> "Source Code"
    DocType.Unknown -> "Document"
}

fun copyToCache(context: Context, uri: Uri): File? {
    return try {
        // Determine extension from MIME type or URI path
        val mime = context.contentResolver.getType(uri)
        val ext  = when {
            mime != null -> mime.substringAfterLast("/").replace("jpeg", "jpg")
            uri.path?.contains(".") == true -> uri.path!!.substringAfterLast(".")
            else -> "pdf"
        }
        val file = File(context.cacheDir, "doc_${uri.hashCode()}.$ext")

        // Always re-copy — don't rely on stale cache
        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { out -> input.copyTo(out) }
        }
        if (file.exists() && file.length() > 0) file else null
    } catch (e: SecurityException) {
        // Permission was lost — cannot read this URI anymore
        null
    } catch (e: Exception) {
        null
    }
}

fun renderPdfPages(file: File): List<Bitmap> {
    val pfd      = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    val renderer = PdfRenderer(pfd)
    val bitmaps  = mutableListOf<Bitmap>()
    val scale    = 2f
    for (i in 0 until renderer.pageCount) {
        val page   = renderer.openPage(i)
        val w      = (page.width  * scale).toInt()
        val h      = (page.height * scale).toInt()
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.WHITE)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
        bitmaps.add(bitmap)
    }
    renderer.close()
    pfd.close()
    return bitmaps
}

fun readTextFile(context: Context, uri: Uri): String? {
    return try {
        context.contentResolver.openInputStream(uri)
            ?.bufferedReader()
            ?.readText()
    } catch (e: Exception) { null }
}

fun openExternally(context: Context, uri: Uri) {
    try {
        val mime   = context.contentResolver.getType(uri) ?: "*/*"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Open with"))
    } catch (e: Exception) {
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, uri).apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        } catch (e2: Exception) { e2.printStackTrace() }
    }
}
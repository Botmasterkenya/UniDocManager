package com.example.mylibrary.ui.viewer

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.text.style.TextAlign
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
    object PDF          : DocType()
    object Image        : DocType()
    object Text         : DocType()
    object Code         : DocType()
    object Word         : DocType()
    object Excel        : DocType()
    object PPT          : DocType()
    object LegacyOffice : DocType()
    object Unknown      : DocType()
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
        // Word files
        mime == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" || path.endsWith(".docx")
            -> DocType.Word
        // Excel files
        mime == "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" || path.endsWith(".xlsx")
            -> DocType.Excel
        // PowerPoint files
        mime == "application/vnd.openxmlformats-officedocument.presentationml.presentation" || path.endsWith(".pptx")
            -> DocType.PPT
        // Legacy office
        mime == "application/msword" || mime == "application/vnd.ms-excel" || mime == "application/vnd.ms-powerpoint"
                || path.endsWith(".doc") || path.endsWith(".xls") || path.endsWith(".ppt")
            -> DocType.LegacyOffice
        // Code files
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
    object Loading                            : ViewerState()
    data class PdfReady(val pages: List<Bitmap>) : ViewerState()
    data class ImageReady(val uri: Uri)       : ViewerState()
    data class TextReady(val content: String) : ViewerState()
    data class WordReady(val document: WordDocument) : ViewerState()
    data class ExcelReady(val workbook: ExcelWorkbook) : ViewerState()
    data class PptReady(val slides: List<PptSlide>) : ViewerState()
    data class Error(val message: String)     : ViewerState()
    object NeedsExternalApp                   : ViewerState()
    object LegacyOfficeWarning                : ViewerState()
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
                    DocType.Word -> {
                        if (cacheFile == null) {
                            ViewerState.Error("Could not read the file.")
                        } else {
                            val doc = parseDocx(cacheFile)
                            if (doc.elements.isEmpty()) ViewerState.Error("Word document has no content or is empty.")
                            else ViewerState.WordReady(doc)
                        }
                    }
                    DocType.Excel -> {
                        if (cacheFile == null) {
                            ViewerState.Error("Could not read the file.")
                        } else {
                            val workbook = parseXlsx(cacheFile)
                            if (workbook.sheets.isEmpty()) ViewerState.Error("Excel workbook has no sheets or content.")
                            else ViewerState.ExcelReady(workbook)
                        }
                    }
                    DocType.PPT -> {
                        if (cacheFile == null) {
                            ViewerState.Error("Could not read the file.")
                        } else {
                            val slides = parsePptx(cacheFile)
                            if (slides.isEmpty()) ViewerState.Error("PowerPoint presentation has no slides or is empty.")
                            else ViewerState.PptReady(slides)
                        }
                    }
                    DocType.LegacyOffice -> ViewerState.LegacyOfficeWarning
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
                is ViewerState.WordReady      -> WordView(document = s.document)
                is ViewerState.ExcelReady     -> ExcelView(workbook = s.workbook)
                is ViewerState.PptReady       -> PptView(slides = s.slides)
                is ViewerState.LegacyOfficeWarning -> LegacyOfficeView { openExternally(context, uri) }
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
            Text("Opening paper...", color = TextSecondary, fontSize = 13.sp)
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
                fontSize = 13.sp,
                textAlign = TextAlign.Center
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
                SupportedBadge("WORD")
                SupportedBadge("EXCEL")
                SupportedBadge("PPT")
                SupportedBadge("TXT")
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
    DocType.PDF          -> "PDF Paper"
    DocType.Image        -> "Image"
    DocType.Text         -> "Text Paper"
    DocType.Code         -> "Source Code"
    DocType.Word         -> "Word Paper"
    DocType.Excel        -> "Excel Paper"
    DocType.PPT          -> "PowerPoint Paper"
    DocType.LegacyOffice -> "Legacy Office Paper"
    DocType.Unknown      -> "Paper"
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

// ── Word view ─────────────────────────────────────────────────────────────────
@Composable
fun WordView(document: WordDocument) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(document.elements) { element ->
            when (element) {
                is WordElement.Paragraph -> {
                    if (element.isHeading) {
                        val fontSize = when (element.headingLevel) {
                            1 -> 22.sp
                            2 -> 19.sp
                            else -> 17.sp
                        }
                        val topPadding = if (element.headingLevel == 1) 16.dp else 8.dp
                        Text(
                            text = element.text,
                            color = NetflixRed,
                            fontSize = fontSize,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = topPadding, bottom = 4.dp)
                        )
                    } else {
                        Text(
                            text = element.text,
                            color = TextPrimary,
                            fontSize = 15.sp,
                            lineHeight = 24.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                is WordElement.Table -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(12.dp)
                        ) {
                            element.rows.forEachIndexed { rowIndex, rowCells ->
                                Row(
                                    modifier = Modifier
                                        .background(if (rowIndex == 0) Color(0xFF2A2A2A) else Color.Transparent)
                                        .border(0.5.dp, Color(0xFF3A3A3A))
                                ) {
                                    rowCells.forEach { cellText ->
                                        Box(
                                            modifier = Modifier
                                                .width(140.dp)
                                                .border(0.5.dp, Color(0xFF3A3A3A))
                                                .padding(8.dp)
                                        ) {
                                            Text(
                                                text = cellText,
                                                color = if (rowIndex == 0) NetflixRed else TextPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = if (rowIndex == 0) FontWeight.Bold else FontWeight.Normal,
                                                maxLines = 4,
                                                overflow = TextOverflow.Ellipsis
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
    }
}

// ── Excel view ────────────────────────────────────────────────────────────────
@Composable
fun ExcelView(workbook: ExcelWorkbook) {
    var selectedSheetIdx by remember { mutableStateOf(0) }
    val sheet = workbook.sheets.getOrNull(selectedSheetIdx) ?: return

    Column(modifier = Modifier.fillMaxSize().background(DarkBg)) {
        // Sheet Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedSheetIdx,
            containerColor = CardBg,
            contentColor = NetflixRed,
            edgePadding = 16.dp,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedSheetIdx]),
                    color = NetflixRed
                )
            }
        ) {
            workbook.sheets.forEachIndexed { index, excelSheet ->
                Tab(
                    selected = selectedSheetIdx == index,
                    onClick = { selectedSheetIdx = index },
                    text = {
                        Text(
                            text = excelSheet.name,
                            color = if (selectedSheetIdx == index) TextPrimary else TextSecondary,
                            fontWeight = if (selectedSheetIdx == index) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                    }
                )
            }
        }

        // Spreadsheet Grid
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .horizontalScroll(rememberScrollState())
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Table Header Row: A, B, C...
                Row {
                    // Empty corner box for row numbers column
                    Box(
                        modifier = Modifier
                            .size(width = 45.dp, height = 30.dp)
                            .background(Color(0xFF2A2A2A))
                            .border(0.5.dp, Color(0xFF3A3A3A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("", fontSize = 11.sp)
                    }
                    val colCount = if (sheet.rows.isNotEmpty()) sheet.rows[0].size else 0
                    for (c in 0 until colCount) {
                        val colLetter = getColLetter(c)
                        Box(
                            modifier = Modifier
                                .size(width = 110.dp, height = 30.dp)
                                .background(Color(0xFF2A2A2A))
                                .border(0.5.dp, Color(0xFF3A3A3A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = colLetter,
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Table Rows
                sheet.rows.forEachIndexed { rIndex, rowCells ->
                    Row {
                        // Row Number Column
                        Box(
                            modifier = Modifier
                                .size(width = 45.dp, height = 40.dp)
                                .background(Color(0xFF2A2A2A))
                                .border(0.5.dp, Color(0xFF3A3A3A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${rIndex + 1}",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Cell Columns
                        rowCells.forEach { cellText ->
                            Box(
                                modifier = Modifier
                                    .size(width = 110.dp, height = 40.dp)
                                    .background(CardBg)
                                    .border(0.2.dp, Color(0xFF3A3A3A))
                                    .padding(horizontal = 6.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = cellText,
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getColLetter(colIdx: Int): String {
    var temp = colIdx
    val sb = StringBuilder()
    while (temp >= 0) {
        sb.insert(0, ('A' + (temp % 26)))
        temp = (temp / 26) - 1
    }
    return sb.toString()
}

// ── PPT view ──────────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PptView(slides: List<PptSlide>) {
    val pagerState = rememberPagerState(pageCount = { slides.size })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(24.dp),
            pageSpacing = 16.dp
        ) { page ->
            val slide = slides[page]
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .aspectRatio(16f / 10f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    slide.paragraphs.forEachIndexed { index, text ->
                        if (index == 0) {
                            Text(
                                text = text,
                                color = NetflixRed,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                            )
                        } else {
                            Text(
                                text = text,
                                color = TextPrimary,
                                fontSize = 15.sp,
                                lineHeight = 22.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }

        // Slide counter indicators
        Row(
            modifier = Modifier
                .padding(bottom = 24.dp, top = 8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Slide ${pagerState.currentPage + 1} of ${slides.size}",
                color = TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ── Legacy Office view ────────────────────────────────────────────────────────
@Composable
fun LegacyOfficeView(onOpenExternal: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.padding(32.dp)
        ) {
            Text("📄", fontSize = 56.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Legacy Office Format", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "This file is in a legacy binary format (.doc, .xls, or .ppt) which cannot be opened in-app.\n\nPlease open it using an external application.",
                color    = TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            ExternalOpenButton(onOpenExternal)
        }
    }
}
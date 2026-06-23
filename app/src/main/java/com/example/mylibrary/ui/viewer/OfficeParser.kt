package com.example.mylibrary.ui.viewer

import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory

// ── Word Data Models ─────────────────────────────────────────────────────────
data class WordDocument(
    val elements: List<WordElement>
)

sealed class WordElement {
    data class Paragraph(val text: String, val isHeading: Boolean, val headingLevel: Int = 0) : WordElement()
    data class Table(val rows: List<List<String>>) : WordElement()
}

// ── Excel Data Models ────────────────────────────────────────────────────────
data class ExcelWorkbook(
    val sheets: List<ExcelSheet>
)

data class ExcelSheet(
    val name: String,
    val rows: List<List<String>>
)

// ── PPT Data Models ──────────────────────────────────────────────────────────
data class PptSlide(
    val slideNumber: Int,
    val paragraphs: List<String>
)

// ── Common Helper Functions ──────────────────────────────────────────────────
private fun getElementsByLocalName(parent: Element, localName: String): List<Element> {
    val result = mutableListOf<Element>()
    val list = parent.getElementsByTagName("*")
    for (i in 0 until list.length) {
        val el = list.item(i) as Element
        if (el.tagName.substringAfter(":") == localName) {
            result.add(el)
        }
    }
    return result
}

private fun getDirectChildrenByLocalName(parent: Element, localName: String): List<Element> {
    val result = mutableListOf<Element>()
    val list = parent.childNodes
    for (i in 0 until list.length) {
        val node = list.item(i)
        if (node.nodeType == Node.ELEMENT_NODE) {
            val el = node as Element
            if (el.tagName.substringAfter(":") == localName) {
                result.add(el)
            }
        }
    }
    return result
}

// ── Word Parser ──────────────────────────────────────────────────────────────
fun parseDocx(file: File): WordDocument {
    var zipFile: ZipFile? = null
    try {
        zipFile = ZipFile(file)
        val entry = zipFile.getEntry("word/document.xml") ?: return WordDocument(emptyList())
        val db = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc = db.parse(zipFile.getInputStream(entry))
        doc.documentElement.normalize()

        val elements = mutableListOf<WordElement>()
        val bodies = getElementsByLocalName(doc.documentElement, "body")
        if (bodies.isNotEmpty()) {
            val body = bodies[0]
            val childNodes = body.childNodes
            for (i in 0 until childNodes.length) {
                val child = childNodes.item(i)
                if (child.nodeType == Node.ELEMENT_NODE) {
                    val element = child as Element
                    val localName = element.tagName.substringAfter(":")
                    when (localName) {
                        "p" -> {
                            val paragraph = parseParagraph(element)
                            if (paragraph != null) {
                                elements.add(paragraph)
                            }
                        }
                        "tbl" -> {
                            val table = parseTable(element)
                            if (table != null) {
                                elements.add(table)
                            }
                        }
                    }
                }
            }
        }
        return WordDocument(elements)
    } catch (e: Exception) {
        e.printStackTrace()
        return WordDocument(emptyList())
    } finally {
        zipFile?.close()
    }
}

private fun parseParagraph(pElement: Element): WordElement.Paragraph? {
    var isHeading = false
    var headingLevel = 0

    val pPrs = getDirectChildrenByLocalName(pElement, "pPr")
    if (pPrs.isNotEmpty()) {
        val pStyle = getDirectChildrenByLocalName(pPrs[0], "pStyle")
        if (pStyle.isNotEmpty()) {
            val styleVal = pStyle[0].getAttribute("w:val") ?: ""
            if (styleVal.startsWith("Heading", ignoreCase = true)) {
                isHeading = true
                headingLevel = styleVal.substringAfter("Heading").toIntOrNull() ?: 1
            }
        }
    }

    val tList = getElementsByLocalName(pElement, "t")
    val textBuilder = StringBuilder()
    for (tNode in tList) {
        textBuilder.append(tNode.textContent)
    }
    val text = textBuilder.toString()
    if (text.isBlank() && !isHeading) return null
    return WordElement.Paragraph(text, isHeading, headingLevel)
}

private fun parseTable(tblElement: Element): WordElement.Table? {
    val rows = mutableListOf<List<String>>()
    val trList = getDirectChildrenByLocalName(tblElement, "tr")
    for (tr in trList) {
        val rowCells = mutableListOf<String>()
        val tcList = getDirectChildrenByLocalName(tr, "tc")
        for (tc in tcList) {
            val tList = getElementsByLocalName(tc, "t")
            val cellText = StringBuilder()
            for (tNode in tList) {
                cellText.append(tNode.textContent)
            }
            rowCells.add(cellText.toString())
        }
        rows.add(rowCells)
    }
    if (rows.isEmpty()) return null
    return WordElement.Table(rows)
}

// ── Excel Parser ─────────────────────────────────────────────────────────────
fun parseXlsx(file: File): ExcelWorkbook {
    var zipFile: ZipFile? = null
    try {
        zipFile = ZipFile(file)

        // 1. Parse sharedStrings.xml
        val sharedStrings = mutableListOf<String>()
        val sharedStringsEntry = zipFile.getEntry("xl/sharedStrings.xml")
        if (sharedStringsEntry != null) {
            val db = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            val doc = db.parse(zipFile.getInputStream(sharedStringsEntry))
            val siList = getElementsByLocalName(doc.documentElement, "si")
            for (si in siList) {
                sharedStrings.add(si.textContent ?: "")
            }
        }

        // 2. Parse workbook.xml to get sheet names
        val sheetNames = mutableListOf<String>()
        val workbookEntry = zipFile.getEntry("xl/workbook.xml")
        if (workbookEntry != null) {
            val db = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            val doc = db.parse(zipFile.getInputStream(workbookEntry))
            val sheetList = getElementsByLocalName(doc.documentElement, "sheet")
            for (i in sheetList.indices) {
                val sheet = sheetList[i]
                sheetNames.add(sheet.getAttribute("name") ?: "Sheet ${i + 1}")
            }
        }

        // 3. Parse each sheet
        val sheets = mutableListOf<ExcelSheet>()
        var sheetIndex = 1
        while (true) {
            val sheetEntry = zipFile.getEntry("xl/worksheets/sheet$sheetIndex.xml") ?: break
            val db = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            val doc = db.parse(zipFile.getInputStream(sheetEntry))

            val rowsList = getElementsByLocalName(doc.documentElement, "row")
            val grid = mutableMapOf<Int, MutableMap<Int, String>>()
            var maxRow = 0
            var maxCol = 0

            for (i in rowsList.indices) {
                val rowEl = rowsList[i]
                val rAttr = rowEl.getAttribute("r")
                val rowIndex = (rAttr.toIntOrNull() ?: (i + 1)) - 1
                if (rowIndex > maxRow) maxRow = rowIndex

                val cList = getDirectChildrenByLocalName(rowEl, "c")
                for (cEl in cList) {
                    val rRef = cEl.getAttribute("r")
                    val coords = cellRefToCoords(rRef) ?: continue
                    val cRow = coords.first
                    val cCol = coords.second

                    if (cRow > maxRow) maxRow = cRow
                    if (cCol > maxCol) maxCol = cCol

                    val type = cEl.getAttribute("t")
                    val vList = getDirectChildrenByLocalName(cEl, "v")
                    var cellVal = ""
                    if (vList.isNotEmpty()) {
                        val rawVal = vList[0].textContent ?: ""
                        cellVal = if (type == "s") {
                            val idx = rawVal.toIntOrNull()
                            if (idx != null && idx >= 0 && idx < sharedStrings.size) {
                                sharedStrings[idx]
                            } else {
                                rawVal
                            }
                        } else {
                            rawVal
                        }
                    } else {
                        // Check inline string
                        val isList = getDirectChildrenByLocalName(cEl, "is")
                        if (isList.isNotEmpty()) {
                            val tList = getDirectChildrenByLocalName(isList[0], "t")
                            if (tList.isNotEmpty()) {
                                cellVal = tList[0].textContent ?: ""
                            }
                        }
                    }

                    val rowMap = grid.getOrPut(cRow) { mutableMapOf() }
                    rowMap[cCol] = cellVal
                }
            }

            // Convert grid map to 2D list
            val rows = mutableListOf<List<String>>()
            for (r in 0..maxRow) {
                val rowCells = mutableListOf<String>()
                val rowMap = grid[r]
                for (c in 0..maxCol) {
                    rowCells.add(rowMap?.get(c) ?: "")
                }
                rows.add(rowCells)
            }

            val sheetName = sheetNames.getOrNull(sheetIndex - 1) ?: "Sheet $sheetIndex"
            sheets.add(ExcelSheet(sheetName, rows))
            sheetIndex++
        }

        return ExcelWorkbook(sheets)
    } catch (e: Exception) {
        e.printStackTrace()
        return ExcelWorkbook(emptyList())
    } finally {
        zipFile?.close()
    }
}

private fun cellRefToCoords(ref: String): Pair<Int, Int>? {
    val colStr = ref.takeWhile { it.isLetter() }
    val rowStr = ref.drop(colStr.length)
    val row = rowStr.toIntOrNull() ?: return null
    var col = 0
    for (char in colStr) {
        col = col * 26 + (char.uppercaseChar() - 'A' + 1)
    }
    return Pair(row - 1, col - 1)
}

// ── PPT Parser ───────────────────────────────────────────────────────────────
fun parsePptx(file: File): List<PptSlide> {
    var zipFile: ZipFile? = null
    try {
        zipFile = ZipFile(file)
        val slides = mutableListOf<PptSlide>()
        var slideIndex = 1
        while (true) {
            val slideEntry = zipFile.getEntry("ppt/slides/slide$slideIndex.xml") ?: break
            val db = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            val doc = db.parse(zipFile.getInputStream(slideEntry))

            val paragraphs = mutableListOf<String>()
            val pList = getElementsByLocalName(doc.documentElement, "p")
            for (pEl in pList) {
                val tList = getElementsByLocalName(pEl, "t")
                val pText = StringBuilder()
                for (tNode in tList) {
                    pText.append(tNode.textContent)
                }
                val text = pText.toString().trim()
                if (text.isNotEmpty()) {
                    paragraphs.add(text)
                }
            }

            slides.add(PptSlide(slideIndex, paragraphs))
            slideIndex++
        }
        return slides
    } catch (e: Exception) {
        e.printStackTrace()
        return emptyList()
    } finally {
        zipFile?.close()
    }
}

package com.example.core

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.R
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

enum class ExportFormat(val extension: String, val mimeType: String, val label: String) {
    TXT("txt", "text/plain", "TXT (Unicode)"),
    DOCX("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "DOCX (Word)"),
    PDF("pdf", "application/pdf", "PDF (নথি)")
}

enum class ExportScope(val labelBn: String) {
    CORRECTED_ONLY("শুধু সংশোধিত লেখা"),
    COMPARISON("মূল বনাম সংশোধিত তুলনা"),
    FULL_REPORT("সম্পূর্ণ বিশ্লেষণ রিপোর্ট")
}

object ManuscriptExporter {

    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    fun sharePlainText(context: Context, title: String, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "শেয়ার করুন — Sobdosuddhi.1.0").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun buildExportContent(
        title: String,
        originalText: String,
        correctedText: String,
        analysisSummary: String,
        scope: ExportScope
    ): String {
        val header = buildString {
            appendLine(title.ifBlank { "শিরোনামহীন পান্ডুলিপি" })
            appendLine("══════════════════════════════════")
            appendLine("Sobdosuddhi.1.0 | Developed by Ovi Biswas")
            appendLine()
        }
        return when (scope) {
            ExportScope.CORRECTED_ONLY -> header + correctedText
            ExportScope.COMPARISON -> buildString {
                append(header)
                appendLine("【 মূল লেখা 】")
                appendLine(originalText.ifBlank { "(খালি)" })
                appendLine()
                appendLine("──────────────────────────────────")
                appendLine("【 সংশোধিত লেখা 】")
                appendLine(correctedText.ifBlank { "(খালি)" })
                appendLine()
                appendLine("──────────────────────────────────")
                appendLine("【 পঙ্ক্তিভিত্তিক পরিবর্তন 】")
                val diffs = BengaliTextUtils.buildLineDiff(originalText, correctedText)
                val changedLines = diffs.filter { it.changed }
                if (changedLines.isEmpty()) {
                    appendLine("কোনো পরিবর্তন পাওয়া যায়নি।")
                } else {
                    changedLines.forEach { diff ->
                        appendLine("পঙ্ক্তি ${BengaliTextUtils.toBengaliDigits(diff.lineNumber)}:")
                        appendLine("  − মূল: ${diff.original}")
                        appendLine("  + নতুন: ${diff.corrected}")
                    }
                }
            }
            ExportScope.FULL_REPORT -> buildString {
                append(header)
                appendLine("【 সংশোধিত পান্ডুলিপি 】")
                appendLine(correctedText)
                appendLine()
                appendLine("══════════════════════════════════")
                appendLine("【 সম্পূর্ণ বিশ্লেষণ রিপোর্ট 】")
                appendLine(analysisSummary.ifBlank { "বিশ্লেষণ রিপোর্ট এখনো তৈরি হয়নি।" })
            }
        }
    }

    fun exportAndShareFile(
        context: Context,
        title: String,
        bodyText: String,
        format: ExportFormat
    ): Result<File> = runCatching {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeName = title.replace(Regex("[^\\p{L}\\p{N}_\\- ]"), "").trim()
            .replace(Regex("\\s+"), "_")
            .take(40)
            .ifBlank { "Sobdosuddhi_Manuscript" }
        val file = File(exportDir, "${safeName}_${System.currentTimeMillis()}.${format.extension}")

        when (format) {
            ExportFormat.TXT -> writeUtf8Txt(file, bodyText)
            ExportFormat.DOCX -> writeUnicodeDocx(file, title, bodyText)
            ExportFormat.PDF -> writeBengaliPdf(context, file, title, bodyText)
        }

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = format.mimeType
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "${format.label} সংরক্ষণ / শেয়ার করুন").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
        file
    }

    fun writeUtf8Txt(file: File, content: String) {
        // UTF-8 BOM + content guarantees crisp Bengali Unicode opening across Windows/Android editors
        FileOutputStream(file).use { out ->
            out.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
            out.write(content.toByteArray(Charsets.UTF_8))
        }
    }

    fun writeUnicodeDocx(file: File, title: String, content: String) {
        val contentTypesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>"""

        val relsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""

        val paragraphsXml = buildString {
            content.lines().forEach { line ->
                val escaped = escapeXml(line)
                append("<w:p><w:r><w:rPr><w:rFonts w:ascii=\"Noto Serif Bengali\" w:hAnsi=\"Noto Serif Bengali\" w:cs=\"Noto Serif Bengali\"/></w:rPr><w:t xml:space=\"preserve\">")
                append(escaped)
                append("</w:t></w:r></w:p>")
            }
        }

        val documentXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:body>
    $paragraphsXml
    <w:sectPr><w:pgSz w:w="11906" w:h="16838"/><w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440"/></w:sectPr>
  </w:body>
</w:document>"""

        ZipOutputStream(FileOutputStream(file)).use { zip ->
            zip.putNextEntry(ZipEntry("[Content_Types].xml"))
            zip.write(contentTypesXml.toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("_rels/.rels"))
            zip.write(relsXml.toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("word/document.xml"))
            zip.write(documentXml.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
    }

    fun writeBengaliPdf(context: Context, file: File, title: String, content: String) {
        val pdfDocument = PdfDocument()
        try {
            val pageWidth = 595 // A4 @ 72dpi
            val pageHeight = 842
            val margin = 48f
            val contentWidth = (pageWidth - margin * 2).toInt()

            val bengaliTypeface = try {
                ResourcesCompat.getFont(context, R.font.noto_serif_bengali) ?: Typeface.SERIF
            } catch (_: Exception) {
                Typeface.SERIF
            }

            val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#241914")
                textSize = 12f
                typeface = bengaliTypeface
            }
            val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#7A6A5E")
                textSize = 9f
                typeface = bengaliTypeface
            }

            val staticLayout = StaticLayout.Builder
                .obtain(content, 0, content.length, bodyPaint, contentWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(4f, 1.25f)
                .setIncludePad(true)
                .build()

            val usableHeight = (pageHeight - margin * 2 - 24f).toInt()
            var currentLine = 0
            var pageNumber = 1

            while (currentLine < staticLayout.lineCount) {
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas
                canvas.drawColor(Color.parseColor("#FAF6EE"))

                var y = margin
                while (currentLine < staticLayout.lineCount) {
                    val lineHeight = staticLayout.getLineBottom(currentLine) - staticLayout.getLineTop(currentLine)
                    if (y + lineHeight > margin + usableHeight && y > margin) {
                        break
                    }
                    val start = staticLayout.getLineStart(currentLine)
                    val end = staticLayout.getLineEnd(currentLine)
                    val lineText = content.substring(start, end).trimEnd('\n', '\r')
                    canvas.drawText(lineText, margin, y - staticLayout.getLineAscent(currentLine), bodyPaint)
                    y += lineHeight
                    currentLine++
                }

                canvas.drawText(
                    "Sobdosuddhi.1.0 • Developed by Ovi Biswas • পৃষ্ঠা ${BengaliTextUtils.toBengaliDigits(pageNumber)}",
                    margin,
                    pageHeight - 24f,
                    footerPaint
                )

                pdfDocument.finishPage(page)
                pageNumber++
            }

            if (staticLayout.lineCount == 0) {
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
                val page = pdfDocument.startPage(pageInfo)
                page.canvas.drawColor(Color.parseColor("#FAF6EE"))
                page.canvas.drawText(title, margin, margin + 16f, bodyPaint)
                pdfDocument.finishPage(page)
            }

            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
        } finally {
            runCatching { pdfDocument.close() }
        }
    }

    private fun escapeXml(input: String): String =
        input.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
}

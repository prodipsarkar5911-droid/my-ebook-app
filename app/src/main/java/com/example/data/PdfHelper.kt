package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object PdfHelper {

    suspend fun extractPdfPages(context: Context, uri: Uri): List<Bitmap> = withContext(Dispatchers.IO) {
        val bitmaps = mutableListOf<Bitmap>()
        var tempFile: File? = null
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null

        try {
            tempFile = File(context.cacheDir, "temp_doc_${System.currentTimeMillis()}.pdf")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            val pageCount = renderer.pageCount

            for (i in 0 until minOf(pageCount, 10)) { // Support up to 10 pages per batch
                val page = renderer.openPage(i)
                // Render at high resolution (target width ~1200-1400px for crisp OCR)
                val targetWidth = 1400
                val scale = targetWidth.toFloat() / page.width.toFloat()
                val targetHeight = (page.height * scale).toInt()

                val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                bitmaps.add(bitmap)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
            tempFile?.delete()
        }
        bitmaps
    }

    fun Bitmap.toBase64Jpeg(quality: Int = 85): String {
        val outputStream = ByteArrayOutputStream()
        this.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    /**
     * Generates a sample book page bitmap with rich formatting for instant live testing.
     */
    fun createSampleBookPageBitmap(
        title: String,
        subtitle: String,
        paragraphs: List<String>,
        hasColoredCallout: Boolean = false,
        fontVariationNote: String? = null
    ): Bitmap {
        val width = 1200
        val height = 1600
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(0xFFFCFBF7.toInt()) // Warm paper background

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        var y = 140f

        // Document Header Bar
        paint.color = 0xFF2A2E3D.toInt()
        paint.textSize = 54f
        paint.isFakeBoldText = true
        canvas.drawText(title, 80f, y, paint)
        y += 70f

        // Subtitle (H2)
        paint.color = 0xFF4B5563.toInt()
        paint.textSize = 36f
        paint.isFakeBoldText = false
        canvas.drawText(subtitle, 80f, y, paint)
        y += 60f

        // Decorative Rule
        paint.color = 0xFFD1D5DB.toInt()
        paint.strokeWidth = 3f
        canvas.drawLine(80f, y, width - 80f, y, paint)
        y += 60f

        // Body Paragraphs
        for ((index, text) in paragraphs.withIndex()) {
            if (hasColoredCallout && index == 1) {
                // Callout box with distinct color
                val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFFFEF3C7.toInt()
                }
                canvas.drawRect(80f, y - 30f, width - 80f, y + 140f, boxPaint)
                
                val calloutTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFFB45309.toInt() // Amber color1
                    textSize = 30f
                    isFakeBoldText = true
                }
                canvas.drawText("NOTE: Special Archival Annotation (Color Class 1)", 110f, y + 20f, calloutTextPaint)
                val calloutSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFF78350F.toInt()
                    textSize = 26f
                }
                canvas.drawText("This segment requires dynamic color span: <span class=\"color1\">", 110f, y + 70f, calloutSubPaint)
                y += 180f
            }

            paint.color = 0xFF1F2937.toInt()
            paint.textSize = 30f
            paint.isFakeBoldText = false

            // Draw paragraph text with simulated line wrapping
            val words = text.split(" ")
            var line = "        " // Visual paragraph indent
            for (word in words) {
                val testLine = "$line $word"
                if (paint.measureText(testLine) > (width - 160f)) {
                    canvas.drawText(line, 80f, y, paint)
                    y += 44f
                    line = word
                } else {
                    line = testLine
                }
            }
            if (line.isNotEmpty()) {
                canvas.drawText(line, 80f, y, paint)
                y += 56f
            }
        }

        // Secondary Section (H3)
        if (fontVariationNote != null) {
            y += 30f
            paint.color = 0xFF1E3A8A.toInt()
            paint.textSize = 34f
            paint.isFakeBoldText = true
            canvas.drawText("Section 2: Typography & Font Variations", 80f, y, paint)
            y += 50f

            paint.color = 0xFF374151.toInt()
            paint.textSize = 28f
            paint.isFakeBoldText = false
            canvas.drawText("        $fontVariationNote", 80f, y, paint)
        }

        return bitmap
    }
}

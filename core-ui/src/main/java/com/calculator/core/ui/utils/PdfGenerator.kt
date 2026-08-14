package com.calculator.core.ui.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

object PdfGenerator {

    /**
     * Generates a PDF containing the calculation details and step-by-step solution,
     * and saves it to the given Uri using ContentResolver.
     */
    suspend fun generateAndSavePdf(
        context: Context,
        uri: Uri,
        title: String,
        expression: String,
        result: String,
        steps: List<String> = emptyList()
    ): Boolean = withContext(Dispatchers.IO) {
        val document = PdfDocument()

        // Page width 595 and height 842 corresponds to standard A4 size (72 dpi)
        val pageWidth = 595
        val pageHeight = 842
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        val titlePaint = TextPaint().apply {
            color = Color.BLACK
            textSize = 24f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val textPaint = TextPaint().apply {
            color = Color.DKGRAY
            textSize = 16f
            isAntiAlias = true
        }

        val resultPaint = TextPaint().apply {
            color = Color.parseColor("#00BFA5") // A nice readable teal color for PDF
            textSize = 20f
            isFakeBoldText = true
            isAntiAlias = true
        }

        var currentY = 50f
        val marginX = 50f
        val lineSpacing = 30f
        val contentWidth = pageWidth - (marginX * 2).toInt()

        // Helper to draw text with word wrap
        fun drawWrappedText(text: String, paint: TextPaint) {
            val staticLayout = StaticLayout.Builder.obtain(text, 0, text.length, paint, contentWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1.2f)
                .setIncludePad(false)
                .build()

            // Page break logic
            if (currentY + staticLayout.height > pageHeight - 50f) {
                document.finishPage(page)
                page = document.startPage(pageInfo)
                canvas = page.canvas
                currentY = 50f
            }

            canvas.save()
            canvas.translate(marginX, currentY)
            staticLayout.draw(canvas)
            canvas.restore()
            
            currentY += staticLayout.height + lineSpacing
        }

        // Draw title
        drawWrappedText(title, titlePaint)
        currentY += lineSpacing / 2

        // Draw expression
        drawWrappedText("Expression:", textPaint)
        currentY -= lineSpacing / 2
        drawWrappedText(expression, titlePaint)
        currentY += lineSpacing / 2

        // Draw result
        drawWrappedText("Result:", textPaint)
        currentY -= lineSpacing / 2
        drawWrappedText(result, resultPaint)
        currentY += lineSpacing

        // Draw Steps
        if (steps.isNotEmpty()) {
            drawWrappedText("Step-by-Step Solution:", titlePaint)
            
            for ((index, step) in steps.withIndex()) {
                val stepText = "${index + 1}. $step"
                drawWrappedText(stepText, textPaint)
            }
        }

        document.finishPage(page)

        var success = false
        try {
            context.contentResolver.openOutputStream(uri)?.use { outStream ->
                document.writeTo(outStream)
                success = true
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            document.close()
        }
        
        success
    }
}

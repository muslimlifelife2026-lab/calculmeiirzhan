package com.aerocalc.smartcalculator.ui.main

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ApplicationProvider
import com.calculator.core.ui.utils.PdfGenerator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PdfExportTest {
    @Test fun longSolutionSpansMultiplePages() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val output = File(context.cacheDir, "long-solution-${System.nanoTime()}.pdf")
        try {
            val saved = PdfGenerator.generateAndSavePdf(
                context, Uri.fromFile(output), "AeroCalc", "2 + 2", "4",
                listOf("Промежуточное объяснение ".repeat(600))
            )
            assertTrue(saved)
            ParcelFileDescriptor.open(output, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                PdfRenderer(descriptor).use { renderer -> assertTrue(renderer.pageCount > 1) }
            }
        } finally {
            output.delete()
        }
    }
}

package com.aerocalc.smartcalculator.ui.main

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import com.calculator.engine.algebra.LinearSystemSolver
import com.calculator.feature.calculator.scanner.MathOcrEngine
import java.io.File
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PrintedEquationOcrTest {
    @Test fun galleryImageRetainsTwoCompleteEquationsAndProducesTheExpectedSolution() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = File(context.cacheDir, "printed-system-ocr-test.png")
        val bitmap = Bitmap.createBitmap(1000, 360, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).apply {
            drawColor(Color.WHITE)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK; textSize = 72f; typeface = Typeface.MONOSPACE
            }
            drawText("2x + y = 5", 50f, 120f, paint)
            drawText("x - y = 1", 50f, 250f, paint)
        }
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        val fixtures = File(context.getExternalFilesDir(null), "ocr-fixtures").apply { mkdirs() }
        file.copyTo(File(fixtures, "printed-system.png"), overwrite = true)
        val engine = MathOcrEngine()
        try {
            val recognized = withTimeout(60000) { engine.processUri(context, Uri.fromFile(file)) }
            assertNotNull("Printed equation image was not recognized", recognized)
            val lines = recognized!!.lines()
            assertEquals(2, lines.size)
            val solution = LinearSystemSolver.solve(lines[0], lines[1])
            assertEquals(mapOf("x" to "2", "y" to "1"), solution.solution)
        } finally {
            engine.close()
            file.delete()
        }
    }
}

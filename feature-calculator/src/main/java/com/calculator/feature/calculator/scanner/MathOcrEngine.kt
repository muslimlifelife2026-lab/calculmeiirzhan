package com.calculator.feature.calculator.scanner

import android.media.Image
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class MathOcrEngine {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    fun close() = recognizer.close()

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    suspend fun processImage(imageProxy: ImageProxy): String? = suspendCancellableCoroutine { continuation ->
        val buffer = imageProxy.planes[0].buffer
        val bytes = ByteArray(buffer.capacity())
        buffer.get(bytes)
        val originalBitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)
        
        if (originalBitmap == null) {
            continuation.resume(null)
            imageProxy.close()
            return@suspendCancellableCoroutine
        }

        // Rotate the original bitmap FIRST so that it matches the Portrait view
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val matrix = android.graphics.Matrix()
        if (rotationDegrees != 0) {
            matrix.postRotate(rotationDegrees.toFloat())
        }
        val rotatedBitmap = android.graphics.Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true)

        // Crop the center region to match the UI bracket (e.g. 90% width, 35% height)
        val width = rotatedBitmap.width
        val height = rotatedBitmap.height
        val cropWidth = (width * 0.9f).toInt()
        val cropHeight = (height * 0.35f).toInt()
        val cropX = (width - cropWidth) / 2
        val cropY = (height - cropHeight) / 2
        
        val croppedBitmap = android.graphics.Bitmap.createBitmap(rotatedBitmap, cropX, cropY, cropWidth, cropHeight)
        val image = InputImage.fromBitmap(croppedBitmap, 0) // Already rotated, pass 0
        
        recognizer.process(image)
            .addOnSuccessListener { text ->
                val raw = text.text
                android.util.Log.d("MathOcrEngine", "Raw recognized text: $raw")
                val cleaned = cleanMathText(raw)
                android.util.Log.d("MathOcrEngine", "Cleaned text: $cleaned")
                continuation.resume(cleaned)
                imageProxy.close()
            }
            .addOnFailureListener { e ->
                android.util.Log.e("MathOcrEngine", "OCR Failed", e)
                continuation.resume(null)
                imageProxy.close()
            }
    }

    private fun cleanMathText(rawText: String): String {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.size != 1) return ""
        val expression = lines.single().substringBefore('=').trim()
            .replace("×", "*").replace("÷", "/").replace("−", "-").replace(',', '.')
        if (expression.isEmpty() || !expression.matches(Regex("[0-9A-Za-zπ+\\-*/().^%\\s]+"))) return ""
        return expression
    }
}

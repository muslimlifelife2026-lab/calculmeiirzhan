package com.calculator.feature.calculator.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.camera.core.ImageProxy
import com.calculator.engine.scanner.MathScanNormalizer
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.math.max
import kotlin.math.roundToInt

/** Owns decoded bitmaps until ML Kit finishes, including after coroutine cancellation. */
class MathOcrEngine {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val lock = Any()
    private var closed = false
    private var pendingTasks = 0

    fun close() = synchronized(lock) {
        if (!closed) {
            closed = true
            if (pendingTasks == 0) recognizer.close()
        }
    }

    suspend fun processImage(imageProxy: ImageProxy, viewportWidth: Int = 0, viewportHeight: Int = 0): String? {
        try {
            return withContext(Dispatchers.IO) {
                var owned: Bitmap? = null
                try {
                    if (imageProxy.format != ImageFormat.JPEG) return@withContext null
                    val buffer = imageProxy.planes.firstOrNull()?.buffer?.duplicate() ?: return@withContext null
                    if (buffer.remaining() !in 1..MAX_COMPRESSED_BYTES) return@withContext null
                    val bytes = ByteArray(buffer.remaining()).also { buffer.get(it) }
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                    options.inSampleSize = sampleSize(options.outWidth, options.outHeight)
                    options.inJustDecodeBounds = false
                    owned = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return@withContext null
                    val rotation = imageProxy.imageInfo.rotationDegrees
                    if (rotation != 0) {
                        val rotated = Bitmap.createBitmap(owned, 0, 0, owned.width, owned.height,
                            Matrix().apply { postRotate(rotation.toFloat()) }, true)
                        if (rotated !== owned) owned.recycle()
                        owned = rotated
                    }
                    // Match FILL_CENTER preview first, then its central 90% × 35% viewfinder.
                    val imageRatio = owned.width.toFloat() / owned.height
                    val viewRatio = if (viewportWidth > 0 && viewportHeight > 0) viewportWidth.toFloat() / viewportHeight else imageRatio
                    val visibleWidth = if (imageRatio > viewRatio) owned.height * viewRatio else owned.width.toFloat()
                    val visibleHeight = if (imageRatio > viewRatio) owned.height.toFloat() else owned.width / viewRatio
                    val cropWidth = (visibleWidth * 0.9f).roundToInt().coerceIn(1, owned.width)
                    val cropHeight = (visibleHeight * 0.35f).roundToInt().coerceIn(1, owned.height)
                    val cropped = Bitmap.createBitmap(owned, (owned.width - cropWidth) / 2,
                        (owned.height - cropHeight) / 2, cropWidth, cropHeight)
                    if (cropped !== owned) owned.recycle()
                    owned = cropped
                    coroutineContext.ensureActive()
                    val input = owned
                    owned = null // The task completion listener now owns it.
                    recognizeOwned(input)
                } finally {
                    owned?.recycle()
                }
            }
        } finally {
            imageProxy.close()
        }
    }

    suspend fun processUri(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        var owned: Bitmap? = null
        try {
            owned = if (Build.VERSION.SDK_INT >= 28) decodeModern(context, uri) else decodeLegacy(context, uri)
            coroutineContext.ensureActive()
            val input = owned ?: return@withContext null
            owned = null
            recognizeOwned(input)
        } finally {
            owned?.recycle()
        }
    }

    private suspend fun recognizeOwned(bitmap: Bitmap): String? = suspendCancellableCoroutine { continuation ->
        if (!continuation.isActive) {
            bitmap.recycle()
            return@suspendCancellableCoroutine
        }
        val accepted = synchronized(lock) {
            if (closed) false else { pendingTasks++; true }
        }
        if (!accepted) {
            bitmap.recycle()
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }
        fun release() {
            bitmap.recycle()
            synchronized(lock) {
                pendingTasks--
                if (closed && pendingTasks == 0) recognizer.close()
            }
        }
        try {
            recognizer.process(InputImage.fromBitmap(bitmap, 0)).addOnCompleteListener { task ->
                try {
                    if (continuation.isActive) continuation.resume(
                        if (task.isSuccessful) MathScanNormalizer.normalize(task.result.text) else null
                    )
                } finally {
                    // ML Kit may still read the bitmap after the caller has cancelled.
                    release()
                }
            }
        } catch (failure: RuntimeException) {
            release()
            if (continuation.isActive) continuation.resume(null)
        }
    }

    @RequiresApi(28)
    private fun decodeModern(context: Context, uri: Uri): Bitmap =
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val longest = max(info.size.width, info.size.height)
            if (longest > MAX_DIMENSION) {
                val factor = MAX_DIMENSION.toFloat() / longest
                decoder.setTargetSize((info.size.width * factor).roundToInt().coerceAtLeast(1),
                    (info.size.height * factor).roundToInt().coerceAtLeast(1))
            }
        }

    private fun decodeLegacy(context: Context, uri: Uri): Bitmap? {
        val resolver = context.contentResolver
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        options.inSampleSize = sampleSize(options.outWidth, options.outHeight)
        options.inJustDecodeBounds = false
        val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) } ?: return null
        val orientation = try {
            resolver.openInputStream(uri)?.use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
        } catch (_: java.io.IOException) { ExifInterface.ORIENTATION_NORMAL }
        val matrix = Matrix().apply {
            when (orientation) {
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
                ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> { setRotate(90f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
                ExifInterface.ORIENTATION_TRANSVERSE -> { setRotate(-90f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(-90f)
            }
        }
        if (matrix.isIdentity) return bitmap
        return try {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } finally { bitmap.recycle() }
    }

    private fun sampleSize(width: Int, height: Int): Int {
        require(width > 0 && height > 0) { "Не удалось прочитать изображение" }
        var sample = 1
        while (max(width, height) / sample > MAX_DIMENSION) sample *= 2
        return sample
    }

    companion object {
        private const val MAX_DIMENSION = 2048
        private const val MAX_COMPRESSED_BYTES = 32 * 1024 * 1024
    }
}

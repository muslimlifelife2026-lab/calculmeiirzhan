package com.calculator.core.ui.components

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.ceil

private class GlassBackdropState {
    var size by mutableStateOf(Size.Zero)
    var origin by mutableStateOf(Offset.Zero)
}

private val LocalGlassBackdrop = staticCompositionLocalOf<GlassBackdropState?> { null }

/** One original, static scene shared by the background and every glass viewport. */
@Composable
fun GlassBackdropHost(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val state = remember { GlassBackdropState() }
    CompositionLocalProvider(LocalGlassBackdrop provides state) {
        Box(modifier.onGloballyPositioned {
            state.size = Size(it.size.width.toFloat(), it.size.height.toFloat())
            state.origin = it.positionInRoot()
        }.drawWithCache {
            val scene = GlassScene(size)
            onDrawBehind { scene.draw(this) }
        }, content = content)
    }
}

/**
 * Samples only the host's procedural background, never text or sibling content.
 * API 33+ also refracts that background near the rounded edge. API 31–32 blurs
 * it in a separate layer. Older devices retain an unblurred crop of the scene.
 */
fun Modifier.glassBackdrop(blurRadius: Dp = 16.dp, cornerRadius: Dp = 28.dp): Modifier = composed {
    val host = LocalGlassBackdrop.current
    if (host == null) return@composed this
    val layer = rememberGraphicsLayer()
    // Keep API 33 classes out of the compatibility implementation's signatures.
    val refraction = remember { if (Build.VERSION.SDK_INT >= 33) Api33EdgeRefraction.createOrNull() else null }
    var origin by remember { mutableStateOf(Offset.Zero) }
    this.onGloballyPositioned { origin = it.positionInRoot() }
        .drawWithCache {
            val globalSize = host.size
            val relativeOrigin = origin - host.origin
            val radius = if (Build.VERSION.SDK_INT >= 31) blurRadius.toPx() else 0f
            // Extra source pixels prevent transparent fringes around the blurred viewport.
            val margin = ceil(radius * 2f)
            val scene = GlassScene(globalSize)
            val blur = if (radius > 0f) BlurEffect(radius, radius, TileMode.Clamp) else null
            layer.renderEffect = refraction?.effect(
                size, margin, cornerRadius.toPx(), 1.8.dp.toPx(), radius
            ) ?: blur
            if (globalSize.width > 0f && globalSize.height > 0f) {
                layer.record(
                    density = this,
                    layoutDirection = layoutDirection,
                    size = IntSize(ceil(size.width + margin * 2f).toInt(), ceil(size.height + margin * 2f).toInt())
                ) {
                    translate(margin - relativeOrigin.x, margin - relativeOrigin.y) { scene.draw(this) }
                }
            }
            onDrawBehind {
                if (globalSize.width > 0f && globalSize.height > 0f) {
                    translate(-margin, -margin) { drawLayer(layer) }
                }
            }
        }
}

private interface EdgeRefraction {
    fun effect(viewport: Size, margin: Float, corner: Float, strength: Float, blur: Float): RenderEffect?
}

/** API-specific class instantiated only after the API 33 guard. */
@RequiresApi(33)
private class Api33EdgeRefraction private constructor() : EdgeRefraction {
    private val shader = android.graphics.RuntimeShader(SOURCE)
    private var configuration: OpticsConfiguration? = null
    private var cachedEffect: RenderEffect? = null

    override fun effect(viewport: Size, margin: Float, corner: Float, strength: Float, blur: Float): RenderEffect? {
        if (disabled || viewport.width <= 0f || viewport.height <= 0f) return null
        val next = OpticsConfiguration(viewport, margin, corner, strength, blur)
        if (configuration == next) return cachedEffect
        return try {
            shader.setFloatUniform("viewportSize", viewport.width, viewport.height)
            shader.setFloatUniform("margin", margin)
            shader.setFloatUniform("cornerRadius", corner.coerceAtMost(viewport.minDimension / 2f))
            shader.setFloatUniform("strength", strength)
            val refract = android.graphics.RenderEffect.createRuntimeShaderEffect(shader, "contents")
            val effect = if (blur > 0f) {
                android.graphics.RenderEffect.createChainEffect(
                    refract,
                    android.graphics.RenderEffect.createBlurEffect(blur, blur, android.graphics.Shader.TileMode.CLAMP)
                )
            } else refract
            effect.asComposeRenderEffect().also {
                configuration = next
                cachedEffect = it
            }
        } catch (failure: RuntimeException) {
            disable(failure)
            null
        }
    }

    private data class OpticsConfiguration(
        val viewport: Size, val margin: Float, val corner: Float, val strength: Float, val blur: Float
    )

    companion object {
        private var disabled = false

        fun createOrNull(): EdgeRefraction? = if (disabled) null else try {
            Api33EdgeRefraction()
        } catch (failure: RuntimeException) {
            disable(failure)
            null
        }

        private fun disable(failure: RuntimeException) {
            if (!disabled) Log.w("AeroCalcGlass", "Edge optics unavailable; keeping background blur", failure)
            disabled = true
        }

        private const val SOURCE = """
            uniform shader contents;
            uniform float2 viewportSize;
            uniform float margin;
            uniform float cornerRadius;
            uniform float strength;

            half4 main(float2 coord) {
                float2 p = coord - float2(margin) - viewportSize * 0.5;
                float2 q = abs(p) - (viewportSize * 0.5 - float2(cornerRadius));
                float2 outside = max(q, float2(0.0));
                float distance = length(outside) + min(max(q.x, q.y), 0.0) - cornerRadius;
                float2 normal;
                if (length(outside) > 0.001) {
                    normal = outside / length(outside) * sign(p);
                } else {
                    normal = q.x > q.y ? float2(sign(p.x), 0.0) : float2(0.0, sign(p.y));
                }
                float edge = 1.0 - smoothstep(0.0, max(5.0, cornerRadius * 0.28), abs(distance));
                return contents.eval(coord - normal * strength * edge);
            }
        """
    }
}

private class GlassScene(private val extent: Size) {
    private val base = Brush.verticalGradient(
        listOf(Color(0xFF30343A), Color(0xFF1D242C), Color(0xFF101820)),
        endY = extent.height.coerceAtLeast(1f)
    )
    private val silver = Brush.radialGradient(
        listOf(Color(0x80959BA2), Color(0x4057626D), Color.Transparent),
        center = Offset(extent.width * 1.12f, extent.height * 0.30f),
        radius = extent.height.coerceAtLeast(1f) * 0.78f
    )
    private val warmLight = Brush.radialGradient(
        listOf(Color(0x306F675D), Color.Transparent),
        center = Offset(-extent.width * 0.2f, extent.height * 0.12f),
        radius = extent.width.coerceAtLeast(1f) * 1.15f
    )
    private val ribbon = Path().apply {
        val w = extent.width
        val h = extent.height
        moveTo(w * 1.15f, -h * 0.10f)
        cubicTo(w * 0.80f, h * 0.25f, -w * 0.05f, h * 0.24f, w * 0.24f, h * 0.46f)
        cubicTo(w * 0.51f, h * 0.67f, w * 1.22f, h * 0.65f, w * 0.69f, h * 1.12f)
        lineTo(w * 1.45f, h * 1.12f)
        lineTo(w * 1.45f, -h * 0.10f)
        close()
    }
    private val ribbonFill = Brush.linearGradient(
        listOf(Color(0x108C949D), Color(0x506C7885), Color(0x285A646F)),
        start = Offset.Zero,
        end = Offset(extent.width, extent.height)
    )

    fun draw(scope: DrawScope) = with(scope) {
        drawRect(base, size = extent)
        drawRect(silver, size = extent)
        drawRect(warmLight, size = extent)
        drawPath(ribbon, ribbonFill)
        drawPath(ribbon, Color(0x60909AA5), style = Stroke(1.1.dp.toPx()))
    }
}

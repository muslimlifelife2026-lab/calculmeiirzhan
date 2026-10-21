package com.calculator.core.ui.components

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.floor
import kotlinx.coroutines.CancellationException

private class GlassBackdropState {
    var size by mutableStateOf(Size.Zero)
    var origin by mutableStateOf(Offset.Zero)
    var scene by mutableStateOf(GlassScene(Size.Zero))
    var images by mutableStateOf<List<ImageBitmap>>(emptyList())
}

private val LocalGlassBackdrop = staticCompositionLocalOf<GlassBackdropState?> { null }

/** One original, static scene shared by the background and every glass viewport. */
@Composable
fun GlassBackdropHost(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    // Three shared blur levels replace an offscreen blur pass for every key/card.
    // Half-resolution textures keep the shared cache bounded to 3/4 of a screen.
    val soft = rememberGraphicsLayer()
    val medium = rememberGraphicsLayer()
    val strong = rememberGraphicsLayer()
    val state = remember { GlassBackdropState() }
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val extent = state.size
    LaunchedEffect(extent, density, direction) {
        state.images = emptyList()
        if (extent.width > 0f && extent.height > 0f) {
            val scene = state.scene
            try {
                val images = listOf(soft, medium, strong).mapIndexed { index, shared ->
                    val radius = with(density) { listOf(6.dp, 12.dp, 16.dp)[index].toPx() } * 0.5f
                    shared.renderEffect = if (Build.VERSION.SDK_INT >= 31) BlurEffect(radius, radius, TileMode.Clamp) else null
                    shared.record(
                        density = density,
                        layoutDirection = direction,
                        size = IntSize(ceil(extent.width * 0.5f).toInt(), ceil(extent.height * 0.5f).toInt())
                    ) { scale(0.5f, 0.5f, pivot = Offset.Zero) { scene.draw(this) } }
                    // A RenderNode reference alone can replay a full-screen blur for
                    // each consumer. Rasterize once, then sample immutable pixels.
                    shared.toImageBitmap()
                }
                state.images = images
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: RuntimeException) {
                // Unsupported graphics capture must leave a usable, sharp crop.
                Log.w("AeroCalcGlass", "Background cache unavailable; keeping scene crop", failure)
            }
        }
    }
    CompositionLocalProvider(LocalGlassBackdrop provides state) {
        Box(modifier.onGloballyPositioned {
            val nextSize = Size(it.size.width.toFloat(), it.size.height.toFloat())
            if (state.size != nextSize) {
                state.size = nextSize
                state.scene = GlassScene(nextSize)
            }
            state.origin = it.positionInRoot()
        }.drawWithCache {
            val scene = state.scene
            onDrawBehind { scene.draw(this) }
        }, content = content)
    }
}

/**
 * Samples only the host's procedural background, never text or sibling content.
 * Blur is shared at three cached resolutions (6, 12 and 16 dp). API 33+ also
 * refracts that background near the rounded edge. Older devices retain a crop.
 */
fun Modifier.glassBackdrop(blurRadius: Dp = 16.dp, cornerRadius: Dp = 28.dp, refract: Boolean = true): Modifier = composed {
    val host = LocalGlassBackdrop.current
    if (host == null) return@composed this
    // Keep API 33 classes out of the compatibility implementation's signatures.
    val refraction = remember(refract) { if (refract && Build.VERSION.SDK_INT >= 33) Api33EdgeRefraction.createOrNull() else null }
    val layer = if (refraction != null) rememberGraphicsLayer() else null
    var origin by remember { mutableStateOf(Offset.Zero) }
    this.onGloballyPositioned { origin = it.positionInRoot() }
        .drawWithCache {
            val globalSize = host.size
            val relativeOrigin = origin - host.origin
            val image = host.images.getOrNull(when {
                blurRadius <= 8.dp -> 0
                blurRadius <= 14.dp -> 1
                else -> 2
            })
            val scene = host.scene
            if (layer == null) {
                // Small keys already have rounded highlights and a contact glow.
                // Sample the cached glass directly without an offscreen shader per key.
                onDrawBehind {
                    if (image != null) drawBackdropCrop(image, relativeOrigin)
                    else translate(-relativeOrigin.x, -relativeOrigin.y) { scene.draw(this) }
                }
            } else {
                // Blur is already cached by the host; only edge refraction needs a margin.
                val margin = ceil(4.dp.toPx() * 2f)
                layer.renderEffect = refraction?.effect(
                    size, margin, cornerRadius.toPx(), 4.dp.toPx(), 0f
                )
                if (globalSize.width > 0f && globalSize.height > 0f) {
                    layer.record(
                        density = this,
                        layoutDirection = layoutDirection,
                        size = IntSize(ceil(size.width + margin * 2f).toInt(), ceil(size.height + margin * 2f).toInt())
                    ) {
                        val sourceOrigin = relativeOrigin - Offset(margin, margin)
                        if (image != null) drawBackdropCrop(image, sourceOrigin)
                        else translate(-sourceOrigin.x, -sourceOrigin.y) { scene.draw(this) }
                    }
                }
                onDrawBehind {
                    if (globalSize.width > 0f && globalSize.height > 0f) {
                        translate(-margin, -margin) { drawLayer(layer) }
                    }
                }
            }
        }
}

/** Draw only the visible source pixels, rather than a screen-sized image for every key. */
private fun DrawScope.drawBackdropCrop(image: ImageBitmap, origin: Offset) {
    val left = floor(origin.x * 0.5f).toInt().coerceIn(0, image.width)
    val top = floor(origin.y * 0.5f).toInt().coerceIn(0, image.height)
    val right = ceil((origin.x + size.width) * 0.5f).toInt().coerceIn(left, image.width)
    val bottom = ceil((origin.y + size.height) * 0.5f).toInt().coerceIn(top, image.height)
    if (right == left || bottom == top) return
    translate(left * 2f - origin.x, top * 2f - origin.y) {
        drawImage(image, srcOffset = IntOffset(left, top), srcSize = IntSize(right - left, bottom - top),
            dstSize = IntSize((right - left) * 2, (bottom - top) * 2))
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
        listOf(Color(0xFF25282D), Color(0xFF191F26), Color(0xFF10161D)),
        endY = extent.height.coerceAtLeast(1f)
    )
    private val silver = Brush.radialGradient(
        listOf(Color(0x70959BA2), Color(0x3057626D), Color.Transparent),
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
        listOf(Color(0x107A818B), Color(0x65778390), Color(0x203B4652)),
        start = Offset.Zero,
        end = Offset(extent.width, extent.height)
    )
    private val fold = Path().apply {
        val w = extent.width
        val h = extent.height
        moveTo(-w * .15f, -h * .08f)
        cubicTo(w * .85f, h * .06f, w * .72f, h * .18f, w * .25f, h * .28f)
        cubicTo(-w * .22f, h * .39f, w * .18f, h * .51f, w * .64f, h * .54f)
        cubicTo(w * .12f, h * .52f, -w * .40f, h * .40f, w * .08f, h * .27f)
        cubicTo(w * .44f, h * .16f, w * .55f, h * .06f, -w * .15f, -h * .02f)
        close()
    }
    private val foldFill = Brush.linearGradient(
        listOf(Color(0x307B7977), Color(0x707F8993), Color(0x18303C49)),
        start = Offset.Zero, end = Offset(extent.width * .65f, extent.height * .55f)
    )

    fun draw(scope: DrawScope) = with(scope) {
        drawRect(base, size = extent)
        drawRect(silver, size = extent)
        drawRect(warmLight, size = extent)
        drawPath(ribbon, ribbonFill)
        drawPath(ribbon, Color(0x4A9DA8B4), style = Stroke(0.9.dp.toPx()))
        drawPath(fold, foldFill)
        drawPath(fold, Color(0x409EA7B0), style = Stroke(0.7.dp.toPx()))
    }
}

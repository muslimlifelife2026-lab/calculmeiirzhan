package com.calculator.feature.geometry

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calculator.core.ui.theme.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.isActive

enum class Shape3DType(val title: String) {
    CUBE("Куб / Параллелепипед"),
    SPHERE("Сфера / Шар"),
    CYLINDER("Цилиндр"),
    CONE("Конус"),
    PYRAMID("Пирамида")
}

data class Point3D(val x: Float, val y: Float, val z: Float)
data class ProjectedPoint(val offset: Offset, val z: Float)

@Composable
fun Stereometry3DCanvas(
    shapeType: Shape3DType,
    modifier: Modifier = Modifier,
    dimA: Float = 10f,
    dimB: Float = 10f,
    dimC: Float = 10f,
    autoRotate: Boolean = false
) {
    var rotX by remember { mutableFloatStateOf(0.45f) } // Initial pitch
    var rotY by remember { mutableFloatStateOf(0.75f) } // Initial yaw

    LaunchedEffect(autoRotate) {
        if (autoRotate) {
            var lastTime = System.nanoTime()
            while (isActive) {
                withFrameNanos { now ->
                    val dt = ((now - lastTime) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                    rotY += 0.8f * dt
                    lastTime = now
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF08090C)) // Pure Obsidian Minimalist Canvas
            .border(1.dp, SurfaceBorder, RoundedCornerShape(18.dp))
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    rotY += dragAmount.x * 0.012f
                    rotX = (rotX + dragAmount.y * 0.012f).coerceIn(-1.4f, 1.4f)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val baseScale = Math.min(size.width, size.height) * 0.42f

            fun project(p: Point3D): ProjectedPoint {
                // Rotate around Y axis (Yaw)
                val x1 = p.x * cos(rotY) + p.z * sin(rotY)
                val z1 = -p.x * sin(rotY) + p.z * cos(rotY)

                // Rotate around X axis (Pitch)
                val y2 = p.y * cos(rotX) - z1 * sin(rotX)
                val z2 = p.y * sin(rotX) + z1 * cos(rotX)

                // Perspective projection factor
                val fov = 3.5f
                val distance = fov + z2
                val persFactor = if (distance > 0.1f) fov / distance else 1f

                return ProjectedPoint(
                    offset = Offset(cx + x1 * baseScale * persFactor, cy - y2 * baseScale * persFactor),
                    z = z2
                )
            }

            // Holographic Perspective Floor Grid
            val floorY = -0.95f
            val gridLines = 4
            val gridSize = 0.9f
            for (i in -gridLines..gridLines) {
                val step = (i.toFloat() / gridLines) * gridSize
                val p1 = project(Point3D(step, floorY, -gridSize))
                val p2 = project(Point3D(step, floorY, gridSize))
                drawLine(
                    color = Color(0xFF38BDF8).copy(alpha = 0.14f),
                    start = p1.offset,
                    end = p2.offset,
                    strokeWidth = 1f
                )
                val p3 = project(Point3D(-gridSize, floorY, step))
                val p4 = project(Point3D(gridSize, floorY, step))
                drawLine(
                    color = Color(0xFF38BDF8).copy(alpha = 0.14f),
                    start = p3.offset,
                    end = p4.offset,
                    strokeWidth = 1f
                )
            }

            when (shapeType) {
                Shape3DType.CUBE -> {
                    val w = 0.55f * (dimA / 10f).coerceIn(0.5f, 1.5f)
                    val h = 0.55f * (dimB / 10f).coerceIn(0.5f, 1.5f)
                    val d = 0.55f * (dimC / 10f).coerceIn(0.5f, 1.5f)

                    val vertices = listOf(
                        Point3D(-w, -h, -d), Point3D(w, -h, -d),
                        Point3D(w, h, -d), Point3D(-w, h, -d),
                        Point3D(-w, -h, d), Point3D(w, -h, d),
                        Point3D(w, h, d), Point3D(-w, h, d)
                    )

                    val edges = listOf(
                        0 to 1, 1 to 2, 2 to 3, 3 to 0, // Back
                        4 to 5, 5 to 6, 6 to 7, 7 to 4, // Front
                        0 to 4, 1 to 5, 2 to 6, 3 to 7  // Connecting
                    )

                    val projected = vertices.map { project(it) }

                    edges.forEach { (start, end) ->
                        val avgZ = (projected[start].z + projected[end].z) / 2f
                        val isBack = avgZ < 0f
                        drawLine(
                            color = if (isBack) Color(0xFF475569) else Color.White,
                            start = projected[start].offset,
                            end = projected[end].offset,
                            strokeWidth = if (isBack) 1.5f else 3f,
                            pathEffect = if (isBack) PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f) else null
                        )
                    }

                    // Render Vertex points
                    projected.forEach { pt ->
                        drawCircle(color = Color(0xFF38BDF8), radius = 3.5.dp.toPx(), center = pt.offset)
                    }
                }

                Shape3DType.SPHERE -> {
                    val r = 0.75f * (dimA / 10f).coerceIn(0.5f, 1.5f)
                    val latCircles = 8
                    val lonCircles = 8
                    val pointsCount = 36

                    // Latitude rings
                    for (i in 1 until latCircles) {
                        val lat = (i.toFloat() / latCircles - 0.5f) * PI.toFloat()
                        val circleR = r * cos(lat)
                        val y = r * sin(lat)

                        val pathFront = Path()
                        val pathBack = Path()
                        var firstFront = true
                        var firstBack = true

                        for (j in 0..pointsCount) {
                            val lon = (j.toFloat() / pointsCount) * 2f * PI.toFloat()
                            val x = circleR * cos(lon)
                            val z = circleR * sin(lon)
                            val pt = project(Point3D(x, y, z))

                            if (pt.z >= 0) {
                                if (firstFront) { pathFront.moveTo(pt.offset.x, pt.offset.y); firstFront = false }
                                else pathFront.lineTo(pt.offset.x, pt.offset.y)
                            } else {
                                if (firstBack) { pathBack.moveTo(pt.offset.x, pt.offset.y); firstBack = false }
                                else pathBack.lineTo(pt.offset.x, pt.offset.y)
                            }
                        }

                        drawPath(pathBack, color = Color(0xFF334155), style = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)))
                        drawPath(pathFront, color = Color(0xFF38BDF8).copy(alpha = 0.8f), style = Stroke(width = 2f))
                    }

                    // Equator highlighted ring
                    val eqPath = Path()
                    for (j in 0..pointsCount) {
                        val lon = (j.toFloat() / pointsCount) * 2f * PI.toFloat()
                        val pt = project(Point3D(r * cos(lon), 0f, r * sin(lon)))
                        if (j == 0) eqPath.moveTo(pt.offset.x, pt.offset.y) else eqPath.lineTo(pt.offset.x, pt.offset.y)
                    }
                    drawPath(eqPath, color = Color.White, style = Stroke(width = 2.5f))

                    // Outer Glow Silhouette
                    drawCircle(color = Color(0xFF38BDF8).copy(alpha = 0.15f), radius = r * baseScale, center = Offset(cx, cy))
                }

                Shape3DType.CYLINDER -> {
                    val r = 0.55f * (dimA / 10f).coerceIn(0.5f, 1.4f)
                    val h = 0.70f * (dimB / 10f).coerceIn(0.5f, 1.6f)
                    val steps = 36

                    val topCircle = Path()
                    val botCircle = Path()

                    for (i in 0..steps) {
                        val angle = (i.toFloat() / steps) * 2f * PI.toFloat()
                        val x = r * cos(angle)
                        val z = r * sin(angle)

                        val topPt = project(Point3D(x, h / 2f, z))
                        val botPt = project(Point3D(x, -h / 2f, z))

                        if (i == 0) {
                            topCircle.moveTo(topPt.offset.x, topPt.offset.y)
                            botCircle.moveTo(botPt.offset.x, botPt.offset.y)
                        } else {
                            topCircle.lineTo(topPt.offset.x, topPt.offset.y)
                            botCircle.lineTo(botPt.offset.x, botPt.offset.y)
                        }
                    }

                    drawPath(botCircle, color = Color(0xFF475569), style = Stroke(width = 2f))
                    drawPath(topCircle, color = Color.White, style = Stroke(width = 2.5f))

                    // Dynamic 8 longitudinal generator lines (rotating with shape)
                    for (k in 0 until 8) {
                        val angle = (k.toFloat() / 8f) * 2f * PI.toFloat()
                        val x = r * cos(angle)
                        val z = r * sin(angle)
                        val topPt = project(Point3D(x, h / 2f, z))
                        val botPt = project(Point3D(x, -h / 2f, z))
                        val isFront = (topPt.z + botPt.z) >= 0

                        drawLine(
                            color = if (isFront) Color(0xFF38BDF8) else Color(0xFF1E293B),
                            start = topPt.offset,
                            end = botPt.offset,
                            strokeWidth = if (isFront) 2f else 1f,
                            pathEffect = if (!isFront) PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f) else null
                        )
                    }
                }

                Shape3DType.CONE -> {
                    val r = 0.60f * (dimA / 10f).coerceIn(0.5f, 1.4f)
                    val h = 0.85f * (dimB / 10f).coerceIn(0.5f, 1.6f)
                    val steps = 36
                    val apex = project(Point3D(0f, h / 2f, 0f))

                    val baseCircle = Path()
                    for (i in 0..steps) {
                        val angle = (i.toFloat() / steps) * 2f * PI.toFloat()
                        val x = r * cos(angle)
                        val z = r * sin(angle)
                        val pt = project(Point3D(x, -h / 2f, z))
                        if (i == 0) baseCircle.moveTo(pt.offset.x, pt.offset.y) else baseCircle.lineTo(pt.offset.x, pt.offset.y)
                    }

                    drawPath(baseCircle, color = Color.White, style = Stroke(width = 2.5f))

                    // Draw 8 generator lines from Apex to Base
                    for (k in 0 until 8) {
                        val angle = (k.toFloat() / 8f) * 2f * PI.toFloat()
                        val x = r * cos(angle)
                        val z = r * sin(angle)
                        val pt = project(Point3D(x, -h / 2f, z))
                        val isFront = pt.z >= 0

                        drawLine(
                            color = if (isFront) Color(0xFF38BDF8) else Color(0xFF1E293B),
                            start = apex.offset,
                            end = pt.offset,
                            strokeWidth = if (isFront) 2f else 1f,
                            pathEffect = if (!isFront) PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f) else null
                        )
                    }

                    // Apex Point
                    drawCircle(color = Color.White, radius = 4.dp.toPx(), center = apex.offset)
                }

                Shape3DType.PYRAMID -> {
                    val w = 0.55f * (dimA / 10f).coerceIn(0.5f, 1.4f)
                    val h = 0.80f * (dimB / 10f).coerceIn(0.5f, 1.6f)

                    val base0 = project(Point3D(-w, -h / 2f, -w))
                    val base1 = project(Point3D(w, -h / 2f, -w))
                    val base2 = project(Point3D(w, -h / 2f, w))
                    val base3 = project(Point3D(-w, -h / 2f, w))
                    val apex = project(Point3D(0f, h / 2f, 0f))

                    val base = listOf(base0, base1, base2, base3)

                    // Draw Base Edges
                    for (i in 0..3) {
                        val p1 = base[i]
                        val p2 = base[(i + 1) % 4]
                        val isFront = (p1.z + p2.z) >= 0
                        drawLine(
                            color = if (isFront) Color.White else Color(0xFF475569),
                            start = p1.offset,
                            end = p2.offset,
                            strokeWidth = if (isFront) 2.5f else 1.5f,
                            pathEffect = if (!isFront) PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f) else null
                        )
                    }

                    // Draw Lateral Edges from Apex
                    base.forEach { pt ->
                        val isFront = pt.z >= 0
                        drawLine(
                            color = if (isFront) Color(0xFF38BDF8) else Color(0xFF1E293B),
                            start = apex.offset,
                            end = pt.offset,
                            strokeWidth = if (isFront) 2.5f else 1.2f,
                            pathEffect = if (!isFront) PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f) else null
                        )
                        drawCircle(color = Color(0xFF38BDF8), radius = 3.dp.toPx(), center = pt.offset)
                    }

                    // Apex Point
                    drawCircle(color = Color.White, radius = 4.dp.toPx(), center = apex.offset)
                }
            }
        }

        // Overlay Rotation Helper Badge
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceElevated)
                .border(1.dp, SurfaceBorder, RoundedCornerShape(10.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "🔄 Вращение 360° (тяните пальцем)",
                color = Color(0xFF38BDF8),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

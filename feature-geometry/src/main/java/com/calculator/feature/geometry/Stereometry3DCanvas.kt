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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calculator.core.ui.theme.AccentViolet
import com.calculator.core.ui.theme.AccentCyan
import com.calculator.core.ui.theme.AccentAmber
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

enum class Shape3DType(val title: String) {
    CUBE("Куб / Параллелепипед"),
    SPHERE("Сфера / Шар"),
    CYLINDER("Цилиндр"),
    CONE("Конус"),
    PYRAMID("Пирамида")
}

data class Point3D(val x: Float, val y: Float, val z: Float)

@Composable
fun Stereometry3DCanvas(
    shapeType: Shape3DType,
    modifier: Modifier = Modifier,
    paramA: Float = 100f,
    paramB: Float = 100f,
    paramC: Float = 100f
) {
    var rotX by remember { mutableStateOf(0.4f) } // Initial pitch
    var rotY by remember { mutableStateOf(0.6f) } // Initial yaw

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .shadow(2.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    rotY += dragAmount.x * 0.01f
                    rotX += dragAmount.y * 0.01f
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val scale = 0.8f

            fun project(p: Point3D): Offset {
                // Rotate Y (yaw)
                val x1 = p.x * cos(rotY) + p.z * sin(rotY)
                val z1 = -p.x * sin(rotY) + p.z * cos(rotY)

                // Rotate X (pitch)
                val y2 = p.y * cos(rotX) - z1 * sin(rotX)
                val z2 = p.y * sin(rotX) + z1 * cos(rotX)

                // Orthographic projection
                return Offset(cx + x1 * scale, cy - y2 * scale)
            }

            when (shapeType) {
                Shape3DType.CUBE -> {
                    val w = (paramA.coerceIn(40f, 180f)) / 2f
                    val h = (paramB.coerceIn(40f, 180f)) / 2f
                    val d = (paramC.coerceIn(40f, 180f)) / 2f

                    val vertices = listOf(
                        Point3D(-w, -h, -d), Point3D(w, -h, -d),
                        Point3D(w, h, -d), Point3D(-w, h, -d),
                        Point3D(-w, -h, d), Point3D(w, -h, d),
                        Point3D(w, h, d), Point3D(-w, h, d)
                    )

                    val edges = listOf(
                        0 to 1, 1 to 2, 2 to 3, 3 to 0,
                        4 to 5, 5 to 6, 6 to 7, 7 to 4,
                        0 to 4, 1 to 5, 2 to 6, 3 to 7
                    )

                    val projected = vertices.map { project(it) }
                    edges.forEach { (start, end) ->
                        drawLine(
                            color = AccentViolet,
                            start = projected[start],
                            end = projected[end],
                            strokeWidth = 3f
                        )
                    }
                }

                Shape3DType.SPHERE -> {
                    val r = paramA.coerceIn(40f, 110f)
                    val circles = 12
                    val pointsPerCircle = 24

                    // Latitude & Longitude circles
                    for (i in 0 until circles) {
                        val lat = (i.toFloat() / circles - 0.5f) * PI.toFloat()
                        val circleR = r * cos(lat)
                        val y = r * sin(lat)

                        val path = Path()
                        for (j in 0..pointsPerCircle) {
                            val lon = (j.toFloat() / pointsPerCircle) * 2f * PI.toFloat()
                            val x = circleR * cos(lon)
                            val z = circleR * sin(lon)
                            val pt = project(Point3D(x, y, z))
                            if (j == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
                        }
                        drawPath(path, color = AccentCyan.copy(alpha = 0.5f), style = Stroke(width = 1.5f))
                    }
                }

                Shape3DType.CYLINDER -> {
                    val r = paramA.coerceIn(30f, 90f)
                    val h = paramB.coerceIn(40f, 140f) / 2f
                    val steps = 30

                    val topCircle = Path()
                    val botCircle = Path()

                    for (i in 0..steps) {
                        val angle = (i.toFloat() / steps) * 2f * PI.toFloat()
                        val x = r * cos(angle)
                        val z = r * sin(angle)

                        val topPt = project(Point3D(x, h, z))
                        val botPt = project(Point3D(x, -h, z))

                        if (i == 0) {
                            topCircle.moveTo(topPt.x, topPt.y)
                            botCircle.moveTo(botPt.x, botPt.y)
                        } else {
                            topCircle.lineTo(topPt.x, topPt.y)
                            botCircle.lineTo(botPt.x, botPt.y)
                        }
                    }

                    drawPath(topCircle, color = AccentViolet, style = Stroke(width = 2.5f))
                    drawPath(botCircle, color = AccentViolet, style = Stroke(width = 2.5f))

                    // Side lines
                    val leftTop = project(Point3D(-r, h, 0f))
                    val leftBot = project(Point3D(-r, -h, 0f))
                    val rightTop = project(Point3D(r, h, 0f))
                    val rightBot = project(Point3D(r, -h, 0f))

                    drawLine(AccentAmber, leftTop, leftBot, strokeWidth = 2f)
                    drawLine(AccentAmber, rightTop, rightBot, strokeWidth = 2f)
                }

                Shape3DType.CONE -> {
                    val r = paramA.coerceIn(30f, 90f)
                    val h = paramB.coerceIn(40f, 140f)
                    val steps = 30
                    val apex = project(Point3D(0f, h / 2f, 0f))

                    val baseCircle = Path()
                    for (i in 0..steps) {
                        val angle = (i.toFloat() / steps) * 2f * PI.toFloat()
                        val x = r * cos(angle)
                        val z = r * sin(angle)
                        val pt = project(Point3D(x, -h / 2f, z))
                        if (i == 0) baseCircle.moveTo(pt.x, pt.y) else baseCircle.lineTo(pt.x, pt.y)
                    }

                    drawPath(baseCircle, color = AccentViolet, style = Stroke(width = 2.5f))

                    val leftBot = project(Point3D(-r, -h / 2f, 0f))
                    val rightBot = project(Point3D(r, -h / 2f, 0f))
                    drawLine(AccentAmber, apex, leftBot, strokeWidth = 2f)
                    drawLine(AccentAmber, apex, rightBot, strokeWidth = 2f)
                }

                Shape3DType.PYRAMID -> {
                    val w = paramA.coerceIn(40f, 140f) / 2f
                    val h = paramB.coerceIn(40f, 140f) / 2f

                    val base0 = project(Point3D(-w, -h, -w))
                    val base1 = project(Point3D(w, -h, -w))
                    val base2 = project(Point3D(w, -h, w))
                    val base3 = project(Point3D(-w, -h, w))
                    val apex = project(Point3D(0f, h, 0f))

                    val base = listOf(base0, base1, base2, base3)
                    for (i in 0..3) {
                        drawLine(AccentViolet, base[i], base[(i + 1) % 4], strokeWidth = 2.5f)
                        drawLine(AccentAmber, apex, base[i], strokeWidth = 2f)
                    }
                }
            }
        }

        // Overlay Rotation Helper Badge
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFEEF2FF))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "🔄 Вращение 360°",
                color = AccentViolet,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

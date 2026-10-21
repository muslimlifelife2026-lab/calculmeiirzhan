package com.calculator.domain.geometry

import kotlinx.serialization.Serializable
import kotlin.math.hypot

@Serializable
data class PlanePoint(val id: Int, val x: Double, val y: Double) {
    val label: String get() = ('A'.code + id - 1).toChar().toString()
}

@Serializable
data class PlaneSegment(val from: Int, val to: Int)

@Serializable
data class PlaneCircle(val center: Int, val through: Int)

/** Small, finite Euclidean construction. Coordinates use the same scale on both axes. */
@Serializable
data class PlaneConstruction(
    val points: List<PlanePoint> = emptyList(),
    val segments: List<PlaneSegment> = emptyList(),
    val circles: List<PlaneCircle> = emptyList()
) {
    fun validate(): PlaneConstruction {
        require(points.size <= MAX_POINTS && points.map { it.id }.distinct().size == points.size)
        require(points.all { it.id in 1..MAX_POINTS && validCoordinate(it.x) && validCoordinate(it.y) })
        require(points.map { (if (it.x == 0.0) 0.0 else it.x) to (if (it.y == 0.0) 0.0 else it.y) }.distinct().size == points.size)
        require(segments.size + circles.size <= MAX_SHAPES)
        require(segments.all { it.from != it.to && point(it.from) != null && point(it.to) != null })
        require(circles.all { it.center != it.through && point(it.center) != null && point(it.through) != null })
        require(segments.map { minOf(it.from, it.to) to maxOf(it.from, it.to) }.distinct().size == segments.size)
        require(circles.map { it.center to distance(it.center, it.through) }.distinct().size == circles.size)
        return this
    }

    fun point(id: Int): PlanePoint? = points.firstOrNull { it.id == id }

    fun addPoint(x: Double, y: Double): PlaneConstruction {
        require(validCoordinate(x) && validCoordinate(y)) { "Координаты должны быть от −10 до 10" }
        require(points.size < MAX_POINTS) { "На одном чертеже можно разместить 24 точки" }
        require(points.none { it.x == x && it.y == y }) { "Точка с такими координатами уже существует" }
        val id = (1..MAX_POINTS).first { candidate -> points.none { it.id == candidate } }
        return copy(points = points + PlanePoint(id, if (x == 0.0) 0.0 else x, if (y == 0.0) 0.0 else y))
    }

    fun addSegment(from: Int, to: Int): PlaneConstruction {
        requirePair(from, to)
        require(segments.none { (it.from == from && it.to == to) || (it.from == to && it.to == from) }) {
            "Этот отрезок уже построен"
        }
        return copy(segments = segments + PlaneSegment(from, to))
    }

    fun addCircle(center: Int, through: Int): PlaneConstruction {
        requirePair(center, through)
        require(circles.none { it.center == center && distance(it.center, it.through) == distance(center, through) }) {
            "Окружность с таким центром и радиусом уже построена"
        }
        return copy(circles = circles + PlaneCircle(center, through))
    }

    fun distance(first: Int, second: Int): Double {
        val a = requireNotNull(point(first)) { "Точка не найдена" }
        val b = requireNotNull(point(second)) { "Точка не найдена" }
        return hypot(a.x - b.x, a.y - b.y)
    }

    fun removePoint(id: Int): PlaneConstruction {
        require(point(id) != null) { "Точка не найдена" }
        return copy(
            points = points.filterNot { it.id == id },
            segments = segments.filterNot { it.from == id || it.to == id },
            circles = circles.filterNot { it.center == id || it.through == id }
        )
    }

    private fun requirePair(first: Int, second: Int) {
        require(first != second) { "Выберите две разные точки" }
        require(point(first) != null && point(second) != null) { "Сначала добавьте обе точки" }
        require(segments.size + circles.size < MAX_SHAPES) { "На одном чертеже можно построить 32 фигуры" }
    }

    companion object {
        const val MAX_POINTS = 24
        const val MAX_SHAPES = 32
        private fun validCoordinate(value: Double) = value.isFinite() && value in -10.0..10.0
    }
}

/** A square world grid centered in any rectangular viewport, with 1 unit = equal pixels in X/Y. */
data class PlaneViewport(val width: Double, val height: Double) {
    init { require(width.isFinite() && height.isFinite() && width > 0 && height > 0) }
    val pixelsPerUnit: Double = minOf(width, height) / 22.0
    fun screenX(x: Double): Double = width / 2 + x * pixelsPerUnit
    fun screenY(y: Double): Double = height / 2 - y * pixelsPerUnit
    fun worldX(x: Double): Double = (x - width / 2) / pixelsPerUnit
    fun worldY(y: Double): Double = (height / 2 - y) / pixelsPerUnit
}

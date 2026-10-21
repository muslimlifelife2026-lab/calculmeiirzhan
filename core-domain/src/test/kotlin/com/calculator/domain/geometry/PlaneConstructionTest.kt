package com.calculator.domain.geometry

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.*

class PlaneConstructionTest {
    @Test fun `removing a point removes dependent shapes and safely reuses its label`() {
        val drawing = PlaneConstruction().addPoint(0.0, 0.0).addPoint(3.0, 4.0).addPoint(2.0, 0.0)
            .addSegment(1, 2).addSegment(2, 3).addCircle(1, 3)
        val changed = drawing.removePoint(1).validate()
        assertEquals(listOf(PlaneSegment(2, 3)), changed.segments)
        assertTrue(changed.circles.isEmpty())
        assertEquals("A", changed.addPoint(-2.0, 0.0).points.last().label)
        assertFailsWith<IllegalArgumentException> { changed.removePoint(1) }
    }

    @Test fun `segments and circles use Euclidean distances and persist coordinates`() {
        val drawing = PlaneConstruction().addPoint(-1.5, 2.0).addPoint(1.5, 6.0)
            .addSegment(1, 2).addCircle(1, 2)
        assertEquals(5.0, drawing.distance(1, 2), 1e-12)
        assertEquals(drawing, Json.decodeFromString<PlaneConstruction>(Json.encodeToString(drawing)).validate())
    }

    @Test fun `rectangular viewport preserves scale orientation and inverse coordinates`() {
        val view = PlaneViewport(600.0, 300.0)
        assertEquals(view.screenX(3.0) - view.screenX(0.0), view.screenY(0.0) - view.screenY(3.0), 1e-12)
        assertEquals(-4.25, view.worldX(view.screenX(-4.25)), 1e-12)
        assertEquals(7.5, view.worldY(view.screenY(7.5)), 1e-12)
        assertTrue(view.screenY(2.0) < view.screenY(0.0))
    }

    @Test fun `invalid degenerate and duplicate shapes are rejected`() {
        assertFailsWith<IllegalArgumentException> { PlaneConstruction().addPoint(Double.NaN, 0.0) }
        assertFailsWith<IllegalArgumentException> { PlaneConstruction().addPoint(11.0, 0.0) }
        val drawing = PlaneConstruction().addPoint(0.0, 0.0).addPoint(3.0, 4.0).addPoint(-3.0, -4.0)
        assertFailsWith<IllegalArgumentException> { drawing.addPoint(-0.0, 0.0) }
        assertFailsWith<IllegalArgumentException> { drawing.addCircle(1, 1) }
        assertFailsWith<IllegalArgumentException> { drawing.addSegment(1, 9) }
        assertFailsWith<IllegalArgumentException> { drawing.addSegment(1, 2).addSegment(2, 1) }
        assertFailsWith<IllegalArgumentException> { drawing.addCircle(1, 2).addCircle(1, 3) }
    }

    @Test fun `saved drawings reject dangling references and excessive data`() {
        assertFailsWith<IllegalArgumentException> { PlaneConstruction(segments = listOf(PlaneSegment(1, 2))).validate() }
        assertFailsWith<IllegalArgumentException> {
            PlaneConstruction(points = listOf(PlanePoint(1, 0.0, 0.0), PlanePoint(2, -0.0, 0.0))).validate()
        }
        assertFailsWith<IllegalArgumentException> {
            PlaneConstruction().addPoint(0.0, 0.0).addPoint(1.0, 0.0)
                .copy(segments = listOf(PlaneSegment(1, 2), PlaneSegment(2, 1))).validate()
        }
        var drawing = PlaneConstruction()
        repeat(24) { drawing = drawing.addPoint(it / 2.0 - 6.0, 0.0) }
        assertEquals("X", drawing.points.last().label)
        assertFailsWith<IllegalArgumentException> { drawing.addPoint(0.0, 1.0) }
    }
}

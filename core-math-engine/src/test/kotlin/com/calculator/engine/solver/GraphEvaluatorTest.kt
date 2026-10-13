package com.calculator.engine.solver

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GraphEvaluatorTest {
    @Test fun discontinuityIsNotIntersectionOrRoot() {
        val inverse = GraphEvaluator("1/x")
        assertTrue(inverse.findIntersections(GraphEvaluator("0"), -1.0, 2.0).isEmpty())
        assertTrue(inverse.findRoots(-1.0, 2.0).isEmpty())
    }

    @Test fun tangentRootAndPolarAliasAreRecognized() {
        val roots = GraphEvaluator("(x-0.37)^2").findRoots(-1.0, 2.0)
        assertTrue(roots.any { kotlin.math.abs(it - 0.37) < 1e-3 })
        val touching = GraphEvaluator("(x-0.37)^2")
            .findIntersections(GraphEvaluator("0"), -1.0, 2.0)
        assertTrue(touching.any { kotlin.math.abs(it.first - 0.37) < 1e-3 })
        assertEquals(1.0, GraphEvaluator("th").evaluate(1.0, varSymbol = "theta"), 0.0)
        assertEquals(220.0, GraphEvaluator("200+10%").evaluate(0.0), 1e-12)
    }
}

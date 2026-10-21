package com.calculator.engine.solver

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GraphEvaluatorTest {
    @Test fun definiteIntegralPreservesLimitOrderAndSmallNonzeroScale() {
        assertEquals(2.0, GraphEvaluator("x").evaluateIntegral(0.0, 2.0), 1e-12)
        assertEquals(-2.0, GraphEvaluator("x").evaluateIntegral(2.0, 0.0), 1e-12)
        assertEquals(2e-12, GraphEvaluator("1e-12").evaluateIntegral(0.0, 2.0), 1e-25)
        assertTrue(GraphEvaluator("1/x").evaluateIntegral(-1.0, 1.0).isNaN())
    }

    @Test fun factorialHasTheSameIntegerDomainAsTheCalculator() {
        val evaluator = GraphEvaluator("fact(x)")
        assertEquals(120.0, evaluator.evaluate(5.0), 0.0)
        assertEquals(1.0, evaluator.evaluate(0.0), 0.0)
        assertEquals(1.0, evaluator.evaluate(1.0), 0.0)
        assertEquals(7.257415615307999e306, evaluator.evaluate(170.0), 1e292)
        assertTrue(evaluator.evaluate(-1.0).isNaN())
        assertTrue(evaluator.evaluate(1.5).isNaN())
        assertTrue(evaluator.evaluate(171.0).isNaN())
        assertTrue(evaluator.evaluate(Double.NaN).isNaN())
        assertTrue(evaluator.evaluate(Double.POSITIVE_INFINITY).isNaN())
    }
    @Test fun smallNonzeroValuesAreNotRoots() {
        assertTrue(GraphEvaluator("1e-12").findRoots(-1.0, 1.0).isEmpty())
        assertTrue(GraphEvaluator("1e-12+(x-0.37)^2").findRoots(-1.0, 1.0).isEmpty())
        val roots = GraphEvaluator("1e-20*(x-0.37)^2").findRoots(-1.0, 1.0)
        assertEquals(1, roots.size)
        assertEquals(0.37, roots.single(), 1e-7)
    }

    @Test fun oneSampleEvaluatesTheStartAndLongIntersectionsDoNotReparseCombinedText() {
        assertEquals(3.0, GraphEvaluator("x+1").evaluateRange(2.0, 3.0, 1).single(), 0.0)
        val longExpression = "x" + "+0".repeat(100)
        val roots = GraphEvaluator(longExpression).findIntersections(GraphEvaluator("1" + "+0".repeat(100)), -1.0, 2.0)
        assertEquals(1.0, roots.single().first, 1e-8)
    }

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

package com.calculator.engine.solver

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChemistrySolverTest {
    @Test fun rejectsInvalidAtomCountsAndEmptyGroups() {
        listOf("H0", "()", "H2147483648", "Ca(OH)0").forEach {
            assertFalse(it, ChemistrySolver.solveMolarMass(it).success)
        }
    }

    @Test fun balancesCommonReactions() {
        assertEquals("2H2 + O2 = 2H2O", ChemistrySolver.balanceReaction("H2 + O2 -> H2O"))
        assertEquals("4Fe + 3O2 = 2Fe2O3", ChemistrySolver.balanceReaction("Fe + O2 -> Fe2O3"))
    }

    @Test fun rejectsMalformedOrExcessivelyLargeReaction() {
        assertTrue(ChemistrySolver.balanceReaction("H2 + -> H2O").startsWith("Ошибка"))
        assertTrue(ChemistrySolver.balanceReaction("H2 + O2 -> H2O + ").startsWith("Ошибка"))
    }
}

package com.calculator.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PeriodicTableTest {
    @Test fun includesAllElementsAndRussianSearchName() {
        assertEquals(118, PeriodicTable.allElements.size)
        assertEquals("Железо", PeriodicTable.getElement("Fe")?.nameRu)
        assertNotNull(PeriodicTable.getElement("U"))
    }

    @Test fun electronShellsAgreeWithConfigurations() {
        assertEquals(listOf(2, 8, 14, 2), PeriodicTable.shellDistribution(26))
        assertEquals(listOf(2, 8, 18, 32, 21, 9, 2), PeriodicTable.shellDistribution(92))
        assertTrue(PeriodicTable.shellDistribution(118).sum() == 118)
        PeriodicTable.allElements.forEach { element ->
            assertEquals(element.symbol, element.atomicNumber, PeriodicTable.shellDistribution(element.atomicNumber).sum())
        }
    }
}

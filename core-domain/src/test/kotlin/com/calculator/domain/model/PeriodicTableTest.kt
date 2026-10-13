package com.calculator.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
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

    @Test fun standardMassesMatchIndependentCiaaw2024ReferenceValues() {
        val references = mapOf("Li" to 6.94, "Ca" to 40.078, "Fe" to 55.845,
            "Zr" to 91.222, "Gd" to 157.25, "Lu" to 174.97, "Pb" to 207.2)
        references.forEach { (symbol, expected) ->
            val element = PeriodicTable.getElement(symbol)!!
            assertEquals(symbol, expected, element.atomicMass, 1e-12)
            assertEquals(AtomicMassType.STANDARD_ATOMIC_WEIGHT, element.atomicMassType)
            assertTrue(element.atomicMassUncertainty!! > 0)
        }
        assertEquals(84, PeriodicTable.allElements.count { it.atomicMassType == AtomicMassType.STANDARD_ATOMIC_WEIGHT })
        assertEquals(0.06, PeriodicTable.getElement("Li")!!.atomicMassUncertainty!!, 1e-12)
    }

    @Test fun missingElectronegativityAndUnspecifiedStandardMassStayUnknown() {
        assertNull(PeriodicTable.getElement("He")!!.electronegativity)
        assertEquals(3.98, PeriodicTable.getElement("F")!!.electronegativity!!, 1e-12)
        val technetium = PeriodicTable.getElement("Tc")!!
        assertEquals(AtomicMassType.REPRESENTATIVE_ISOTOPE_MASS, technetium.atomicMassType)
        assertNull(technetium.atomicMassUncertainty)
        assertTrue(technetium.atomicMassSource.contains("PubChem"))
    }
}

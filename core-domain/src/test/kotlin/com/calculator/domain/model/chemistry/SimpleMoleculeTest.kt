package com.calculator.domain.model.chemistry

import java.math.BigDecimal
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SimpleMoleculeTest {
    @Test fun waterAndEthanolIncludeImplicitHydrogen() {
        val water = SimpleMolecule().addAtom(StructureElement.O)
        assertEquals("H2O", water.formula())
        assertEquals(BigDecimal("18.015"), water.molarMass())
        val ethanol = SimpleMolecule().addAtom(StructureElement.C)
            .addAtom(StructureElement.C, 1).addAtom(StructureElement.O, 2)
        assertEquals("C2H6O", ethanol.formula())
        assertEquals(BigDecimal("46.069"), ethanol.molarMass())
        assertEquals(3, ethanol.implicitHydrogens(1))
        assertEquals(2, ethanol.implicitHydrogens(2))
        assertEquals(1, ethanol.implicitHydrogens(3))
    }

    @Test fun multipleBondsChangeHydrogenCountWithoutChangingAtoms() {
        val molecule = SimpleMolecule().addAtom(StructureElement.C).addAtom(StructureElement.C, 1)
        assertEquals("C2H6", molecule.formula())
        assertEquals("C2H4", molecule.withBond(1, 2, 2).formula())
        assertEquals("C2H2", molecule.withBond(1, 2, 3).formula())
        assertEquals("N2", SimpleMolecule().addAtom(StructureElement.N).addAtom(StructureElement.N, 1, 3).formula())
    }

    @Test fun preventsOverValenceAndUnboundHydrogenIsAnUnfinishedDraft() {
        val water = SimpleMolecule().addAtom(StructureElement.O)
            .addAtom(StructureElement.H, 1).addAtom(StructureElement.H, 1)
        assertEquals("H2O", water.formula())
        assertFailsWith<IllegalArgumentException> { water.addAtom(StructureElement.H, 1) }
        assertFailsWith<IllegalArgumentException> { water.withBond(1, 2, 2) }
        val hydrogen = SimpleMolecule().addAtom(StructureElement.H)
        assertFalse(hydrogen.isComplete)
        assertFailsWith<IllegalArgumentException> { hydrogen.formula() }
        assertEquals("H2", hydrogen.addAtom(StructureElement.H, 1).formula())
    }

    @Test fun deletionRemovesIncidentBondsAndDisconnectedDraftCanBeRepaired() {
        val ethanol = SimpleMolecule().addAtom(StructureElement.C)
            .addAtom(StructureElement.C, 1).addAtom(StructureElement.O, 2)
        val ethane = ethanol.removeAtom(3)
        assertEquals("C2H6", ethane.formula())
        assertEquals(1, ethane.bonds.size)
        val fragments = ethanol.removeAtom(2)
        assertEquals(0, fragments.bonds.size)
        assertFalse(fragments.isComplete)
        assertFailsWith<IllegalArgumentException> { fragments.toMolfile() }
        assertEquals("CH4O", fragments.withBond(1, 3, 1).formula())
        assertEquals(0, SimpleMolecule().addAtom(StructureElement.O).removeAtom(1).atoms.size)
    }

    @Test fun molfileHasExplicitHydrogensValidIndicesAndUnspecifiedDoubleStereo() {
        val molecule = SimpleMolecule().addAtom(StructureElement.C).addAtom(StructureElement.C, 1, 2)
        val previousLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            val lines = molecule.toMolfile().lines()
            assertEquals(6, lines[3].substring(0, 3).trim().toInt())
            assertEquals(5, lines[3].substring(3, 6).trim().toInt())
            assertEquals("V2000", lines[3].takeLast(5))
            val atoms = lines.subList(4, 10)
            assertEquals(listOf("C", "C", "H", "H", "H", "H"), atoms.map { it.substring(31, 34).trim() })
            atoms.forEach { it.substring(0, 10).trim().toDouble() }
            val bonds = lines.subList(10, 15)
            assertEquals(2, bonds[0].substring(6, 9).trim().toInt())
            assertEquals(3, bonds[0].substring(9, 12).trim().toInt())
            bonds.forEach { line ->
                assertTrue(line.substring(0, 3).trim().toInt() in 1..6)
                assertTrue(line.substring(3, 6).trim().toInt() in 1..6)
            }
            assertEquals("M  END", lines[15])
        } finally {
            Locale.setDefault(previousLocale)
        }
    }

    @Test fun draftRoundTripPreservesSelectionsAndRejectsMalformedOrInvalidGraphs() {
        val state = StructureDraft(
            molecule = SimpleMolecule().addAtom(StructureElement.C).addAtom(StructureElement.O, 1, 2),
            selectedAtomId = 2, nextElement = StructureElement.N, bondOrder = 3
        )
        assertEquals(state, StructureDraft.decode(state.encode()))
        assertEquals(null, StructureDraft.decode("{broken"))
        assertEquals(null, StructureDraft.decode(state.encode().replace("\"order\":2", "\"order\":9")))
        assertFailsWith<IllegalArgumentException> {
            SimpleMolecule(listOf(StructureAtom(1, StructureElement.C, Double.NaN, 0.0)))
        }
        assertFailsWith<IllegalArgumentException> {
            state.molecule.withBond(1, 1, 1)
        }
        assertFailsWith<IllegalArgumentException> {
            SimpleMolecule(state.molecule.atoms, listOf(StructureBond(1, 2, 1), StructureBond(2, 1, 1)))
        }
    }
}

package com.calculator.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SolubilityDataTest {
    @Test fun missingCellNeverClaimsSoluble() {
        assertEquals(SolubilityStatus.UNKNOWN, SolubilityData.getStatus("Cu²⁺", "F⁻"))
        assertEquals(SolubilityStatus.INSOLUBLE, SolubilityData.getStatus("Ag⁺", "Cl⁻"))
    }
}

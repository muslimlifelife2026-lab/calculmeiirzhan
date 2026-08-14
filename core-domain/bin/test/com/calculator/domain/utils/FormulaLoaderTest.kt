package com.calculator.domain.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FormulaLoaderTest {

    @Test
    fun testLoadFormulas() {
        val list = FormulaLoader.formulas
        assertTrue("Formulas list should not be empty", list.isNotEmpty())
        
        val kinematics = list.find { it.id == "velocity_kinematics" }
        assertNotNull("Kinematics formula should be loaded", kinematics)
        assertEquals("velocity_kinematics", kinematics!!.id)
        assertTrue("Variables should be loaded", kinematics.variables.size >= 4)
        
        val circle = list.find { it.id == "circle_area" }
        assertNotNull("Circle area formula should be loaded", circle)
        assertEquals("GEOMETRY", circle!!.category)
    }
}

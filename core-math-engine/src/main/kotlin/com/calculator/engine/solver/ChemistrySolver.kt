package com.calculator.engine.solver

import com.calculator.domain.model.Element
import com.calculator.domain.model.PeriodicTable
import java.util.Stack

data class ChemistryResult(
    val success: Boolean,
    val formula: String = "",
    val molarMass: Double = 0.0,
    val elementCounts: Map<Element, Int> = emptyMap(),
    val errorMessage: String? = null
)

object ChemistrySolver {

    /**
     * Parses a chemical formula like "H2SO4" or "Ca(OH)2"
     * and calculates its total molar mass and element composition.
     */
    fun solveMolarMass(formula: String): ChemistryResult {
        if (formula.isBlank()) return ChemistryResult(false, errorMessage = "Formula is empty")

        val stack = Stack<MutableMap<Element, Int>>()
        stack.push(mutableMapOf())

        var i = 0
        val n = formula.length

        while (i < n) {
            val c = formula[i]

            when {
                c == '(' -> {
                    stack.push(mutableMapOf())
                    i++
                }
                c == ')' -> {
                    if (stack.size < 2) {
                        return ChemistryResult(false, errorMessage = "Mismatched parentheses")
                    }
                    val topGroup = stack.pop()
                    i++

                    // Find multiplier after ')'
                    var multiplier = 0
                    while (i < n && formula[i].isDigit()) {
                        multiplier = multiplier * 10 + (formula[i] - '0')
                        i++
                    }
                    if (multiplier == 0) multiplier = 1

                    // Apply multiplier and merge with the group below
                    val targetGroup = stack.peek()
                    for ((element, count) in topGroup) {
                        targetGroup[element] = targetGroup.getOrDefault(element, 0) + count * multiplier
                    }
                }
                c.isUpperCase() -> {
                    // Parse Element Symbol
                    val start = i
                    i++
                    while (i < n && formula[i].isLowerCase()) {
                        i++
                    }
                    val symbol = formula.substring(start, i)
                    val element = PeriodicTable.getElement(symbol)
                        ?: return ChemistryResult(false, errorMessage = "Unknown element: $symbol")

                    // Parse multiplier
                    var count = 0
                    while (i < n && formula[i].isDigit()) {
                        count = count * 10 + (formula[i] - '0')
                        i++
                    }
                    if (count == 0) count = 1

                    // Add to current group
                    val currentGroup = stack.peek()
                    currentGroup[element] = currentGroup.getOrDefault(element, 0) + count
                }
                c.isWhitespace() -> {
                    i++
                }
                else -> {
                    return ChemistryResult(false, errorMessage = "Invalid character in formula: $c")
                }
            }
        }

        if (stack.size != 1) {
            return ChemistryResult(false, errorMessage = "Mismatched parentheses")
        }

        val finalComposition = stack.pop()
        var totalMass = 0.0

        for ((element, count) in finalComposition) {
            totalMass += element.atomicMass * count
        }

        return ChemistryResult(
            success = true,
            formula = formula,
            molarMass = totalMass,
            elementCounts = finalComposition
        )
    }

    /**
     * Balances a chemical equation like "H2 + O2 = H2O" or "Fe + O2 = Fe2O3"
     * using brute-force recursion for coefficients up to 10.
     */
    fun balanceReaction(equation: String): String {
        val cleanEq = equation
            .replace(" ", "")
            .replace("->", "=")
            .replace("=>", "=")
            .replace("→", "=")
        val parts = cleanEq.split("=")
        if (parts.size != 2) return "Ошибка: используйте '=' или '->' для разделения реагентов и продуктов"
        
        val leftSp = parts[0].split("+").filter { it.isNotEmpty() }
        val rightSp = parts[1].split("+").filter { it.isNotEmpty() }
        
        if (leftSp.isEmpty() || rightSp.isEmpty()) return "Ошибка: пустые части уравнения"

        val leftComps = leftSp.map { solveMolarMass(it) }
        val rightComps = rightSp.map { solveMolarMass(it) }

        val firstFailed = leftComps.firstOrNull { !it.success } ?: rightComps.firstOrNull { !it.success }
        if (firstFailed != null) {
            return "Ошибка в формуле: ${firstFailed.errorMessage}"
        }

        val allElements = (leftComps + rightComps).flatMap { it.elementCounts.keys }.distinct()
        val totalMolecules = leftSp.size + rightSp.size
        val coefs = IntArray(totalMolecules) { 1 }

        fun checkBalance(): Boolean {
            val leftSum = mutableMapOf<Element, Int>()
            val rightSum = mutableMapOf<Element, Int>()
            
            for (i in leftSp.indices) {
                val c = coefs[i]
                for ((el, count) in leftComps[i].elementCounts) {
                    leftSum[el] = leftSum.getOrDefault(el, 0) + count * c
                }
            }
            for (i in rightSp.indices) {
                val c = coefs[leftSp.size + i]
                for ((el, count) in rightComps[i].elementCounts) {
                    rightSum[el] = rightSum.getOrDefault(el, 0) + count * c
                }
            }
            
            for (el in allElements) {
                if (leftSum[el] != rightSum[el]) return false
            }
            return true
        }

        var found = false
        fun solve(index: Int): Boolean {
            if (index == totalMolecules) {
                return checkBalance()
            }
            for (c in 1..10) {
                coefs[index] = c
                if (solve(index + 1)) {
                    found = true
                    return true
                }
            }
            return false
        }

        solve(0)

        if (found) {
            val leftStr = leftSp.mapIndexed { idx, it -> 
                val c = coefs[idx]
                if (c > 1) "$c$it" else it
            }.joinToString(" + ")
            
            val rightStr = rightSp.mapIndexed { idx, it -> 
                val c = coefs[leftSp.size + idx]
                if (c > 1) "$c$it" else it
            }.joinToString(" + ")
            
            return "$leftStr = $rightStr"
        }

        return "Не удалось подобрать коэффициенты (требуются коэффициенты > 10)"
    }
}

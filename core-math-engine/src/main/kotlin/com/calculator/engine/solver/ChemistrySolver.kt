package com.calculator.engine.solver

import com.calculator.domain.model.Element
import com.calculator.domain.model.PeriodicTable
import java.util.Stack
import java.math.BigInteger

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
        if (formula.length > 100) return ChemistryResult(false, errorMessage = "Formula is too long")

        fun parseCount(start: Int): Pair<Int, Int>? {
            var end = start
            while (end < formula.length && formula[end].isDigit()) end++
            if (end == start) return 1 to start
            val count = formula.substring(start, end).toIntOrNull() ?: return null
            return if (count in 1..1_000_000) count to end else null
        }

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
                    if (topGroup.isEmpty()) return ChemistryResult(false, errorMessage = "Empty group")
                    i++

                    // Find multiplier after ')'
                    val parsed = parseCount(i) ?: return ChemistryResult(false, errorMessage = "Invalid atom count")
                    val (multiplier, nextIndex) = parsed
                    i = nextIndex

                    // Apply multiplier and merge with the group below
                    val targetGroup = stack.peek()
                    for ((element, count) in topGroup) {
                        val total = targetGroup.getOrDefault(element, 0).toLong() + count.toLong() * multiplier
                        if (total > 1_000_000) return ChemistryResult(false, errorMessage = "Atom count is too large")
                        targetGroup[element] = total.toInt()
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
                    val parsed = parseCount(i) ?: return ChemistryResult(false, errorMessage = "Invalid atom count")
                    val (count, nextIndex) = parsed
                    i = nextIndex

                    // Add to current group
                    val currentGroup = stack.peek()
                    val total = currentGroup.getOrDefault(element, 0).toLong() + count
                    if (total > 1_000_000) return ChemistryResult(false, errorMessage = "Atom count is too large")
                    currentGroup[element] = total.toInt()
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
        if (finalComposition.isEmpty()) return ChemistryResult(false, errorMessage = "Formula has no atoms")
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

    /** Balances an equation by finding a positive integer vector in the matrix nullspace. */
    fun balanceReaction(equation: String): String {
        if (equation.length > 200) return "Ошибка: уравнение слишком длинное"
        val cleanEq = equation
            .replace(" ", "")
            .replace("->", "=")
            .replace("=>", "=")
            .replace("→", "=")
        val parts = cleanEq.split("=")
        if (parts.size != 2) return "Ошибка: используйте '=' или '->' для разделения реагентов и продуктов"
        
        val leftSp = parts[0].split("+")
        val rightSp = parts[1].split("+")
        
        if (leftSp.any { it.isEmpty() } || rightSp.any { it.isEmpty() }) return "Ошибка: пустые части уравнения"
        if (leftSp.size + rightSp.size > 12) return "Ошибка: слишком много веществ (максимум 12)"

        val leftComps = leftSp.map { solveMolarMass(it) }
        val rightComps = rightSp.map { solveMolarMass(it) }

        val firstFailed = leftComps.firstOrNull { !it.success } ?: rightComps.firstOrNull { !it.success }
        if (firstFailed != null) {
            return "Ошибка в формуле: ${firstFailed.errorMessage}"
        }

        val allElements = (leftComps + rightComps).flatMap { it.elementCounts.keys }.distinct()
        val totalMolecules = leftSp.size + rightSp.size
        val compounds = leftComps + rightComps
        val matrix = Array(allElements.size) { row ->
            Array(totalMolecules) { col ->
                val sign = if (col < leftSp.size) 1 else -1
                Fraction.of((compounds[col].elementCounts[allElements[row]] ?: 0) * sign)
            }
        }
        val pivots = mutableListOf<Int>()
        var pivotRow = 0
        for (col in 0 until totalMolecules) {
            val foundRow = (pivotRow until matrix.size).firstOrNull { matrix[it][col].numerator != BigInteger.ZERO }
                ?: continue
            val temp = matrix[pivotRow]
            matrix[pivotRow] = matrix[foundRow]
            matrix[foundRow] = temp
            val pivot = matrix[pivotRow][col]
            for (j in col until totalMolecules) matrix[pivotRow][j] = matrix[pivotRow][j] / pivot
            for (row in matrix.indices) {
                if (row == pivotRow) continue
                val factor = matrix[row][col]
                for (j in col until totalMolecules) matrix[row][j] = matrix[row][j] - factor * matrix[pivotRow][j]
            }
            pivots += col
            pivotRow++
            if (pivotRow == matrix.size) break
        }
        val free = (0 until totalMolecules).filter { it !in pivots }
        if (free.isEmpty()) return "Не удалось сбалансировать: нет решения"
        val vector = Array(totalMolecules) { Fraction.of(1) }
        for (row in pivots.indices.reversed()) {
            val col = pivots[row]
            var sum = Fraction.of(0)
            for (j in col + 1 until totalMolecules) sum += matrix[row][j] * vector[j]
            vector[col] = -sum
        }
        if (vector.any { it.numerator <= BigInteger.ZERO }) return "Не удалось подобрать положительные коэффициенты"
        val denominator = vector.fold(BigInteger.ONE) { acc, fraction ->
            acc / acc.gcd(fraction.denominator) * fraction.denominator
        }
        val raw = vector.map { it.numerator * (denominator / it.denominator) }
        val divisor = raw.reduce { a, b -> a.gcd(b) }
        val coefs = raw.map { it / divisor }
        if (coefs.any { it > BigInteger.valueOf(1_000_000) }) return "Ошибка: коэффициенты слишком велики"
        run {
            val leftStr = leftSp.mapIndexed { idx, it -> 
                val c = coefs[idx]
                if (c > BigInteger.ONE) "$c$it" else it
            }.joinToString(" + ")
            
            val rightStr = rightSp.mapIndexed { idx, it -> 
                val c = coefs[leftSp.size + idx]
                if (c > BigInteger.ONE) "$c$it" else it
            }.joinToString(" + ")
            
            return "$leftStr = $rightStr"
        }
    }
}

private data class Fraction private constructor(val numerator: BigInteger, val denominator: BigInteger) {
    companion object {
        fun of(value: Int) = Fraction(BigInteger.valueOf(value.toLong()), BigInteger.ONE)
        fun of(n: BigInteger, d: BigInteger): Fraction {
            require(d != BigInteger.ZERO)
            val sign = if (d.signum() < 0) -1 else 1
            val gcd = n.gcd(d)
            return Fraction(n / gcd * BigInteger.valueOf(sign.toLong()), d / gcd * BigInteger.valueOf(sign.toLong()))
        }
    }
    operator fun plus(other: Fraction) = of(numerator * other.denominator + other.numerator * denominator, denominator * other.denominator)
    operator fun minus(other: Fraction) = of(numerator * other.denominator - other.numerator * denominator, denominator * other.denominator)
    operator fun times(other: Fraction) = of(numerator * other.numerator, denominator * other.denominator)
    operator fun div(other: Fraction) = of(numerator * other.denominator, denominator * other.numerator)
    operator fun unaryMinus() = of(-numerator, denominator)
}

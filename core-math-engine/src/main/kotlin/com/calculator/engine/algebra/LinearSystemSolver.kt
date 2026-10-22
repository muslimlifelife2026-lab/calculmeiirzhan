package com.calculator.engine.algebra

import com.calculator.engine.parser.MathParser
import com.calculator.engine.parser.MathToken
import java.math.BigDecimal
import java.math.BigInteger

enum class LinearSystemKind { UNIQUE, NONE, INFINITE }

data class LinearSystemResult(
    val kind: LinearSystemKind,
    val solution: Map<String, String>,
    val steps: List<String>,
    val graphExpressions: List<String> = emptyList()
)

/** Two linear equations over the rationals; no floating-point rank thresholds are used. */
object LinearSystemSolver {
    const val MAX_INPUT_LENGTH = 256
    private val numberPattern = Regex("(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:[eE][+-]?[0-9]+)?")
    private val identifierPattern = Regex("[\\p{L}_]+")

    fun solve(firstEquation: String, secondEquation: String): LinearSystemResult {
        val first = parseEquation(firstEquation)
        val second = parseEquation(secondEquation)
        val graphs = graphExpressions(first, second)
        val steps = mutableListOf(
            "Исходная система: $firstEquation; $secondEquation.",
            "Раскрываем скобки, переносим неизвестные влево, числа вправо и приводим подобные: ${first.text()}; ${second.text()}."
        )
        val impossible = listOf(first, second).firstOrNull { it.isConstant && !it.rhs.isZero }
        if (impossible != null) {
            steps += "Получено неверное равенство ${impossible.text()}. Ни одна пара x, y его не удовлетворяет."
            return result(LinearSystemKind.NONE, steps, graphs)
        }
        if (first.isConstant && second.isConstant) {
            steps += "Оба равенства свелись к 0 = 0. Подходит любая пара действительных чисел x, y."
            return result(LinearSystemKind.INFINITE, steps, graphs)
        }

        val pivotIsX = !first.x.isZero || !second.x.isZero
        val pivotName = if (pivotIsX) "x" else "y"
        val otherName = if (pivotIsX) "y" else "x"
        fun pivot(row: Equation) = if (pivotIsX) row.x else row.y
        fun other(row: Equation) = if (pivotIsX) row.y else row.x
        val row1: Equation
        val row2: Equation
        if (pivot(first).isZero) {
            row1 = second
            row2 = first
            steps += "Меняем уравнения местами: в первом нужен ненулевой коэффициент при $pivotName."
        } else {
            row1 = first
            row2 = second
        }
        val factor = pivot(row2) / pivot(row1)
        val reduced = row2 - row1.times(factor)
        steps += if (factor.isZero) {
            "Исключать $pivotName из второго уравнения не нужно: его коэффициент уже равен 0. Получаем ${reduced.text()}."
        } else {
            "Чтобы исключить $pivotName, умножаем первое уравнение на ($factor) и вычитаем из второго: ${reduced.text()}."
        }
        if (other(reduced).isZero) {
            if (!reduced.rhs.isZero) {
                steps += "Получено противоречие ${reduced.text()}. Поэтому решений нет."
                return result(LinearSystemKind.NONE, steps, graphs)
            }
            val constant = row1.rhs / pivot(row1)
            val coefficient = -other(row1) / pivot(row1)
            steps += "Второе уравнение свелось к 0 = 0: осталось одно независимое уравнение."
            steps += "$otherName = t, $pivotName = ${affineText(constant, coefficient, "t")}, где t — любое действительное число. Решений бесконечно много."
            return result(LinearSystemKind.INFINITE, steps, graphs)
        }
        val otherValue = reduced.rhs / other(reduced)
        val pivotValue = (row1.rhs - other(row1) * otherValue) / pivot(row1)
        val x = if (pivotIsX) pivotValue else otherValue
        val y = if (pivotIsX) otherValue else pivotValue
        steps += "$otherName = (${reduced.rhs})/(${other(reduced)}) = $otherValue."
        steps += "Подставляем $otherName = $otherValue в ${row1.text()}: $pivotName = (${row1.rhs} − (${other(row1)})·($otherValue))/(${pivot(row1)}) = $pivotValue."
        val firstCheck = first.x * x + first.y * y
        val secondCheck = second.x * x + second.y * y
        check(firstCheck == first.rhs && secondCheck == second.rhs) { "Нарушена точная проверка решения системы" }
        steps += "Проверка в приведённой системе: (${first.x})·($x) + (${first.y})·($y) = $firstCheck = ${first.rhs}; (${second.x})·($x) + (${second.y})·($y) = $secondCheck = ${second.rhs}. Оба равенства верны."
        return LinearSystemResult(LinearSystemKind.UNIQUE, linkedMapOf("x" to x.toString(), "y" to y.toString()), steps, graphs)
    }

    private fun result(kind: LinearSystemKind, steps: List<String>, graphs: List<String>) = LinearSystemResult(kind, emptyMap(), steps, graphs)

    private fun graphExpressions(first: Equation, second: Equation): List<String> {
        if (first.y.isZero || second.y.isZero) return emptyList()
        val expressions = listOf(first, second).map { "(${it.rhs}-(${it.x})*x)/(${it.y})" }
        return expressions.takeIf { pair -> pair.all { it.length <= MAX_INPUT_LENGTH } } ?: emptyList()
    }

    private fun parseEquation(input: String): Equation {
        require(input.length <= MAX_INPUT_LENGTH) { "В каждом уравнении допускается не больше $MAX_INPUT_LENGTH символов" }
        val normalized = input.replace('−', '-').replace('–', '-').replace('×', '*').replace('·', '*')
            .replace('÷', '/').replace(',', '.').replace("²", "^2").replace("³", "^3")
        val sides = normalized.split('=')
        require(sides.size == 2 && sides.all { it.isNotBlank() }) { "В каждом уравнении нужны левая и правая части и один знак =" }
        val moved = parseSide(sides[0]) - parseSide(sides[1])
        return Equation(moved.x, moved.y, -moved.constant)
    }

    private fun parseSide(input: String): Linear {
        // Reject symbolic constants before the common tokenizer approximates pi/e.
        val identifiers = identifierPattern.findAll(numberPattern.replace(input, "")).map { it.value }
        require(identifiers.all { it.equals("x", true) || it.equals("y", true) }) {
            "Поддерживаются только переменные x, y и числовые коэффициенты"
        }
        val rpn = try {
            val tokens = MathParser.tokenize(input)
            require(tokens.size <= MAX_INPUT_LENGTH)
            MathParser.shuntingYard(tokens)
        } catch (exception: IllegalArgumentException) {
            throw IllegalArgumentException("Проверьте числа, знаки операций и парные скобки в уравнении", exception)
        }
        val stack = ArrayDeque<Linear>()
        for (token in rpn) {
            when (token) {
                is MathToken.Number -> stack.addLast(Linear.constant(Rational.fromDecimal(token.value)))
                is MathToken.Variable -> stack.addLast(when (token.symbol.lowercase()) {
                    "x" -> Linear(Rational.ONE, Rational.ZERO, Rational.ZERO, true)
                    "y" -> Linear(Rational.ZERO, Rational.ONE, Rational.ZERO, true)
                    else -> throw IllegalArgumentException("Поддерживаются только переменные x и y")
                })
                is MathToken.Operator -> {
                    require(stack.isNotEmpty()) { "Пропущено число или переменная" }
                    val right = stack.removeLast()
                    if (token.symbol == "u-") {
                        stack.addLast(-right)
                    } else {
                        require(stack.isNotEmpty()) { "Пропущено число или переменная" }
                        val left = stack.removeLast()
                        stack.addLast(when (token.symbol) {
                            "+" -> left + right
                            "-" -> left - right
                            "*" -> left * right
                            "/" -> {
                                require(!right.hasVariable) { "Деление на выражение с переменными не поддерживается" }
                                left.divide(right.constant)
                            }
                            "^" -> {
                                require(!right.hasVariable && right.constant.denominator == BigInteger.ONE &&
                                    right.constant.numerator >= BigInteger.ZERO && right.constant.numerator <= BigInteger.valueOf(12)) {
                                    "Степень числа должна быть целой от 0 до 12"
                                }
                                val exponent = right.constant.numerator.toInt()
                                if (left.hasVariable) {
                                    require(exponent == 1) { "Система должна быть линейной: степени переменных и их произведения не поддерживаются" }
                                    left
                                } else {
                                    require(!left.constant.isZero || exponent != 0) { "0^0 не определено" }
                                    Linear.constant(left.constant.pow(exponent))
                                }
                            }
                            else -> throw IllegalArgumentException("Здесь поддерживаются +, −, *, / и скобки")
                        })
                    }
                }
                else -> throw IllegalArgumentException("Система должна содержать только линейные выражения с x и y")
            }
        }
        require(stack.size == 1) { "Проверьте запись уравнения" }
        return stack.single()
    }

    private data class Equation(val x: Rational, val y: Rational, val rhs: Rational) {
        val isConstant get() = x.isZero && y.isZero
        fun times(value: Rational) = Equation(x * value, y * value, rhs * value)
        operator fun minus(other: Equation) = Equation(x - other.x, y - other.y, rhs - other.rhs)
        fun text() = "${termsText(listOf(x to "x", y to "y"))} = $rhs"
    }

    private data class Linear(val x: Rational, val y: Rational, val constant: Rational, val hasVariable: Boolean) {
        operator fun unaryMinus() = Linear(-x, -y, -constant, hasVariable)
        operator fun plus(other: Linear) = Linear(x + other.x, y + other.y, constant + other.constant, hasVariable || other.hasVariable)
        operator fun minus(other: Linear) = this + (-other)
        operator fun times(other: Linear): Linear {
            require(!hasVariable || !other.hasVariable) { "Система должна быть линейной: произведения переменных не поддерживаются" }
            return if (other.hasVariable) other.times(this) else Linear(x * other.constant, y * other.constant, constant * other.constant, hasVariable)
        }
        fun divide(value: Rational) = Linear(x / value, y / value, constant / value, hasVariable)
        companion object {
            fun constant(value: Rational) = Linear(Rational.ZERO, Rational.ZERO, value, false)
        }
    }

    private fun affineText(constant: Rational, coefficient: Rational, variable: String): String =
        termsText(listOf(constant to "", coefficient to variable))

    private fun termsText(terms: List<Pair<Rational, String>>): String = buildString {
        terms.filterNot { it.first.isZero }.forEach { (value, name) ->
            if (isNotEmpty()) append(if (value.numerator.signum() < 0) " − " else " + ")
            else if (value.numerator.signum() < 0) append("−")
            val absolute = if (value.numerator.signum() < 0) -value else value
            if (name.isEmpty() || absolute != Rational.ONE) {
                append(absolute)
                if (name.isNotEmpty()) append("·")
            }
            append(name)
        }
        if (isEmpty()) append("0")
    }

    private data class Rational private constructor(val numerator: BigInteger, val denominator: BigInteger) {
        val isZero get() = numerator.signum() == 0
        operator fun unaryMinus() = create(numerator.negate(), denominator)
        operator fun plus(other: Rational) = create(numerator * other.denominator + other.numerator * denominator, denominator * other.denominator)
        operator fun minus(other: Rational) = this + (-other)
        operator fun times(other: Rational) = create(numerator * other.numerator, denominator * other.denominator)
        operator fun div(other: Rational) = create(numerator * other.denominator, denominator * other.numerator)
        fun pow(exponent: Int): Rational {
            var value = ONE
            repeat(exponent) { value *= this }
            return value
        }
        override fun toString() = if (denominator == BigInteger.ONE) numerator.toString() else "$numerator/$denominator"
        companion object {
            val ZERO = Rational(BigInteger.ZERO, BigInteger.ONE)
            val ONE = Rational(BigInteger.ONE, BigInteger.ONE)
            fun fromDecimal(value: BigDecimal): Rational {
                require(value.precision() <= 128 && value.scale() in -128..128) { "Числа: до 128 значащих цифр и десятичная степень от −128 до 128" }
                return if (value.scale() >= 0) create(value.unscaledValue(), BigInteger.TEN.pow(value.scale()))
                else create(value.unscaledValue() * BigInteger.TEN.pow(-value.scale()), BigInteger.ONE)
            }
            private fun create(n: BigInteger, d: BigInteger): Rational {
                require(d.signum() != 0) { "Деление на ноль" }
                if (n.signum() == 0) return ZERO
                val gcd = n.gcd(d)
                val sign = BigInteger.valueOf(d.signum().toLong())
                val numerator = n / gcd * sign
                val denominator = d / gcd * sign
                require(numerator.bitLength() <= 8192 && denominator.bitLength() <= 8192) { "Коэффициенты слишком велики: упростите уравнение" }
                return Rational(numerator, denominator)
            }
        }
    }
}

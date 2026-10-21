package com.calculator.engine.algebra

import ch.obermuhlner.math.big.BigDecimalMath
import com.calculator.engine.parser.MathParser
import com.calculator.engine.parser.MathToken
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode

enum class AlgebraOperation { SIMPLIFY, DERIVATIVE, SOLVE }

data class AlgebraRoot(val exact: String, val approximate: String? = null, val multiplicity: Int = 1)

data class AlgebraResult(
    val normalized: String,
    val answer: String,
    val steps: List<String>,
    val roots: List<AlgebraRoot> = emptyList()
)

/** Bounded, offline polynomial algebra. Coefficients stay rational until displaying irrational roots. */
object PolynomialAlgebra {
    const val MAX_DEGREE = 12
    const val MAX_INPUT_LENGTH = 256
    private val rootContext = MathContext(48, RoundingMode.HALF_EVEN)
    private val displayContext = MathContext(14, RoundingMode.HALF_EVEN)

    fun calculate(input: String, operation: AlgebraOperation): AlgebraResult {
        require(input.length <= MAX_INPUT_LENGTH) { "Не больше $MAX_INPUT_LENGTH символов" }
        require(input.isNotBlank()) { "Введите выражение или уравнение с x" }
        val sides = input.replace('−', '-').replace('×', '*').replace('÷', '/')
            .replace('–', '-').replace(',', '.').replace("²", "^2").replace("³", "^3").split('=')
        require(sides.size <= 2 && sides.all { it.isNotBlank() }) { "Укажите обе части одного уравнения" }
        require(operation == AlgebraOperation.SOLVE || sides.size == 1) {
            "Для уравнения со знаком = выберите «Решить»"
        }
        val left = parse(sides[0])
        val polynomial = if (sides.size == 2) left - parse(sides[1]) else left
        val normalized = polynomial.text()
        return when (operation) {
            AlgebraOperation.SIMPLIFY -> AlgebraResult(normalized, normalized, listOf(
                "Раскрываем скобки, выполняем умножение и собираем коэффициенты при одинаковых степенях x.",
                "Коэффициенты вычисляются точно как рациональные числа; дроби сокращаются.",
                "Результат: $normalized"
            ))
            AlgebraOperation.DERIVATIVE -> {
                val derivative = polynomial.derivative().text()
                AlgebraResult(normalized, derivative, listOf(
                    "Приводим исходное выражение: f(x) = $normalized",
                    "Применяем (c·x^n)′ = n·c·x^(n−1) к каждому члену. Производная константы равна 0.",
                    "f′(x) = $derivative"
                ))
            }
            AlgebraOperation.SOLVE -> solve(polynomial, sides.size == 2)
        }
    }

    private fun parse(expression: String): Polynomial {
        require(!Regex("(?i)(?<![\\p{L}\\p{N}_])(?:pi|e)(?![\\p{L}\\p{N}_])").containsMatchIn(expression)) {
            "Коэффициенты задавайте числами или дробями; константы pi и e здесь не поддерживаются"
        }
        val tokens = MathParser.tokenize(expression)
        require(tokens.size <= MAX_INPUT_LENGTH) { "Выражение слишком сложное" }
        val rpn = MathParser.shuntingYard(tokens)
        val stack = ArrayDeque<Polynomial>()
        for (token in rpn) {
            when (token) {
                is MathToken.Number -> stack.addLast(Polynomial.constant(Rational.fromDecimal(token.value)))
                is MathToken.Variable -> {
                    require(token.symbol.equals("x", ignoreCase = true)) { "Поддерживается одна переменная x" }
                    stack.addLast(Polynomial.variable())
                }
                is MathToken.Operator -> {
                    require(token.symbol != "%") { "Проценты здесь не поддерживаются; используйте деление на 100" }
                    if (token.symbol == "u-") {
                        stack.addLast(-stack.removeLast())
                    } else {
                        val right = stack.removeLast()
                        val left = stack.removeLast()
                        stack.addLast(when (token.symbol) {
                            "+" -> left + right
                            "-" -> left - right
                            "*" -> left * right
                            "/" -> {
                                require(right.degree == 0) {
                                    "Деление на выражение с x не поддерживается: оно меняет область определения"
                                }
                                left.divide(right[0])
                            }
                            "^" -> {
                                require(right.degree == 0 && right[0].denominator == BigInteger.ONE &&
                                    right[0].numerator >= BigInteger.ZERO && right[0].numerator <= BigInteger.valueOf(MAX_DEGREE.toLong())) {
                                    "Степень должна быть целым числом от 0 до $MAX_DEGREE"
                                }
                                val exponent = right[0].numerator.toInt()
                                require(!(left.isZero && exponent == 0)) { "0^0 не определено" }
                                require(exponent != 0 || left.degree == 0) {
                                    "Нулевая степень выражения с x может скрыть 0^0; здесь она не поддерживается"
                                }
                                left.pow(exponent)
                            }
                            else -> throw IllegalArgumentException("Неподдерживаемая операция")
                        })
                    }
                }
                else -> throw IllegalArgumentException("Поддерживаются полиномы: x, числа, +, −, *, /, ^ и скобки")
            }
        }
        return stack.single()
    }

    private fun solve(p: Polynomial, hasEquals: Boolean): AlgebraResult {
        val normalized = "${p.text()} = 0"
        val steps = mutableListOf(
            if (hasEquals) "Переносим правую часть влево и приводим подобные члены: $normalized"
            else "Знак = не указан: решаем введённый полином, приравняв его к нулю: $normalized"
        )
        require(p.degree <= 2) { "Упрощённая степень ${p.degree}. Сейчас решаются только линейные и квадратные уравнения" }
        if (p.degree == 0) {
            val answer = if (p.isZero) "Любое действительное x" else "Решений нет"
            steps += if (p.isZero) "Получено тождество 0 = 0." else "Получено неверное равенство ${p[0]} = 0."
            return AlgebraResult(normalized, answer, steps)
        }
        if (p.degree == 1) {
            val root = -p[0] / p[1]
            steps += "Для a·x + b = 0 имеем a = ${p[1]}, b = ${p[0]}."
            steps += "x = −b/a = $root"
            return AlgebraResult(normalized, "x = $root", steps, listOf(rationalRoot(root)))
        }
        val a = p[2]
        val b = p[1]
        val c = p[0]
        val d = b * b - Rational.of(4) * a * c
        steps += "a = $a, b = $b, c = $c"
        steps += "D = b² − 4ac = $d"
        if (d.signum < 0) {
            steps += "D < 0: действительных корней нет. Этот раздел решает уравнения в действительных числах."
            return AlgebraResult(normalized, "Действительных корней нет", steps)
        }
        val twiceA = Rational.of(2) * a
        if (d.signum == 0) {
            val root = -b / twiceA
            steps += "D = 0: x = −b/(2a) = $root. Это корень кратности 2."
            return AlgebraResult(normalized, "x = $root · двойной корень", steps, listOf(rationalRoot(root).copy(multiplicity = 2)))
        }
        steps += "D > 0: x = (−b ± √D)/(2a)."
        val exactSqrt = d.sqrtExact()
        val roots = if (exactSqrt != null) {
            listOf((-b - exactSqrt) / twiceA, (-b + exactSqrt) / twiceA)
                .sorted().map(::rationalRoot)
        } else {
            // The q formulation avoids subtracting two nearly equal numbers for small roots.
            val decimalB = b.decimal(rootContext)
            val sqrtD = BigDecimalMath.sqrt(d.decimal(rootContext), rootContext)
            val signedSqrt = if (b.signum < 0) sqrtD.negate() else sqrtD
            val q = decimalB.add(signedSqrt, rootContext).negate().divide(BigDecimal(2), rootContext)
            val qRoot = q.divide(a.decimal(rootContext), rootContext)
            val secondRoot = c.decimal(rootContext).divide(q, rootContext)
            val minusApprox = if (b.signum < 0) secondRoot else qRoot
            val plusApprox = if (b.signum < 0) qRoot else secondRoot
            listOf(
                AlgebraRoot("(${-b} − √($d))/($twiceA)", approximate(minusApprox)),
                AlgebraRoot("(${-b} + √($d))/($twiceA)", approximate(plusApprox))
            )
        }
        roots.forEachIndexed { index, root -> steps += "x${index + 1} = ${root.exact}" }
        return AlgebraResult(normalized, "Два действительных корня", steps, roots)
    }

    private fun rationalRoot(value: Rational): AlgebraRoot {
        val decimal = value.decimal(rootContext)
        return AlgebraRoot(value.toString(), if (value.denominator == BigInteger.ONE) null else approximate(decimal))
    }

    private fun approximate(value: BigDecimal): String = value.round(displayContext).stripTrailingZeros().let {
        if (it.scale() > 10 || it.precision() - it.scale() > 14) it.toString() else it.toPlainString()
    }

    private data class Polynomial(private val terms: Map<Int, Rational>) {
        val degree: Int get() = terms.keys.maxOrNull() ?: 0
        val isZero: Boolean get() = terms.isEmpty()
        operator fun get(power: Int) = terms[power] ?: Rational.ZERO
        operator fun unaryMinus() = create(terms.mapValues { -it.value })
        operator fun plus(other: Polynomial) = create((terms.keys + other.terms.keys).associateWith { this[it] + other[it] })
        operator fun minus(other: Polynomial) = this + (-other)
        operator fun times(other: Polynomial): Polynomial {
            require(degree + other.degree <= MAX_DEGREE || isZero || other.isZero) { "Максимальная степень полинома — $MAX_DEGREE" }
            val result = mutableMapOf<Int, Rational>()
            for ((a, va) in terms) for ((b, vb) in other.terms) {
                result[a + b] = (result[a + b] ?: Rational.ZERO) + va * vb
            }
            return create(result)
        }
        fun divide(value: Rational): Polynomial {
            require(value.signum != 0) { "Деление на ноль" }
            return create(terms.mapValues { it.value / value })
        }
        fun pow(power: Int): Polynomial {
            require(degree * power <= MAX_DEGREE) { "Максимальная степень полинома — $MAX_DEGREE" }
            var result = constant(Rational.ONE)
            repeat(power) { result *= this }
            return result
        }
        fun derivative() = create(terms.filterKeys { it > 0 }.map { (power, value) -> power - 1 to value * Rational.of(power.toLong()) }.toMap())
        fun text(): String {
            if (isZero) return "0"
            return buildString {
                terms.toSortedMap(reverseOrder()).forEach { (power, value) ->
                    if (isNotEmpty()) append(if (value.signum < 0) " − " else " + ") else if (value.signum < 0) append("−")
                    val coefficient = value.abs()
                    if (power == 0 || coefficient != Rational.ONE) {
                        append(coefficient)
                        if (power > 0) append("·")
                    }
                    if (power > 0) append("x")
                    if (power > 1) append("^$power")
                }
            }
        }
        companion object {
            fun create(terms: Map<Int, Rational>) = Polynomial(terms.filterValues { it.signum != 0 })
            fun constant(value: Rational) = create(mapOf(0 to value))
            fun variable() = create(mapOf(1 to Rational.ONE))
        }
    }

    private data class Rational(val numerator: BigInteger, val denominator: BigInteger) : Comparable<Rational> {
        val signum: Int get() = numerator.signum()
        operator fun unaryMinus() = create(numerator.negate(), denominator)
        operator fun plus(other: Rational) = create(numerator * other.denominator + other.numerator * denominator, denominator * other.denominator)
        operator fun minus(other: Rational) = this + (-other)
        operator fun times(other: Rational) = create(numerator * other.numerator, denominator * other.denominator)
        operator fun div(other: Rational) = create(numerator * other.denominator, denominator * other.numerator)
        override fun compareTo(other: Rational) = (numerator * other.denominator).compareTo(other.numerator * denominator)
        fun abs() = if (signum < 0) -this else this
        fun decimal(context: MathContext) = BigDecimal(numerator).divide(BigDecimal(denominator), context)
        override fun toString() = if (denominator == BigInteger.ONE) numerator.toString() else "$numerator/$denominator"
        fun sqrtExact(): Rational? {
            if (signum < 0) return null
            val n = integerSqrt(numerator)
            val d = integerSqrt(denominator)
            return if (n * n == numerator && d * d == denominator) create(n, d) else null
        }
        companion object {
            val ZERO = Rational(BigInteger.ZERO, BigInteger.ONE)
            val ONE = Rational(BigInteger.ONE, BigInteger.ONE)
            fun of(value: Long) = Rational(BigInteger.valueOf(value), BigInteger.ONE)
            fun fromDecimal(value: BigDecimal): Rational {
                require(value.precision() <= 128 && value.scale() in -128..128) { "Числа: до 128 цифр и десятичная степень от −128 до 128" }
                return if (value.scale() >= 0) create(value.unscaledValue(), BigInteger.TEN.pow(value.scale()))
                else create(value.unscaledValue() * BigInteger.TEN.pow(-value.scale()), BigInteger.ONE)
            }
            fun create(n: BigInteger, d: BigInteger): Rational {
                require(d.signum() != 0) { "Деление на ноль" }
                if (n.signum() == 0) return ZERO
                val gcd = n.gcd(d)
                val sign = BigInteger.valueOf(d.signum().toLong())
                val numerator = n / gcd * sign
                val denominator = d / gcd * sign
                require(numerator.bitLength() <= 8192 && denominator.bitLength() <= 8192) { "Коэффициенты слишком велики: упростите выражение" }
                return Rational(numerator, denominator)
            }
            private fun integerSqrt(value: BigInteger): BigInteger {
                if (value <= BigInteger.ONE) return value
                var estimate = BigInteger.ONE.shiftLeft((value.bitLength() + 1) / 2)
                while (true) {
                    val next = (estimate + value / estimate).shiftRight(1)
                    if (next >= estimate) return estimate
                    estimate = next
                }
            }
        }
    }
}

package com.calculator.engine.parser

import ch.obermuhlner.math.big.BigDecimalMath
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.Stack

object MathParser {

    private val OPERATORS = mapOf(
        "+" to MathToken.Operator("+", 2),
        "-" to MathToken.Operator("-", 2),
        "*" to MathToken.Operator("*", 3),
        "/" to MathToken.Operator("/", 3),
        "^" to MathToken.Operator("^", 4, MathToken.Assoc.RIGHT),
        "u-" to MathToken.Operator("u-", 5, MathToken.Assoc.RIGHT)
    )

    private val FUNCTIONS = setOf(
        "sin", "cos", "tan", "asin", "acos", "atan",
        "sinh", "cosh", "tanh", "sqrt", "ln", "log", "exp", "abs", "fact"
    )

    fun tokenize(expression: String): List<MathToken> {
        val tokens = mutableListOf<MathToken>()
        var i = 0
        val len = expression.length

        // Helper to check if a character is an operator
        fun isOperatorChar(c: Char) = c in "+-*/^"

        while (i < len) {
            val c = expression[i]
            when {
                c.isWhitespace() -> {
                    i++
                }
                c.isDigit() || c == '.' -> {
                    val start = i
                    while (i < len && (expression[i].isDigit() || expression[i] == '.')) {
                        i++
                    }
                    tokens.add(MathToken.Number(BigDecimal(expression.substring(start, i))))
                }
                c == '(' -> {
                    tokens.add(MathToken.LeftParenthesis)
                    i++
                }
                c == ')' -> {
                    tokens.add(MathToken.RightParenthesis)
                    i++
                }
                c == ',' -> {
                    tokens.add(MathToken.Comma)
                    i++
                }
                isOperatorChar(c) -> {
                    val opStr = c.toString()
                    // Determine if unary minus
                    if (opStr == "-") {
                        val prev = tokens.lastOrNull()
                        val isUnary = prev == null || 
                                      prev is MathToken.Operator || 
                                      prev == MathToken.LeftParenthesis || 
                                      prev == MathToken.Comma
                        if (isUnary) {
                            tokens.add(OPERATORS["u-"]!!)
                        } else {
                            tokens.add(OPERATORS["-"]!!)
                        }
                    } else {
                        tokens.add(OPERATORS[opStr] ?: throw IllegalArgumentException("Unknown operator: $opStr"))
                    }
                    i++
                }
                c.isLetter() || c == '_' -> {
                    val start = i
                    while (i < len && (expression[i].isLetterOrDigit() || expression[i] == '_')) {
                        i++
                    }
                    val name = expression.substring(start, i)
                    when {
                        FUNCTIONS.contains(name.lowercase()) -> {
                            tokens.add(MathToken.Func(name.lowercase()))
                        }
                        name.lowercase() == "pi" -> {
                            tokens.add(MathToken.Number(BigDecimalMath.pi(MathContext.DECIMAL128)))
                        }
                        name.lowercase() == "e" -> {
                            tokens.add(MathToken.Number(BigDecimalMath.e(MathContext.DECIMAL128)))
                        }
                        else -> {
                            tokens.add(MathToken.Variable(name))
                        }
                    }
                }
                else -> {
                    throw IllegalArgumentException("Unexpected character: $c at position $i")
                }
            }
        }
        return tokens
    }

    fun shuntingYard(tokens: List<MathToken>): List<MathToken> {
        val outputQueue = mutableListOf<MathToken>()
        val operatorStack = Stack<MathToken>()

        for (token in tokens) {
            when (token) {
                is MathToken.Number, is MathToken.Variable -> {
                    outputQueue.add(token)
                }
                is MathToken.Func -> {
                    operatorStack.push(token)
                }
                is MathToken.Comma -> {
                    while (operatorStack.isNotEmpty() && operatorStack.peek() != MathToken.LeftParenthesis) {
                        outputQueue.add(operatorStack.pop())
                    }
                    if (operatorStack.isEmpty()) {
                        throw IllegalArgumentException("Mismatch parenthesis or misplaced comma")
                    }
                }
                is MathToken.Operator -> {
                    while (operatorStack.isNotEmpty() && operatorStack.peek() is MathToken.Operator) {
                        val top = operatorStack.peek() as MathToken.Operator
                        val o1 = token
                        val o2 = top
                        val condition = if (o1.associativity == MathToken.Assoc.LEFT) {
                            o1.precedence <= o2.precedence
                        } else {
                            o1.precedence < o2.precedence
                        }
                        if (condition) {
                            outputQueue.add(operatorStack.pop())
                        } else {
                            break
                        }
                    }
                    operatorStack.push(token)
                }
                is MathToken.LeftParenthesis -> {
                    operatorStack.push(token)
                }
                is MathToken.RightParenthesis -> {
                    while (operatorStack.isNotEmpty() && operatorStack.peek() != MathToken.LeftParenthesis) {
                        outputQueue.add(operatorStack.pop())
                    }
                    if (operatorStack.isEmpty()) {
                        throw IllegalArgumentException("Parentheses mismatch")
                    }
                    operatorStack.pop() // Remove LeftParenthesis
                    if (operatorStack.isNotEmpty() && operatorStack.peek() is MathToken.Func) {
                        outputQueue.add(operatorStack.pop())
                    }
                }
            }
        }

        while (operatorStack.isNotEmpty()) {
            val top = operatorStack.peek()
            if (top == MathToken.LeftParenthesis || top == MathToken.RightParenthesis) {
                throw IllegalArgumentException("Parentheses mismatch")
            }
            outputQueue.add(operatorStack.pop())
        }

        return outputQueue
    }

    data class MathStep(val description: String, val expression: String)

    fun evaluateRPN(
        rpn: List<MathToken>,
        variables: Map<String, BigDecimal> = emptyMap(),
        precision: Int = 34,
        isDegrees: Boolean = false
    ): BigDecimal {
        return evaluateRPNWithSteps(rpn, variables, precision, isDegrees).first
    }


    fun evaluateRPNWithSteps(
        rpn: List<MathToken>,
        variables: Map<String, BigDecimal> = emptyMap(),
        precision: Int = 34,
        isDegrees: Boolean = false
    ): Pair<BigDecimal, List<MathStep>> {
        val stack = Stack<BigDecimal>()
        val mc = MathContext(precision, RoundingMode.HALF_UP)
        val steps = mutableListOf<MathStep>()

        // Formatter to clean up intermediate trailing decimals
        fun formatVal(v: BigDecimal): String = v.setScale(6, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()

        for (token in rpn) {
            when (token) {
                is MathToken.Number -> {
                    stack.push(token.value)
                }
                is MathToken.Variable -> {
                    val value = variables[token.symbol] 
                        ?: throw IllegalArgumentException("Variable not initialized: ${token.symbol}")
                    stack.push(value)
                }
                is MathToken.Operator -> {
                    if (token.symbol == "u-") {
                        if (stack.size < 1) throw IllegalArgumentException("Empty stack for unary minus")
                        val a = stack.pop()
                        val res = a.negate()
                        stack.push(res)
                        steps.add(MathStep("Смена знака числа (${formatVal(a)}) на противоположный", "-(${formatVal(a)}) = ${formatVal(res)}"))
                    } else {
                        if (stack.size < 2) throw IllegalArgumentException("Insufficient operands for binary operator ${token.symbol}")
                        val b = stack.pop()
                        val a = stack.pop()
                        val result = when (token.symbol) {
                            "+" -> {
                                val res = a.add(b, mc)
                                steps.add(MathStep("Складываем ${formatVal(a)} и ${formatVal(b)}", "${formatVal(a)} + ${formatVal(b)} = ${formatVal(res)}"))
                                res
                            }
                            "-" -> {
                                val res = a.subtract(b, mc)
                                steps.add(MathStep("Вычитаем ${formatVal(b)} из ${formatVal(a)}", "${formatVal(a)} - ${formatVal(b)} = ${formatVal(res)}"))
                                res
                            }
                            "*" -> {
                                val res = a.multiply(b, mc)
                                steps.add(MathStep("Умножаем ${formatVal(a)} на ${formatVal(b)}", "${formatVal(a)} * ${formatVal(b)} = ${formatVal(res)}"))
                                res
                            }
                            "/" -> {
                                if (b.compareTo(BigDecimal.ZERO) == 0) throw ArithmeticException("Division by zero")
                                val res = a.divide(b, mc)
                                steps.add(MathStep("Делим ${formatVal(a)} на ${formatVal(b)}", "${formatVal(a)} / ${formatVal(b)} = ${formatVal(res)}"))
                                res
                            }
                            "^" -> {
                                val res = BigDecimalMath.pow(a, b, mc)
                                steps.add(MathStep("Возводим ${formatVal(a)} в степень ${formatVal(b)}", "${formatVal(a)} ^ ${formatVal(b)} = ${formatVal(res)}"))
                                res
                            }
                            else -> throw IllegalArgumentException("Unsupported operator: ${token.symbol}")
                        }
                        stack.push(result)
                    }
                }
                is MathToken.Func -> {
                    if (stack.isEmpty()) throw IllegalArgumentException("Empty stack for function ${token.name}")
                    val a = stack.pop()
                    val trigAngle = if (isDegrees && (token.name in setOf("sin", "cos", "tan"))) {
                        a.multiply(BigDecimalMath.pi(mc), mc).divide(BigDecimal(180), mc)
                    } else a

                    val angleUnitStr = if (isDegrees && (token.name in setOf("sin", "cos", "tan"))) "°" else " рад"

                    val result = when (token.name) {
                        "sin" -> {
                            val res = BigDecimalMath.sin(trigAngle, mc)
                            steps.add(MathStep("Вычисляем синус от ${formatVal(a)}$angleUnitStr", "sin(${formatVal(a)}$angleUnitStr) = ${formatVal(res)}"))
                            res
                        }
                        "cos" -> {
                            val res = BigDecimalMath.cos(trigAngle, mc)
                            steps.add(MathStep("Вычисляем косинус от ${formatVal(a)}$angleUnitStr", "cos(${formatVal(a)}$angleUnitStr) = ${formatVal(res)}"))
                            res
                        }
                        "tan" -> {
                            val res = BigDecimalMath.tan(trigAngle, mc)
                            steps.add(MathStep("Вычисляем тангенс от ${formatVal(a)}$angleUnitStr", "tan(${formatVal(a)}$angleUnitStr) = ${formatVal(res)}"))
                            res
                        }
                        "asin" -> {
                            val raw = BigDecimalMath.asin(a, mc)
                            val res = if (isDegrees) raw.multiply(BigDecimal(180), mc).divide(BigDecimalMath.pi(mc), mc) else raw
                            val unitOut = if (isDegrees) "°" else " рад"
                            steps.add(MathStep("Вычисляем арксинус от ${formatVal(a)}", "arcsin(${formatVal(a)}) = ${formatVal(res)}$unitOut"))
                            res
                        }
                        "acos" -> {
                            val raw = BigDecimalMath.acos(a, mc)
                            val res = if (isDegrees) raw.multiply(BigDecimal(180), mc).divide(BigDecimalMath.pi(mc), mc) else raw
                            val unitOut = if (isDegrees) "°" else " рад"
                            steps.add(MathStep("Вычисляем арккосинус от ${formatVal(a)}", "arccos(${formatVal(a)}) = ${formatVal(res)}$unitOut"))
                            res
                        }
                        "atan" -> {
                            val raw = BigDecimalMath.atan(a, mc)
                            val res = if (isDegrees) raw.multiply(BigDecimal(180), mc).divide(BigDecimalMath.pi(mc), mc) else raw
                            val unitOut = if (isDegrees) "°" else " рад"
                            steps.add(MathStep("Вычисляем арктангенс от ${formatVal(a)}", "arctan(${formatVal(a)}) = ${formatVal(res)}$unitOut"))
                            res
                        }
                        "sinh" -> {
                            val res = BigDecimalMath.sinh(a, mc)
                            steps.add(MathStep("Вычисляем гиперболический синус от ${formatVal(a)}", "sinh(${formatVal(a)}) = ${formatVal(res)}"))
                            res
                        }
                        "cosh" -> {
                            val res = BigDecimalMath.cosh(a, mc)
                            steps.add(MathStep("Вычисляем гиперболический косинус от ${formatVal(a)}", "cosh(${formatVal(a)}) = ${formatVal(res)}"))
                            res
                        }
                        "tanh" -> {
                            val res = BigDecimalMath.tanh(a, mc)
                            steps.add(MathStep("Вычисляем гиперболический тангенс от ${formatVal(a)}", "tanh(${formatVal(a)}) = ${formatVal(res)}"))
                            res
                        }
                        "sqrt" -> {
                            val res = BigDecimalMath.sqrt(a, mc)
                            steps.add(MathStep("Извлекаем квадратный корень из ${formatVal(a)}", "sqrt(${formatVal(a)}) = ${formatVal(res)}"))
                            res
                        }
                        "ln" -> {
                            val res = BigDecimalMath.log(a, mc)
                            steps.add(MathStep("Вычисляем натуральный логарифм от ${formatVal(a)}", "ln(${formatVal(a)}) = ${formatVal(res)}"))
                            res
                        }
                        "log" -> {
                            val res = BigDecimalMath.log10(a, mc)
                            steps.add(MathStep("Вычисляем десятичный логарифм от ${formatVal(a)}", "log(${formatVal(a)}) = ${formatVal(res)}"))
                            res
                        }
                        "exp" -> {
                            val res = BigDecimalMath.exp(a, mc)
                            steps.add(MathStep("Вычисляем экспоненту e ^ ${formatVal(a)}", "exp(${formatVal(a)}) = ${formatVal(res)}"))
                            res
                        }
                        "abs" -> {
                            val res = a.abs(mc)
                            steps.add(MathStep("Находим модуль числа ${formatVal(a)}", "|${formatVal(a)}| = ${formatVal(res)}"))
                            res
                        }
                        "fact" -> {
                            val res = factorial(a, mc)
                            steps.add(MathStep("Вычисляем факториал числа ${formatVal(a)}", "${formatVal(a)}! = ${formatVal(res)}"))
                            res
                        }
                        else -> throw IllegalArgumentException("Unsupported function: ${token.name}")
                    }
                    stack.push(result)
                }
                else -> {
                    throw IllegalArgumentException("Unsupported RPN token: $token")
                }
            }
        }

        if (stack.size != 1) {
            throw IllegalArgumentException("Invalid expression result state")
        }

        return Pair(stack.pop(), steps)
    }

    private fun factorial(n: BigDecimal, mc: MathContext): BigDecimal {
        val integerPart = n.toBigInteger()
        if (n.subtract(BigDecimal(integerPart)).compareTo(BigDecimal.ZERO) != 0 || n.signum() < 0) {
            throw IllegalArgumentException("Факториал определен только для целых неотрицательных чисел")
        }
        var result = BigDecimal.ONE
        val limit = integerPart.toLong()
        if (limit > 5000) throw IllegalArgumentException("Значение слишком велико для факториала")
        for (i in 2..limit) {
            result = result.multiply(BigDecimal(i), mc)
        }
        return result
    }
}


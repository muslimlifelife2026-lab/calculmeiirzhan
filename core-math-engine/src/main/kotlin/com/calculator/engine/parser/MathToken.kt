package com.calculator.engine.parser

import java.math.BigDecimal

sealed class MathToken {
    data class Number(val value: BigDecimal) : MathToken()
    data class Variable(val symbol: String) : MathToken()
    data class Func(val name: String) : MathToken()
    
    enum class Assoc { LEFT, RIGHT }
    
    data class Operator(
        val symbol: String,
        val precedence: Int,
        val associativity: Assoc = Assoc.LEFT
    ) : MathToken()
    
    object LeftParenthesis : MathToken() {
        override fun toString() = "("
    }
    
    object RightParenthesis : MathToken() {
        override fun toString() = ")"
    }
    
    object Comma : MathToken() {
        override fun toString() = ","
    }
}

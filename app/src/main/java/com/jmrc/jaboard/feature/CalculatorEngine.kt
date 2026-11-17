package com.jmrc.jaboard.feature

import kotlin.math.*

/** A small deterministic expression evaluator; it never uses eval or JavaScript. */
object CalculatorEngine {
    fun evaluate(input: String): Double {
        val parser = Parser(input.replace('×', '*').replace('÷', '/').replace('−', '-'))
        val result = parser.expression()
        parser.skipSpaces()
        require(parser.atEnd()) { "Unexpected input" }
        require(result.isFinite()) { "Result is not finite" }
        return result
    }

    private class Parser(private val source: String) {
        private var index = 0
        fun atEnd() = index >= source.length
        fun skipSpaces() { while (!atEnd() && source[index].isWhitespace()) index++ }
        private fun eat(char: Char): Boolean { skipSpaces(); if (!atEnd() && source[index] == char) { index++; return true }; return false }

        fun expression(): Double {
            var value = term()
            while (true) {
                value = when {
                    eat('+') -> value + term()
                    eat('-') -> value - term()
                    else -> return value
                }
            }
        }
        private fun term(): Double {
            var value = unary()
            while (true) {
                value = when {
                    eat('*') -> value * unary()
                    eat('/') -> value / unary().also { require(it != 0.0) { "Cannot divide by zero" } }
                    eat('%') -> value / 100.0
                    else -> return value
                }
            }
        }
        private fun unary(): Double {
            if (eat('+')) return unary()
            if (eat('-')) return -unary()
            var value = primary()
            if (eat('^')) value = value.pow(unary())
            return value
        }
        private fun primary(): Double {
            skipSpaces()
            require(!atEnd()) { "Enter an expression" }
            if (eat('(')) {
                val value = expression()
                require(eat(')')) { "Missing closing parenthesis" }
                return value
            }
            val start = index
            if (source[index].isLetter() || source[index] == '√') {
                while (!atEnd() && (source[index].isLetter() || source[index] == '√')) index++
                val token = source.substring(start, index).lowercase()
                if (token == "pi" || token == "π") return PI
                require(eat('(')) { "Use $token(value)" }
                val argument = expression()
                require(eat(')')) { "Missing closing parenthesis" }
                return when (token) {
                    "sqrt", "√" -> sqrt(argument)
                    "sin" -> sin(Math.toRadians(argument))
                    "cos" -> cos(Math.toRadians(argument))
                    "tan" -> tan(Math.toRadians(argument))
                    "ln" -> ln(argument)
                    "log" -> log10(argument)
                    else -> error("Unknown function: $token")
                }
            }
            while (!atEnd() && (source[index].isDigit() || source[index] == '.')) index++
            require(index > start) { "Expected a number" }
            return source.substring(start, index).toDoubleOrNull() ?: error("Invalid number")
        }
    }
}

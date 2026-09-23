package com.abrarshakhi.bricks.core.domain.search

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow

class ExpressionEvaluator {

    fun evaluate(input: String): Double? {
        val source = input
            .replace('×', '*')
            .replace('÷', '/')
            .replace('−', '-')
            .replace(',', '.')
            .filterNot { it.isWhitespace() }
        if (source.isEmpty() || source.none { it in OPERATORS } || source.none { it.isDigit() }) return null
        if (source.any { it !in ALLOWED && !it.isDigit() }) return null
        return runCatching {
            val parser = Parser(source)
            val value = parser.parseExpression()
            if (!parser.isAtEnd || !parser.sawOperator) null else value
        }.getOrNull()?.takeIf { it.isFinite() }
    }

    fun format(value: Double): String {
        if (abs(value) >= 1e15 || (abs(value) < 1e-6 && value != 0.0)) {
            return String.format(Locale.ROOT, "%.6e", value)
        }
        val rounded = BigDecimal(value).round(MathContext(12, RoundingMode.HALF_UP))
        val plain = rounded.stripTrailingZeros().toPlainString()
        return if (plain == "-0") "0" else plain
    }

    private class Parser(private val text: String) {
        private var position = 0
        var sawOperator = false
            private set

        val isAtEnd: Boolean
            get() = position >= text.length

        fun parseExpression(): Double {
            var value = parseTerm()
            while (!isAtEnd) {
                value = when (text[position]) {
                    '+' -> binary { value + parseTerm() }
                    '-' -> binary { value - parseTerm() }
                    else -> return value
                }
            }
            return value
        }

        private fun parseTerm(): Double {
            var value = parseUnary()
            while (!isAtEnd) {
                value = when (text[position]) {
                    '*' -> binary { value * parseUnary() }
                    '/' -> binary {
                        val divisor = parseUnary()
                        require(divisor != 0.0)
                        value / divisor
                    }
                    '%' -> binary {
                        val divisor = parseUnary()
                        require(divisor != 0.0)
                        value % divisor
                    }
                    else -> return value
                }
            }
            return value
        }

        private fun parseUnary(): Double {
            if (!isAtEnd && text[position] == '-') {
                position++
                return -parseUnary()
            }
            if (!isAtEnd && text[position] == '+') {
                position++
                return parseUnary()
            }
            return parsePower()
        }

        private fun parsePower(): Double {
            val base = parsePrimary()
            if (!isAtEnd && text[position] == '^') {
                return binary { base.pow(parseUnary()) }
            }
            return base
        }

        private fun parsePrimary(): Double {
            require(!isAtEnd)
            if (text[position] == '(') {
                position++
                val value = parseExpression()
                require(!isAtEnd && text[position] == ')')
                position++
                return value
            }
            val start = position
            while (!isAtEnd && (text[position].isDigit() || text[position] == '.')) position++
            require(position > start)
            return text.substring(start, position).toDouble()
        }

        private inline fun binary(block: () -> Double): Double {
            position++
            sawOperator = true
            return block()
        }
    }

    private companion object {
        const val OPERATORS = "+-*/%^"
        const val ALLOWED = "+-*/%^()."
    }
}

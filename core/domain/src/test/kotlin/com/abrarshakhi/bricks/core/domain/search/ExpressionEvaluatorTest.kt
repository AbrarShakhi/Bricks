package com.abrarshakhi.bricks.core.domain.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExpressionEvaluatorTest {

    private val evaluator = ExpressionEvaluator()

    private fun eval(input: String): String? = evaluator.evaluate(input)?.let(evaluator::format)

    @Test
    fun `respects precedence and parentheses`() {
        assertEquals("14", eval("2 + 3 * 4"))
        assertEquals("20", eval("(2 + 3) * 4"))
        assertEquals("0.5", eval("1/2"))
    }

    @Test
    fun `handles unary minus and powers`() {
        assertEquals("-4", eval("-2^2"))
        assertEquals("512", eval("2^3^2"))
        assertEquals("0.5", eval("2^-1"))
        assertEquals("-1", eval("3 - -2 * -2"))
    }

    @Test
    fun `accepts typographic operators and decimal commas`() {
        assertEquals("12", eval("3×4"))
        assertEquals("2.5", eval("5÷2"))
        assertEquals("3.75", eval("1,25*3"))
    }

    @Test
    fun `rounds floating point noise`() {
        assertEquals("0.3", eval("0.1+0.2"))
    }

    @Test
    fun `rejects non expressions`() {
        assertNull(eval("42"))
        assertNull(eval("-7"))
        assertNull(eval("calendar"))
        assertNull(eval("2+"))
        assertNull(eval("1/0"))
        assertNull(eval("(1+2"))
        assertNull(eval("1.2.3+4"))
    }
}

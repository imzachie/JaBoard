package com.jmrc.jaboard.feature

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CalculatorEngineTest {
    @Test fun respectsOperatorPrecedenceAndParentheses() {
        assertEquals(14.0, CalculatorEngine.evaluate("2 + 3 * 4"), 1e-9)
        assertEquals(20.0, CalculatorEngine.evaluate("(2 + 3) * 4"), 1e-9)
    }

    @Test fun supportsUnaryAndScientificOperations() {
        assertEquals(-12.0, CalculatorEngine.evaluate("-3 * 4"), 1e-9)
        assertEquals(3.5, CalculatorEngine.evaluate("sqrt(9) + cos(60)"), 1e-9)
        assertEquals(0.5, CalculatorEngine.evaluate("sin(30)"), 1e-9)
        assertEquals(Math.PI, CalculatorEngine.evaluate("π"), 1e-9)
    }

    @Test fun percentIsAUnaryPercentageOperator() {
        assertEquals(0.5, CalculatorEngine.evaluate("50%"), 1e-9)
        assertEquals(2.0, CalculatorEngine.evaluate("10% * 20"), 1e-9)
    }

    @Test fun rejectsInvalidAndNonFiniteExpressions() {
        assertThrows(IllegalArgumentException::class.java) { CalculatorEngine.evaluate("1 / 0") }
        assertThrows(IllegalArgumentException::class.java) { CalculatorEngine.evaluate("(2 + 3") }
        assertThrows(IllegalArgumentException::class.java) { CalculatorEngine.evaluate("1 + nope") }
    }
}

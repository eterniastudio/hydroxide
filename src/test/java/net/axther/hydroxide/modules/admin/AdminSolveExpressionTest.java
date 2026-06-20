package net.axther.hydroxide.modules.admin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminSolveExpressionTest {

    @Test
    void evaluatesArithmeticConstantsAndFunctions() {
        assertEquals(14.0D, AdminSolveExpression.evaluate("2+3*4").orElseThrow(), 0.000001D);
        assertEquals(Math.PI, AdminSolveExpression.evaluate("pi").orElseThrow(), 0.000001D);
        assertEquals(1.0D, AdminSolveExpression.evaluate("sin(pi/2)").orElseThrow(), 0.000001D);
        assertEquals(Math.sqrt(9.0D) + Math.pow(2.0D, 3.0D), AdminSolveExpression.evaluate("sqrt(9)+2^3").orElseThrow(), 0.000001D);
    }

    @Test
    void supportsCmiStyleJoinedEquationTokens() {
        double expected = Math.cos(1.0D) * Math.PI / 0.4D + Math.tan(5.0D);

        assertEquals(expected, AdminSolveExpression.evaluate("cos(1)*pi/0.4+tan(5)").orElseThrow(), 0.000001D);
    }

    @Test
    void rejectsInvalidUnsafeOrNonFiniteExpressions() {
        assertTrue(AdminSolveExpression.evaluate("").isEmpty());
        assertTrue(AdminSolveExpression.evaluate("2+").isEmpty());
        assertTrue(AdminSolveExpression.evaluate("1/0").isEmpty());
        assertTrue(AdminSolveExpression.evaluate("java.lang.Runtime").isEmpty());
        assertTrue(AdminSolveExpression.evaluate("unknown(1)").isEmpty());
    }
}

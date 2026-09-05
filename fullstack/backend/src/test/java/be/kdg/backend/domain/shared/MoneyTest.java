package be.kdg.backend.domain.shared;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MoneyTest {

    @Test
    void zeroIsZero() {
        assertTrue(Money.ZERO.isZero());
        assertFalse(Money.ZERO.isPositive());
        assertEquals("EUR", Money.ZERO.currency());
    }

    @Test
    void ofEurosScalesToTwo() {
        Money m = Money.ofEuros(10.005);
        assertEquals(0, m.amount().compareTo(java.math.BigDecimal.valueOf(10.01)));
    }

    @Test
    void addSameCurrency() {
        Money a = Money.ofEuros(10);
        Money b = Money.ofEuros(5);
        assertEquals(0, a.add(b).amount().compareTo(java.math.BigDecimal.valueOf(15.00)));
    }

    @Test
    void addDifferentCurrencyThrows() {
        Money a = Money.ofEuros(10);
        Money b = new Money(java.math.BigDecimal.TEN, "USD");
        assertThrows(IllegalArgumentException.class, () -> a.add(b));
    }

    @Test
    void multiplyByInt() {
        Money m = Money.ofEuros(2.50).multiply(4);
        assertEquals(0, m.amount().compareTo(java.math.BigDecimal.valueOf(10.00)));
    }

    @Test
    void multiplyNegativeThrows() {
        assertThrows(IllegalArgumentException.class, () -> Money.ofEuros(2.50).multiply(-1));
    }

    @Test
    void equalityUsesCompareToToAvoidScaleIssues() {
        Money a = Money.ofEuros(1);
        Money b = new Money(java.math.BigDecimal.valueOf(1.00), "EUR");
        assertEquals(a, b);
    }
}
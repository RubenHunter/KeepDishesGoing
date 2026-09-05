package be.kdg.backend.domain.shared;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class QuantityTest {

    @Test
    void positiveQuantityCreates() {
        Quantity q = Quantity.of(2);
        assertEquals(2, q.value());
    }

    @Test
    void zeroThrows() {
        assertThrows(IllegalArgumentException.class, () -> Quantity.of(0));
    }

    @Test
    void negativeThrows() {
        assertThrows(IllegalArgumentException.class, () -> Quantity.of(-3));
    }

    @Test
    void plusAdds() {
        assertEquals(5, Quantity.of(2).plus(Quantity.of(3)).value());
    }

    @Test
    void minusRejectsNonPositive() {
        assertThrows(IllegalArgumentException.class, () -> Quantity.of(2).minus(Quantity.of(2)));
    }

    @Test
    void minusPos() {
        assertEquals(2, Quantity.of(5).minus(Quantity.of(3)).value());
    }
}
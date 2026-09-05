package be.kdg.backend.domain.shared;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AddressTest {

    @Test
    void createsWithAllFields() {
        Address a = new Address("Langestraat", "1", "2000", "Antwerpen", "BE");
        assertEquals("Langestraat", a.street());
        assertEquals("Langestraat 1, 2000 Antwerpen, BE", a.singleLine());
    }

    @Test
    void rejectsBlank() {
        assertThrows(IllegalArgumentException.class, () -> new Address("", "1", "2000", "Antwerpen", "BE"));
        assertThrows(IllegalArgumentException.class, () -> new Address("Straat", "", "2000", "Antwerpen", "BE"));
        assertThrows(IllegalArgumentException.class, () -> new Address("Straat", "1", null, "Antwerpen", "BE"));
    }
}
package be.kdg.backend.domain.shared;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EmailTest {

    @Test
    void acceptsValid() {
        assertDoesNotThrow(() -> new Email("ruben@example.com"));
    }

    @Test
    void rejectsMissingLocal() {
        assertThrows(IllegalArgumentException.class, () -> new Email("@example.com"));
    }

    @Test
    void rejectsBlank() {
        assertThrows(IllegalArgumentException.class, () -> new Email(""));
    }

    @Test
    void rejectsBadFormat() {
        assertThrows(IllegalArgumentException.class, () -> new Email("no-at-symbol"));
        assertThrows(IllegalArgumentException.class, () -> new Email("foo@bar"));
    }
}
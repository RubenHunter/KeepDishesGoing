package be.kdg.backend.domain.driver;

import be.kdg.backend.domain.shared.DeliveryId;
import be.kdg.backend.domain.shared.DeliveryPersonId;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class DeliveryPersonTest {

    private DeliveryPerson newDriver() {
        return new DeliveryPerson(DeliveryPersonId.generate(), "Ruben", "BICYCLE", true);
    }

    @Test
    void newDriverHasNoAssignment() {
        DeliveryPerson dp = newDriver();
        assertTrue(dp.canAccept());
        assertFalse(dp.hasActiveAssignment());
        assertTrue(dp.available());
    }

    @Test
    void assignDeliverySetsFields() {
        DeliveryPerson dp = newDriver();
        dp.assignDelivery(DeliveryId.generate(), LocalDateTime.now());
        assertTrue(dp.hasActiveAssignment());
        assertFalse(dp.canAccept());
        assertFalse(dp.available());
        assertNotNull(dp.assignmentTime());
    }

    @Test
    void doubleAssignThrows() {
        DeliveryPerson dp = newDriver();
        dp.assignDelivery(DeliveryId.generate(), LocalDateTime.now());
        assertThrows(DeliveryPersonAlreadyAssignedException.class,
                () -> dp.assignDelivery(DeliveryId.generate(), LocalDateTime.now()));
    }

    @Test
    void releaseRestoresAvailability() {
        DeliveryPerson dp = newDriver();
        dp.assignDelivery(DeliveryId.generate(), LocalDateTime.now());
        dp.release(LocalDateTime.now());
        assertFalse(dp.hasActiveAssignment());
        assertTrue(dp.available());
        assertTrue(dp.canAccept());
    }

    @Test
    void cannotMarkUnavailableWhileBusy() {
        DeliveryPerson dp = newDriver();
        dp.assignDelivery(DeliveryId.generate(), LocalDateTime.now());
        assertThrows(IllegalStateException.class, () -> dp.updateAvailability(false, LocalDateTime.now()));
    }

    @Test
    void constructorRejectsBlankName() {
        assertThrows(IllegalArgumentException.class,
                () -> new DeliveryPerson(DeliveryPersonId.generate(), "", "BIKE", true));
        assertThrows(IllegalArgumentException.class,
                () -> new DeliveryPerson(DeliveryPersonId.generate(), "Ruben", "", true));
    }

    @Test
    void rehydrateValidatesConsistency() {
        DeliveryPersonId id = DeliveryPersonId.generate();
        assertThrows(IllegalStateException.class, () ->
                DeliveryPerson.rehydrate(id, "X", "BIKE", true, null, LocalDateTime.now(), null));
    }

    @Test
    void emailIsStoredAndTrimmed() {
        DeliveryPerson dp = new DeliveryPerson(DeliveryPersonId.generate(), "Ruben", "  ruben@kdg.dev  ", "BIKE", true);
        assertEquals("ruben@kdg.dev", dp.email());
    }
}
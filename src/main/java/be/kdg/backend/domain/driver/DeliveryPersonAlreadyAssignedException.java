package be.kdg.backend.domain.driver;

import be.kdg.backend.domain.shared.DeliveryPersonId;

/** US31: courier already has an active assignment. */
public class DeliveryPersonAlreadyAssignedException extends RuntimeException {
    public DeliveryPersonAlreadyAssignedException(DeliveryPersonId id) {
        super("Delivery person " + id.value() + " already has an active assignment (US31)");
    }
}
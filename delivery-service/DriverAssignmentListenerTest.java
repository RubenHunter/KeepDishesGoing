package be.kdg.backend.application.messaging;

import be.kdg.backend.application.DeliveryPersonService;
import be.kdg.backend.domain.shared.DeliveryId;
import be.kdg.backend.domain.shared.DeliveryPersonId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DriverAssignmentListenerTest {

    @Mock DeliveryPersonService driverService;

    @Test
    void onCourierAssignedDelegatesToDriverService() {
        DriverAssignmentListener listener = new DriverAssignmentListener(driverService);
        InternalEvents.CourierAssignedEvent event = new InternalEvents.CourierAssignedEvent(
                DeliveryId.generate(), DeliveryPersonId.generate(), LocalDateTime.now());

        listener.onCourierAssigned(event);

        verify(driverService).assignDriver(event.deliveryId(), event.driverId(), event.at());
    }

    @Test
    void onCourierReleasedDelegatesToDriverService() {
        DriverAssignmentListener listener = new DriverAssignmentListener(driverService);
        InternalEvents.CourierReleasedEvent event = new InternalEvents.CourierReleasedEvent(
                DeliveryPersonId.generate(), LocalDateTime.now());

        listener.onCourierReleased(event);

        verify(driverService).releaseDriver(event.driverId(), event.at());
    }
}

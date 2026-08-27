package be.kdg.backend.application;

import be.kdg.backend.domain.driver.DeliveryPerson;
import be.kdg.backend.domain.driver.DeliveryPersonNotFoundException;
import be.kdg.backend.domain.driver.DeliveryPersonRepository;
import be.kdg.backend.domain.shared.DeliveryId;
import be.kdg.backend.domain.shared.DeliveryPersonId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryPersonServiceTest {

    @Mock DeliveryPersonRepository driverRepository;

    private DeliveryPersonService sut;

    private final LocalDateTime now = LocalDateTime.of(2024, 1, 1, 12, 0);

    @BeforeEach
    void setUp() {
        sut = new DeliveryPersonService(driverRepository);
    }

    @Test
    void registerDriverPersists() {
        when(driverRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        DeliveryPersonId id = sut.registerDriver("Ruben", "BICYCLE");

        assertThat(id).isNotNull();
        verify(driverRepository).save(any());
    }

    @Test
    void registerDriverIfAbsentSkipsExisting() {
        DeliveryPersonId id = DeliveryPersonId.generate();
        given(driverRepository.findById(id)).willReturn(Optional.of(
                new DeliveryPerson(id, "Ruben", "BICYCLE", true)));

        sut.registerDriverIfAbsent(id, "Ruben", "BICYCLE");

        verify(driverRepository, never()).save(any());
    }

    @Test
    void registerDriverIfAbsentPersistsNew() {
        DeliveryPersonId id = DeliveryPersonId.generate();
        given(driverRepository.findById(id)).willReturn(Optional.empty());

        sut.registerDriverIfAbsent(id, "Ruben", "BICYCLE");

        verify(driverRepository).save(any());
    }

    @Test
    void getThrowsWhenMissing() {
        DeliveryPersonId id = DeliveryPersonId.generate();
        given(driverRepository.findById(id)).willReturn(Optional.empty());

        assertThrows(DeliveryPersonNotFoundException.class, () -> sut.get(id));
    }

    @Test
    void assignDriverAssignsAndPersists() {
        DeliveryPerson dp = new DeliveryPerson(DeliveryPersonId.generate(), "Ruben", "BICYCLE", true);
        given(driverRepository.findById(dp.id())).willReturn(Optional.of(dp));

        sut.assignDriver(DeliveryId.generate(), dp.id(), now);

        assertThat(dp.assignedDeliveryId()).isNotNull();
        verify(driverRepository).save(dp);
    }

    @Test
    void releaseDriverClearsAssignment() {
        DeliveryPerson dp = new DeliveryPerson(DeliveryPersonId.generate(), "Ruben", "BICYCLE", true);
        dp.assignDelivery(DeliveryId.generate(), now);
        given(driverRepository.findById(dp.id())).willReturn(Optional.of(dp));

        sut.releaseDriver(dp.id(), now);

        verify(driverRepository).save(dp);
    }

    @Test
    void setAvailabilityPersists() {
        DeliveryPerson dp = new DeliveryPerson(DeliveryPersonId.generate(), "Ruben", "BICYCLE", true);
        given(driverRepository.findById(dp.id())).willReturn(Optional.of(dp));

        sut.setAvailability(dp.id(), false, now);

        verify(driverRepository).save(dp);
    }
}

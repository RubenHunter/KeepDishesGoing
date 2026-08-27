package be.kdg.backend.application;

import be.kdg.backend.application.messaging.InternalEvents;
import be.kdg.backend.domain.payout.PayoutPolicy;
import be.kdg.backend.domain.payout.PayoutRepository;
import be.kdg.backend.domain.shared.DeliveryId;
import be.kdg.backend.domain.shared.DeliveryPersonId;
import be.kdg.backend.domain.shared.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayoutServiceTest {

    @Mock PayoutRepository payoutRepository;

    private PayoutPolicy policy;
    private PayoutService sut;

    @BeforeEach
    void setUp() {
        policy = new PayoutPolicy() {
            @Override public Money baseFee() { return Money.ofEuros(3.0); }
            @Override public Money perMinuteFee() { return Money.ofEuros(0.3); }
            @Override public int minMinutes() { return 5; }
            @Override public int maxMinutes() { return 30; }
        };
        sut = new PayoutService(payoutRepository, policy);
    }

    @Test
    void onDeliveryDeliveredComputesAndSavesPayout() {
        InternalEvents.DeliveryDeliveredEvent event = new InternalEvents.DeliveryDeliveredEvent(
                DeliveryId.generate(), DeliveryPersonId.generate(),
                LocalDateTime.of(2024, 1, 1, 10, 0), LocalDateTime.of(2024, 1, 1, 10, 15));

        sut.onDeliveryDelivered(event);

        verify(payoutRepository).save(any());
    }

    @Test
    void onDeliveryDeliveredSkipsWhenTimestampsMissing() {
        InternalEvents.DeliveryDeliveredEvent event = new InternalEvents.DeliveryDeliveredEvent(
                DeliveryId.generate(), DeliveryPersonId.generate(), null, null);

        sut.onDeliveryDelivered(event);

        verify(payoutRepository, never()).save(any());
    }

    @Test
    void summarySumsPayoutRows() {
        DeliveryPersonId driver = DeliveryPersonId.generate();
        given(payoutRepository.findByDriverId(driver)).willReturn(List.of());
        Money total = sut.summary(driver).total();
        assertThat(total.isZero()).isTrue();
    }

    @Test
    void allPayoutsDelegatesToRepository() {
        given(payoutRepository.findByDateRange(any(), any())).willReturn(List.of());
        assertThat(sut.allPayouts(LocalDateTime.now(), LocalDateTime.now())).isEmpty();
    }
}

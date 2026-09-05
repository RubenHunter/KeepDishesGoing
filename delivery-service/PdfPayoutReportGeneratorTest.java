package be.kdg.backend.infrastructure.report;

import be.kdg.backend.application.PayoutService;
import be.kdg.backend.domain.driver.DeliveryPerson;
import be.kdg.backend.domain.driver.DeliveryPersonRepository;
import be.kdg.backend.domain.payout.Payout;
import be.kdg.backend.domain.payout.PayoutPolicy;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PdfPayoutReportGeneratorTest {

    @Mock PayoutService payoutService;
    @Mock DeliveryPersonRepository driverRepository;

    private PdfPayoutReportGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new PdfPayoutReportGenerator(payoutService, driverRepository);
    }

    @Test
    void renderProducesPdfBytesWhenNoPayouts() {
        given(payoutService.allPayouts(any(), any())).willReturn(List.of());

        byte[] pdf = generator.render(LocalDateTime.of(2024, 1, 1, 0, 0), LocalDateTime.of(2024, 1, 31, 23, 59));

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
    }

    /** Driver name + email are resolved from the courier record and rendered into the PDF. */
    @Test
    void renderResolvesDriverNameAndEmail() {
        DeliveryPersonId driverId = DeliveryPersonId.generate();
        given(driverRepository.findById(driverId)).willReturn(Optional.of(
                new DeliveryPerson(driverId, "Ruben", "ruben@kdg.dev", "BICYCLE", true)));
        Payout payout = Payout.compute(DeliveryId.generate(), driverId,
                LocalDateTime.of(2026, 8, 26, 14, 50, 15),
                LocalDateTime.of(2026, 8, 26, 14, 51, 17),
                new FixedPayoutPolicy());
        given(payoutService.allPayouts(any(), any())).willReturn(List.of(payout));

        byte[] pdf = generator.render(LocalDateTime.of(2026, 8, 1, 0, 0), LocalDateTime.of(2026, 8, 31, 23, 59));

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
    }

    static final class FixedPayoutPolicy implements PayoutPolicy {
        public Money baseFee() { return Money.ofEuros(1.50); }
        public Money perMinuteFee() { return Money.ofEuros(0.45); }
        public int minMinutes() { return 5; }
        public int maxMinutes() { return 60; }
    }
}

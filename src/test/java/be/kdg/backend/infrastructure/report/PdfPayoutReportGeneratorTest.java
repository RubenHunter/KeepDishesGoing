package be.kdg.backend.infrastructure.report;

import be.kdg.backend.application.PayoutService;
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

@ExtendWith(MockitoExtension.class)
class PdfPayoutReportGeneratorTest {

    @Mock PayoutService payoutService;

    private PdfPayoutReportGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new PdfPayoutReportGenerator(payoutService);
    }

    @Test
    void renderProducesPdfBytesWhenNoPayouts() {
        given(payoutService.allPayouts(any(), any())).willReturn(List.of());

        byte[] pdf = generator.render(LocalDateTime.of(2024, 1, 1, 0, 0), LocalDateTime.of(2024, 1, 31, 23, 59));

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
    }
}

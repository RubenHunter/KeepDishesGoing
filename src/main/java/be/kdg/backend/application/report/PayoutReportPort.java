package be.kdg.backend.application.report;

import java.time.LocalDateTime;

/**
 * Port (interface) for rendering the platform-wide payout report PDF (US38).
 * Implemented in {@code infrastructure.report.PdfPayoutReportGenerator}.
 */
public interface PayoutReportPort {
    byte[] render(LocalDateTime from, LocalDateTime to);
}
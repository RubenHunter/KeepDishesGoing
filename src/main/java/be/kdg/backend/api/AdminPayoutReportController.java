package be.kdg.backend.api;

import be.kdg.backend.application.PayoutService;
import be.kdg.backend.application.report.PayoutReportPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * US38 — platform admin endpoint returning a PDF payouts report for a date range.
 * Secured with the {@code admin} role.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminPayoutReportController {

    private final PayoutReportPort reportPort;
    private final PayoutService payoutService;

    @GetMapping(value = "/payouts/report", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAuthority('admin')")
    public ResponseEntity<byte[]> report(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from must be <= to");
        }
        byte[] pdf = reportPort.render(from, to);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", "attachment; filename=payout-report.pdf")
                .body(pdf);
    }
}
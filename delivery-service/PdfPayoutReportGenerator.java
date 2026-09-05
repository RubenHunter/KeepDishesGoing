package be.kdg.backend.infrastructure.report;

import be.kdg.backend.application.PayoutService;
import be.kdg.backend.application.report.PayoutReportPort;
import be.kdg.backend.domain.driver.DeliveryPerson;
import be.kdg.backend.domain.driver.DeliveryPersonRepository;
import be.kdg.backend.domain.payout.Payout;
import be.kdg.backend.domain.shared.DeliveryPersonId;
import be.kdg.backend.domain.shared.Money;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * US38 — generates the payouts PDF report using OpenHTMLtoPDF + Thymeleaf.
 *
 * Layout: per-driver grouped summary (name + email) + total + detailed list of payouts in the
 * chosen date range. Timestamps are formatted to a human-readable {@code yyyy-MM-dd HH:mm}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PdfPayoutReportGenerator implements PayoutReportPort {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final PayoutService payoutService;
    private final DeliveryPersonRepository driverRepository;

    @Override
    public byte[] render(LocalDateTime from, LocalDateTime to) {
        log.info("Rendering payout report from {} to {}", from, to);
        List<Payout> rows = payoutService.allPayouts(from, to);

        // Group by driver
        Map<UUID, List<Payout>> byDriver = rows.stream()
                .collect(Collectors.groupingBy(p -> p.driverId().value()));

        List<DriverSummary> driverSummaries = byDriver.entrySet().stream()
                .map(e -> {
                    UUID driverId = e.getKey();
                    List<Payout> list = e.getValue();
                    Money driverTotal = list.stream().map(Payout::total).reduce(Money.ofEuros(0), Money::add);
                    DeliveryPerson person = driverRepository.findById(DeliveryPersonId.of(driverId)).orElse(null);
                    String name = person == null ? "Unknown" : person.name();
                    String email = person == null ? "" : person.email();
                    List<RowView> rowViews = list.stream()
                            .sorted(Comparator.comparing(Payout::deliveredAt))
                            .map(RowView::from)
                            .toList();
                    return new DriverSummary(driverId, name, email, list.size(), driverTotal, rowViews);
                })
                .sorted(Comparator.comparing(DriverSummary::name))
                .toList();

        Money grandTotal = driverSummaries.stream()
                .map(d -> d.total)
                .reduce(Money.ofEuros(0), Money::add);

        Context ctx = new Context();
        ctx.setVariable("from", from.format(TIME));
        ctx.setVariable("to", to.format(TIME));
        ctx.setVariable("drivers", driverSummaries);
        ctx.setVariable("grandTotal", formatMoney(grandTotal));

        TemplateEngine engine = newTemplateEngine();
        String html = engine.process("payout-report", ctx);

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, "classpath:/templates/");
            builder.toStream(os);
            builder.run();
            log.info("Payout report generated: {} bytes", os.size());
            return os.toByteArray();
        } catch (Exception e) {
            log.error("PDF render failed: {}", e.getMessage(), e);
            throw new RuntimeException("PDF render failed", e);
        }
    }

    private static String formatMoney(Money m) {
        return "€ " + m.amount().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private TemplateEngine newTemplateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        TemplateEngine engine = new TemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    /** Pre-formatted payout row for the report template. */
    public record RowView(String payoutId, String deliveryId, String readyAt, String deliveredAt,
                          int billableMinutes, String total) {
        public static RowView from(Payout p) {
            return new RowView(
                    p.id().value().toString(),
                    p.deliveryId().value().toString(),
                    p.readyAt().format(TIME),
                    p.deliveredAt().format(TIME),
                    p.billableMinutes(),
                    formatMoney(p.total())
            );
        }
    }

    public record DriverSummary(UUID driverId, String name, String email, int numDeliveries,
                                Money total, List<RowView> rows) {}
}

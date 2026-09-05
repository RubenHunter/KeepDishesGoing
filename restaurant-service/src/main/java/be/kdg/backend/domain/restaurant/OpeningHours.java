package be.kdg.backend.domain.restaurant;

import be.kdg.backend.domain.ValidationException;
import org.jmolecules.ddd.annotation.ValueObject;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Value object for a restaurant's weekly opening-hours schedule (US11/US13/US14).
 *
 * <p>Parses the stored free-text format, e.g. {@code "Mon–Fri 09:00–22:00; Sat–Sun 10:00–23:00"}
 * or {@code "Mon-Sun 11:00-23:00"}. Hyphens and en/em-dashes are both accepted as range separators,
 * and multiple day groups are separated by {@code ';'}. Closing times before opening times are
 * treated as wrapping past midnight (e.g. {@code "Mon-Sun 10:00-01:00"}).
 *
 * <p>Immutable and framework-free; parse failures throw {@link ValidationException}.
 */
@ValueObject
public final class OpeningHours {

    private static final Pattern SEGMENT = Pattern.compile(
            "(?i)^\\s*([a-z]{3})\\s*(?:[-\\u2013\\u2014]\\s*([a-z]{3}))?\\s+"
                    + "(\\d{1,2}):(\\d{2})\\s*[-\\u2013\\u2014]\\s*(\\d{1,2}):(\\d{2})\\s*$");

    /** One contiguous day/time window, e.g. Mon–Fri 09:00–22:00. */
    public record Window(DayOfWeek fromDay, DayOfWeek toDay, LocalTime opens, LocalTime closes) {}

    private final List<Window> windows;
    private final String raw;

    private OpeningHours(List<Window> windows, String raw) {
        this.windows = List.copyOf(windows);
        this.raw = raw;
    }

    public static OpeningHours parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ValidationException("Opening hours must not be blank");
        }
        List<Window> windows = new ArrayList<>();
        for (String segment : raw.split(";")) {
            windows.add(parseWindow(segment));
        }
        if (windows.isEmpty()) {
            throw new ValidationException("Opening hours has no valid windows: " + raw);
        }
        return new OpeningHours(windows, raw.trim());
    }

    /** Non-throwing variant — returns empty on malformed input. */
    public static Optional<OpeningHours> tryParse(String raw) {
        try {
            return Optional.of(parse(raw));
        } catch (ValidationException e) {
            return Optional.empty();
        }
    }

    private static Window parseWindow(String segment) {
        Matcher m = SEGMENT.matcher(segment);
        if (!m.matches()) {
            throw new ValidationException("Invalid opening-hours segment: '" + segment.trim() + "'");
        }
        DayOfWeek from = dayOf(m.group(1));
        DayOfWeek to = m.group(2) == null ? from : dayOf(m.group(2));
        if (to.getValue() < from.getValue()) {
            throw new ValidationException("Day range must not wrap the week: '" + segment.trim() + "'");
        }
        LocalTime opens = timeOf(Integer.parseInt(m.group(3)), Integer.parseInt(m.group(4)));
        LocalTime closes = timeOf(Integer.parseInt(m.group(5)), Integer.parseInt(m.group(6)));
        if (opens.equals(closes)) {
            throw new ValidationException("Opening and closing time must differ: '" + segment.trim() + "'");
        }
        return new Window(from, to, opens, closes);
    }

    private static DayOfWeek dayOf(String token) {
        return switch (token.toLowerCase()) {
            case "mon" -> DayOfWeek.MONDAY;
            case "tue" -> DayOfWeek.TUESDAY;
            case "wed" -> DayOfWeek.WEDNESDAY;
            case "thu" -> DayOfWeek.THURSDAY;
            case "fri" -> DayOfWeek.FRIDAY;
            case "sat" -> DayOfWeek.SATURDAY;
            case "sun" -> DayOfWeek.SUNDAY;
            default -> throw new ValidationException("Unknown day: " + token);
        };
    }

    private static LocalTime timeOf(int hour, int minute) {
        if (hour > 23 || minute > 59) {
            throw new ValidationException("Invalid time: " + hour + ":" + minute);
        }
        return LocalTime.of(hour, minute);
    }

    /** True when the schedule is open at the given date-time. */
    public boolean isOpenAt(LocalDateTime at) {
        DayOfWeek day = at.getDayOfWeek();
        LocalTime time = at.toLocalTime();
        for (Window w : windows) {
            if (!covers(w, day)) {
                continue;
            }
            if (w.closes().isAfter(w.opens())) {
                if (!time.isBefore(w.opens()) && time.isBefore(w.closes())) {
                    return true;
                }
            } else {
                // midnight wrap: open from opens..midnight and midnight..closes
                if (!time.isBefore(w.opens()) || time.isBefore(w.closes())) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Closing moment of the window currently open at {@code at}; empty when closed. */
    public Optional<LocalDateTime> closingAt(LocalDateTime at) {
        DayOfWeek day = at.getDayOfWeek();
        LocalTime time = at.toLocalTime();
        for (Window w : windows) {
            if (!covers(w, day)) {
                continue;
            }
            if (w.closes().isAfter(w.opens())) {
                if (!time.isBefore(w.opens()) && time.isBefore(w.closes())) {
                    return Optional.of(at.toLocalDate().atTime(w.closes()));
                }
            } else if (!time.isBefore(w.opens())) {
                return Optional.of(at.toLocalDate().plusDays(1).atTime(w.closes()));
            } else if (time.isBefore(w.closes())) {
                return Optional.of(at.toLocalDate().atTime(w.closes()));
            }
        }
        return Optional.empty();
    }

    /** Earliest opening strictly after {@code at}, scanning up to 8 days ahead. */
    public Optional<LocalDateTime> nextOpeningAfter(LocalDateTime at) {
        LocalDateTime candidate = null;
        for (int offset = 0; offset <= 7; offset++) {
            LocalDate date = at.toLocalDate().plusDays(offset);
            for (Window w : windows) {
                if (!covers(w, date.getDayOfWeek())) {
                    continue;
                }
                LocalDateTime opens = date.atTime(w.opens());
                if (opens.isAfter(at) && (candidate == null || opens.isBefore(candidate))) {
                    candidate = opens;
                }
            }
        }
        return Optional.ofNullable(candidate);
    }

    private static boolean covers(Window w, DayOfWeek day) {
        return day.getValue() >= w.fromDay().getValue() && day.getValue() <= w.toDay().getValue();
    }

    public List<Window> windows() { return windows; }
    public String raw() { return raw; }
}

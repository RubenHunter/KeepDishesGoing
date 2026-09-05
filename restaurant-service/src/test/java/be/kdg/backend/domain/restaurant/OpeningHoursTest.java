package be.kdg.backend.domain.restaurant;

import be.kdg.backend.domain.ValidationException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * US11/US13/US14 — parsing + open/close boundaries of {@link OpeningHours}.
 * Dates chosen so 2024-01-01 is a Monday.
 */
class OpeningHoursTest {

    private static LocalDateTime at(int dayOfMonth, int hour, int minute) {
        return LocalDateTime.of(2024, 1, dayOfMonth, hour, minute);
    }

    @Test
    void parsesSingleDayRangeHyphen() {
        OpeningHours h = OpeningHours.parse("Mon-Sun 11:00-23:00");
        assertTrue(h.isOpenAt(at(1, 11, 0)));
        assertTrue(h.isOpenAt(at(7, 22, 59)));
        assertFalse(h.isOpenAt(at(1, 10, 59)));
        assertFalse(h.isOpenAt(at(1, 23, 0)));
    }

    @Test
    void parsesWeekdayRange() {
        OpeningHours h = OpeningHours.parse("Tue-Sun 12:00-21:00");
        assertFalse(h.isOpenAt(at(1, 12, 0)));   // Monday closed
        assertTrue(h.isOpenAt(at(2, 12, 0)));    // Tuesday opens
        assertFalse(h.isOpenAt(at(2, 11, 59)));
        assertFalse(h.isOpenAt(at(7, 21, 0)));   // Sunday closes at 21:00
    }

    @Test
    void parsesMultipleSemicolonGroupsWithEnDash() {
        OpeningHours h = OpeningHours.parse("Mon\u2013Fri 09:00\u201322:00; Sat\u2013Sun 10:00\u201323:00");
        assertTrue(h.isOpenAt(at(5, 21, 0)));    // Friday evening (weekday group)
        assertFalse(h.isOpenAt(at(5, 22, 0)));   // Friday closed after 22:00
        assertTrue(h.isOpenAt(at(6, 10, 0)));    // Saturday opens 10:00
        assertTrue(h.isOpenAt(at(7, 22, 59)));   // Sunday open until 23:00
        assertFalse(h.isOpenAt(at(7, 23, 0)));
    }

    @Test
    void handlesMidnightWrap() {
        OpeningHours h = OpeningHours.parse("Mon-Sun 10:00-01:00");
        assertTrue(h.isOpenAt(at(1, 0, 30)));    // after midnight, still open from previous day
        assertFalse(h.isOpenAt(at(1, 9, 59)));
        assertTrue(h.isOpenAt(at(1, 10, 0)));
        assertTrue(h.isOpenAt(at(1, 23, 59)));
        assertFalse(h.isOpenAt(at(2, 1, 0)));    // closes at 01:00
    }

    @Test
    void closingAtIsEndOfCurrentWindow() {
        OpeningHours h = OpeningHours.parse("Mon-Sun 11:00-23:00");
        assertEquals(at(1, 23, 0), h.closingAt(at(1, 12, 0)).orElseThrow());
    }

    @Test
    void closingAtAcrossMidnightIsNextDay() {
        OpeningHours h = OpeningHours.parse("Mon-Sun 10:00-01:00");
        assertEquals(at(2, 1, 0), h.closingAt(at(1, 23, 0)).orElseThrow());
        assertEquals(at(1, 1, 0), h.closingAt(at(1, 0, 30)).orElseThrow());
    }

    @Test
    void nextOpeningAfterWhenClosed() {
        OpeningHours h = OpeningHours.parse("Mon-Fri 09:00-22:00; Sat-Sun 10:00-23:00");
        assertEquals(at(2, 9, 0), h.nextOpeningAfter(at(1, 22, 30)).orElseThrow()); // Mon eve → Tue 09:00
        assertEquals(at(6, 10, 0), h.nextOpeningAfter(at(5, 22, 30)).orElseThrow()); // Fri eve → Sat 10:00
    }

    @Test
    void parseRejectsMalformedInput() {
        assertThrows(ValidationException.class, () -> OpeningHours.parse(""));
        assertThrows(ValidationException.class, () -> OpeningHours.parse("garbage"));
        assertThrows(ValidationException.class, () -> OpeningHours.parse("Mon-Fri"));           // no time
        assertThrows(ValidationException.class, () -> OpeningHours.parse("Mon 25:00-26:00"));   // bad hour
        assertThrows(ValidationException.class, () -> OpeningHours.parse("Xyz 09:00-10:00"));   // unknown day
        assertThrows(ValidationException.class, () -> OpeningHours.parse("Sun-Mon 09:00-10:00")); // wraps week
    }

    @Test
    void tryParseReturnsEmptyOnMalformed() {
        assertTrue(OpeningHours.tryParse("nonsense").isEmpty());
        assertTrue(OpeningHours.tryParse("Mon-Sun 11:00-23:00").isPresent());
    }
}

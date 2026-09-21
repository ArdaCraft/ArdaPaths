package space.ajcool.ardapaths.core.data;

import org.junit.jupiter.api.Test;
import space.ajcool.ardapaths.screens.widgets.TextValidationError;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for date-time conversion on the marker time timeline.
 */
class TimeOfDayTest {

    /**
     * Verifies the chosen legacy anchor maps noon to the expected absolute tick.
     *
     * @throws TextValidationError when the fixture date is malformed
     */
    @Test
    void parsesLegacyAnchorNoon() throws TextValidationError {
        assertEquals(5_910_000L, TimeOfDay.parse("04/09/3006 12:00"));
    }

    /**
     * Verifies formatted date-times can be parsed back to the same tick.
     *
     * @throws TextValidationError when the fixture date is malformed
     */
    @Test
    void formatParseRoundTrips() throws TextValidationError {
        long ticks = TimeOfDay.parse("25/12/3006 18:30");

        assertEquals(ticks, TimeOfDay.parse(TimeOfDay.format(ticks)));
    }

    /**
     * Verifies blank text clears the marker time.
     *
     * @throws TextValidationError when blank text is rejected
     */
    @Test
    void blankParsesToUnset() throws TextValidationError {
        assertEquals(TimeOfDay.UNSET, TimeOfDay.parse(""));
    }

    /**
     * Verifies invalid dates and old time-only text are rejected.
     */
    @Test
    void rejectsMalformedValues() {
        assertThrows(TextValidationError.class, () -> TimeOfDay.parse("31/02/3006 12:00"));
        assertThrows(TextValidationError.class, () -> TimeOfDay.parse("04/09/3006 24:00"));
        assertThrows(TextValidationError.class, () -> TimeOfDay.parse("12:00"));
    }

    /**
     * Verifies world day time can be anchored on the configured baseline date.
     */
    @Test
    void fromDayTimeAnchorsWorldTimeOnBaselineDate() {
        assertEquals(5_910_000L, TimeOfDay.fromDayTime(TimeOfDay.DEFAULT_BASELINE_DATE, 6000));
    }

    /**
     * Verifies world day time wraps to the selected date instead of carrying the world's day count.
     */
    @Test
    void fromDayTimeWrapsWorldTimeToSelectedDate() {
        assertEquals(5_910_000L, TimeOfDay.fromDayTime(LocalDate.of(3006, 9, 4), 54_000));
    }

    /**
     * Verifies snapping preserves the absolute day.
     *
     * @throws TextValidationError when the fixture date is malformed
     */
    @Test
    void snapPreservesDay() throws TextValidationError {
        long base = TimeOfDay.parse("04/09/3006 12:00");
        long snapped = TimeOfDay.snap(base + 60L);

        assertEquals(Math.floorDiv(base, TimeOfDay.DAY_TICKS), Math.floorDiv(snapped, TimeOfDay.DAY_TICKS));
        assertEquals(TimeOfDay.parse(TimeOfDay.format(base + 60L)), snapped);
    }

    /**
     * Verifies clock times before dawn belong to the calendar day that follows midnight.
     *
     * @throws TextValidationError when the fixture date is malformed
     */
    @Test
    void preDawnTimesKeepTheirCalendarDay() throws TextValidationError {
        long ticks = TimeOfDay.parse("06/09/3006 05:45");

        assertEquals("06/09/3006 05:45", TimeOfDay.format(ticks));
        assertEquals(ticks, TimeOfDay.parse(TimeOfDay.format(ticks)));
        assertEquals(ticks, TimeOfDay.snap(ticks));
        assertEquals("06/09/3006 00:00", TimeOfDay.format(TimeOfDay.parse("06/09/3006 00:00")));
        assertEquals("06/09/3006 06:00", TimeOfDay.format(TimeOfDay.parse("06/09/3006 06:00")));
    }

    /**
     * Verifies an evening marker and the next morning's marker are less than one night apart.
     *
     * @throws TextValidationError when the fixture dates are malformed
     */
    @Test
    void nightSegmentSpansOnlyTheNight() throws TextValidationError {
        long evening = TimeOfDay.parse("05/09/3006 19:00");
        long morning = TimeOfDay.parse("06/09/3006 05:45");

        assertEquals(10_750L, morning - evening);
    }

    /**
     * Verifies every minute of the day round trips through formatting.
     *
     * @throws TextValidationError when a generated date-time is rejected
     */
    @Test
    void everyMinuteOfDayRoundTrips() throws TextValidationError {
        long midnight = TimeOfDay.parse("06/09/3006 00:00");

        for (int minute = 0; minute < 1440; minute++) {
            long ticks = TimeOfDay.parse(String.format("06/09/3006 %02d:%02d", minute / 60, minute % 60));

            assertEquals(ticks, TimeOfDay.parse(TimeOfDay.format(ticks)));
            assertEquals(ticks, TimeOfDay.snap(ticks));
            assertEquals(midnight + Math.round(minute * TimeOfDay.DAY_TICKS / 1440.0D), ticks);
        }
    }

    /**
     * Verifies dates before the epoch can be represented and formatted.
     *
     * @throws TextValidationError when the fixture date is malformed
     */
    @Test
    void preEpochDatesRoundTrip() throws TextValidationError {
        long ticks = TimeOfDay.parse("19/12/1989 06:00");

        assertEquals("19/12/1989 06:00", TimeOfDay.format(ticks));
    }
}

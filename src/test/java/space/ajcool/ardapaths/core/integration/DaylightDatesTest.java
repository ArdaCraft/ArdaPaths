package space.ajcool.ardapaths.core.integration;

import org.junit.jupiter.api.Test;
import space.ajcool.ardapaths.core.data.TimeOfDay;
import space.ajcool.ardapaths.screens.widgets.TextValidationError;

import java.time.LocalDate;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for DaylightChangerStruggle date-frame conversion.
 */
class DaylightDatesTest {
    /**
     * UTC timezone used by the test-local DaylightChangerStruggle mirror.
     */
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    /**
     * Verifies remapped provider ticks stay below the range that caused visible float quantization.
     *
     * @throws TextValidationError when the fixture date is malformed
     */
    @Test
    void providerTicksStaySmall() throws TextValidationError {
        long ticks = DaylightDates.toProviderTicks(TimeOfDay.parse("24/09/3018 08:29"));

        assertTrue(ticks > 0L);
        assertTrue(ticks < 100_000_000L);
    }

    /**
     * Verifies converted Arda dates keep their provider day, month, and time of day.
     *
     * @throws TextValidationError when a fixture date is malformed
     */
    @Test
    void providerTicksPreserveDayMonthAndDayTime() throws TextValidationError {
        assertProviderDisplay("04/09/3006 12:00");
        assertProviderDisplay("24/09/3018 08:29");
        assertProviderDisplay("29/02/3020 06:00");
        assertProviderDisplay("31/12/3020 23:45");
    }

    /**
     * Verifies converted ticks are continuous while provider years do not wrap.
     *
     * @throws TextValidationError when a fixture date is malformed
     */
    @Test
    void providerTicksAreContinuousWithoutYearWrap() throws TextValidationError {
        assertProviderDeltaMatchesArdaDelta("24/09/3018 08:29", "24/09/3018 08:44");
        assertProviderDeltaMatchesArdaDelta("31/12/3018 23:00", "01/01/3019 01:00");
    }

    /**
     * Verifies provider years can be mapped back to the corresponding Arda years.
     */
    @Test
    void ardaYearForMapsProviderYearsAroundLastArdaYear() {
        assertEquals(3018, DaylightDates.ardaYearFor(6, 3018));
        assertEquals(3019, DaylightDates.ardaYearFor(7, 3019));
        assertEquals(3020, DaylightDates.ardaYearFor(4, 3020));
    }

    /**
     * Asserts that a provider tick value displays with the requested day, month, and time.
     *
     * @param text expected Arda date-time
     * @throws TextValidationError when the fixture date is malformed
     */
    private static void assertProviderDisplay(String text) throws TextValidationError {
        long ardaTicks = TimeOfDay.parse(text);
        ProviderDateTime display = mirroredMinecraftTicksToDate(DaylightDates.toProviderTicks(ardaTicks));
        LocalDate date = TimeOfDay.date(ardaTicks);

        assertEquals(date.getDayOfMonth(), display.date().getDayOfMonth());
        assertEquals(date.getMonthValue(), display.date().getMonthValue());
        assertEquals(Math.floorMod(ardaTicks, TimeOfDay.DAY_TICKS), Math.floorMod(display.ticks(), TimeOfDay.DAY_TICKS));
    }

    /**
     * Asserts that provider and Arda deltas match for two fixture times.
     *
     * @param startText start fixture date-time
     * @param endText   end fixture date-time
     * @throws TextValidationError when a fixture date is malformed
     */
    private static void assertProviderDeltaMatchesArdaDelta(String startText, String endText) throws TextValidationError {
        long start = TimeOfDay.parse(startText);
        long end = TimeOfDay.parse(endText);

        assertEquals(end - start, DaylightDates.toProviderTicks(end) - DaylightDates.toProviderTicks(start));
    }

    /**
     * Mirrors DaylightChangerStruggle's Minecraft tick to date display conversion.
     *
     * @param ticks provider ticks to display
     * @return displayed provider date-time
     */
    private static ProviderDateTime mirroredMinecraftTicksToDate(long ticks) {
        long shiftedTicks = ticks + 6000L;
        long day = shiftedTicks / TimeOfDay.DAY_TICKS;
        long remainder = shiftedTicks % TimeOfDay.DAY_TICKS;
        int hour = (int) (remainder / 1000L);
        int minute = (int) ((remainder % 1000L) / 16L);
        int second = (int) ((((remainder % 1000L) % 16L) / 16.0D) * 60.0D);

        Calendar calendar = Calendar.getInstance(UTC, Locale.ENGLISH);
        calendar.clear();
        calendar.setLenient(true);
        calendar.set(0, Calendar.JANUARY, Math.toIntExact(day + 1L), hour, minute, second);

        LocalDate date = LocalDate.of(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        long displayTicks = (day * TimeOfDay.DAY_TICKS) + Math.floorMod(ticks, TimeOfDay.DAY_TICKS);
        return new ProviderDateTime(date, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), displayTicks);
    }

    /**
     * Displayed date-time returned by the mirrored provider conversion.
     *
     * @param date   displayed date
     * @param hour   displayed hour
     * @param minute displayed minute
     * @param ticks  displayed provider ticks
     */
    private record ProviderDateTime(LocalDate date, int hour, int minute, long ticks) {
    }
}

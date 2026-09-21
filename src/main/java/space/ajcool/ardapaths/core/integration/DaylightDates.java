package space.ajcool.ardapaths.core.integration;

import space.ajcool.ardapaths.core.data.TimeOfDay;

import java.time.LocalDate;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Converts ArdaPaths marker times into DaylightChangerStruggle's static-time date frame.
 *
 * <p>DaylightChangerStruggle and parts of Minecraft's sky rendering path handle time through 32-bit floats. Passing
 * ArdaPaths' full 3006-based timeline into that path produces visible sun jumps because float precision is coarse at
 * tens of billions of ticks. This class remaps only the provider-facing value into years 4-7 AD, preserving day,
 * month, leap-year status, and time of day while keeping the tick magnitude small.</p>
 *
 * <p>When an Arda year congruent to 0 modulo 4 rolls over, the provider year wraps from 7 AD to 4 AD. The time of day
 * stays continuous, but DaylightChangerStruggle's displayed provider year and moon phase can jump. Gregorian century
 * years that are not leap years also map to Julian leap years; the authored timeline is not currently near those
 * dates.</p>
 */
public final class DaylightDates {

    /**
     * UTC timezone used to reproduce DaylightChangerStruggle's date math without daylight-saving offsets.
     */
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    /**
     * Number of milliseconds in one civil day.
     */
    private static final long DAY_MILLIS = 86_400_000L;

    /**
     * Last Arda year whose provider offset was cached.
     */
    private static volatile int cachedYear = Integer.MIN_VALUE;

    /**
     * Cached provider tick offset for {@link #cachedYear}.
     */
    private static volatile long cachedOffsetTicks;

    /**
     * Prevents construction of this utility class.
     */
    private DaylightDates() {
    }

    /**
     * Calculates the DaylightChangerStruggle day index for a calendar date.
     *
     * @param date target calendar date
     * @return provider day index for the date
     */
    @SuppressWarnings("MagicConstant")
    static long providerDayIndex(LocalDate date) {
        Calendar base = calendar();
        base.set(0, Calendar.JANUARY, 1, 0, 0, 0);

        Calendar target = calendar();
        target.set(date.getYear(), date.getMonthValue() - 1, date.getDayOfMonth(), 0, 0, 0);

        long day = (target.getTimeInMillis() - base.getTimeInMillis()) / DAY_MILLIS;
        for (long candidate = day - 2L; candidate <= day + 2L; candidate++) {
            if (providerDate(candidate).equals(date)) {
                return candidate;
            }
        }

        return day;
    }

    /**
     * Converts ArdaPaths absolute ticks into DaylightChangerStruggle provider ticks.
     *
     * @param ardaTicks absolute ticks in ArdaPaths' storage timeline
     * @return ticks in DaylightChangerStruggle's date frame
     */
    public static long toProviderTicks(long ardaTicks) {
        return ardaTicks + yearOffsetTicks(TimeOfDay.date(ardaTicks).getYear());
    }

    /**
     * Maps a provider display year back to the nearest Arda year around the last controlled Arda year.
     *
     * @param providerYear provider year currently displayed by DaylightChangerStruggle
     * @param lastArdaYear most recent Arda year sent to DaylightChangerStruggle
     * @return Arda year corresponding to the provider year
     */
    static int ardaYearFor(int providerYear, int lastArdaYear) {
        return lastArdaYear + (providerYear - providerYear(lastArdaYear));
    }

    /**
     * Returns the most recent Arda year used for a provider tick conversion.
     *
     * @return cached Arda year, or the storage epoch year before any conversion has run
     */
    static int lastArdaYear() {
        int year = cachedYear;
        if (year == Integer.MIN_VALUE) {
            return TimeOfDay.EPOCH.getYear();
        }

        return year;
    }

    /**
     * Returns the provider tick offset for a whole Arda year.
     *
     * @param year Arda calendar year
     * @return tick offset that maps the year's day-of-year values into the provider's low-magnitude frame
     */
    private static long yearOffsetTicks(int year) {
        if (cachedYear == year) {
            return cachedOffsetTicks;
        }

        synchronized (DaylightDates.class) {
            if (cachedYear == year) {
                return cachedOffsetTicks;
            }

            long offsetTicks = calculateYearOffsetTicks(year);
            cachedOffsetTicks = offsetTicks;
            cachedYear = year;
            return offsetTicks;
        }
    }

    /**
     * Calculates the provider tick offset for a whole Arda year.
     *
     * @param year Arda calendar year
     * @return provider tick offset for the year
     */
    private static long calculateYearOffsetTicks(int year) {
        LocalDate ardaYearStart = LocalDate.of(year, 1, 1);
        LocalDate providerYearStart = LocalDate.of(providerYear(year), 1, 1);
        long ardaStartDays = ardaYearStart.toEpochDay() - TimeOfDay.EPOCH.toEpochDay();
        long offsetDays = providerDayIndex(providerYearStart) - ardaStartDays;
        return offsetDays * TimeOfDay.DAY_TICKS;
    }

    /**
     * Maps an Arda year to a nearby AD provider year with the same modulo-four leap-year status.
     *
     * @param ardaYear Arda calendar year
     * @return provider year in the range 4-7 AD
     */
    private static int providerYear(int ardaYear) {
        return 4 + Math.floorMod(ardaYear, 4);
    }

    /**
     * Creates the lenient hybrid calendar used by DaylightChangerStruggle date display code.
     *
     * @return calendar instance in UTC
     */
    private static Calendar calendar() {
        Calendar calendar = Calendar.getInstance(UTC, Locale.ENGLISH);
        calendar.clear();
        calendar.setLenient(true);
        return calendar;
    }

    /**
     * Resolves the calendar date displayed for a provider day index.
     *
     * @param day provider day index
     * @return displayed calendar date
     */
    private static LocalDate providerDate(long day) {
        Calendar calendar = calendar();
        calendar.set(0, Calendar.JANUARY, Math.toIntExact(day + 1L), 0, 0, 0);
        return LocalDate.of(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH));
    }
}

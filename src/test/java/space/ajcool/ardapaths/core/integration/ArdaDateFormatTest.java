package space.ajcool.ardapaths.core.integration;

import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for the DaylightChangerStruggle date formatter wrapper.
 */
class ArdaDateFormatTest {
    /**
     * UTC timezone used by deterministic formatter fixtures.
     */
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    /**
     * DaylightChangerStruggle's date display pattern.
     */
    private static final String DCS_PATTERN = "DD yyyy-MM-dd HH:mm:ss";

    /**
     * Verifies provider years are replaced with the current Arda year.
     */
    @Test
    void formatsProviderDateWithArdaYear() {
        SimpleDateFormat original = originalFormatter();
        ArdaDateFormat format = new ArdaDateFormat(original, () -> 3018);

        assertEquals("267 3018-09-24 08:29:00", format.format(providerDate()));
    }

    /**
     * Verifies the wrapped formatter is unchanged when the Arda override is not installed.
     */
    @Test
    void originalFormatterKeepsProviderYear() {
        SimpleDateFormat original = originalFormatter();
        SimpleDateFormat control = originalFormatter();

        assertEquals(control.format(providerDate()), original.format(providerDate()));
        assertEquals("267 0006-09-24 08:29:00", original.format(providerDate()));
    }

    /**
     * Creates a formatter matching DaylightChangerStruggle's public date formatter.
     *
     * @return deterministic DCS-style formatter
     */
    private static SimpleDateFormat originalFormatter() {
        SimpleDateFormat format = new SimpleDateFormat(DCS_PATTERN, Locale.ENGLISH);
        format.setTimeZone(UTC);
        return format;
    }

    /**
     * Creates a provider-side date in year 6 AD.
     *
     * @return date fixture for the provider readout
     */
    private static Date providerDate() {
        Calendar calendar = new GregorianCalendar(UTC, Locale.ENGLISH);
        calendar.clear();
        calendar.set(6, Calendar.SEPTEMBER, 24, 8, 29, 0);
        return calendar.getTime();
    }
}

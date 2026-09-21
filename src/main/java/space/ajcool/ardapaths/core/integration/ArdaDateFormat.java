package space.ajcool.ardapaths.core.integration;

import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;
import java.util.function.IntSupplier;

/**
 * Date formatter that keeps DaylightChangerStruggle's provider date but prints ArdaPaths' authored year.
 */
class ArdaDateFormat extends SimpleDateFormat {

    /**
     * Serialization identifier for the formatter subclass.
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Formatter that was installed by DaylightChangerStruggle before ArdaPaths wrapped it.
     */
    private final SimpleDateFormat original;

    /**
     * Supplies the last Arda year sent to DaylightChangerStruggle.
     */
    private final IntSupplier lastArdaYearSupplier;

    /**
     * Pattern copied from the wrapped formatter.
     */
    private final String originalPattern;

    /**
     * Calendar used to read the provider year from formatted dates.
     */
    private final Calendar providerCalendar;

    /**
     * Last Arda year used to build {@link #delegate}.
     */
    private transient int delegateArdaYear = Integer.MIN_VALUE;

    /**
     * Formatter with the year field replaced by a quoted Arda year literal.
     */
    private transient SimpleDateFormat delegate;

    /**
     * Creates a formatter that maps provider years through {@link DaylightDates#lastArdaYear()}.
     *
     * @param original formatter currently installed by DaylightChangerStruggle
     */
    ArdaDateFormat(SimpleDateFormat original) {
        this(original, DaylightDates::lastArdaYear);
    }

    /**
     * Creates a formatter with an explicit last-year supplier for tests.
     *
     * @param original             formatter currently installed by DaylightChangerStruggle
     * @param lastArdaYearSupplier source for the most recent Arda year
     */
    ArdaDateFormat(SimpleDateFormat original, IntSupplier lastArdaYearSupplier) {
        super(original.toPattern(), original.getDateFormatSymbols());
        this.original = (SimpleDateFormat) original.clone();
        this.lastArdaYearSupplier = Objects.requireNonNull(lastArdaYearSupplier, "lastArdaYearSupplier");
        this.originalPattern = original.toPattern();
        setTimeZone(original.getTimeZone());
        setLenient(original.isLenient());
        providerCalendar = (Calendar) original.getCalendar().clone();
    }

    @Override
    public StringBuffer format(@NotNull Date date, @NotNull StringBuffer toAppendTo, @NotNull FieldPosition pos) {
        providerCalendar.setTime(date);
        int providerYear = providerCalendar.get(Calendar.YEAR);
        int ardaYear = DaylightDates.ardaYearFor(providerYear, lastArdaYearSupplier.getAsInt());
        return delegate(ardaYear).format(date, toAppendTo, pos);
    }

    /**
     * Returns a formatter whose year field is pinned to a literal Arda year.
     *
     * @param ardaYear Arda year to print
     * @return cached delegate formatter
     */
    private SimpleDateFormat delegate(int ardaYear) {
        if (delegate != null && delegateArdaYear == ardaYear) {
            return delegate;
        }

        SimpleDateFormat formatter = new SimpleDateFormat(ardaYearPattern(ardaYear), original.getDateFormatSymbols());
        formatter.setCalendar((Calendar) original.getCalendar().clone());
        formatter.setTimeZone(original.getTimeZone());
        formatter.setLenient(original.isLenient());
        delegate = formatter;
        delegateArdaYear = ardaYear;
        return formatter;
    }

    /**
     * Builds the copied pattern with its calendar-year token replaced by a quoted literal.
     *
     * @param ardaYear Arda year to inject
     * @return pattern that prints the supplied year literally
     */
    private String ardaYearPattern(int ardaYear) {
        return originalPattern.replace("yyyy", "'" + ardaYear + "'");
    }
}

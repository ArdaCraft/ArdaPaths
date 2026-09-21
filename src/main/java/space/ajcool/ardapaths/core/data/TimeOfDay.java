package space.ajcool.ardapaths.core.data;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import space.ajcool.ardapaths.screens.widgets.TextValidationError;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for converting marker date-time settings between text and absolute Minecraft ticks.
 */
public final class TimeOfDay {

    /**
     * Marker value used when no custom date and time is configured.
     */
    public static final long UNSET = Long.MIN_VALUE;

    /**
     * Storage epoch represented by absolute tick zero. This is the persisted marker timeline and must not change.
     */
    public static final LocalDate EPOCH = LocalDate.of(3006, 1, 1);

    /**
     * Default date used when anchoring the current world time before a marker sets an explicit date-time.
     */
    public static final LocalDate DEFAULT_BASELINE_DATE = LocalDate.of(3006, 9, 4);

    /**
     * Full date-time input pattern in DD/MM/YYYY HH:MM format.
     */
    private static final Pattern TIME_PATTERN = Pattern.compile("^(\\d{1,2})/(\\d{1,2})/(\\d{1,4}) ([01]\\d|2[0-3]):([0-5]\\d)$");

    /**
     * Number of Minecraft daytime ticks in one in-game day.
     */
    public static final int DAY_TICKS = 24000;

    /**
     * Number of wall-clock minutes represented by a full in-game day.
     */
    private static final int DAY_MINUTES = 1440;

    /**
     * Wall-clock hour represented by daytime tick zero.
     */
    private static final int DAWN_HOUR = 6;

    /**
     * Tick and RGB keyframes used to visualize a configured marker time in compact lists.
     */
    private static final TimeColorKeyframe[] COLOR_KEYFRAMES = new TimeColorKeyframe[]{
            new TimeColorKeyframe(0, 0xFF8C1A),
            new TimeColorKeyframe(6000, 0x87CEEB),
            new TimeColorKeyframe(12000, 0xFF6F91),
            new TimeColorKeyframe(18000, 0x000000)
    };

    /**
     * Prevents construction of this utility class.
     */
    private TimeOfDay() {
    }

    /**
     * Parses user-entered date-time text into absolute Minecraft ticks.
     *
     * @param text the date-time text in DD/MM/YYYY HH:MM format, or blank for unset
     * @return absolute ticks, or {@link #UNSET} when blank
     * @throws TextValidationError when the text is not a valid date-time
     */
    public static long parse(String text) throws TextValidationError {
        if (text == null || text.isBlank()) {
            return UNSET;
        }

        Matcher matcher = TIME_PATTERN.matcher(text);
        if (!matcher.matches()) {
            throw new TextValidationError(Component.translatable("ardapaths.generic.validation.error.time").getString());
        }

        try {
            int day = Integer.parseInt(matcher.group(1));
            int month = Integer.parseInt(matcher.group(2));
            int year = Integer.parseInt(matcher.group(3));
            int hours = Integer.parseInt(matcher.group(4));
            int minutes = Integer.parseInt(matcher.group(5));
            LocalDate date = LocalDate.of(year, month, day);

            return toTicks(date, hours, minutes);
        } catch (DateTimeException exception) {
            throw new TextValidationError(Component.translatable("ardapaths.generic.validation.error.time").getString());
        }
    }

    /**
     * Formats absolute Minecraft ticks as user-editable date-time text.
     *
     * @param ticks absolute ticks, or {@link #UNSET} when unset
     * @return DD/MM/YYYY HH:MM date-time text, or an empty string when unset
     */
    public static String format(long ticks) {
        if (ticks == UNSET) {
            return "";
        }

        DateTimeParts parts = parts(ticks);
        return String.format("%02d/%02d/%04d %02d:%02d", parts.date().getDayOfMonth(), parts.date().getMonthValue(), parts.date().getYear(), parts.hours(), parts.minutes());
    }

    /**
     * Snaps absolute ticks onto the minute grid used by {@link #format} and {@link #parse}.
     *
     * @param ticks absolute ticks, or {@link #UNSET}
     * @return the canonical tick value for the displayed date-time, or {@link #UNSET}
     */
    public static long snap(long ticks) {
        if (ticks == UNSET) {
            return UNSET;
        }

        return fromClockMinutes(toClockMinutes(ticks));
    }

    /**
     * Returns the calendar date represented by absolute ticks.
     *
     * @param ticks absolute ticks
     * @return calendar date containing the supplied ticks
     */
    public static LocalDate date(long ticks) {
        return parts(ticks).date();
    }

    /**
     * Places a world's day-relative time on a chosen calendar date.
     *
     * @param date         calendar date to assign to the world time
     * @param worldDayTime world time whose time-of-day should be preserved
     * @return absolute ticks on the supplied date
     */
    public static long fromDayTime(LocalDate date, long worldDayTime) {
        long day = date.toEpochDay() - EPOCH.toEpochDay();
        return (day * DAY_TICKS) + Math.floorMod(worldDayTime, DAY_TICKS);
    }

    /**
     * Maps a marker time to an opaque ARGB color for visual scanning.
     *
     * @param ticks absolute ticks; callers should pass configured times rather than {@link #UNSET}
     * @return opaque ARGB color interpolated across sunrise, day, sunset, and night
     */
    public static int gradientColor(long ticks) {
        int dayTicks = Math.floorMod(ticks, DAY_TICKS);

        for (int index = 0; index < COLOR_KEYFRAMES.length; index++) {
            TimeColorKeyframe start = COLOR_KEYFRAMES[index];
            TimeColorKeyframe end = index == COLOR_KEYFRAMES.length - 1
                    ? new TimeColorKeyframe(DAY_TICKS, COLOR_KEYFRAMES[0].rgb())
                    : COLOR_KEYFRAMES[index + 1];

            if (dayTicks >= start.tick() && dayTicks <= end.tick()) {
                float progress = (dayTicks - start.tick()) / (float) (end.tick() - start.tick());
                return 0xFF000000 | lerpRgb(start.rgb(), end.rgb(), progress);
            }
        }

        return 0xFF000000 | COLOR_KEYFRAMES[0].rgb();
    }

    /**
     * Converts parsed calendar values into absolute ticks.
     *
     * @param date    parsed calendar date
     * @param hours   parsed hour of day
     * @param minutes parsed minute of hour
     * @return absolute ticks for the supplied date and clock time
     */
    private static long toTicks(LocalDate date, int hours, int minutes) {
        long day = date.toEpochDay() - EPOCH.toEpochDay();
        return fromClockMinutes((day * DAY_MINUTES) + (hours * 60L) + minutes);
    }

    /**
     * Decomposes absolute ticks into calendar date and clock time.
     *
     * <p>The calendar date rolls over at midnight, while Minecraft's own day index rolls over at dawn, so the
     * date is derived from wall-clock minutes rather than from the tick day index.</p>
     *
     * @param ticks absolute ticks to decompose
     * @return date and clock-time parts represented by the ticks
     */
    private static DateTimeParts parts(long ticks) {
        long clockMinutes = toClockMinutes(ticks);
        long day = Math.floorDiv(clockMinutes, DAY_MINUTES);
        int minuteOfDay = (int) Math.floorMod(clockMinutes, (long) DAY_MINUTES);

        return new DateTimeParts(EPOCH.plusDays(day), minuteOfDay / 60, minuteOfDay % 60);
    }

    /**
     * Converts absolute ticks into wall-clock minutes counted from midnight on the epoch date.
     *
     * @param ticks absolute ticks to convert
     * @return wall-clock minutes since midnight on {@link #EPOCH}
     */
    private static long toClockMinutes(long ticks) {
        return Math.round(ticks * DAY_MINUTES / (double) DAY_TICKS) + (DAWN_HOUR * 60L);
    }

    /**
     * Converts wall-clock minutes counted from midnight on the epoch date into absolute ticks.
     *
     * @param clockMinutes wall-clock minutes since midnight on {@link #EPOCH}
     * @return absolute ticks for the supplied wall-clock minute
     */
    private static long fromClockMinutes(long clockMinutes) {
        return Math.round((clockMinutes - (DAWN_HOUR * 60L)) * DAY_TICKS / (double) DAY_MINUTES);
    }

    /**
     * Interpolates each RGB channel independently.
     *
     * @param startRgb starting RGB color
     * @param endRgb   ending RGB color
     * @param progress interpolation progress from zero to one
     * @return interpolated RGB color
     */
    private static int lerpRgb(int startRgb, int endRgb, float progress) {
        int red = Mth.lerpInt(progress, (startRgb >> 16) & 0xFF, (endRgb >> 16) & 0xFF);
        int green = Mth.lerpInt(progress, (startRgb >> 8) & 0xFF, (endRgb >> 8) & 0xFF);
        int blue = Mth.lerpInt(progress, startRgb & 0xFF, endRgb & 0xFF);
        return (red << 16) | (green << 8) | blue;
    }

    /**
     * Color stop for marker-list time visualization.
     *
     * @param tick daytime tick where the color is exact
     * @param rgb  RGB color shown at the tick
     */
    private record TimeColorKeyframe(int tick, int rgb) {

    }

    /**
     * Calendar and clock fields decoded from absolute ticks.
     *
     * @param date    calendar date
     * @param hours   clock hour
     * @param minutes clock minute
     */
    private record DateTimeParts(LocalDate date, int hours, int minutes) {

    }
}

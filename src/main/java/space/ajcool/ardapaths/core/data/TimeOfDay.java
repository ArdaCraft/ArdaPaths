package space.ajcool.ardapaths.core.data;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import space.ajcool.ardapaths.screens.widgets.TextValidationError;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for converting marker time-of-day settings between text and Minecraft daytime ticks.
 */
public final class TimeOfDay {

    /**
     * Marker value used when no custom time of day is configured.
     */
    public static final int UNSET = -1;

    /**
     * Transition range used when a marker switches time instantly.
     */
    public static final int DEFAULT_TRANSITION_RANGE = 0;

    /**
     * Maximum transition distance, in blocks, accepted by clients and server spread calculations.
     */
    public static final int MAX_TRANSITION_RANGE = 200;

    /**
     * Transition range sentinel that makes time a live function of the current trail segment.
     */
    public static final int COMPUTED_TRANSITION_RANGE = -1;

    /**
     * User-facing keyword accepted by marker editors for computed segment time.
     */
    public static final String COMPUTED_KEYWORD = "computed";

    /**
     * Full time-of-day input pattern in 24-hour hh:mm format.
     */
    private static final Pattern TIME_PATTERN = Pattern.compile("^([01]\\d|2[0-3]):([0-5]\\d)$");

    /**
     * Number of Minecraft daytime ticks in one in-game day.
     */
    private static final int DAY_TICKS = 24000;

    /**
     * Number of daytime ticks represented by one wall-clock hour.
     */
    private static final int TICKS_PER_HOUR = 1000;

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
     * Parses user-entered time text into Minecraft daytime ticks.
     *
     * @param text the time text in hh:mm format, or blank for unset
     * @return daytime ticks, or {@link #UNSET} when blank
     * @throws TextValidationError when the text is not a valid hh:mm time
     */
    public static int parse(String text) throws TextValidationError {
        if (text == null || text.isBlank()) {
            return UNSET;
        }

        Matcher matcher = TIME_PATTERN.matcher(text);
        if (!matcher.matches()) {
            throw new TextValidationError(Component.translatable("ardapaths.generic.validation.error.time").getString());
        }

        int hours = Integer.parseInt(matcher.group(1));
        int minutes = Integer.parseInt(matcher.group(2));

        return Math.floorMod(((hours - 6) * TICKS_PER_HOUR) + Math.round(minutes * TICKS_PER_HOUR / 60.0F), DAY_TICKS);
    }

    /**
     * Formats Minecraft daytime ticks as user-editable time text.
     *
     * @param ticks daytime ticks, or {@link #UNSET} when unset
     * @return hh:mm time text, or an empty string when unset
     */
    public static String format(int ticks) {
        if (ticks == UNSET) {
            return "";
        }

        int dayTicks = Math.floorMod(ticks, DAY_TICKS);
        int totalMinutes = Math.round(dayTicks * DAY_MINUTES / (float) DAY_TICKS) % DAY_MINUTES;
        int shiftedMinutes = (totalMinutes + (DAWN_HOUR * 60)) % DAY_MINUTES;
        int hours = shiftedMinutes / 60;
        int minutes = shiftedMinutes % 60;

        return String.format("%02d:%02d", hours, minutes);
    }

    /**
     * Snaps daytime ticks onto the minute grid used by {@link #format} and {@link #parse}.
     *
     * @param ticks daytime ticks, or {@link #UNSET}
     * @return the canonical tick value for the displayed time, or {@link #UNSET}
     */
    public static int snap(int ticks) {
        if (ticks == UNSET) {
            return UNSET;
        }

        int dayTicks = Math.floorMod(ticks, DAY_TICKS);
        int totalMinutes = Math.round(dayTicks * DAY_MINUTES / (float) DAY_TICKS) % DAY_MINUTES;
        int hours = ((totalMinutes + (DAWN_HOUR * 60)) % DAY_MINUTES) / 60;
        int minutes = (totalMinutes + (DAWN_HOUR * 60)) % DAY_MINUTES % 60;

        return Math.floorMod(((hours - DAWN_HOUR) * TICKS_PER_HOUR) + Math.round(minutes * TICKS_PER_HOUR / 60.0F), DAY_TICKS);
    }

    /**
     * Maps a marker time to an opaque ARGB color for visual scanning.
     *
     * @param ticks daytime ticks; callers should pass configured times rather than {@link #UNSET}
     * @return opaque ARGB color interpolated across sunrise, day, sunset, and night
     */
    public static int gradientColor(int ticks) {
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
     * Parses user-entered transition range text.
     *
     * @param text transition range text, or blank for computed segment interpolation
     * @return numeric transition range or {@link #COMPUTED_TRANSITION_RANGE}
     * @throws TextValidationError when the text is neither computed nor a bounded integer
     */
    public static int parseTransitionRange(String text) throws TextValidationError {
        if (text == null || text.isBlank() || COMPUTED_KEYWORD.equalsIgnoreCase(text.trim())) {
            return COMPUTED_TRANSITION_RANGE;
        }

        try {
            int range = Integer.parseInt(text.trim());
            if (range < DEFAULT_TRANSITION_RANGE || range > MAX_TRANSITION_RANGE) {
                throw new TextValidationError(String.format("Must be between %d and %d, or %s.",
                        DEFAULT_TRANSITION_RANGE,
                        MAX_TRANSITION_RANGE,
                        COMPUTED_KEYWORD));
            }

            return range;
        } catch (NumberFormatException exception) {
            throw new TextValidationError(Component.translatable("ardapaths.generic.validation.error.transition_range").getString());
        }
    }

    /**
     * Formats a marker transition range for editing.
     *
     * @param range numeric transition range or {@link #COMPUTED_TRANSITION_RANGE}
     * @return editable transition range text
     */
    public static String formatTransitionRange(int range) {
        if (isComputed(range)) {
            return COMPUTED_KEYWORD;
        }

        return String.valueOf(Math.max(DEFAULT_TRANSITION_RANGE, Math.min(MAX_TRANSITION_RANGE, range)));
    }

    /**
     * Checks whether a transition range selects computed segment interpolation.
     *
     * @param range marker transition range
     * @return true when the range uses the computed sentinel
     */
    public static boolean isComputed(int range) {
        return range == COMPUTED_TRANSITION_RANGE;
    }

    /**
     * Color stop for marker-list time visualization.
     *
     * @param tick daytime tick where the color is exact
     * @param rgb  RGB color shown at the tick
     */
    private record TimeColorKeyframe(int tick, int rgb) {

    }
}

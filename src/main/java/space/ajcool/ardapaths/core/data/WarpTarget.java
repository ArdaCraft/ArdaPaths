package space.ajcool.ardapaths.core.data;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * Utility for discriminating authored warp targets from raw block coordinates.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class WarpTarget {
    /**
     * Pattern for three signed integer block coordinates separated by whitespace.
     */
    private static final String COORDINATE_PATTERN = "^[+-]?\\d+\\s+[+-]?\\d+\\s+[+-]?\\d+$";

    /**
     * Checks whether a configured target is a raw coordinate triple.
     *
     * @param value the configured target text
     * @return true when the target is shaped like {@code x y z} in-range integer coordinates
     */
    public static boolean isCoordinates(String value) {
        return parseComponents(value) != null;
    }

    /**
     * Parses a coordinate target into a block position.
     *
     * @param value the configured target text
     * @return the parsed block position, or null when the target is not coordinates
     */
    public static @Nullable BlockPos parseCoordinates(String value) {
        int[] coords = parseComponents(value);
        if (coords == null) return null;

        return new BlockPos(coords[0], coords[1], coords[2]);
    }

    /**
     * Parses coordinate text into its three integer components.
     *
     * @param value the configured target text
     * @return the three components, or null when the text is not a valid in-range coordinate triple
     */
    private static int @Nullable [] parseComponents(@Nullable String value) {
        if (value == null || !value.trim().matches(COORDINATE_PATTERN)) return null;

        String[] tokens = value.trim().split("\\s+");
        int[] components = new int[tokens.length];
        for (int index = 0; index < tokens.length; index++) {
            try {
                long parsed = Long.parseLong(tokens[index]);
                if (parsed < Integer.MIN_VALUE || parsed > Integer.MAX_VALUE) return null;
                components[index] = (int) parsed;
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        return components;
    }

    /**
     * Formats a block position as the {@code x y z} text used by coordinate input fields.
     *
     * @param pos the position to format, or null for an empty field
     * @return coordinate text, or an empty string when no position is set
     */
    public static String formatCoordinates(@Nullable BlockPos pos) {
        if (pos == null) return "";
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }
}

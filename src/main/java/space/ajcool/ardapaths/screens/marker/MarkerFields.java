package space.ajcool.ardapaths.screens.marker;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import space.ajcool.ardapaths.core.data.GiveItemAction;
import space.ajcool.ardapaths.core.data.TimeOfDay;
import space.ajcool.ardapaths.core.data.WarpTarget;
import space.ajcool.ardapaths.screens.widgets.InputBoxWidget;
import space.ajcool.ardapaths.screens.widgets.TextValidationError;
import space.ajcool.ardapaths.screens.widgets.TextValidator;

/**
 * Validators and parsing helpers for marker-edit form fields.
 */
public final class MarkerFields {

    /** Lowest activation range that the marker editor slider can select. */
    private static final int MIN_ACTIVATION_RANGE = 0;

    /** Highest activation range that the marker editor slider can select. */
    private static final int MAX_ACTIVATION_RANGE = 100;

    /**
     * Prevents construction of the static marker-field helper.
     */
    private MarkerFields() {
    }

    /**
     * Parses a text input, falling back to the last known valid value if the field is invalid.
     *
     * @param input         the input widget to parse
     * @param fallbackValue the value to use when validation fails
     * @return the input text or the fallback value
     */
    public static String parseTextOrFallback(InputBoxWidget input, String fallbackValue) {
        if (input == null) return fallbackValue;
        if (!input.validateText()) return fallbackValue;
        return input.getValue().trim();
    }

    /**
     * Parses an integer input, falling back to the last known valid value if the field is malformed.
     *
     * @param input         the input widget to parse
     * @param fallbackValue the value to use when parsing fails
     * @return the parsed integer or the fallback value
     */
    public static int parseIntegerOrFallback(InputBoxWidget input, int fallbackValue) {
        if (input == null) return fallbackValue;

        try {
            return Integer.parseInt(input.getValue());
        } catch (NumberFormatException e) {
            return fallbackValue;
        }
    }

    /**
     * Parses a date-time input, falling back to the last known valid value if the field is malformed.
     *
     * @param input         the input widget to parse
     * @param fallbackValue the value to use when parsing fails
     * @return the parsed absolute date-time ticks or the fallback value
     */
    public static long parseTimeOfDayOrFallback(InputBoxWidget input, long fallbackValue) {
        if (input == null) return fallbackValue;

        try {
            return TimeOfDay.parse(input.getValue());
        } catch (TextValidationError e) {
            return fallbackValue;
        }
    }

    /**
     * Converts a normalized slider position to a marker activation range.
     *
     * @param sliderValue slider position, normally from zero to one
     * @return activation range represented by the slider position
     */
    public static int sliderToActivationRange(double sliderValue) {
        return Mth.floor(Mth.clampedLerp(sliderValue, MIN_ACTIVATION_RANGE, MAX_ACTIVATION_RANGE));
    }

    /**
     * Converts a stored marker activation range to a normalized slider position.
     *
     * @param range marker activation range to display
     * @return slider position clamped to the visible track
     */
    public static double activationRangeToSlider(int range) {
        return Mth.clamp((double) range / MAX_ACTIVATION_RANGE, 0.0, 1.0);
    }

    /**
     * Creates an integer validator constrained to the supplied inclusive bounds.
     *
     * @param min the minimum accepted value
     * @param max the maximum accepted value
     * @return validator for bounded integer text
     */
    public static TextValidator rangeValidator(int min, int max) {
        return text -> {
            try {
                int value = Integer.parseInt(text);
                if (value < min || value > max) {
                    throw new TextValidationError(String.format("Must be between %d and %d.", min, max));
                }
            } catch (NumberFormatException e) {
                throw new TextValidationError(Component.translatable("ardapaths.generic.validation.error.integer").getString());
            }
        };
    }

    /**
     * Validates the optional current-marker time.
     *
     * @param text input text to validate
     * @throws TextValidationError when the time is malformed
     */
    public static void validateCurrentTimeOfDay(String text) throws TextValidationError {
        TimeOfDay.parse(text);
    }

    /**
     * Validates the optional auto-teleport target shape.
     *
     * @param text input text to validate
     * @throws TextValidationError when the target is neither coordinates nor a single-token warp name
     */
    public static void validateAutoTeleportTarget(String text) throws TextValidationError {
        String value = text.trim();
        if (value.isEmpty() || WarpTarget.isCoordinates(value)) return;
        if (!value.isBlank() && !value.matches(".*\\s+.*")) return;
        throw new TextValidationError(Component.translatable("ardapaths.client.marker.configuration.screens.auto_teleport_target.invalid").getString());
    }

    /**
     * Validates the optional client focus target shape.
     *
     * @param text input text to validate
     * @throws TextValidationError when the target is not blank or coordinate text
     */
    public static void validateLookAt(String text) throws TextValidationError {
        String value = text.trim();
        if (value.isEmpty() || WarpTarget.isCoordinates(value)) return;
        throw new TextValidationError(Component.translatable("ardapaths.client.marker.configuration.screens.look_at.invalid").getString());
    }

    /**
     * Validates the all-or-nothing target marker continuation fields.
     *
     * @param dimension target marker dimension text
     * @param x         target marker X coordinate text
     * @param y         target marker Y coordinate text
     * @param z         target marker Z coordinate text
     * @throws TextValidationError when only part of the group is filled or coordinates are malformed
     */
    public static void validateTargetMarker(String dimension, String x, String y, String z) throws TextValidationError {
        boolean hasDimension = dimension != null && !dimension.trim().isEmpty();
        boolean hasX = x != null && !x.trim().isEmpty();
        boolean hasY = y != null && !y.trim().isEmpty();
        boolean hasZ = z != null && !z.trim().isEmpty();

        if (!hasDimension && !hasX && !hasY && !hasZ) return;
        if (!hasDimension || !hasX || !hasY || !hasZ || Identifier.tryParse(dimension.trim()) == null) {
            throw new TextValidationError(Component.translatable("ardapaths.client.marker.configuration.screens.target_marker.invalid").getString());
        }

        validateInteger(x);
        validateInteger(y);
        validateInteger(z);
    }

    /**
     * Validates one integer text field.
     *
     * @param text input text to validate
     * @throws TextValidationError when the input is not an integer
     */
    public static void validateInteger(String text) throws TextValidationError {
        try {
            Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            throw new TextValidationError(Component.translatable("ardapaths.generic.validation.error.integer").getString());
        }
    }

    /**
     * Validates that the optional give-item value names a registered item.
     *
     * @param text input text to validate
     * @throws TextValidationError when the item identifier is malformed or unknown
     */
    public static void validateGiveItem(String text) throws TextValidationError {
        String value = text.trim();
        if (value.isEmpty()) return;
        if (GiveItemAction.isClear(value)) return;

        Identifier id = Identifier.tryParse(value);
        if (id != null && BuiltInRegistries.ITEM.containsKey(id)) return;

        throw new TextValidationError(Component.translatable("ardapaths.client.marker.configuration.screens.give_item.invalid").getString());
    }
}

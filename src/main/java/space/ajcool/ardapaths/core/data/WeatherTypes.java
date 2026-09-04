package space.ajcool.ardapaths.core.data;

import lombok.Getter;

/**
 * Defines renderable weather types to switch to on the trail
 */
public enum WeatherTypes {

    /** Clear weather */
    CLEAR("Clear"),
    /** Rain weather */
    RAIN("Rain"),
    /** Thunder weather */
    THUNDER("Thunder"),
    /** Default weather */
    DEFAULT("Default");

    /** The display name for this weather type */
    @Getter
    private final String displayName;

    /**
     * Initializes a Weather Type with its associated display name
     *
     * @param displayName the name to display on screen
     */
    WeatherTypes(String displayName) {

        this.displayName = displayName;
    }

    /**
     * Returns a weather type from its associated int value or DEFAULT if it doesn't exist.
     *
     * @param value the ordinal value to convert
     * @return the associated weather type
     */
    public static WeatherTypes fromInt(int value) {

        for (WeatherTypes weatherType : WeatherTypes.values())
            if (weatherType.ordinal() == value) return weatherType;

        return WeatherTypes.DEFAULT;
    }
}

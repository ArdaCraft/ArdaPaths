package space.ajcool.ardapaths.core.data;

/**
 * Marker-authored activation mode for applying a configured time of day.
 */
public enum TimeActivation {
    /**
     * Time is interpolated from the previous timed marker along the trail segment.
     */
    COMPUTED(-1),

    /**
     * Time is ramped inside the marker's normal activation range.
     */
    MARKER_RANGE(0);

    /**
     * Integer value persisted in marker NBT and backup JSON.
     */
    private final int nbtValue;

    /**
     * Creates an activation mode with its persisted representation.
     *
     * @param nbtValue integer value stored for this mode
     */
    TimeActivation(int nbtValue) {
        this.nbtValue = nbtValue;
    }

    /**
     * Converts this activation mode to the value stored in NBT.
     *
     * @return persisted integer value
     */
    public int toNbtValue() {
        return nbtValue;
    }

    /**
     * Reads an activation mode from the legacy transition-range storage slot.
     *
     * @param value persisted transition-range value
     * @return computed only for the sentinel value; marker range for old numeric values
     */
    public static TimeActivation fromNbtValue(int value) {
        return value == COMPUTED.nbtValue ? COMPUTED : MARKER_RANGE;
    }
}

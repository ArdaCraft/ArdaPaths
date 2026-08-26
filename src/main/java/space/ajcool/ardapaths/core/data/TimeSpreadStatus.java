package space.ajcool.ardapaths.core.data;

/**
 * Server result codes for a marker time-spread request.
 */
public enum TimeSpreadStatus {
    /**
     * The spread request completed successfully.
     */
    OK,

    /**
     * The spread completed, but the configured segment is too short for the guideline step size.
     */
    OK_STEP_EXCEEDED,

    /**
     * The player is not allowed to edit path marker data.
     */
    UNAUTHORIZED,

    /**
     * The request references invalid config or marker data.
     */
    INVALID_DATA,

    /**
     * A backup or restore is already using the marker worker.
     */
    BUSY,

    /**
     * The requested chain is longer than the server allows.
     */
    TARGET_TOO_FAR,

    /**
     * The marker chain does not reach the requested target.
     */
    CHAIN_BROKEN,

    /**
     * The marker chain ended before it reached the requested target.
     */
    CHAIN_ENDED
}

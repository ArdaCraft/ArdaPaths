package space.ajcool.ardapaths.core.data;

/**
 * Result code for saving path marker data on the server.
 */
public enum PathMarkerUpdateStatus {

    /** The path marker was resolved and updated. */
    OK,

    /** The requester lacks permission to update marker data. */
    UNAUTHORIZED,

    /** No path marker exists at the requested position. */
    NOT_FOUND
}

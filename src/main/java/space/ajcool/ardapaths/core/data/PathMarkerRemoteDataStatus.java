package space.ajcool.ardapaths.core.data;

/**
 * Result code for loading a path marker that is not currently loaded on the client.
 */
public enum PathMarkerRemoteDataStatus {
    /**
     * The path marker was resolved and its full NBT is included in the response.
     */
    OK,

    /**
     * The requester lacks permission to inspect editor-only path marker data.
     */
    UNAUTHORIZED,

    /**
     * No path marker exists at the requested position.
     */
    NOT_FOUND
}

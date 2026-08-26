package space.ajcool.ardapaths.core.data;

/**
 * Result code for a server-provided chapter marker list request.
 */
public enum ChapterMarkersStatus {
    /**
     * The chapter chain was resolved and contains the edited marker.
     */
    OK,

    /**
     * The chapter chain was resolved and the edited marker starts a detached chain.
     */
    OK_WITH_BREAK,

    /**
     * No configured chapter start marker could be found.
     */
    NO_CHAPTER_START,

    /**
     * The requester lacks permission to inspect editor-only chapter marker data.
     */
    UNAUTHORIZED,

    /**
     * The request referenced missing or invalid path, chapter, world, or marker data.
     */
    INVALID_DATA
}

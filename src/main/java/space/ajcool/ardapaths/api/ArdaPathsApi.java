package space.ajcool.ardapaths.api;

/**
 * ArdaPaths API contract definition.
 */
public interface ArdaPathsApi {

    /**
     * Selects the specified path and chapter for the player, optionally teleporting them to the chapter's location.
     * @param pathId               the path ID to select
     * @param chapterId            the chapter ID to select
     * @param putPathfinderInHands whether to make the player wield the pathfinder or not
     * @param teleport             whether to teleport the player to the location or not
     */
    @SuppressWarnings("unused")
    void selectPathAndChapter(String pathId, String chapterId, boolean putPathfinderInHands, boolean teleport);
}

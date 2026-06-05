package space.ajcool.ardapaths.api;

import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.core.networking.PacketRegistry;
import space.ajcool.ardapaths.core.networking.packets.EmptyPacket;
import space.ajcool.ardapaths.paths.Paths;

/**
 * Public API entry point for the ArdaPaths mod.
 *
 * <p>Other mods interact with ArdaPaths through the static methods on this class.
 * Call these methods during your mod's {@code onInitialize()} — all Fabric mod
 * initializers run before the server starts, so any registration is visible by
 * the time ArdaPaths validates and schedules its refresh.</p>
 */
public final class ArdaPathsApiImpl implements ArdaPathsApi {

    /** API Instance */
    private static ArdaPathsApiImpl instance;

    private ArdaPathsApiImpl() { /* Not instantiable */ }

    /**
     * Get the current initialized instance of the API
     * @return a valid instance of the API
     */
    public static ArdaPathsApiImpl getInstance() {

        return instance;
    }

    /**
     * Initializes the API
     */
    public static void initialize() {

        if (instance == null)
            instance = new ArdaPathsApiImpl();
    }

    /**
     * Selects the specified path and chapter for the player, optionally teleporting them to the chapter's location.
     * @param pathId               the path ID to select
     * @param chapterId            the chapter ID to select
     * @param putPathfinderInHands whether to make the player wield the pathfinder or not
     * @param teleport             whether to teleport the player to the location or not
     */
    @Override
    public void selectPathAndChapter(String pathId, String chapterId, boolean putPathfinderInHands, boolean teleport) {

        if (pathId == null) {
            ArdaPaths.LOGGER.error("[ArdaPathsApi] pathId must not be null");
            return;
        }

        if (chapterId == null) {

            ArdaPaths.LOGGER.error("[ArdaPathsApi] chapterId must not be null");
            return;
        }

        if (putPathfinderInHands) {

            // Request to put the pathfinder in the player's hands if needed
            PacketRegistry.WIELD_PATHFINDER_REQUEST.send(new EmptyPacket(), response -> {
                selectPathAndChapter(pathId, chapterId, teleport);
            });
        } else {

            selectPathAndChapter(pathId, chapterId, teleport);
        }
    }

    /**
     * Selects the specified path and chapter for the player
     * @param pathId               the path ID to select
     * @param chapterId            the chapter ID to select
     * @param teleport             whether to teleport the player to the location or not
     */
    private void selectPathAndChapter(String pathId, String chapterId, boolean teleport) {

        Paths.setSelectedPath(pathId);
        Paths.gotoChapter(chapterId, teleport);
    }
}

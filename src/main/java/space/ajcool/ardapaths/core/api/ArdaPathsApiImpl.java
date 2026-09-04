package space.ajcool.ardapaths.core.api;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import space.ajcool.ardapaths.api.ArdaPathsApi;
import space.ajcool.ardapaths.core.networking.PacketRegistry;
import space.ajcool.ardapaths.core.networking.packets.EmptyPacket;
import space.ajcool.ardapaths.paths.Paths;

/**
 * Internal implementation of the ArdaPaths public API.
 */
@Slf4j(topic = "ardapaths")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
// Accessed via Lombok-generated accessor; IntelliJ entry-point analysis can't follow it.
@SuppressWarnings("unused")
public final class ArdaPathsApiImpl implements ArdaPathsApi {

    /**
     * The singleton instance of the ArdaPaths API implementation.
     */
    @Getter
    private static ArdaPathsApiImpl instance;

    /**
     * Initializes the API.
     */
    public static void initialize() {

        if (instance == null)
            instance = new ArdaPathsApiImpl();
    }

    /**
     * Selects the specified path and chapter for the player, optionally teleporting them to the chapter's location.
     *
     * @param pathId               the path ID to select
     * @param chapterId            the chapter ID to select
     * @param putPathfinderInHands whether to make the player wield the pathfinder or not
     * @param teleport             whether to teleport the player to the location or not
     */
    @Override
    public void selectPathAndChapter(String pathId, String chapterId, boolean putPathfinderInHands, boolean teleport) {

        if (pathId == null) {
            log.error("[ArdaPathsApi] pathId must not be null");
            return;
        }

        if (chapterId == null) {

            log.error("[ArdaPathsApi] chapterId must not be null");
            return;
        }

        if (putPathfinderInHands) {

            PacketRegistry.WIELD_PATHFINDER_REQUEST.send(new EmptyPacket(EmptyPacket.WIELD_PATHFINDER_TYPE), response -> selectPathAndChapter(pathId, chapterId, teleport));
        } else {

            selectPathAndChapter(pathId, chapterId, teleport);
        }
    }

    /**
     * Selects the specified path and chapter for the player.
     *
     * @param pathId    the path ID to select
     * @param chapterId the chapter ID to select
     * @param teleport  whether to teleport the player to the location or not
     */
    private void selectPathAndChapter(String pathId, String chapterId, boolean teleport) {

        Paths.setSelectedPath(pathId);
        Paths.gotoChapter(chapterId, teleport);
    }
}

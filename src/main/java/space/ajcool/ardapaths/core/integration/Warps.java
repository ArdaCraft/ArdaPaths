package space.ajcool.ardapaths.core.integration;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;
import space.ajcool.ardapaths.core.executors.WarpExecutor;

/**
 * Facade for optional server-side warp integrations.
 */
@Slf4j(topic = "ardapaths")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Warps {

    /**
     * Whether the optional warp service has already been resolved.
     */
    private static volatile boolean resolved = false;

    /**
     * The active warp service, or null when HuskHomes is absent or unusable.
     */
    private static volatile @Nullable WarpService service;

    /**
     * @return true when a usable optional warp service is available
     */
    public static boolean isAvailable() {
        return resolve() != null;
    }

    /**
     * Warps the player through the optional service, falling back when unavailable or invalid.
     *
     * @param server    the server that owns the destination world
     * @param player    the player to move
     * @param warpName  the configured warp name
     * @param onFailure fallback action for missing or invalid warp targets
     */
    public static void warpTo(MinecraftServer server, ServerPlayerEntity player, String warpName, Runnable onFailure) {
        WarpService warpService = resolve();
        if (warpService != null) warpService.warpTo(server, player, warpName, onFailure);
        else onFailure.run();
    }

    /**
     * Resolves the optional warp service once.
     *
     * @return a warp service, or null when no usable service is available
     */
    private static @Nullable WarpService resolve() {
        if (!resolved) {
            synchronized (Warps.class) {
                if (!resolved) {
                    service = createService();
                    resolved = true;
                }
            }
        }

        return service;
    }

    /**
     * Creates the optional warp service behind a presence and linkage guard.
     *
     * @return a warp service, or null when HuskHomes is absent or unusable
     */
    private static @Nullable WarpService createService() {
        if (!FabricLoader.getInstance().isModLoaded("huskhomes")) return null;

        try {
            return new WarpExecutor();
        } catch (Throwable throwable) {
            log.warn("HuskHomes warp integration is unavailable; falling back to coordinates.", throwable);
            return null;
        }
    }
}

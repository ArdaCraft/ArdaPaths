package space.ajcool.ardapaths.core.executors;

import lombok.extern.slf4j.Slf4j;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.william278.huskhomes.api.FabricHuskHomesAPI;
import space.ajcool.ardapaths.core.integration.WarpLocation;
import space.ajcool.ardapaths.core.integration.WarpService;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Server-side executor for warping players to HuskHomes locations.
 * This class integrates with HuskHomes to handle chapter start teleportation.
 */
@Slf4j(topic = "ardapaths")
public class WarpExecutor implements WarpService {

    /**
     * The HuskHomes API instance used to resolve and execute warps.
     */
    private final FabricHuskHomesAPI huskHomesAPI;

    /**
     * Constructs a WarpExecutor and initializes the HuskHomes API connection.
     */
    public WarpExecutor() {
        this.huskHomesAPI = FabricHuskHomesAPI.getInstance();
    }

    /**
     * Resolves a named HuskHomes warp location without teleporting.
     *
     * @param server   the server that owns the destination world
     * @param warpName the configured warp name
     * @return future optional destination for the named warp
     */
    @Override
    public CompletableFuture<Optional<WarpLocation>> resolveWarp(MinecraftServer server, String warpName) {
        return this.huskHomesAPI.getWarp(warpName).thenApply(warp -> {
            if (warp.isEmpty()) {
                log.warn("resolveWarp: warp not found: {}", warpName);
                return Optional.<WarpLocation>empty();
            }

            final var targetWarp = warp.get();
            final var worldId = Identifier.tryParse(targetWarp.getWorld().getName());

            if (worldId == null) {
                log.warn("resolveWarp: invalid world id for warp {}: {}", warpName, targetWarp.getWorld().getName());
                return Optional.<WarpLocation>empty();
            }

            final var worldKey = ResourceKey.create(Registries.DIMENSION, worldId);
            final var serverWorld = server.getLevel(worldKey);

            if (serverWorld == null) {
                log.warn("resolveWarp: world not found for warp {}: {}", warpName, worldId);
                return Optional.<WarpLocation>empty();
            }

            return Optional.of(new WarpLocation(worldKey, BlockPos.containing(targetWarp.getX(), targetWarp.getY(), targetWarp.getZ())));
        }).exceptionally(throwable -> {
            log.warn("Failed to resolve warp {}", warpName, throwable);
            return Optional.empty();
        });
    }

    /**
     * Warps a player to a named HuskHomes warp location.
     * Asynchronously resolves the warp location and teleports the player if successful.
     *
     * @param server    the Minecraft server instance
     * @param player    the player to warp
     * @param warpName  the name of the HuskHomes warp location
     * @param onFailure fallback action for missing or invalid warp targets
     */
    @Override
    public void warpTo(MinecraftServer server, ServerPlayer player, String warpName, Runnable onFailure) {

        this.huskHomesAPI.getWarp(warpName).thenAccept(warp -> {

            log.info("Warping {} to {}", player.getStringUUID(), warpName);

            if (warp.isEmpty()) {
                log.warn("warpTo: warp not found: {}", warpName);
                server.execute(onFailure);
                return;
            }

            warp.ifPresent(targetWarp -> {

                final var worldId = Identifier.tryParse(targetWarp.getWorld().getName());

                if (worldId == null) {
                    log.warn("warpTo: invalid world id for warp {}: {}", warpName, targetWarp.getWorld().getName());
                    server.execute(onFailure);
                    return;
                }

                if (server.getLevel(ResourceKey.create(Registries.DIMENSION, worldId)) == null) {
                    log.warn("warpTo: world not found for warp {}: {}", warpName, worldId);
                    server.execute(onFailure);
                    return;
                }

                log.info("Warp ongoing for {} to {}", player.getStringUUID(), targetWarp);
                var teleportTarget = huskHomesAPI.getTeleportTarget(targetWarp);
                if (teleportTarget == null) {
                    log.warn("warpTo: teleport target not found for warp {}", warpName);
                    server.execute(onFailure);
                    return;
                }
                server.execute(() -> player.teleport(teleportTarget));

            });
        });
    }
}

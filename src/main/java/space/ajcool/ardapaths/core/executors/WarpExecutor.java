package space.ajcool.ardapaths.core.executors;

import lombok.extern.slf4j.Slf4j;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.william278.huskhomes.api.FabricHuskHomesAPI;
import space.ajcool.ardapaths.core.integration.WarpService;

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
     * Warps a player to a named HuskHomes warp location.
     * Asynchronously resolves the warp location and teleports the player if successful.
     *
     * @param server   the Minecraft server instance
     * @param player   the player to warp
     * @param warpName the name of the HuskHomes warp location
     * @param onFailure fallback action for missing or invalid warp targets
     */
    @Override
    public void warpTo(MinecraftServer server, ServerPlayerEntity player, String warpName, Runnable onFailure) {

        this.huskHomesAPI.getWarp(warpName).thenAccept(warp -> {

            log.info("Warping {} to {}", player.getUuidAsString(), warpName);

            if (warp.isEmpty()) {
                log.warn("Warp not found: {}", warpName);
                server.execute(onFailure);
                return;
            }

            warp.ifPresent(targetWarp -> {

                final var worldId = Identifier.tryParse(targetWarp.getWorld().getName());

                if (worldId == null) {
                    log.warn("Invalid world id for warp {}: {}", warpName, targetWarp.getWorld().getName());
                    server.execute(onFailure);
                    return;
                }

                final var serverWorld = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, worldId));

                if (serverWorld == null) {
                    log.warn("World not found for warp {}: {}", warpName, worldId);
                    server.execute(onFailure);
                    return;
                }

                log.info("Warp ongoing for {} to {}", player.getUuidAsString(), targetWarp);
                server.execute(() -> player.teleport(serverWorld,
                            targetWarp.getX(),
                            targetWarp.getY(),
                            targetWarp.getZ(),
                            targetWarp.getYaw(),
                            targetWarp.getPitch()));

            });
        });
    }
}

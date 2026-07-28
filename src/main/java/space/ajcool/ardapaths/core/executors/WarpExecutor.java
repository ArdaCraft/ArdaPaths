package space.ajcool.ardapaths.core.executors;

import lombok.extern.slf4j.Slf4j;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.william278.huskhomes.api.FabricHuskHomesAPI;

/**
 * Server-side executor for warping players to HuskHomes locations.
 * This class integrates with HuskHomes to handle chapter start teleportation.
 */
@Environment(EnvType.SERVER)
@Slf4j(topic = "ardapaths")
public class WarpExecutor {

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
     */
    public void warpTo(MinecraftServer server, ServerPlayerEntity player, String warpName) {

        this.huskHomesAPI.getWarp(warpName).thenAccept(warp -> {

            log.info("Warping {} to {}", player.getUuidAsString(), warpName);
            warp.ifPresent(targetWarp -> {

                final var worldId = Identifier.tryParse(targetWarp.getWorld().getName());

                if (worldId == null) {
                    log.warn("Invalid world id for warp {}: {}", warpName, targetWarp.getWorld().getName());
                    return;
                }

                final var serverWorld = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, worldId));

                if (serverWorld == null) {
                    log.warn("World not found for warp {}: {}", warpName, worldId);
                    return;
                }

                log.info("Warp ongoing for {} to {}", player.getUuidAsString(), targetWarp);
                player.teleport(serverWorld,
                        targetWarp.getX(),
                        targetWarp.getY(),
                        targetWarp.getZ(),
                        targetWarp.getYaw(),
                        targetWarp.getPitch());

            });
        });
    }
}

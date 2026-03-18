package space.ajcool.ardapaths.core.executors;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.william278.huskhomes.api.FabricHuskHomesAPI;
import space.ajcool.ardapaths.ArdaPaths;

@Environment(EnvType.SERVER)
public class WarpExecutor {

    private final FabricHuskHomesAPI huskHomesAPI;

    public WarpExecutor() {
        this.huskHomesAPI = FabricHuskHomesAPI.getInstance();
    }

    public void warpTo(MinecraftServer server, ServerPlayerEntity player, String warpName) {

        this.huskHomesAPI.getWarp(warpName).thenAccept(warp -> {

            ArdaPaths.LOGGER.info("Warping {} to {}", player.getUuidAsString(), warpName);
            warp.ifPresent(targetWarp -> {

                final var worldId = Identifier.tryParse(targetWarp.getWorld().getName());

                if (worldId == null) {
                    ArdaPaths.LOGGER.warn("Invalid world id for warp {}: {}", warpName, targetWarp.getWorld().getName());
                    return;
                }

                final var serverWorld = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, worldId));

                if (serverWorld == null) {
                    ArdaPaths.LOGGER.warn("World not found for warp {}: {}", warpName, worldId);
                    return;
                }

                ArdaPaths.LOGGER.info("Warp ongoing for {} to {}", player.getUuidAsString(), targetWarp);
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

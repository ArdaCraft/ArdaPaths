package space.ajcool.ardapaths.core.executors;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.server.network.ServerPlayerEntity;
import net.william278.huskhomes.api.FabricHuskHomesAPI;
import space.ajcool.ardapaths.ArdaPaths;

@Environment(EnvType.SERVER)
public class WarpExecutor {

    private final FabricHuskHomesAPI huskHomesAPI;

    public WarpExecutor() {
        this.huskHomesAPI = FabricHuskHomesAPI.getInstance();
    }

    public void warpTo(ServerPlayerEntity player, String warpName) {

        this.huskHomesAPI.getWarp(warpName).thenAccept(warp -> {

            ArdaPaths.LOGGER.info("Warping {} to {}", player.getUuidAsString(), warpName);
            warp.ifPresent(targetWarp -> player.teleport(player.getServerWorld(), targetWarp.getX(), targetWarp.getY(), targetWarp.getZ(), targetWarp.getYaw(), targetWarp.getPitch()));
        });
    }
}

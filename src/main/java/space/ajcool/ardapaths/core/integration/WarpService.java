package space.ajcool.ardapaths.core.integration;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * First-party bridge for optional chapter-start warp integrations.
 */
public interface WarpService {

    /**
     * Warps a player to a named location, or runs the fallback when no usable warp is available.
     *
     * @param server    the server that owns the destination world
     * @param player    the player to move
     * @param warpName  the configured warp name
     * @param onFailure fallback action for missing or invalid warp targets
     */
    void warpTo(MinecraftServer server, ServerPlayerEntity player, String warpName, Runnable onFailure);
}

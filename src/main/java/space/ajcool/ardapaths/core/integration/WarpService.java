package space.ajcool.ardapaths.core.integration;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * First-party bridge for optional chapter-start warp integrations.
 */
public interface WarpService {

    /**
     * Resolves a named warp to a world and block position.
     *
     * @param server   the server that owns the destination world
     * @param warpName the configured warp name
     * @return future optional destination for the named warp
     */
    CompletableFuture<Optional<WarpLocation>> resolveWarp(MinecraftServer server, String warpName);

    /**
     * Warps a player to a named location, or runs the fallback when no usable warp is available.
     *
     * @param server    the server that owns the destination world
     * @param player    the player to move
     * @param warpName  the configured warp name
     * @param onFailure fallback action for missing or invalid warp targets
     */
    void warpTo(MinecraftServer server, ServerPlayer player, String warpName, Runnable onFailure);
}

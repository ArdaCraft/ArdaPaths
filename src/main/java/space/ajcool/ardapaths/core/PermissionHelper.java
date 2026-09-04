package space.ajcool.ardapaths.core;

import lombok.extern.slf4j.Slf4j;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.core.networking.PacketRegistry;
import space.ajcool.ardapaths.core.networking.packets.EmptyPacket;

/**
 * Helper for checking edit permissions with server-authoritative client-side caching.
 */
@Slf4j(topic = "ardapaths")
public class PermissionHelper {

    /**
     * How long (in milliseconds) to cache a permission check result before refreshing from the server.
     */
    private static final long PERMISSION_CHECK_COOLDOWN_MS = 60_000; // 1 minute cooldown

    /**
     * Cached result of the last edit permission check on the client.
     */
    private static volatile Boolean hasEditPermission = false;

    /**
     * Timestamp of the last permission check made to the server.
     */
    private static volatile long lastPermissionCheckTime = 0;

    /**
     * Checks whether the given player has the {@code ardapaths.edit} permission.
     * On the server, checks Fabric Permissions API with vanilla operator fallback.
     * On the client, sends a permission check packet to the server with caching to avoid excessive network traffic.
     *
     * @param player the player to check permissions for, or null
     * @return true if the player has the edit permission, false otherwise
     */
    public static boolean hasEditPermission(@Nullable Player player) {

        if (player == null) return false;

        if (ArdaPaths.amITheServer()) return serverEditPermissionCheck(player);
        else return clientEditPermissionCheck();
    }

    /**
     * Checks the player's edit permission through Fabric Permissions API on the server.
     *
     * @param player the player to check permissions for
     * @return true if the player has the {@code ardapaths.edit} permission, false otherwise
     */
    private static boolean serverEditPermissionCheck(@NotNull Player player) {

        var serverPlayer = resolveServerPlayer(player);

        if (serverPlayer == null) {
            log.debug("Server check edit permission for player {} - no server player available", player.getName());
            return false;
        }

        boolean result = Permissions.check(serverPlayer, ArdaPaths.MOD_EDIT_PERMISSION, PermissionLevel.GAMEMASTERS);
        log.debug("Server check edit permission for player {} - {}", player.getName(), result);

        return result;
    }

    /**
     * Checks the player's edit permission on the client side with local caching.
     * Sends a permission check packet to the server if the cached result is stale.
     *
     * @return true if the cached permission result is true, false otherwise
     */
    private static boolean clientEditPermissionCheck() {

        log.debug("Client check edit permission for player {}", hasEditPermission);

        var currentTime = System.currentTimeMillis();
        var delta = currentTime - lastPermissionCheckTime;

        if (hasEditPermission == null || delta > PERMISSION_CHECK_COOLDOWN_MS) {

            log.debug("Refreshing permissions");

            lastPermissionCheckTime = currentTime;
            PacketRegistry.PERMISSION_CHECK.send(new EmptyPacket(EmptyPacket.PERMISSION_CHECK_TYPE), response -> hasEditPermission = response.hasPermission());

            // Default to false until we get a response from the server
            return hasEditPermission != null ? hasEditPermission : false;
        }

        return hasEditPermission;
    }

    /**
     * Resolves the logical-server player matching the given player.
     * In single player, client-side callers pass a client player entity, which the Fabric Permissions API
     * cannot check; in that case the counterpart is looked up on the integrated server.
     *
     * @param player the player to resolve
     * @return the matching server player, or null if none could be resolved
     */
    private static @Nullable ServerPlayer resolveServerPlayer(@NotNull Player player) {

        if (player instanceof ServerPlayer serverPlayer) return serverPlayer;

        // Dedicated servers only ever see real server players, so there is nothing else to resolve
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) return null;

        return Client.getIntegratedServerPlayer(player);
    }

    /**
     * Clears the client-side permission cache after leaving a server.
     */
    public static void resetClientCache() {
        hasEditPermission = false;
        lastPermissionCheckTime = 0;
    }
}

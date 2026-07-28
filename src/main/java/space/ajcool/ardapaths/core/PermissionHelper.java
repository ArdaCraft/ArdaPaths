package space.ajcool.ardapaths.core;

import lombok.extern.slf4j.Slf4j;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedPermissionData;
import net.luckperms.api.model.user.User;
import net.luckperms.api.util.Tristate;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.core.networking.PacketRegistry;
import space.ajcool.ardapaths.core.networking.packets.EmptyPacket;

/**
 * Helper for checking LuckPerms edit permissions with client-side caching.
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
    private static Boolean hasEditPermission = false;

    /**
     * Timestamp of the last permission check made to the server.
     */
    private static long lastPermissionCheckTime = 0;

    /**
     * Checks whether the given player has the {@code ardapaths.edit} permission.
     * On the server, queries LuckPerms directly. On the client, sends a permission check packet
     * to the server with caching to avoid excessive network traffic.
     *
     * @param player the player to check permissions for, or null
     * @return true if the player has the edit permission, false otherwise
     */
    public static boolean hasEditPermission(@Nullable PlayerEntity player) {

        if (player == null) return false;

        if (ArdaPaths.amITheServer()) return serverEditPermissionCheck(player);
        else return clientEditPermissionCheck();
    }

    /**
     * Checks the player's edit permission by querying LuckPerms directly on the server.
     *
     * @param player the player to check permissions for
     * @return true if the player has the {@code ardapaths.edit} permission, false otherwise
     */
    private static boolean serverEditPermissionCheck(@NotNull PlayerEntity player) {

        if (player instanceof ServerPlayerEntity serverPlayer) {

            LuckPerms luckpermsApi = LuckPermsProvider.get();
            User user = luckpermsApi.getPlayerAdapter(ServerPlayerEntity.class).getUser(serverPlayer);

            CachedPermissionData permissionData = user.getCachedData().getPermissionData();
            Tristate checkResult = permissionData.checkPermission(ArdaPaths.MOD_EDIT_PERMISSION);

            boolean result = checkResult.asBoolean();
            log.info("Server check edit permission for player {} - {}", player.getName(), result);

            return result;
        }

        return false;
    }

    /**
     * Checks the player's edit permission on the client side with local caching.
     * Sends a permission check packet to the server if the cached result is stale.
     *
     * @return true if the cached permission result is true, false otherwise
     */
    private static boolean clientEditPermissionCheck() {

        log.info("Client check edit permission for player {}", hasEditPermission);

        var currentTime = System.currentTimeMillis();
        var delta = currentTime - lastPermissionCheckTime;

        if (hasEditPermission == null || delta > PERMISSION_CHECK_COOLDOWN_MS) {

            log.info("Refreshing permissions");

            lastPermissionCheckTime = currentTime;
            PacketRegistry.PERMISSION_CHECK.send(new EmptyPacket(), response -> hasEditPermission = response.hasPermission());

            // Default to false until we get a response from the server
            return hasEditPermission != null ? hasEditPermission : false;
        }

        return hasEditPermission;
    }
}

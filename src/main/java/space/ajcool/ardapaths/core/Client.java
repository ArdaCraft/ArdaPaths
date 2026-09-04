package space.ajcool.ardapaths.core;

import lombok.experimental.UtilityClass;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Client-side utility methods for accessing the Minecraft client instance,
 * player, world, and other client-specific information.
 * This class can only be used on the client side.
 */
@SuppressWarnings("resource")
@Environment(EnvType.CLIENT)
@UtilityClass
public class Client {

    /**
     * @return The client's player, or null if not available
     */
    public static @Nullable LocalPlayer player() {
        return mc().player;
    }

    /**
     * Gets the Minecraft client instance. We annotate this with
     * {@link NotNull} because utility methods should only be
     * invoked after the client has been initialized.
     *
     * @return The Minecraft client instance
     */
    public static @NotNull Minecraft mc() {
        return Minecraft.getInstance();
    }

    /**
     * @return The address of the current server, or an empty string if the client is in single player mode
     */
    public static String getServerAddress() {
        Minecraft client = mc();
        if (client.isLocalServer()) return "";
        ServerData server = client.getCurrentServer();
        if (server == null) return "";
        return server.ip;
    }

    /**
     * @return The player's UUID as a string, or an empty string if not available
     */
    public static String getUuidString() {
        return mc().getUser().getProfileId().toString();
    }

    /**
     * @return True if the client is in a single player world, otherwise false
     */
    public static boolean isInSinglePlayer() {
        Minecraft client = mc();
        return client.isLocalServer();
    }

    /**
     * Resolves the logical-server counterpart of a player on the integrated (single player) server.
     * Client-side code holds a {@link LocalPlayer}, which carries no permission information;
     * server-side checks need the {@link ServerPlayer} owned by the integrated server.
     *
     * @param player the player to resolve, typically the client player
     * @return The matching player on the integrated server, or null if there is no integrated server
     * or the player is not connected to it
     */
    public static @Nullable ServerPlayer getIntegratedServerPlayer(@Nullable Player player) {
        if (player == null) return null;

        IntegratedServer server = mc().getSingleplayerServer();
        if (server == null) return null;

        return server.getPlayerList().getPlayer(player.getUUID());
    }

    /**
     * @return True if the client is holding the control key, otherwise false
     */
    public static boolean isCtrlDown() {
        ClientLevel level = world();
        return level != null && level.isClientSide() && Screen.hasControlDown();
    }

    /**
     * @return The client's world, or null if not available
     */
    public static @Nullable ClientLevel world() {
        return mc().level;
    }
}

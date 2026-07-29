package space.ajcool.ardapaths.core;

import lombok.experimental.UtilityClass;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.network.ServerPlayerEntity;
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
    public static @Nullable ClientPlayerEntity player() {
        return mc().player;
    }

    /**
     * Gets the Minecraft client instance. We annotate this with
     * {@link NotNull} because utility methods should only be
     * invoked after the client has been initialized.
     *
     * @return The Minecraft client instance
     */
    public static @NotNull MinecraftClient mc() {
        return MinecraftClient.getInstance();
    }

    /**
     * @return The address of the current server, or an empty string if the client is in single player mode
     */
    public static String getServerAddress() {
        MinecraftClient client = mc();
        if (client.isInSingleplayer()) return "";
        ServerInfo server = client.getCurrentServerEntry();
        if (server == null) return "";
        return server.address;
    }

    /**
     * @return The player's UUID as a string, or an empty string if not available
     */
    public static String getUuidString() {
        return mc().getSession().getUuid();
    }

    /**
     * @return True if the client is in a single player world, otherwise false
     */
    public static boolean isInSinglePlayer() {
        MinecraftClient client = mc();
        return client.isInSingleplayer();
    }

    /**
     * Resolves the logical-server counterpart of a player on the integrated (single player) server.
     * Client-side code holds a {@link ClientPlayerEntity}, which carries no permission information;
     * server-side checks need the {@link ServerPlayerEntity} owned by the integrated server.
     *
     * @param player the player to resolve, typically the client player
     * @return The matching player on the integrated server, or null if there is no integrated server
     * or the player is not connected to it
     */
    public static @Nullable ServerPlayerEntity getIntegratedServerPlayer(@Nullable PlayerEntity player) {
        if (player == null) return null;

        IntegratedServer server = mc().getServer();
        if (server == null) return null;

        return server.getPlayerManager().getPlayer(player.getUuid());
    }

    /**
     * @return True if the client is holding the control key, otherwise false
     */
    public static boolean isCtrlDown() {
        ClientWorld level = world();
        return level != null && level.isClient() && Screen.hasControlDown();
    }

    /**
     * @return The client's world, or null if not available
     */
    public static @Nullable ClientWorld world() {
        return mc().world;
    }
}

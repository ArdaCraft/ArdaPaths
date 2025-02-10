package space.ajcool.ardapaths.utils;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.world.ClientWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Environment(EnvType.CLIENT)
public class Client {
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
     * @return The client's world, or null if not available
     */
    public static @Nullable ClientWorld world() {
        return mc().world;
    }

    /**
     * @return The client's player, or null if not available
     */
    public static @Nullable ClientPlayerEntity player() {
        return mc().player;
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
     * @return The player's UUID, or null if not available
     */
    public static @Nullable UUID getUuid() {
        return mc().getSession().getUuidOrNull();
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
     * @return True if the client is holding the control key, otherwise false
     */
    public static boolean isCtrlDown() {
        ClientWorld level = world();
        return level != null && level.isClient() && Screen.hasControlDown();
    }
}

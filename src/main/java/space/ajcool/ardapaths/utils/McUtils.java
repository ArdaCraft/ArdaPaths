package space.ajcool.ardapaths.utils;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.jetbrains.annotations.NotNull;

public class McUtils {
    /**
     * @return The Minecraft client instance.
     */
    public static @NotNull MinecraftClient mc() {
        return MinecraftClient.getInstance();
    }

    /**
     * @return The player entity, or null if not available.
     */
    public static ClientPlayerEntity player() {
        return mc().player;
    }

    /**
     * Check if the current thread is running on the client.
     */
    public static boolean isClient() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    }
}

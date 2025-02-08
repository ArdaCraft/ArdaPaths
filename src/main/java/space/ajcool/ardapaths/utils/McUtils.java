package space.ajcool.ardapaths.utils;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.jetbrains.annotations.Nullable;

public class McUtils {
    /**
     * Get the {@link MinecraftClient} instance.
     */
    public static @Nullable MinecraftClient mc() {
        return MinecraftClient.getInstance();
    }

    /**
     * Get the {@link ClientPlayerEntity} instance.
     */
    public static @Nullable ClientPlayerEntity player() {
        return mc().player;
    }

    /**
     * Check if the current thread is running on the client.
     */
    public static boolean isClient() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    }
}

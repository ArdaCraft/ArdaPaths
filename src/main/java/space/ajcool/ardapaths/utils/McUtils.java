package space.ajcool.ardapaths.utils;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

public class McUtils {
    /**
     * Check if the current thread is running on the client.
     */
    public static boolean isClient() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    }
}

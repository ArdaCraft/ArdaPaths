package space.ajcool.ardapaths.core;

import lombok.experimental.UtilityClass;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Fabric-specific utility methods for checking the environment type.
 */
@UtilityClass
public class Fabric {

    /**
     * Check if the current thread is running on the client.
     *
     * @return true if running on the client environment, false if on the server
     */
    public static boolean isClient() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    }
}

package space.ajcool.ardapaths.core;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

public class Fabric
{
    /**
     * Check if the current thread is running on the client.
     */
    public static boolean isClient()
    {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    }
}

package space.ajcool.ardapaths.core;

import net.minecraft.resources.Identifier;
import space.ajcool.ardapaths.ArdaPaths;

/**
 * Shared constants and identifier helpers for ArdaPaths runtime code.
 */
public final class ModConstants {

    /**
     * Prevents construction of this utility class.
     */
    private ModConstants() {
    }

    /**
     * Creates an ArdaPaths namespaced identifier.
     *
     * @param path resource path within the ArdaPaths namespace
     * @return namespaced resource identifier
     */
    public static Identifier modId(String path) {
        return Identifier.tryBuild(ArdaPaths.MOD_ID, path);
    }
}

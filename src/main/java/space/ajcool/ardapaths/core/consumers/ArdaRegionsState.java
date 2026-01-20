package space.ajcool.ardapaths.core.consumers;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Maintains the state of whether Arda Regions is currently displaying on the client.
 */
@Environment(EnvType.CLIENT)
public final class ArdaRegionsState {

    /** Indicates if Arda Regions is currently displaying */
    private static volatile boolean displaying = false;

    /** class should not be instantiated */
    private ArdaRegionsState() {/* Singleton */}


    /**
     * Checks if Arda Regions is currently being displayed.
     *
     * @return true if Arda Regions is displaying, false otherwise
     */
    public static boolean isDisplaying() {
        return displaying;
    }

    /**
     * Sets the displaying state of Arda Regions.
     *
     * @param value true to indicate Arda Regions is displaying, false otherwise
     */
    static void setDisplaying(boolean value) {
        displaying = value;
    }
}
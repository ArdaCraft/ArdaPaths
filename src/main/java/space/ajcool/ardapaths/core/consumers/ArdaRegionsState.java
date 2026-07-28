package space.ajcool.ardapaths.core.consumers;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Maintains the state of whether Arda Regions is currently displaying on the client.
 */
@Environment(EnvType.CLIENT)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ArdaRegionsState {

    /**
     * Indicates if Arda Regions is currently displaying
     */
    @Getter
    private static volatile boolean displaying = false;

    /**
     * Sets the displaying state of Arda Regions.
     *
     * @param value true to indicate Arda Regions is displaying, false otherwise
     */
    static void setDisplaying(boolean value) {
        displaying = value;
    }
}

package space.ajcool.ardapaths.core.integration;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Maintains the state of whether Arda Regions is currently displaying on the client.
 */
@Environment(EnvType.CLIENT)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ArdaRegionsState {

    /**
     * Indicates if Arda Regions is currently displaying some text.
     */
    @Setter
    @Getter
    private static volatile boolean displaying = false;

}

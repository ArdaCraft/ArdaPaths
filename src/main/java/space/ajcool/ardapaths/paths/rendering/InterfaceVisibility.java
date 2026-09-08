package space.ajcool.ardapaths.paths.rendering;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import space.ajcool.ardapaths.ArdaPathsClient;
import space.ajcool.ardapaths.core.Client;
import space.ajcool.ardapaths.mc.items.ModItems;

/**
 * Central client-side gate for deciding whether vanilla interface elements should render.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class InterfaceVisibility {
    /**
     * Checks the persisted interface preference against the player's currently held item.
     *
     * @return true when vanilla interface elements should be suppressed, false otherwise
     */
    public static boolean isInterfaceHidden() {
        if (!ArdaPathsClient.CONFIG.hideInterface()) return false;

        var player = Client.player();
        return player != null && player.isHolding(ModItems.PATH_REVEALER);
    }
}

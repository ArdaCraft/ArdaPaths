package space.ajcool.ardapaths.core.consumers;

import lombok.extern.slf4j.Slf4j;
import mc.ardacraft.ardaregions.api.ArdaRegionsAPI;
import mc.ardacraft.ardaregions.api.ArdaRegionsApiEntrypoint;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.core.integration.ArdaRegionsState;

/**
 * Consumer for the Arda Regions API that listens for client discovery popup events
 * and updates the ArdaRegionsState accordingly.
 */
@Slf4j(topic = "ardapaths")
public class ArdaRegionsConsumer implements ArdaRegionsApiEntrypoint {

    /**
     * Called when the Arda Regions API is ready.
     * Registers a listener for client discovery popup events to update the displaying state.
     *
     * @param api the Arda Regions API instance
     */
    @Override
    public void onApiReady(ArdaRegionsAPI api) {

        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) {
            log.info("{}, skipping Arda Regions consumer registration on server side.", ArdaPaths.MOD_ID);
            return;
        }
        log.info("Arda Regions API is ready, registering consumer.");
        api.getClientDiscoveryPopupEvent().register(
                (regionId, regionName, description, alpha) -> ArdaRegionsState.setDisplaying(alpha > 0.2f)
        );
    }
}

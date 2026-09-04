package space.ajcool.ardapaths.core.consumers;

import com.duom.ardamaps.api.ArdaMapsApi;
import com.duom.ardamaps.api.ArdaMapsApiEntrypoint;
import com.duom.ardamaps.api.waypoints.ApiWaypoint;
import lombok.extern.slf4j.Slf4j;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.ArdaPathsClient;
import space.ajcool.ardapaths.core.Client;
import space.ajcool.ardapaths.core.integration.WaypointProvider;
import space.ajcool.ardapaths.core.integration.Waypoints;

import java.util.Objects;

/**
 * Consumer for the ArdaMaps API.
 * Manages waypoints displayed on the ArdaMaps for the next trail node in the current path.
 */
@Slf4j(topic = "ardapaths")
public class ArdaMapsConsumer implements ArdaMapsApiEntrypoint, WaypointProvider {

    /**
     * The ArdaMaps API instance, or null if the API is not available.
     */
    private ArdaMapsApi api;

    /**
     * The currently displayed next waypoint, or null if no waypoint is shown.
     */
    private Vec3 currentWaypoint = null;

    /**
     * Clears all waypoints created by ArdaPaths from the ArdaMaps display.
     */
    @Override
    public void clearWaypoints() {

        // API is unavailable, stop here
        if (api == null) return;

        api.getWaypointsApi().removeWaypoints(ArdaPaths.MOD_ID);
        currentWaypoint = null;
    }

    /**
     * Sets or updates the next trail node waypoint displayed on ArdaMaps.
     * Creates a new waypoint at the target location if enabled and different from the current one.
     *
     * @param target the position of the next trail node
     */
    @SuppressWarnings("resource")
    @Override
    public void setNextTrailNode(Vec3 target) {

        if (api == null) return;

        if (shouldShowWaypoint(target)) {

            assert Client.mc().level != null;

            var dimension = Client.mc().level.dimension().location().toString();

            if (currentWaypoint != null)
                api.getWaypointsApi().removeWaypoints(ArdaPaths.MOD_ID);

            var waypoint = new ApiWaypoint((int) target.x, (int) target.z,
                    Component.translatable("ardapaths.client.next.trail.waypoint").getString(),
                    1.0f, 1.0f, 1.0f,
                    ArdaPaths.MOD_ID, dimension,
                    false, ArdaPaths.MOD_ID + ":textures/item/path_marker.png");

            currentWaypoint = target;

            api.getWaypointsApi().addWaypoint(waypoint);
        }
    }

    /**
     * Validates whether a waypoint should be shown for the given target
     *
     * @param target the target to display
     * @return true if a waypoint should be shown, false otherwise
     */
    @SuppressWarnings("resource")
    private boolean shouldShowWaypoint(Vec3 target) {

        if (!ArdaPathsClient.CONFIG.showTrailWaypoints()) return false;

        // If target is null, stop here
        if (target == null) return false;

        // Will not happen client side - sanity check
        if (Client.mc().level == null) return false;

        return !Objects.equals(currentWaypoint, target);
    }

    /**
     * Called when the ArdaMaps API becomes available.
     * Stores the API instance for later use.
     *
     * @param ardaMapsApi the ArdaMaps API instance
     */
    @Override
    public void onApiReady(ArdaMapsApi ardaMapsApi) {

        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) {
            log.info("{}, skipping ArdaMaps consumer registration on server side.", ArdaPaths.MOD_ID);
            return;
        }

        api = ardaMapsApi;
        Waypoints.register(this);
    }
}

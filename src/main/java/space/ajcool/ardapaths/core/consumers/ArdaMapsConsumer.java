package space.ajcool.ardapaths.core.consumers;

import com.duom.ardamaps.api.ArdaMapsApi;
import com.duom.ardamaps.api.ArdaMapsApiEntrypoint;
import com.duom.ardamaps.core.Client;
import com.duom.ardamaps.core.data.map.Waypoint;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.ArdaPathsClient;

import java.util.Objects;

/**
 * Consumer for the ArdaMaps API.
 * Manages waypoints displayed on the ArdaMaps for the next trail node in the current path.
 */
public class ArdaMapsConsumer implements ArdaMapsApiEntrypoint {

    /**
     * The ArdaMaps API instance, or null if the API is not available.
     */
    private static ArdaMapsApi INSTANCE;

    /**
     * The currently displayed next waypoint, or null if no waypoint is shown.
     */
    private static Vec3d CURRENT_WAYPOINT = null;

    /**
     * Clears all waypoints created by ArdaPaths from the ArdaMaps display.
     */
    public static void clearMapMarkers() {

        // API is unavailable, stop here
        if (INSTANCE == null) return;

        INSTANCE.getWaypointsApi().removeWaypoints(ArdaPaths.MOD_ID);
    }

    /**
     * Sets or updates the next trail node waypoint displayed on ArdaMaps.
     * Creates a new waypoint at the target location if enabled and different from the current one.
     *
     * @param target the position of the next trail node
     */
    public static void setNextTrailNode(Vec3d target) {

        if (INSTANCE == null) return;

        if (shouldShowWaypoint(target)) {

            assert Client.mc().world != null;

            var dimension = Client.mc().world.getRegistryKey().getValue().toString();

            if (CURRENT_WAYPOINT != null)
                INSTANCE.getWaypointsApi().removeWaypoints(ArdaPaths.MOD_ID);

            var waypoint = new Waypoint((int) target.x, (int) target.z,
                    Text.translatable("ardapaths.client.next.trail.waypoint").getString(),
                    1.0f, 1.0f, 1.0f,
                    ArdaPaths.MOD_ID, dimension,
                    false, new Identifier(ArdaPaths.MOD_ID, "textures/item/path_marker.png"));

            CURRENT_WAYPOINT = target;

            INSTANCE.getWaypointsApi().addWaypoint(waypoint);
        }
    }

    /**
     * Validates whether a waypoint should be shown for the given target
     *
     * @param target the target to display
     * @return true if a waypoint should be shown, false otherwise
     */
    private static boolean shouldShowWaypoint(Vec3d target) {

        if (!ArdaPathsClient.CONFIG.showTrailWaypoints()) return false;

        // If target is null, stop here
        if (target == null) return false;

        // Will not happen client side - sanity check
        if (Client.mc().world == null) return false;

        return !Objects.equals(CURRENT_WAYPOINT, target);
    }

    /**
     * Called when the ArdaMaps API becomes available.
     * Stores the API instance for later use.
     *
     * @param ardaMapsApi the ArdaMaps API instance
     */
    @Override
    public void onApiReady(ArdaMapsApi ardaMapsApi) {

        INSTANCE = ardaMapsApi;
    }
}

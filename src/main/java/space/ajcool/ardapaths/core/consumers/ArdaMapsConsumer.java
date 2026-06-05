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
 */
public class ArdaMapsConsumer implements ArdaMapsApiEntrypoint {

    /** Whether the API is ready to be queried or not **/
    private static ArdaMapsApi INSTANCE;

    /** Currently displayed next waypoint */
    private static Vec3d CURRENT_WAYPOINT = null;

    /**
     * Clears all the ArdaMaps ArdaPaths waypoints
     */
    public static void clearMapMarkers() {

        // API is unavailable, stop here
        if (INSTANCE == null) return;

        INSTANCE.getWaypointsApi().removeWaypoints(ArdaPaths.MOD_ID);
    }

    @Override
    public void onApiReady(ArdaMapsApi ardaMapsApi) {

        INSTANCE = ardaMapsApi;
    }

    /**
     * Sets the next trail node to the given target
     * @param target the target to set
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
     * @param target the target to display
     * @return true if a waypoint should be shown, false otherwise
     */
    private static boolean shouldShowWaypoint(Vec3d target)
    {

        if (!ArdaPathsClient.CONFIG.showTrailWaypoints()) return false;

        // If target is null, stop here
        if (target == null) return false;

        // Will not happen client side - sanity check
        if (Client.mc().world == null) return false;

        return !Objects.equals(CURRENT_WAYPOINT, target);
    }
}

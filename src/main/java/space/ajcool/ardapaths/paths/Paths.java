package space.ajcool.ardapaths.paths;

import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;

import java.util.ArrayList;
import java.util.List;

public class Paths {
    private static final List<PathMarkerBlockEntity> tickingMarkers = new ArrayList<>();

    public static void addTickingMarker(PathMarkerBlockEntity marker) {
        tickingMarkers.add(marker);
    }

    public static void removeTickingMarker(PathMarkerBlockEntity marker) {
        tickingMarkers.remove(marker);
    }

    public static List<PathMarkerBlockEntity> getTickingMarkers() {
        return List.copyOf(tickingMarkers);
    }

    public static void clearTickingMarkers() {
        tickingMarkers.clear();
    }
}

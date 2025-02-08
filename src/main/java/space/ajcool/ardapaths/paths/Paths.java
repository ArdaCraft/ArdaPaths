package space.ajcool.ardapaths.paths;

import space.ajcool.ardapaths.config.shared.PathSettings;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;

import java.util.ArrayList;
import java.util.List;

public class Paths {
    private static final List<PathSettings> paths = new ArrayList<>();
    private static final List<PathMarkerBlockEntity> tickingMarkers = new ArrayList<>();

    public static void addPath(PathSettings path) {
        paths.add(path);
    }

    public static List<PathSettings> getPaths() {
        return List.copyOf(paths);
    }

    public static PathSettings getPath(int id) {
        return paths.stream().filter(path -> path.id == id).findFirst().orElse(null);
    }

    public static void clearPaths() {
        paths.clear();
    }

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

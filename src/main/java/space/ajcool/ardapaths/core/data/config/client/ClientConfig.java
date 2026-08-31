package space.ajcool.ardapaths.core.data.config.client;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;
import space.ajcool.ardapaths.core.Client;
import space.ajcool.ardapaths.core.data.config.shared.ChapterData;
import space.ajcool.ardapaths.core.data.config.shared.PathData;
import space.ajcool.ardapaths.paths.movement.AutoWalker;
import space.ajcool.ardapaths.paths.rendering.objects.AnimatedMessage;
import space.ajcool.ardapaths.paths.rendering.objects.AnimatedTitle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client-side configuration containing player preferences and selected path/chapter.
 * Mirrors the server configuration and adds per-player settings for rendering and display.
 * Serialized to JSON in config.json.
 */
public class ClientConfig {
    /**
     * Map of server addresses to their selected path/chapter data.
     */
    @SerializedName("selected_paths")
    private final Map<String, SelectedPathData> selectedPaths = new HashMap<>();

    /**
     * Whether to show proximity messages when near path markers.
     */
    @Setter
    @SerializedName("proximity_messages")
    private boolean proximityMessages;

    /**
     * Whether to display chapter title overlays when chapters start.
     */
    @Setter
    @SerializedName("chapter_titles")
    private boolean chapterTitles;

    /**
     * Whether to show ArdaMaps waypoints for the next trail node.
     */
    @Setter
    @SerializedName("trail_waypoints")
    private boolean trailWaypoints;

    /**
     * Whether followed trail markers may dynamically change time and weather.
     */
    @Setter
    @SerializedName("dynamic_environment")
    private boolean dynamicEnvironment;

    /**
     * Speed multiplier for proximity message animation speed (0.0-1.0+).
     */
    @Setter
    @SerializedName("proximity_text_speed_multiplier")
    private Double proximityTextSpeedMultiplier;

    /**
     * Speed factor used by client-side auto-walk movement.
     */
    @Setter
    @SerializedName("auto_walk_speed_factor")
    private Double autoWalkSpeedFactor;

    /**
     * Duration in milliseconds to display chapter titles.
     */
    @Setter
    @SerializedName("chapter_title_display_speed")
    private Float chapterTitleDisplaySpeed;

    /**
     * List of paths serialized to JSON (mirror of server config).
     */
    @Getter
    @Setter
    @SerializedName("paths")
    private List<PathData> clientPaths = new ArrayList<>();

    /**
     * Transient list of paths loaded from the server, used during runtime.
     */
    private transient List<PathData> paths = new ArrayList<>();

    /**
     * @return True if proximity messages should be shown, otherwise false
     */
    public boolean showProximityMessages() {
        return proximityMessages;
    }

    /**
     * @return True if waypoints should be added when the player follows a path, false otherwise
     */
    public boolean showTrailWaypoints() {
        return trailWaypoints;
    }

    /**
     * @return True if trail markers may change time and weather when matching integrations are available, false otherwise
     */
    public boolean useDynamicEnvironment() {
        return dynamicEnvironment;
    }

    /**
     * @return True if chapter titles should be displayed, otherwise false
     */
    public boolean showChapterTitles() {
        return chapterTitles;
    }

    /**
     * @return the chapter title display duration in milliseconds
     */
    public Float getChapterTitleDisplaySpeed() {
        return chapterTitleDisplaySpeed != null ? chapterTitleDisplaySpeed : AnimatedTitle.DEFAULT_CHAPTER_TITLE_DISPLAY_SPEED;
    }

    /**
     * @return the factor with which the speed of the text should be displayed
     */
    public Double getProximityTextSpeedMultiplier() {
        return proximityTextSpeedMultiplier != null ? proximityTextSpeedMultiplier : AnimatedMessage.DEFAULT_PROXIMITY_TEXT_SPEED_MULTIPLIER;
    }

    /**
     * @return the factor applied to auto-walk movement speed
     */
    public Double getAutoWalkSpeedFactor() {
        return autoWalkSpeedFactor != null ? autoWalkSpeedFactor : AutoWalker.DEFAULT_AUTO_WALK_SPEED_FACTOR;
    }

    /**
     * @return The selected path, or an empty string if no path is selected
     */
    public String getSelectedPathId() {
        String identifier = getIdentifier();
        return getSelectedPathId(identifier);
    }

    /**
     * @return The current identifier for accessing the selected path data
     */
    private static String getIdentifier() {
        if (Client.isInSinglePlayer()) return Client.getUuidString();
        return Client.getServerAddress();
    }

    /**
     * @param identifier The identifier, usually a server address or the player UUID
     * @return The selected path for the given identifier, or an empty string if no path is selected
     */
    public String getSelectedPathId(String identifier) {
        if (!selectedPaths.containsKey(identifier)) return "frodo";
        return selectedPaths.get(identifier).getPathId();
    }

    /**
     * @return The selected path data, or null if no path is selected
     */
    public @Nullable PathData getSelectedPath() {
        String identifier = getIdentifier();
        return getSelectedPath(identifier);
    }

    /**
     * @param identifier The identifier, usually a server address or the player UUID
     * @return The selected path data for the given identifier, or null if no path is selected
     */
    public @Nullable PathData getSelectedPath(String identifier) {
        String pathId = getSelectedPathId(identifier);
        if (pathId.isEmpty()) return null;
        return getPath(pathId);
    }

    /**
     * @param id The ID of the path
     * @return The path with the given ID, or null if not found
     */
    public @Nullable PathData getPath(String id) {
        List<PathData> paths = getPaths();
        for (PathData path : paths) {
            if (path.getId().equalsIgnoreCase(id)) {
                return path;
            }
        }
        return null;
    }

    /**
     * @return The list of available paths
     */
    public List<PathData> getPaths() {
        return Client.isInSinglePlayer() ? this.clientPaths : this.paths;
    }

    /**
     * Sets the list of paths available on this server.
     *
     * @param paths The new list of paths
     */
    public void setPaths(List<PathData> paths) {
        if (Client.isInSinglePlayer()) {
            this.clientPaths = paths;
        } else {
            this.paths = paths;
        }
    }

    /**
     * Sets the selected path for the current identifier.
     *
     * @param path The selected path ID
     */
    public void setSelectedPath(String path) {
        String identifier = getIdentifier();
        setSelectedPath(identifier, path);
    }

    /**
     * Sets the selected path ID for the given identifier.
     *
     * @param identifier The identifier, usually a server address or the player UUID
     * @param path       The path to select
     */
    public void setSelectedPath(String identifier, String path) {
        if (identifier.isEmpty()) return;
        if (!selectedPaths.containsKey(identifier)) {
            selectedPaths.put(identifier, new SelectedPathData());
        }
        selectedPaths.get(identifier).setPathId(path);
    }

    /**
     * @return The current chapter ID, or an empty string if no chapter is selected
     */
    public String getCurrentChapterId() {
        String identifier = getIdentifier();
        return getCurrentChapterId(identifier);
    }

    /**
     * @param identifier The identifier, usually a server address or the player UUID
     * @return The chapter ID for the given identifier, or an empty string if no chapter is selected
     */
    public String getCurrentChapterId(String identifier) {
        if (!selectedPaths.containsKey(identifier)) return "default";
        return selectedPaths.get(identifier).getChapterId();
    }

    /**
     * @return The current chapter, or null if no chapter is selected
     */
    public @Nullable ChapterData getCurrentChapter() {
        String identifier = getIdentifier();
        return getCurrentChapter(identifier);
    }

    /**
     * @param identifier The identifier, usually a server address or the player UUID
     * @return The current chapter for the given server, or null if no chapter is selected
     */
    public @Nullable ChapterData getCurrentChapter(String identifier) {
        String chapterId = getCurrentChapterId(identifier);
        if (chapterId.isEmpty()) return null;
        PathData path = getSelectedPath(identifier);
        if (path == null) return null;
        return path.getChapter(chapterId);
    }

    /**
     * Sets the current chapter for the current identifier.
     *
     * @param chapter The chapter ID to set
     */
    public void setCurrentChapter(String chapter) {
        String identifier = getIdentifier();
        setCurrentChapter(identifier, chapter);
    }

    /**
     * Sets the current chapter for the given identifier.
     *
     * @param identifier The identifier, usually a server address or the player UUID
     * @param chapter    The chapter to set
     */
    public void setCurrentChapter(String identifier, String chapter) {
        if (identifier.isEmpty()) return;
        if (!selectedPaths.containsKey(identifier)) {
            selectedPaths.put(identifier, new SelectedPathData());
        }
        selectedPaths.get(identifier).setChapterId(chapter);
    }

}

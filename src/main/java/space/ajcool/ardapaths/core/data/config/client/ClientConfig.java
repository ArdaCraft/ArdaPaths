package space.ajcool.ardapaths.core.data.config.client;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import space.ajcool.ardapaths.core.Client;
import space.ajcool.ardapaths.core.data.TimeOfDay;
import space.ajcool.ardapaths.core.data.config.shared.ChapterData;
import space.ajcool.ardapaths.core.data.config.shared.PathData;
import space.ajcool.ardapaths.paths.movement.AutoWalker;
import space.ajcool.ardapaths.paths.rendering.objects.AnimatedMessage;
import space.ajcool.ardapaths.paths.rendering.objects.AnimatedTitle;
import space.ajcool.ardapaths.screens.widgets.TextValidationError;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client-side configuration containing player preferences and selected path/chapter.
 * Mirrors the server configuration and adds per-player settings for rendering and display.
 * Serialized to JSON in config.json.
 */
@Slf4j(topic = "ardapaths")
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
     * Date used to anchor the current world time before any marker sets time explicitly.
     */
    @SerializedName("baseline_date")
    private String baselineDate = "04/09/3006 12:00";

    /**
     * Whether vanilla interface elements should be hidden while holding the Pathfinder.
     */
    @Setter
    @SerializedName("hide_interface")
    private boolean hideInterface;

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

    /** Default path used before a server or world-specific selection has been written. */
    private static final String DEFAULT_PATH_ID = "frodo";

    /** Default chapter used before a server or world-specific selection has been written. */
    private static final String DEFAULT_CHAPTER_ID = "default";

    /**
     * Transient list of paths loaded from the server, used during runtime.
     */
    @Setter
    @Getter
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
     * @return the configured baseline date, or the default when the config value is invalid
     */
    public LocalDate getBaselineDate() {
        try {
            return TimeOfDay.date(TimeOfDay.parse(baselineDate));
        } catch (TextValidationError exception) {
            log.warn("[ArdaPaths] Invalid baseline_date '{}'; using {}", baselineDate, TimeOfDay.DEFAULT_BASELINE_DATE);
            return TimeOfDay.DEFAULT_BASELINE_DATE;
        }
    }

    /**
     * @return True if vanilla interface elements should be hidden while holding the Pathfinder, false otherwise
     */
    public boolean hideInterface() {
        return hideInterface;
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
        return getOrCreateSelection(identifier).getPathId();
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
        getOrCreateSelection(identifier).setPathId(path);
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
        return getOrCreateSelection(identifier).getChapterId();
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
        getOrCreateSelection(identifier).setChapterId(chapter);
    }

    /**
     * Gets the persisted selection for an identifier, creating one with effective defaults if missing.
     *
     * @param identifier The identifier, usually a server address or the player UUID
     * @return the stored or newly seeded selected path data
     */
    private SelectedPathData getOrCreateSelection(String identifier) {
        SelectedPathData selection = selectedPaths.get(identifier);
        if (selection == null) {
            selection = new SelectedPathData();
            selection.setPathId(DEFAULT_PATH_ID);
            selection.setChapterId(DEFAULT_CHAPTER_ID);
            selectedPaths.put(identifier, selection);
        }
        return selection;
    }

}

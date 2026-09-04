package space.ajcool.ardapaths.core.data.config.shared;

import com.google.gson.annotations.SerializedName;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a path with an ID, name, three colours, and a collection of chapters.
 * This is a primary configuration object that is serialized to JSON.
 */
public class PathData {

    /**
     * Map of chapter IDs to chapter data objects, representing all chapters in this path.
     */
    // Populated by Gson reflective deserialization; IntelliJ cannot trace the field access.
    @SuppressWarnings("unused")
    @SerializedName("chapters")
    private final Map<String, ChapterData> chapters = new HashMap<>();

    /**
     * The unique identifier for this path.
     */
    @Setter
    // Populated by Gson reflective deserialization; IntelliJ cannot trace the field access.
    @SuppressWarnings("unused")
    @SerializedName("id")
    private String id;

    /**
     * The display name of this path.
     */
    @Setter
    // Populated by Gson reflective deserialization; IntelliJ cannot trace the field access.
    @SuppressWarnings("unused")
    @SerializedName("name")
    private String name;

    /**
     * The primary colour used for rendering this path's trails.
     */
    @Setter
    // Populated by Gson reflective deserialization; IntelliJ cannot trace the field access.
    @SuppressWarnings("unused")
    @SerializedName("primaryColor")
    private Color primaryColor;

    /**
     * The secondary colour used for rendering this path's trails.
     */
    @Setter
    // Populated by Gson reflective deserialization; IntelliJ cannot trace the field access.
    @SuppressWarnings("unused")
    @SerializedName("secondaryColor")
    private Color secondaryColor;

    /**
     * The tertiary colour used for rendering this path's trails.
     */
    @Setter
    // Populated by Gson reflective deserialization; IntelliJ cannot trace the field access.
    @SuppressWarnings("unused")
    @SerializedName("tertiaryColor")
    private Color tertiaryColor;

    /**
     * @return The ID of this path
     */
    public String getId() {
        return id == null ? "" : id;
    }

    /**
     * @return The name of this path
     */
    public String getName() {
        return name == null ? "" : name;
    }

    /**
     * @return Array containing the primary, secondary, and tertiary colours of this path.
     */
    public Color[] getColors() {
        return new Color[]{getPrimaryColor(), getSecondaryColor(), getTertiaryColor()};
    }

    /**
     * @return The primary colour of this path.
     */
    public Color getPrimaryColor() {
        return primaryColor == null ? new Color(191, 64, 191) : primaryColor;
    }

    /**
     * @return The secondary colour of this path.
     */
    public Color getSecondaryColor() {
        return secondaryColor == null ? new Color(191, 64, 191) : secondaryColor;
    }

    /**
     * @return The tertiary colour of this path.
     */
    public Color getTertiaryColor() {
        return tertiaryColor == null ? new Color(191, 64, 191) : tertiaryColor;
    }

    /**
     * @return The IDs of the chapters in this path
     */
    public List<String> getChapterIds() {
        return chapters.keySet().stream().toList();
    }

    /**
     * @return The chapters in this path
     */
    public List<ChapterData> getChapters() {
        return chapters.values().stream().toList();
    }

    /**
     * @param id The ID of the chapter
     * @return The chapter with the given ID, or null if not found
     */
    public @Nullable ChapterData getChapter(String id) {
        return chapters.get(id);
    }

    /**
     * Sets a chapter in this path.
     *
     * @param chapter the chapter data to set
     * @return this PathData instance for chaining
     */
    public PathData setChapter(ChapterData chapter) {
        chapters.put(chapter.getId(), chapter);
        return this;
    }

    /**
     * Removes the chapter with the given ID.
     *
     * @param id the ID of the chapter to remove
     * @return this PathData instance for chaining
     */
    @SuppressWarnings("UnusedReturnValue")
    public PathData removeChapter(String id) {
        chapters.remove(id);
        return this;
    }
}

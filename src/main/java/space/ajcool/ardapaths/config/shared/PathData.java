package space.ajcool.ardapaths.config.shared;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PathData {
    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("color")
    private Color color;

    @SerializedName("chapters")
    private Map<String, ChapterData> chapters = new HashMap<>();

    /**
     * @return The ID of this path
     */
    public String getId() {
        return id == null ? "" : id;
    }

    /**
     * Sets the ID of this path.
     *
     * @param id The new ID
     */
    public PathData setId(String id) {
        this.id = id;
        return this;
    }

    /**
     * @return The name of this path
     */
    public String getName() {
        return name == null ? "" : name;
    }

    /**
     * Sets the name of this path.
     *
     * @param name The new name
     */
    public PathData setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * @return The color of this path
     */
    public Color getColor() {
        return color == null ? new Color(100, 100, 100) : color;
    }

    /**
     * Sets the color of this path.
     *
     * @param color The new color
     */
    public PathData setColor(Color color) {
        this.color = color;
        return this;
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
     * Sets the chapter with the given ID.
     *
     * @param id The ID of the chapter
     * @param chapter The chapter data
     */
    public PathData setChapter(String id, ChapterData chapter) {
        chapters.put(id, chapter);
        return this;
    }

    /**
     * Removes the chapter with the given ID.
     *
     * @param id The ID of the chapter
     */
    public PathData removeChapter(String id) {
        chapters.remove(id);
        return this;
    }
}

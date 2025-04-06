package space.ajcool.ardapaths.core.data.config.shared;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PathData
{
    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("primaryColor")
    private Color primaryColor;

    @SerializedName("secondaryColor")
    private Color secondaryColor;

    @SerializedName("tertiaryColor")
    private Color tertiaryColor;

    @SerializedName("chapters")
    private Map<String, ChapterData> chapters = new HashMap<>();

    /**
     * @return The ID of this path
     */
    public String getId()
    {
        return id == null ? "" : id;
    }

    /**
     * Sets the ID of this path.
     *
     * @param id The new ID
     */
    public PathData setId(String id)
    {
        this.id = id;
        return this;
    }

    /**
     * @return The name of this path
     */
    public String getName()
    {
        return name == null ? "" : name;
    }

    /**
     * Sets the name of this path.
     *
     * @param name The new name
     */
    public PathData setName(String name)
    {
        this.name = name;
        return this;
    }

    /**
     * @return The primary color of this path.
     */
    public Color getPrimaryColor()
    {
        return primaryColor == null ? new Color(191, 64, 191) : primaryColor;
    }

    /**
     * @return The secondary color of this path.
     */
    public Color getSecondaryColor()
    {
        return secondaryColor == null ? new Color(191, 64, 191) : secondaryColor;
    }

    /**
     * @return The tertiary color of this path.
     */
    public Color getTertiaryColor()
    {
        return tertiaryColor == null ? new Color(191, 64, 191) : tertiaryColor;
    }

    /**
     * @return Array containing the primary, secondary, and tertiary colors of this path.
     */
    public Color[] getColors()
    {
        return new Color[]{getPrimaryColor(), getSecondaryColor(), getTertiaryColor()};
    }

    /**
     * Sets the primary color of this path.
     *
     * @param color The new color
     */
    public PathData setPrimaryColor(Color color)
    {
        this.primaryColor = color;
        return this;
    }

    /**
     * Sets the secondary color of this path.
     *
     * @param color The new color
     */
    public PathData setSecondaryColor(Color color)
    {
        this.secondaryColor = color;
        return this;
    }

    /**
     * Sets the tertiary color of this path.
     *
     * @param color The new color
     */
    public PathData setTertiaryColor(Color color)
    {
        this.tertiaryColor = color;
        return this;
    }

    /**
     * @return The IDs of the chapters in this path
     */
    public List<String> getChapterIds()
    {
        return chapters.keySet().stream().toList();
    }

    /**
     * @return The chapters in this path
     */
    public List<ChapterData> getChapters()
    {
        return chapters.values().stream().toList();
    }

    /**
     * @param id The ID of the chapter
     * @return The chapter with the given ID, or null if not found
     */
    public @Nullable ChapterData getChapter(String id)
    {
        return chapters.get(id);
    }

    /**
     * Sets a chapter in this path.
     *
     * @param chapter The chapter data
     */
    public PathData setChapter(ChapterData chapter)
    {
        chapters.put(chapter.getId(), chapter);
        return this;
    }

    /**
     * Removes the chapter with the given ID.
     *
     * @param id The ID of the chapter
     */
    public PathData removeChapter(String id)
    {
        chapters.remove(id);
        return this;
    }
}

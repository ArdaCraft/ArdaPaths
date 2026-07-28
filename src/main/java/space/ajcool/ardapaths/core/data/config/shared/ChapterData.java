package space.ajcool.ardapaths.core.data.config.shared;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a chapter within a path, including metadata like name, date, and an optional warp destination.
 * This is a configuration object that is serialized to JSON.
 */
public class ChapterData {
    /**
     * The unique identifier for this chapter within its path.
     */
    @Setter
    @SerializedName("id")
    private String id;

    /**
     * The display name of this chapter.
     */
    @Setter
    @SerializedName("name")
    private String name;

    /**
     * The in-game date when this chapter takes place.
     */
    @SerializedName("date")
    private String date;

    /**
     * The order index of this chapter relative to others in the path.
     */
    @Getter
    @SerializedName("index")
    private int index;

    /**
     * Optional warp destination (e.g., a home name) for the "Return to Chapter Start" feature.
     */
    @SerializedName("warp")
    private String warp;

    /**
     * Constructs a ChapterData without a warp destination.
     *
     * @param id    the unique identifier for this chapter
     * @param name  the display name
     * @param date  the in-game date
     * @param index the order index
     */
    public ChapterData(String id, String name, String date, int index) {
        this.id = id;
        this.name = name;
        this.date = date;
        this.index = index;
    }

    /**
     * Constructs a ChapterData with a warp destination.
     *
     * @param id    the unique identifier for this chapter
     * @param name  the display name
     * @param date  the in-game date
     * @param index the order index
     * @param warp  the optional warp destination
     */
    public ChapterData(String id, String name, String date, int index, String warp) {
        this.id = id;
        this.name = name;
        this.date = date;
        this.index = index;
        this.warp = warp;
    }

    /**
     * @return The ID of this chapter
     */
    public String getId() {
        return id == null ? "" : id;
    }

    /**
     * @return The name of this chapter
     */
    public String getName() {
        return name == null ? "" : name;
    }

    /**
     * @return The start date of this chapter
     */
    public String getDate() {
        return date == null ? "" : date;
    }

    /**
     * @return returns the warp point for the beginning of this chapter
     */
    public String getWarp() {
        return warp == null ? "" : warp;
    }
}

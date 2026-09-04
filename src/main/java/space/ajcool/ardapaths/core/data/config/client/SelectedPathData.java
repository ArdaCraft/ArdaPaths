package space.ajcool.ardapaths.core.data.config.client;

import com.google.gson.annotations.SerializedName;
import lombok.Setter;

/**
 * Stores the currently selected path and chapter for a particular server/world.
 * This data is serialized to JSON as part of the client configuration.
 */
public class SelectedPathData {

    /**
     * The ID of the currently selected path.
     */
    @Setter
    @SerializedName("path")
    private String pathId;

    /**
     * The ID of the currently selected chapter within the path.
     */
    @Setter
    @SerializedName("chapter")
    private String chapterId;

    /**
     * @return The path selected for this server
     */
    public String getPathId() {
        return pathId == null ? "" : pathId;
    }

    /**
     * @return The chapter selected for this server
     */
    public String getChapterId() {
        return chapterId == null ? "" : chapterId;
    }
}

package space.ajcool.ardapaths.core.data.config.client;

import com.google.gson.annotations.SerializedName;

public class SelectedPathData
{
    @SerializedName("path")
    private String path;

    @SerializedName("chapter")
    private String chapter;

    /**
     * @return The path selected for this server
     */
    public String getPathId()
    {
        return path == null ? "" : path;
    }

    /**
     * Sets the path selected for this server.
     *
     * @param path The selected path ID
     */
    public void setPathId(String path)
    {
        this.path = path;
    }

    /**
     * @return The chapter selected for this server
     */
    public String getChapterId()
    {
        return chapter == null ? "" : chapter;
    }

    /**
     * Sets the chapter selected for this server.
     *
     * @param chapter The selected chapter ID
     */
    public void setChapterId(String chapter)
    {
        this.chapter = chapter;
    }
}

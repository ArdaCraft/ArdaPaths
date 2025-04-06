package space.ajcool.ardapaths.core.data.config.shared;

import com.google.gson.annotations.SerializedName;

public class ChapterData
{
    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("date")
    private String date;

    @SerializedName("index")
    private int index;

    public ChapterData(String id, String name, String date, int index)
    {
        this.id = id;
        this.name = name;
        this.date = date;
        this.index = index;
    }

    /**
     * @return The ID of this chapter
     */
    public String getId()
    {
        return id == null ? "" : id;
    }

    /**
     * Sets the ID of this chapter.
     *
     * @param id The new ID
     */
    public ChapterData setId(String id)
    {
        this.id = id;
        return this;
    }

    /**
     * @return The name of this chapter
     */
    public String getName()
    {
        return name == null ? "" : name;
    }

    /**
     * Sets the name of this chapter.
     *
     * @param name The new name
     */
    public ChapterData setName(String name)
    {
        this.name = name;
        return this;
    }

    /**
     * @return The start date of this chapter
     */
    public String getDate()
    {
        return date == null ? "" : date;
    }

    /**
     * Sets the start date of this chapter.
     *
     * @param date The start date
     */
    public ChapterData setDate(String date)
    {
        this.date = date;
        return this;
    }

    /**
     * @return The index of this chapter
     */
    public int getIndex()
    {
        return index;
    }

    /**
     * Sets the index of this chapter.
     *
     * @param index The new index
     */
    public ChapterData setIndex(int index)
    {
        this.index = index;
        return this;
    }
}

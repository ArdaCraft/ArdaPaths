package space.ajcool.ardapaths.config.shared;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class PathSettings {
    public int id;

    public String name;

    public List<ChapterSettings> chapters;

    @SerializedName("primary_color")
    public Color primaryColor;

    @SerializedName("secondary_color")
    public Color secondaryColor;

    @SerializedName("tertiary_color")
    public Color tertiaryColor;

    public PathSettings(int id, String name, List<ChapterSettings> chapters, Color primaryColor, Color secondaryColor, Color tertiaryColor) {
        this.id = id;
        this.name = name;
        this.chapters = chapters;
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
        this.tertiaryColor = tertiaryColor;
    }
}

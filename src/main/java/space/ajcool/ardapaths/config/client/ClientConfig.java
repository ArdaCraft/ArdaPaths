package space.ajcool.ardapaths.config.client;

import com.google.gson.annotations.SerializedName;
import space.ajcool.ardapaths.config.shared.PathSettings;

import java.util.List;

public class ClientConfig {
    @SerializedName("proximity_messages")
    public boolean proximityMessages;

    @SerializedName("selected_path")
    public int selectedPath;

    @SerializedName("current_chapter")
    public String currentChapter;

    public List<PathSettings> paths;
}

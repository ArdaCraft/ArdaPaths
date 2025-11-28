package space.ajcool.ardapaths.core.data.config.client;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Nullable;
import space.ajcool.ardapaths.core.Client;
import space.ajcool.ardapaths.core.data.config.shared.ChapterData;
import space.ajcool.ardapaths.core.data.config.shared.PathData;
import space.ajcool.ardapaths.paths.rendering.objects.AnimatedMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientConfig
{
    @SerializedName("proximity_messages")
    private boolean proximityMessages;

    @SerializedName("proximity_text_speed_multiplier")
    private Double proximityTextSpeedMultiplier;

    @SerializedName("selected_paths")
    private Map<String, SelectedPathData> selectedPaths = new HashMap<>();

    @SerializedName("paths")
    private List<PathData> clientPaths = new ArrayList<>();

    private transient List<PathData> paths = new ArrayList<>();

    /**
     * @return True if proximity messages should be shown, otherwise false
     */
    public boolean showProximityMessages()
    {
        return proximityMessages;
    }

    /**
     * Sets whether proximity messages should be shown.
     *
     * @param proximityMessages True if proximity messages should be shown, otherwise false
     */
    public void showProximityMessages(boolean proximityMessages)
    {
        this.proximityMessages = proximityMessages;
    }

    /**
     * @return the factor with which the speed of the text should be displayed
     */
    public Double getProximityTextSpeedMultiplier() {
        return proximityTextSpeedMultiplier != null ? proximityTextSpeedMultiplier : AnimatedMessage.DEFAULT_PROXIMITY_TEXT_SPEED_MULTIPLIER;
    }

    /**
     * Defines the factor at which the proximity messages are displayed
     * @param proximityTextSpeedMultiplier the factor to set
     */
    public void setProximityTextSpeedMultiplier(Double proximityTextSpeedMultiplier) {
        this.proximityTextSpeedMultiplier = proximityTextSpeedMultiplier;
    }

    /**
     * Toggles whether proximity messages should be shown.
     */
    public void toggleProximityMessages()
    {
        proximityMessages = !proximityMessages;
    }

    /**
     * @return The selected path, or an empty string if no path is selected
     */
    public String getSelectedPathId()
    {
        String identifier = getIdentifier();
        return getSelectedPathId(identifier);
    }

    /**
     * @param identifier The identifier, usually a server address or the player UUID
     * @return The selected path for the given identifier, or an empty string if no path is selected
     */
    public String getSelectedPathId(String identifier)
    {
        if (!selectedPaths.containsKey(identifier)) return "frodo";
        return selectedPaths.get(identifier).getPathId();
    }

    /**
     * @return The selected path data, or null if no path is selected
     */
    public @Nullable PathData getSelectedPath()
    {
        String identifier = getIdentifier();
        return getSelectedPath(identifier);
    }

    /**
     * @param identifier The identifier, usually a server address or the player UUID
     * @return The selected path data for the given identifier, or null if no path is selected
     */
    public @Nullable PathData getSelectedPath(String identifier)
    {
        String pathId = getSelectedPathId(identifier);
        if (pathId.isEmpty()) return null;
        return getPath(pathId);
    }

    /**
     * Sets the selected path for the current identifier.
     *
     * @param path The selected path ID
     */
    public void setSelectedPath(String path)
    {
        String identifier = getIdentifier();
        setSelectedPath(identifier, path);
    }

    /**
     * Sets the selected path ID for the given identifier.
     *
     * @param identifier The identifier, usually a server address or the player UUID
     * @param path       The path to select
     */
    public void setSelectedPath(String identifier, String path)
    {
        if (identifier.isEmpty()) return;
        if (!selectedPaths.containsKey(identifier))
        {
            selectedPaths.put(identifier, new SelectedPathData());
        }
        selectedPaths.get(identifier).setPathId(path);
    }

    /**
     * @return The current chapter ID, or an empty string if no chapter is selected
     */
    public String getCurrentChapterId()
    {
        String identifier = getIdentifier();
        return getCurrentChapterId(identifier);
    }

    /**
     * @param identifier The identifier, usually a server address or the player UUID
     * @return The chapter ID for the given identifier, or an empty string if no chapter is selected
     */
    public String getCurrentChapterId(String identifier)
    {
        if (!selectedPaths.containsKey(identifier)) return "default";
        return selectedPaths.get(identifier).getChapterId();
    }

    /**
     * @return The current chapter, or null if no chapter is selected
     */
    public @Nullable ChapterData getCurrentChapter()
    {
        String identifier = getIdentifier();
        return getCurrentChapter(identifier);
    }

    /**
     * @param identifier The identifier, usually a server address or the player UUID
     * @return The current chapter for the given server, or null if no chapter is selected
     */
    public @Nullable ChapterData getCurrentChapter(String identifier)
    {
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
    public void setCurrentChapter(String chapter)
    {
        String identifier = getIdentifier();
        setCurrentChapter(identifier, chapter);
    }

    /**
     * Sets the current chapter for the given identifier.
     *
     * @param identifier The identifier, usually a server address or the player UUID
     * @param chapter    The chapter to set
     */
    public void setCurrentChapter(String identifier, String chapter)
    {
        if (identifier.isEmpty()) return;
        if (!selectedPaths.containsKey(identifier))
        {
            selectedPaths.put(identifier, new SelectedPathData());
        }
        selectedPaths.get(identifier).setChapterId(chapter);
    }

    /**
     * @return The list of available paths
     */
    public List<PathData> getPaths()
    {
        return Client.isInSinglePlayer() ? this.clientPaths : this.paths;
    }

    /**
     * @param id The ID of the path
     * @return The path with the given ID, or null if not found
     */
    public @Nullable PathData getPath(String id)
    {
        List<PathData> paths = getPaths();
        for (PathData path : paths)
        {
            if (path.getId().equalsIgnoreCase(id))
            {
                return path;
            }
        }
        return null;
    }

    /**
     * Sets the list of paths available on this server.
     *
     * @param paths The new list of paths
     */
    public void setPaths(List<PathData> paths)
    {
        if (Client.isInSinglePlayer())
        {
            this.clientPaths = paths;
        }
        else
        {
            this.paths = paths;
        }
    }

    /**
     * Sets the path with the given ID.
     *
     * @param path The path data
     */
    public void setPath(PathData path)
    {
        List<PathData> paths = getPaths();
        for (int i = 0; i < paths.size(); i++)
        {
            if (paths.get(i).getId().equalsIgnoreCase(path.getId()))
            {
                paths.set(i, path);
                return;
            }
        }
        paths.add(path);
    }

    /**
     * @return The current identifier for accessing the selected path data
     */
    private static String getIdentifier()
    {
        if (Client.isInSinglePlayer()) return Client.getUuidString();
        return Client.getServerAddress();
    }
}

package space.ajcool.ardapaths.core.data.config.server;

import com.google.gson.annotations.SerializedName;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import space.ajcool.ardapaths.core.data.config.shared.ChapterData;
import space.ajcool.ardapaths.core.data.config.shared.PathData;

import java.util.*;

public class ServerConfig
{
    @SerializedName("paths")
    private List<PathData> paths = new ArrayList<>();

    @SerializedName("chapter_starts")
    private Map<String, PositionData> chapterStarts = new HashMap<>();

    /**
     * @return The list of paths available on this server
     */
    public @Nullable PathData getPath(String id)
    {
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
     * @return The list of paths available on this server
     */
    public List<PathData> getPaths()
    {
        return paths;
    }

    /**
     * Adds a path to the list of paths available on this server.
     *
     * @param path The path to add
     */
    public void addPath(PathData path)
    {
        for (PathData p : paths)
        {
            if (p.getId().equalsIgnoreCase(path.getId()))
            {
                return;
            }
        }
        paths.add(path);
    }

    /**
     * Updates a path in the list of paths available on this server.
     *
     * @param path The updated path
     */
    public void updatePath(PathData path)
    {
        for (PathData p : paths)
        {
            if (p.getId().equalsIgnoreCase(path.getId()))
            {
                paths.remove(p);
                paths.add(path);
                return;
            }
        }
    }

    /**
     * Removes a path from the list of paths available on this server.
     *
     * @param id The ID of the path to remove
     */
    public void removePath(String id)
    {
        for (PathData path : paths)
        {
            if (path.getId().equalsIgnoreCase(id))
            {
                paths.remove(path);
                return;
            }
        }
    }

    /**
     * @param pathId    The ID of the path
     * @param chapterId The ID of the chapter
     * @return The chapter start position for the given path
     */
    public @NotNull Optional<String> getChapterStartWarp(String pathId, String chapterId)
    {
        Optional<String> startWarp = Optional.empty();
        Optional<PathData> pathData = paths.stream()
                .filter(item -> pathId.equals(item.getId()))
                .findFirst();

        if (pathData.isPresent()) {

            String warpData = "";
            ChapterData chapterData = pathData.get().getChapter(chapterId);

            if (chapterData != null) {

                warpData = chapterData.getWarp();

                if (warpData != null && ! warpData.isBlank())  {

                    // Check if the warp is coordinates
                    boolean isCoordinates = warpData.matches("^[+-]?\\d+\\s+[+-]?\\d+\\s+[+-]?\\d+$");

                    if (!isCoordinates) {
                        startWarp = Optional.of(warpData.trim());
                    }
                }
            }
        }

        return startWarp;
    }

    /**
     * @param pathId    The ID of the path
     * @param chapterId The ID of the chapter
     * @return The chapter start position for the given path
     */
    public @Nullable BlockPos getChapterStartCoordinates(String pathId, String chapterId)
    {
        BlockPos startPosition = null;
        Optional<PathData> pathData = paths.stream()
                .filter(item -> pathId.equals(item.getId()))
                .findFirst();

        if (pathData.isPresent()) {

            String warpData = "";
            ChapterData chapterData = pathData.get().getChapter(chapterId);

            if (chapterData != null) {

                warpData = chapterData.getWarp();

                if (warpData != null && ! warpData.isBlank())  {

                    // Check if the warp is coordinates
                    boolean isCoordinates = warpData.matches("^[+-]?\\d+\\s+[+-]?\\d+\\s+[+-]?\\d+$");

                    if (isCoordinates) {
                        String[] coords = warpData.trim().split("\\s+");
                        startPosition = new BlockPos(Integer.parseInt(coords[0]), Integer.parseInt(coords[1]), Integer.parseInt(coords[2]));
                    }
                }
            }
        }

        return startPosition != null ? startPosition : getChapterStartPosition(pathId, chapterId);
    }

    private BlockPos getChapterStartPosition(String pathId, String chapterId) {

        var data = chapterStarts.get(pathId + ":" + chapterId);

        return data != null ? data.toBlockPos() : null;
    }

    /**
     * Sets the chapter start position for the given path.
     *
     * @param pathId    The ID of the path
     * @param chapterId The ID of the chapter
     * @param pos       The chapter start position
     */
    public void setChapterStart(String pathId, String chapterId, PositionData pos)
    {
        chapterStarts.put(pathId + ":" + chapterId, pos);
    }

    /**
     * Removes the chapter start position for the given path.
     *
     * @param pathId    The ID of the path
     * @param chapterId The ID of the chapter
     */
    public void removeChapterStart(String pathId, String chapterId)
    {
        chapterStarts.remove(pathId + ":" + chapterId);
    }
}

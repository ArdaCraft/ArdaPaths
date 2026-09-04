package space.ajcool.ardapaths.core.data.config.server;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import space.ajcool.ardapaths.core.data.WarpTarget;
import space.ajcool.ardapaths.core.data.config.shared.ChapterData;
import space.ajcool.ardapaths.core.data.config.shared.PathData;
import space.ajcool.ardapaths.core.data.config.shared.PositionData;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Server-side configuration containing all paths and chapter metadata.
 * This is the authoritative source for path data on the server.
 * Serialized to and from JSON in server.json.
 */
public class ServerConfig {

    /**
     * List of all paths available on this server.
     */
    @Getter
    // Populated by Gson reflective deserialization; IntelliJ cannot trace the field access.
    @SuppressWarnings("unused")
    @SerializedName("paths")
    private final List<PathData> paths = new ArrayList<>();

    /**
     * Get a path by its ID.
     *
     * @param id the path ID to look up
     * @return the path data, or null if not found
     */
    public @Nullable PathData getPath(String id) {
        for (PathData path : paths) {
            if (path.getId().equalsIgnoreCase(id)) {
                return path;
            }
        }
        return null;
    }

    /**
     * Adds a path to the list of paths available on this server.
     *
     * @param path The path to add
     */
    public void addPath(PathData path) {
        for (PathData p : paths) {
            if (p.getId().equalsIgnoreCase(path.getId())) {
                return;
            }
        }
        paths.add(path);
    }

    /**
     * @param pathId    The ID of the path
     * @param chapterId The ID of the chapter
     * @return The chapter start position for the given path
     */
    public @NotNull Optional<String> getChapterStartWarp(String pathId, String chapterId) {
        Optional<String> startWarp = Optional.empty();
        ChapterData chapterData = resolveChapter(pathId, chapterId);

        if (chapterData != null) {

            String warpData = chapterData.getWarp();

            if (warpData != null && !warpData.isBlank()) {

                if (!WarpTarget.isCoordinates(warpData)) {
                    startWarp = Optional.of(warpData.trim());
                }
            }
        }

        return startWarp;
    }

    /**
     * Resolves a chapter from the authoritative path list.
     *
     * @param pathId    The ID of the path
     * @param chapterId The ID of the chapter
     * @return chapter data, or null when the path or chapter is unknown
     */
    private @Nullable ChapterData resolveChapter(String pathId, String chapterId) {
        Optional<PathData> pathData = paths.stream()
                .filter(item -> pathId.equals(item.getId()))
                .findFirst();
        return pathData.map(path -> path.getChapter(chapterId)).orElse(null);
    }

    /**
     * Checks whether the supplied block position is a configured chapter start.
     *
     * @param pos the position to validate
     * @return true when any configured chapter start resolves to the supplied position
     */
    public boolean isChapterStartPosition(BlockPos pos) {
        for (PathData path : paths) {
            for (ChapterData chapter : path.getChapters()) {
                BlockPos chapterStart = getChapterStartCoordinates(path.getId(), chapter.getId());
                if (pos.equals(chapterStart)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * @param pathId    The ID of the path
     * @param chapterId The ID of the chapter
     * @return The chapter start position for the given path
     */
    public @Nullable BlockPos getChapterStartCoordinates(String pathId, String chapterId) {
        ChapterData chapter = resolveChapter(pathId, chapterId);
        PositionData coordinates = chapter == null ? null : chapter.getCoordinates();
        return coordinates == null ? null : coordinates.toBlockPos();
    }

    /**
     * Gets the dimension for a configured coordinate chapter start.
     *
     * @param pathId    The ID of the path
     * @param chapterId The ID of the chapter
     * @return dimension identifier, or null when no coordinates are configured
     */
    public @Nullable String getChapterStartDimension(String pathId, String chapterId) {
        ChapterData chapter = resolveChapter(pathId, chapterId);
        if (chapter == null || chapter.getCoordinates() == null) return null;
        return chapter.getDimension();
    }

    /**
     * Sets the chapter start position for the given path.
     *
     * @param pathId    The ID of the path
     * @param chapterId The ID of the chapter
     * @param pos       The chapter start position
     * @param dimension The dimension containing the chapter start position
     */
    public void setChapterStart(String pathId, String chapterId, PositionData pos, String dimension) {
        ChapterData chapter = resolveChapter(pathId, chapterId);
        if (chapter == null) return;
        chapter.setCoordinates(pos);
        chapter.setDimension(dimension);
    }

    /**
     * Removes the chapter start position for the given path when it matches the expected position.
     *
     * @param pathId      The ID of the path
     * @param chapterId   The ID of the chapter
     * @param expectedPos Position the client believes is currently recorded
     * @return true when the chapter start was present and removed
     */
    public boolean removeChapterStart(String pathId, String chapterId, PositionData expectedPos) {
        ChapterData chapter = resolveChapter(pathId, chapterId);
        PositionData recordedStart = chapter == null ? null : chapter.getCoordinates();
        if (!expectedPos.equals(recordedStart)) return false;

        chapter.setCoordinates(null);
        chapter.setDimension(null);
        return true;
    }
}

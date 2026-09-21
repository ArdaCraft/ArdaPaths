package space.ajcool.ardapaths.core.backup.dto;

import com.google.gson.annotations.SerializedName;
import space.ajcool.ardapaths.core.data.config.shared.PositionData;

import java.util.List;

/**
 * Exported chapter definition plus its marker nodes.
 *
 * @param id       chapter identifier
 * @param name     chapter display name
 * @param index    chapter order index
 * @param warp     chapter warp destination
 * @param coordinates chapter start coordinates, or null when unset
 * @param dimension dimension identifier containing the chapter start coordinates, or null when unset
 * @param startPos legacy packed chapter start position used for reading older backups
 * @param nodes    marker nodes belonging to this path chapter
 */
public record PathChapterDto(
        @SerializedName("id") String id,
        @SerializedName("name") String name,
        @SerializedName("index") int index,
        @SerializedName("warp") String warp,
        @SerializedName("coordinates") PositionData coordinates,
        @SerializedName("dimension") String dimension,
        @SerializedName("start_pos") Long startPos,
        @SerializedName("nodes") List<PathNodeDto> nodes
) {
    /**
     * Creates an export chapter DTO without the legacy {@code start_pos} write-only shape.
     *
     * @param id          chapter identifier
     * @param name        chapter display name
     * @param index       chapter order index
     * @param warp        chapter warp destination
     * @param coordinates chapter start coordinates, or null when unset
     * @param dimension   dimension identifier containing the chapter start coordinates, or null when unset
     * @param nodes       marker nodes belonging to this path chapter
     */
    public PathChapterDto(String id, String name, int index, String warp, PositionData coordinates, String dimension, List<PathNodeDto> nodes) {
        this(id, name, index, warp, coordinates, dimension, null, nodes);
    }
}

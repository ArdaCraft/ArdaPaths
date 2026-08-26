package space.ajcool.ardapaths.core.backup.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Exported chapter definition plus its marker nodes.
 *
 * @param id       chapter identifier
 * @param name     chapter display name
 * @param date     chapter in-game date
 * @param index    chapter order index
 * @param warp     chapter warp destination
 * @param startPos packed chapter start position, or null when unset
 * @param nodes    marker nodes belonging to this path chapter
 */
public record PathChapterDto(
        @SerializedName("id") String id,
        @SerializedName("name") String name,
        @SerializedName("date") String date,
        @SerializedName("index") int index,
        @SerializedName("warp") String warp,
        @SerializedName("start_pos") Long startPos,
        @SerializedName("nodes") List<PathNodeDto> nodes
) {
}

package space.ajcool.ardapaths.core.backup.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Exported per-path chapter data for one marker.
 *
 * @param dimension           dimension identifier containing the marker
 * @param pos                 packed absolute marker position
 * @param next                packed absolute next marker position, or null when unlinked
 * @param chapterStart        whether this node starts its chapter
 * @param titleOnTrail        whether this node shows the chapter title while trailing
 * @param displayAboveBlocks  whether this node's trail renders above terrain
 * @param weather             configured weather ordinal, or null for legacy unset data
 * @param timeOfDay           configured time of day in ticks, or null for legacy unset data
 * @param timeTransitionRange configured time transition range in blocks, or null for legacy backups
 * @param autoTeleportTarget  target coordinates or warp name triggered by this node, or null for legacy backups
 * @param lookAt              absolute look-at coordinates as {@code x y z} text, or null for legacy backups
 * @param giveItem            item identifier granted by this node, or null for legacy backups
 * @param message             proximity message text
 * @param range               proximity activation range
 * @param anim                proximity animation settings
 */
public record PathNodeDto(
        @SerializedName("dimension") String dimension,
        @SerializedName("pos") long pos,
        @SerializedName("next") Long next,
        @SerializedName("chapter_start") boolean chapterStart,
        @SerializedName("title_on_trail") boolean titleOnTrail,
        @SerializedName("display_above_blocks") boolean displayAboveBlocks,
        @SerializedName("weather") Integer weather,
        @SerializedName("time_of_day") Integer timeOfDay,
        @SerializedName("time_transition_range") Integer timeTransitionRange,
        @SerializedName("auto_teleport_target") String autoTeleportTarget,
        @SerializedName("look_at") String lookAt,
        @SerializedName("give_item") String giveItem,
        @SerializedName("message") String message,
        @SerializedName("range") int range,
        @SerializedName("anim") NodeAnimDto anim
) {

}

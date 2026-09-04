package space.ajcool.ardapaths.core.backup.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Exported proximity animation settings.
 *
 * @param packed  packed animation data as stored on the marker
 * @param decoded decoded animation values for human inspection
 */
public record NodeAnimDto(
        @SerializedName("packed") long packed,
        @SerializedName("decoded") int[] decoded
) {

}

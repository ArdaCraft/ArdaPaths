package space.ajcool.ardapaths.core.backup.dto;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

/**
 * Human-readable index of marker positions by dimension.
 *
 * @param markers map of dimension identifiers to packed-position keys and [x, y, z] values
 */
public record MarkerIndexDto(
        @SerializedName("markers") Map<String, Map<String, int[]>> markers
) {

}

package space.ajcool.ardapaths.core.backup.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Aggregate counts written into the backup manifest.
 *
 * @param dimensions the number of dimensions with exported markers
 * @param markers    the number of marker block entities exported
 * @param paths      the number of path files exported
 * @param chapters   the number of chapters exported
 * @param nodes      the number of per-path chapter marker nodes exported
 */
@SuppressWarnings("unused")
public record BackupCountsDto(
        @SerializedName("dimensions") int dimensions,
        @SerializedName("markers") int markers,
        @SerializedName("paths") int paths,
        @SerializedName("chapters") int chapters,
        @SerializedName("nodes") int nodes
) {

}

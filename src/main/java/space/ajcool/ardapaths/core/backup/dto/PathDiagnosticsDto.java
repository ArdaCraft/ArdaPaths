package space.ajcool.ardapaths.core.backup.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

/**
 * Informational graph diagnostics for exported path files.
 *
 * @param orphans      node positions that no exported node links to
 * @param danglingNext next positions that do not point at exported markers
 * @param cycles       cycle descriptions detected while following next links
 * @param multiRoot    chapters with more than one detected root node
 */
public record PathDiagnosticsDto(
        @SerializedName("orphans") List<Long> orphans,
        @SerializedName("dangling_next") List<Long> danglingNext,
        @SerializedName("cycles") List<List<Long>> cycles,
        @SerializedName("multi_root") Map<String, List<Long>> multiRoot
) {

}

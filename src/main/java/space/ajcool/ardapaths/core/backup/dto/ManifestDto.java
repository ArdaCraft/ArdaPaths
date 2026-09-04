package space.ajcool.ardapaths.core.backup.dto;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

/**
 * Self-checking manifest for an ArdaPaths backup directory.
 *
 * @param schemaVersion the export schema version
 * @param created       the ISO-8601 timestamp when the manifest was generated
 * @param counts        the exported object counts
 * @param files         the per-file SHA-256 hashes keyed by relative path
 */
public record ManifestDto(
        @SerializedName("schema_version") int schemaVersion,
        @SerializedName("created") String created,
        @SerializedName("counts") BackupCountsDto counts,
        @SerializedName("files") Map<String, String> files
) {

}

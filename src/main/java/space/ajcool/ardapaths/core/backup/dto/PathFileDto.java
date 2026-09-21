package space.ajcool.ardapaths.core.backup.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Exported path definition file.
 *
 * @param id          path identifier
 * @param name        path display name
 * @param colors      path trail colours
 * @param hideDefault whether the default chapter is hidden from player-facing chapter lists
 * @param chapters    exported chapter definitions
 * @param diagnostics informational path graph diagnostics
 */
public record PathFileDto(
        @SerializedName("id") String id,
        @SerializedName("name") String name,
        @SerializedName("colors") PathColorDto colors,
        @SerializedName("hideDefault") boolean hideDefault,
        @SerializedName("chapters") List<PathChapterDto> chapters,
        @SerializedName("diagnostics") PathDiagnosticsDto diagnostics
) {

}

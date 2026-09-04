package space.ajcool.ardapaths.core.backup.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Exported path colour triplet.
 *
 * @param primary   primary RGB colour
 * @param secondary secondary RGB colour
 * @param tertiary  tertiary RGB colour
 */
public record PathColorDto(
        @SerializedName("primary") int[] primary,
        @SerializedName("secondary") int[] secondary,
        @SerializedName("tertiary") int[] tertiary
) {

}

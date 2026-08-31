package space.ajcool.ardapaths.core.data;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import space.ajcool.ardapaths.screens.widgets.TextValidationError;

/**
 * Utility for converting marker references between clipboard text and structured data.
 */
public final class MarkerId {
    /**
     * Prevents construction of this utility class.
     */
    private MarkerId() {
    }

    /**
     * Formats a marker position into a copyable marker ID.
     *
     * @param pos the marker position
     * @return marker ID as a packed block position
     */
    public static String format(BlockPos pos) {
        return String.valueOf(pos.asLong());
    }

    /**
     * Parses a copyable marker ID into a packed marker position.
     *
     * @param text marker ID text
     * @return packed absolute marker block position
     * @throws TextValidationError when the ID is blank or malformed
     */
    public static long parse(String text) throws TextValidationError {
        if (text == null || text.isBlank()) {
            throw new TextValidationError(Component.translatable("ardapaths.client.marker.configuration.screens.marker_id.invalid").getString());
        }

        try {
            return Long.parseLong(text.trim());
        } catch (NumberFormatException e) {
            throw new TextValidationError(Component.translatable("ardapaths.client.marker.configuration.screens.marker_id.invalid").getString());
        }
    }
}

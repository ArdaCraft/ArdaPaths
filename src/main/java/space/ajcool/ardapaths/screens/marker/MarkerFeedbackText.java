package space.ajcool.ardapaths.screens.marker;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import space.ajcool.ardapaths.core.data.TimeSpreadStatus;
import space.ajcool.ardapaths.core.networking.packets.client.MarkerBulkClearResponsePacket;
import space.ajcool.ardapaths.core.networking.packets.client.MarkerTimeSpreadResponsePacket;
import space.ajcool.ardapaths.core.networking.packets.client.PathMarkerRemoteDataResponsePacket;

import java.util.Locale;

/**
 * Localized feedback text builders for marker editor server responses.
 */
public final class MarkerFeedbackText {
    /**
     * Prevents construction of the static feedback-text helper.
     */
    private MarkerFeedbackText() {
    }

    /**
     * Converts a time-spread response into localized feedback text.
     *
     * @param response server response packet
     * @return localized status text
     */
    public static Component timeSpreadStatusText(MarkerTimeSpreadResponsePacket response) {
        if (response.status() == TimeSpreadStatus.CHAIN_BROKEN || response.status() == TimeSpreadStatus.CHAIN_ENDED) {
            if (response.lastValidPos() == null) {
                return Component.translatable("ardapaths.client.marker.configuration.screens.marker.time_spread.status.invalid_data");
            }

            BlockPos pos = response.lastValidPos();
            return Component.translatable(statusKey("time_spread", response.status()), pos.getX(), pos.getY(), pos.getZ());
        }

        return Component.translatable(statusKey("time_spread", response.status()), response.updatedCount());
    }

    /**
     * Converts a bulk-clear response into localized feedback text.
     *
     * @param response server response packet
     * @return localized status text
     */
    public static Component bulkClearStatusText(MarkerBulkClearResponsePacket response) {
        return Component.translatable(statusKey("bulk_clear", response.status()), response.updatedCount());
    }

    /**
     * Converts a remote path marker data response into localized feedback text.
     *
     * @param response server response packet
     * @return localized status text
     */
    public static Component remoteMarkerStatusText(PathMarkerRemoteDataResponsePacket response) {
        return Component.translatable(statusKey("load", response.status()));
    }

    /**
     * Builds the shared marker editor status translation key.
     *
     * @param operation marker operation segment in the translation key
     * @param status    operation status enum
     * @return full translation key for the status
     */
    private static String statusKey(String operation, Enum<?> status) {
        return "ardapaths.client.marker.configuration.screens.marker." + operation + ".status." + status.name().toLowerCase(Locale.ROOT);
    }
}

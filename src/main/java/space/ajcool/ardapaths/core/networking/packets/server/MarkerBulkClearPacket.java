package space.ajcool.ardapaths.core.networking.packets.server;

import net.fabricmc.fabric.api.networking.v1.FriendlyByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import space.ajcool.ardapaths.core.ModConstants;
import space.ajcool.ardapaths.core.consumers.networking.IRespondablePacket;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Packet sent from client to server to clear environment fields on selected markers.
 *
 * @param requestId       request correlation id
 * @param packedPositions packed absolute marker positions selected by the editor
 * @param pathId          path identifier for the marker chapter data
 * @param chapterId       chapter identifier for the marker chapter data
 * @param clearTime       whether marker time fields should be cleared
 * @param clearWeather    whether marker weather fields should be cleared
 */
public record MarkerBulkClearPacket(UUID requestId, List<Long> packedPositions, String pathId, String chapterId,
                                    boolean clearTime,
                                    boolean clearWeather) implements IRespondablePacket<MarkerBulkClearPacket> {

    /**
     * Network channel used for marker bulk-clear requests.
     */
    public static final Identifier CHANNEL = ModConstants.modId("marker_bulk_clear");

    /**
     * Custom payload type used for typed Fabric networking.
     */
    public static final CustomPacketPayload.Type<MarkerBulkClearPacket> TYPE = new CustomPacketPayload.Type<>(CHANNEL);

    /**
     * Creates a bulk-clear request before request correlation is assigned.
     *
     * @param packedPositions packed absolute marker positions selected by the editor
     * @param pathId          path identifier for the marker chapter data
     * @param chapterId       chapter identifier for the marker chapter data
     * @param clearTime       whether marker time fields should be cleared
     * @param clearWeather    whether marker weather fields should be cleared
     */
    public MarkerBulkClearPacket(List<Long> packedPositions, String pathId, String chapterId, boolean clearTime, boolean clearWeather) {
        this(IRespondablePacket.UNASSIGNED_REQUEST_ID, packedPositions, pathId, chapterId, clearTime, clearWeather);
    }

    /**
     * Reads a bulk clear request packet from a network buffer.
     *
     * @param buf packet data buffer
     * @return decoded packet
     */
    public static MarkerBulkClearPacket read(FriendlyByteBuf buf) {
        UUID requestId = buf.readUUID();
        int count = buf.readInt();
        List<Long> packedPositions = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            packedPositions.add(buf.readLong());
        }
        return new MarkerBulkClearPacket(requestId, packedPositions, buf.readUtf(), buf.readUtf(), buf.readBoolean(), buf.readBoolean());
    }

    /**
     * Creates a request with the supplied request id.
     *
     * @param requestId request correlation id
     * @return packet carrying the supplied request id
     */
    @Override
    public MarkerBulkClearPacket withRequestId(UUID requestId) {
        return new MarkerBulkClearPacket(requestId, packedPositions, pathId, chapterId, clearTime, clearWeather);
    }

    /**
     * Gets the custom payload type for this packet.
     *
     * @return this packet's payload type
     */
    @Override
    public CustomPacketPayload.@NotNull Type<MarkerBulkClearPacket> type() {
        return TYPE;
    }

    @Override
    public FriendlyByteBuf build() {
        FriendlyByteBuf buf = FriendlyByteBufs.create();
        buf.writeUUID(requestId);
        buf.writeInt(packedPositions.size());
        for (Long packedPosition : packedPositions) {
            buf.writeLong(packedPosition);
        }
        buf.writeUtf(pathId);
        buf.writeUtf(chapterId);
        buf.writeBoolean(clearTime);
        buf.writeBoolean(clearWeather);
        return buf;
    }
}

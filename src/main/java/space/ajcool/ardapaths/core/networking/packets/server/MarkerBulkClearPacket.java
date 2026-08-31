package space.ajcool.ardapaths.core.networking.packets.server;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
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
public record MarkerBulkClearPacket(UUID requestId, List<Long> packedPositions, String pathId, String chapterId, boolean clearTime, boolean clearWeather) implements IRespondablePacket<MarkerBulkClearPacket> {
    /**
     * Network channel used for marker bulk-clear requests.
     */
    public static final ResourceLocation CHANNEL = ModConstants.modId("marker_bulk_clear");

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
     * Serializes this request for client-to-server transmission.
     *
     * @return packet data buffer
     */
    @Override
    public FriendlyByteBuf build() {
        FriendlyByteBuf buf = PacketByteBufs.create();
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
}

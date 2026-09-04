package space.ajcool.ardapaths.core.networking.packets.server;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import space.ajcool.ardapaths.core.ModConstants;
import space.ajcool.ardapaths.core.consumers.networking.IRespondablePacket;

import java.util.UUID;

/**
 * Packet sent from client to server to request server-computed marker time progression.
 *
 * @param requestId       request correlation id
 * @param sourcePackedPos packed absolute source marker position
 * @param targetPackedPos packed absolute target marker position
 * @param sourceTime      expected source marker time in daytime ticks
 * @param targetTime      expected target marker time in daytime ticks
 * @param pathId          path identifier for the marker chapter chain
 * @param chapterId       chapter identifier for the marker chapter chain
 * @param clear           whether the request should clear existing time data instead of spreading it
 */
public record MarkerTimeSpreadPacket(UUID requestId, long sourcePackedPos, long targetPackedPos, int sourceTime,
                                     int targetTime, String pathId, String chapterId,
                                     boolean clear) implements IRespondablePacket<MarkerTimeSpreadPacket> {

    /**
     * Network channel used for marker time-spread requests.
     */
    public static final ResourceLocation CHANNEL = ModConstants.modId("marker_time_spread");

    /**
     * Custom payload type used for typed Fabric networking.
     */
    public static final CustomPacketPayload.Type<MarkerTimeSpreadPacket> TYPE = new CustomPacketPayload.Type<>(CHANNEL);

    /**
     * Creates a time-spread request before request correlation is assigned.
     *
     * @param sourcePackedPos packed absolute source marker position
     * @param targetPackedPos packed absolute target marker position
     * @param sourceTime      expected source marker time in daytime ticks
     * @param targetTime      expected target marker time in daytime ticks
     * @param pathId          path identifier for the marker chapter chain
     * @param chapterId       chapter identifier for the marker chapter chain
     * @param clear           whether the request should clear existing time data instead of spreading it
     */
    public MarkerTimeSpreadPacket(long sourcePackedPos, long targetPackedPos, int sourceTime, int targetTime, String pathId, String chapterId, boolean clear) {
        this(IRespondablePacket.UNASSIGNED_REQUEST_ID, sourcePackedPos, targetPackedPos, sourceTime, targetTime, pathId, chapterId, clear);
    }

    /**
     * Reads a marker time-spread packet from a network buffer.
     *
     * @param buf packet data buffer
     * @return decoded packet
     */
    public static MarkerTimeSpreadPacket read(FriendlyByteBuf buf) {
        return new MarkerTimeSpreadPacket(buf.readUUID(), buf.readLong(), buf.readLong(), buf.readInt(), buf.readInt(), buf.readUtf(), buf.readUtf(), buf.readBoolean());
    }

    /**
     * Creates a request with the supplied request id.
     *
     * @param requestId request correlation id
     * @return packet carrying the supplied request id
     */
    @Override
    public MarkerTimeSpreadPacket withRequestId(UUID requestId) {
        return new MarkerTimeSpreadPacket(requestId, sourcePackedPos, targetPackedPos, sourceTime, targetTime, pathId, chapterId, clear);
    }

    /**
     * Gets the custom payload type for this packet.
     *
     * @return this packet's payload type
     */
    @Override
    public CustomPacketPayload.@NotNull Type<MarkerTimeSpreadPacket> type() {
        return TYPE;
    }

    @Override
    public FriendlyByteBuf build() {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeUUID(requestId);
        buf.writeLong(sourcePackedPos);
        buf.writeLong(targetPackedPos);
        buf.writeInt(sourceTime);
        buf.writeInt(targetTime);
        buf.writeUtf(pathId);
        buf.writeUtf(chapterId);
        buf.writeBoolean(clear);
        return buf;
    }
}

package space.ajcool.ardapaths.core.networking.packets.client;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import space.ajcool.ardapaths.core.ModConstants;
import space.ajcool.ardapaths.core.consumers.networking.IRespondablePacket;
import space.ajcool.ardapaths.core.data.TimeSpreadStatus;

import java.util.UUID;

/**
 * Packet sent from server to client with the result of a marker time-spread request.
 *
 * @param requestId    request correlation id
 * @param status       status code describing the result
 * @param updatedCount number of markers updated by the server
 * @param lastValidPos last valid marker position for broken-chain diagnostics
 */
public record MarkerTimeSpreadResponsePacket(UUID requestId, TimeSpreadStatus status, int updatedCount, @Nullable BlockPos lastValidPos) implements IRespondablePacket<MarkerTimeSpreadResponsePacket> {
    /**
     * Network channel used for marker time-spread responses.
     */
    public static final ResourceLocation CHANNEL = ModConstants.modId("marker_time_spread_response");

    /**
     * Creates a time-spread response before request correlation is assigned.
     *
     * @param status       status code describing the result
     * @param updatedCount number of markers updated by the server
     * @param lastValidPos last valid marker position for broken-chain diagnostics
     */
    public MarkerTimeSpreadResponsePacket(TimeSpreadStatus status, int updatedCount, @Nullable BlockPos lastValidPos) {
        this(IRespondablePacket.UNASSIGNED_REQUEST_ID, status, updatedCount, lastValidPos);
    }

    /**
     * Creates a response with the supplied request id.
     *
     * @param requestId request correlation id
     * @return packet carrying the supplied request id
     */
    @Override
    public MarkerTimeSpreadResponsePacket withRequestId(UUID requestId) {
        return new MarkerTimeSpreadResponsePacket(requestId, status, updatedCount, lastValidPos);
    }

    /**
     * Serializes this response for server-to-client transmission.
     *
     * @return packet data buffer
     */
    @Override
    public FriendlyByteBuf build() {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeUUID(requestId);
        buf.writeEnum(status);
        buf.writeInt(updatedCount);
        buf.writeBoolean(lastValidPos != null);
        if (lastValidPos != null) {
            buf.writeBlockPos(lastValidPos);
        }
        return buf;
    }

    /**
     * Reads a marker time-spread response packet from a network buffer.
     *
     * @param buf packet data buffer
     * @return decoded packet
     */
    public static MarkerTimeSpreadResponsePacket read(FriendlyByteBuf buf) {
        UUID requestId = buf.readUUID();
        TimeSpreadStatus status = buf.readEnum(TimeSpreadStatus.class);
        int updatedCount = buf.readInt();
        BlockPos lastValidPos = buf.readBoolean() ? buf.readBlockPos() : null;
        return new MarkerTimeSpreadResponsePacket(requestId, status, updatedCount, lastValidPos);
    }
}

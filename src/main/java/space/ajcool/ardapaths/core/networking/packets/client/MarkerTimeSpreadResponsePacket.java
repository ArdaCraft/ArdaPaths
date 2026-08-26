package space.ajcool.ardapaths.core.networking.packets.client;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import space.ajcool.ardapaths.core.consumers.networking.IPacket;
import space.ajcool.ardapaths.core.data.TimeSpreadStatus;

/**
 * Packet sent from server to client with the result of a marker time-spread request.
 *
 * @param status       status code describing the result
 * @param updatedCount number of markers updated by the server
 * @param lastValidPos last valid marker position for broken-chain diagnostics
 */
public record MarkerTimeSpreadResponsePacket(TimeSpreadStatus status, int updatedCount, @Nullable BlockPos lastValidPos) implements IPacket {
    /**
     * Serializes this response for server-to-client transmission.
     *
     * @return packet data buffer
     */
    @Override
    public PacketByteBuf build() {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeEnumConstant(status);
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
    public static MarkerTimeSpreadResponsePacket read(PacketByteBuf buf) {
        TimeSpreadStatus status = buf.readEnumConstant(TimeSpreadStatus.class);
        int updatedCount = buf.readInt();
        BlockPos lastValidPos = buf.readBoolean() ? buf.readBlockPos() : null;
        return new MarkerTimeSpreadResponsePacket(status, updatedCount, lastValidPos);
    }
}

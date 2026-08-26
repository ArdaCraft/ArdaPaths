package space.ajcool.ardapaths.core.networking.packets.client;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import space.ajcool.ardapaths.core.consumers.networking.IPacket;
import space.ajcool.ardapaths.core.data.TimeSpreadStatus;

/**
 * Packet sent from server to client with the result of a marker bulk-clear request.
 *
 * @param status       status code describing the result
 * @param updatedCount number of markers updated by the server
 */
public record MarkerBulkClearResponsePacket(TimeSpreadStatus status, int updatedCount) implements IPacket {
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
        return buf;
    }

    /**
     * Reads a marker bulk-clear response packet from a network buffer.
     *
     * @param buf packet data buffer
     * @return decoded packet
     */
    public static MarkerBulkClearResponsePacket read(PacketByteBuf buf) {
        return new MarkerBulkClearResponsePacket(buf.readEnumConstant(TimeSpreadStatus.class), buf.readInt());
    }
}

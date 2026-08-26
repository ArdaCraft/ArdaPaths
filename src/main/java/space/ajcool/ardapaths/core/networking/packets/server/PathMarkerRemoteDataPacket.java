package space.ajcool.ardapaths.core.networking.packets.server;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import space.ajcool.ardapaths.core.consumers.networking.IPacket;

/**
 * Packet sent from client to server to request one path marker that is not currently loaded on the client.
 *
 * @param packedPos packed position of the requested path marker
 */
public record PathMarkerRemoteDataPacket(long packedPos) implements IPacket {
    /**
     * Serializes this request for client-to-server transmission.
     *
     * @return packet data buffer
     */
    @Override
    public PacketByteBuf build() {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeLong(packedPos);
        return buf;
    }

    /**
     * Reads a remote path marker data request packet from a network buffer.
     *
     * @param buf packet data buffer
     * @return decoded packet
     */
    public static PathMarkerRemoteDataPacket read(PacketByteBuf buf) {
        return new PathMarkerRemoteDataPacket(buf.readLong());
    }
}

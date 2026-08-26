package space.ajcool.ardapaths.core.networking.packets.client;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import space.ajcool.ardapaths.core.consumers.networking.IPacket;
import space.ajcool.ardapaths.core.data.PathMarkerRemoteDataStatus;

/**
 * Packet sent from server to client with data for one path marker that is not currently loaded on the client.
 *
 * @param status    status code describing the result
 * @param packedPos packed position of the requested path marker
 * @param data      path marker NBT data when the request succeeds
 */
public record PathMarkerRemoteDataResponsePacket(PathMarkerRemoteDataStatus status, long packedPos, NbtCompound data) implements IPacket {
    /**
     * Serializes this response for server-to-client transmission.
     *
     * @return packet data buffer
     */
    @Override
    public PacketByteBuf build() {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeEnumConstant(status);
        buf.writeLong(packedPos);
        buf.writeNbt(data);
        return buf;
    }

    /**
     * Reads a remote path marker data response packet from a network buffer.
     *
     * @param buf packet data buffer
     * @return decoded packet
     */
    public static PathMarkerRemoteDataResponsePacket read(PacketByteBuf buf) {
        return new PathMarkerRemoteDataResponsePacket(
                buf.readEnumConstant(PathMarkerRemoteDataStatus.class),
                buf.readLong(),
                buf.readNbt()
        );
    }
}

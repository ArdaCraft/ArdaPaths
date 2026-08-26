package space.ajcool.ardapaths.core.networking.packets.server;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import space.ajcool.ardapaths.core.consumers.networking.IPacket;

/**
 * Packet sent from client to server to request a complete chapter marker chain.
 *
 * @param pathId           path identifier for the requested chapter
 * @param chapterId        chapter identifier for the requested marker chain
 * @param currentPackedPos packed position of the marker currently being edited
 */
public record ChapterPathMarkersPacket(String pathId, String chapterId, long currentPackedPos) implements IPacket {
    /**
     * Serializes this request for client-to-server transmission.
     *
     * @return packet data buffer
     */
    @Override
    public PacketByteBuf build() {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(pathId);
        buf.writeString(chapterId);
        buf.writeLong(currentPackedPos);
        return buf;
    }

    /**
     * Reads a chapter marker list request packet from a network buffer.
     *
     * @param buf packet data buffer
     * @return decoded packet
     */
    public static ChapterPathMarkersPacket read(PacketByteBuf buf) {
        return new ChapterPathMarkersPacket(buf.readString(), buf.readString(), buf.readLong());
    }
}

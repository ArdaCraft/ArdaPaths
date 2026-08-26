package space.ajcool.ardapaths.core.networking.packets.server;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import space.ajcool.ardapaths.core.consumers.networking.IPacket;

/**
 * Packet sent from client to server to request server-computed marker time progression.
 *
 * @param sourcePackedPos packed absolute source marker position
 * @param targetPackedPos packed absolute target marker position
 * @param sourceTime      expected source marker time in daytime ticks
 * @param targetTime      expected target marker time in daytime ticks
 * @param pathId          path identifier for the marker chapter chain
 * @param chapterId       chapter identifier for the marker chapter chain
 * @param clear           whether the request should clear existing time data instead of spreading it
 */
public record MarkerTimeSpreadPacket(long sourcePackedPos, long targetPackedPos, int sourceTime, int targetTime, String pathId, String chapterId, boolean clear) implements IPacket {
    /**
     * Serializes this request for client-to-server transmission.
     *
     * @return packet data buffer
     */
    @Override
    public PacketByteBuf build() {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeLong(sourcePackedPos);
        buf.writeLong(targetPackedPos);
        buf.writeInt(sourceTime);
        buf.writeInt(targetTime);
        buf.writeString(pathId);
        buf.writeString(chapterId);
        buf.writeBoolean(clear);
        return buf;
    }

    /**
     * Reads a marker time-spread packet from a network buffer.
     *
     * @param buf packet data buffer
     * @return decoded packet
     */
    public static MarkerTimeSpreadPacket read(PacketByteBuf buf) {
        return new MarkerTimeSpreadPacket(buf.readLong(), buf.readLong(), buf.readInt(), buf.readInt(), buf.readString(), buf.readString(), buf.readBoolean());
    }
}

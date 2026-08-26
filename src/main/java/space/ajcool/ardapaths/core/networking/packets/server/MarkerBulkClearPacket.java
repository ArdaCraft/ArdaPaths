package space.ajcool.ardapaths.core.networking.packets.server;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import space.ajcool.ardapaths.core.consumers.networking.IPacket;

import java.util.ArrayList;
import java.util.List;

/**
 * Packet sent from client to server to clear environment fields on selected markers.
 *
 * @param packedPositions packed absolute marker positions selected by the editor
 * @param pathId          path identifier for the marker chapter data
 * @param chapterId       chapter identifier for the marker chapter data
 * @param clearTime       whether marker time fields should be cleared
 * @param clearWeather    whether marker weather fields should be cleared
 */
public record MarkerBulkClearPacket(List<Long> packedPositions, String pathId, String chapterId, boolean clearTime, boolean clearWeather) implements IPacket {
    /**
     * Serializes this request for client-to-server transmission.
     *
     * @return packet data buffer
     */
    @Override
    public PacketByteBuf build() {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(packedPositions.size());
        for (Long packedPosition : packedPositions) {
            buf.writeLong(packedPosition);
        }
        buf.writeString(pathId);
        buf.writeString(chapterId);
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
    public static MarkerBulkClearPacket read(PacketByteBuf buf) {
        int count = buf.readInt();
        List<Long> packedPositions = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            packedPositions.add(buf.readLong());
        }
        return new MarkerBulkClearPacket(packedPositions, buf.readString(), buf.readString(), buf.readBoolean(), buf.readBoolean());
    }
}

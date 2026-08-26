package space.ajcool.ardapaths.core.networking.packets.server;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;
import space.ajcool.ardapaths.core.consumers.networking.IPacket;

/**
 * Packet sent from client to server when a player reaches an authored marker action node.
 *
 * @param markerPos the block position of the marker whose actions may trigger
 * @param pathId    the selected path identifier
 * @param chapterId the selected chapter identifier
 */
public record MarkerActionTriggerPacket(BlockPos markerPos, String pathId, String chapterId) implements IPacket {
    /**
     * Builds this marker-action trigger into a packet buffer.
     *
     * @return packet buffer containing the marker position and path chapter ids
     */
    @Override
    public PacketByteBuf build() {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(markerPos);
        buf.writeString(pathId);
        buf.writeString(chapterId);
        return buf;
    }

    /**
     * Reads a marker-action trigger from a packet buffer.
     *
     * @param buf packet buffer to read
     * @return decoded marker-action trigger packet
     */
    public static MarkerActionTriggerPacket read(PacketByteBuf buf) {
        BlockPos markerPos = buf.readBlockPos();
        String pathId = buf.readString();
        String chapterId = buf.readString();
        return new MarkerActionTriggerPacket(markerPos, pathId, chapterId);
    }
}

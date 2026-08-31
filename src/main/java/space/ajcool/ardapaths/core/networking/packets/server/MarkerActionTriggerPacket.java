package space.ajcool.ardapaths.core.networking.packets.server;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import space.ajcool.ardapaths.core.ModConstants;
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
     * Network channel used for authored marker action triggers.
     */
    public static final ResourceLocation CHANNEL = ModConstants.modId("marker_action_trigger");

    /**
     * Builds this marker-action trigger into a packet buffer.
     *
     * @return packet buffer containing the marker position and path chapter ids
     */
    @Override
    public FriendlyByteBuf build() {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(markerPos);
        buf.writeUtf(pathId);
        buf.writeUtf(chapterId);
        return buf;
    }

    /**
     * Reads a marker-action trigger from a packet buffer.
     *
     * @param buf packet buffer to read
     * @return decoded marker-action trigger packet
     */
    public static MarkerActionTriggerPacket read(FriendlyByteBuf buf) {
        BlockPos markerPos = buf.readBlockPos();
        String pathId = buf.readUtf();
        String chapterId = buf.readUtf();
        return new MarkerActionTriggerPacket(markerPos, pathId, chapterId);
    }
}

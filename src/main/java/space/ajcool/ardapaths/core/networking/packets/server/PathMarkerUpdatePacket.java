package space.ajcool.ardapaths.core.networking.packets.server;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import space.ajcool.ardapaths.core.ModConstants;
import space.ajcool.ardapaths.core.consumers.networking.IRespondablePacket;

import java.util.UUID;

/**
 * Packet sent from client to server to update a path marker block's NBT data.
 *
 * @param requestId request correlation id
 * @param position the block position of the marker to update
 * @param data     the NBT compound containing the updated marker configuration
 */
public record PathMarkerUpdatePacket(UUID requestId, BlockPos position, CompoundTag data) implements IRespondablePacket<PathMarkerUpdatePacket> {

    /**
     * Network channel used for marker NBT updates.
     */
    public static final ResourceLocation CHANNEL = ModConstants.modId("path_marker_update");

    /**
     * Custom payload type used for typed Fabric networking.
     */
    public static final CustomPacketPayload.Type<PathMarkerUpdatePacket> TYPE = new CustomPacketPayload.Type<>(CHANNEL);

    /**
     * Creates a marker update packet before request correlation is assigned.
     *
     * @param position the block position of the marker to update
     * @param data     the updated marker configuration
     */
    public PathMarkerUpdatePacket(BlockPos position, CompoundTag data) {
        this(IRespondablePacket.UNASSIGNED_REQUEST_ID, position, data);
    }

    /**
     * Reads a marker update packet from a network buffer.
     *
     * @param buf packet data buffer
     * @return decoded packet
     */
    public static PathMarkerUpdatePacket read(FriendlyByteBuf buf) {
        final UUID requestId = buf.readUUID();
        final BlockPos position = buf.readBlockPos();
        final CompoundTag data = buf.readNbt();
        return new PathMarkerUpdatePacket(requestId, position, data);
    }

    /**
     * Creates a request with the supplied request id.
     *
     * @param requestId request correlation id
     * @return packet carrying the supplied request id
     */
    @Override
    public PathMarkerUpdatePacket withRequestId(UUID requestId) {
        return new PathMarkerUpdatePacket(requestId, position, data);
    }

    /**
     * Gets the custom payload type for this packet.
     *
     * @return this packet's payload type
     */
    @Override
    public CustomPacketPayload.@NotNull Type<PathMarkerUpdatePacket> type() {
        return TYPE;
    }

    @Override
    public FriendlyByteBuf build() {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeUUID(requestId);
        buf.writeBlockPos(position);
        buf.writeNbt(data);
        return buf;
    }
}

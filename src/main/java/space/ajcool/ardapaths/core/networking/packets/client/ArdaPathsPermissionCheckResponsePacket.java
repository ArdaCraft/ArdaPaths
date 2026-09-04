package space.ajcool.ardapaths.core.networking.packets.client;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import space.ajcool.ardapaths.core.ModConstants;
import space.ajcool.ardapaths.core.consumers.networking.IRespondablePacket;

import java.util.UUID;

/**
 * Packet sent from server to client in response to a permission check request.
 * Contains the result of whether the player has permission to edit paths.
 *
 * @param requestId     request correlation id
 * @param hasPermission true if the player has the required edit permissions, false otherwise
 */
public record ArdaPathsPermissionCheckResponsePacket(UUID requestId,
                                                     boolean hasPermission) implements IRespondablePacket<ArdaPathsPermissionCheckResponsePacket> {

    /**
     * Network channel used for permission check responses.
     */
    public static final ResourceLocation CHANNEL = ModConstants.modId("arda_paths_permission_check_response");

    /**
     * Custom payload type used for typed Fabric networking.
     */
    public static final CustomPacketPayload.Type<ArdaPathsPermissionCheckResponsePacket> TYPE = new CustomPacketPayload.Type<>(CHANNEL);

    /**
     * Creates a permission response before request correlation is assigned.
     *
     * @param hasPermission true when the player may edit ArdaPaths data
     */
    public ArdaPathsPermissionCheckResponsePacket(boolean hasPermission) {
        this(IRespondablePacket.UNASSIGNED_REQUEST_ID, hasPermission);
    }

    public static ArdaPathsPermissionCheckResponsePacket read(FriendlyByteBuf buf) {
        final UUID requestId = buf.readUUID();
        final boolean hasPerm = buf.readBoolean();
        return new ArdaPathsPermissionCheckResponsePacket(requestId, hasPerm);
    }

    /**
     * Creates a permission response with a different request id.
     *
     * @param requestId request correlation id
     * @return packet carrying the supplied request id
     */
    @Override
    public ArdaPathsPermissionCheckResponsePacket withRequestId(UUID requestId) {
        return new ArdaPathsPermissionCheckResponsePacket(requestId, hasPermission);
    }

    /**
     * Gets the custom payload type for this packet.
     *
     * @return this packet's payload type
     */
    @Override
    public CustomPacketPayload.@NotNull Type<ArdaPathsPermissionCheckResponsePacket> type() {
        return TYPE;
    }

    @Override
    public FriendlyByteBuf build() {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeUUID(requestId);
        buf.writeBoolean(hasPermission);
        return buf;
    }
}

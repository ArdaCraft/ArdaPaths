package space.ajcool.ardapaths.core.networking.packets.client;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import space.ajcool.ardapaths.core.ModConstants;
import space.ajcool.ardapaths.core.consumers.networking.IRespondablePacket;

import java.util.UUID;

/**
 * Packet sent from server to client in response to a permission check request.
 * Contains the result of whether the player has permission to edit paths.
 * @param requestId request correlation id
 * @param hasPermission true if the player has the required edit permissions, false otherwise
 */
public record ArdaPathsPermissionCheckResponsePacket(UUID requestId, boolean hasPermission) implements IRespondablePacket<ArdaPathsPermissionCheckResponsePacket> {
    /**
     * Network channel used for permission check responses.
     */
    public static final ResourceLocation CHANNEL = ModConstants.modId("arda_paths_permission_check_response");

    /**
     * Creates a permission response before request correlation is assigned.
     *
     * @param hasPermission true when the player may edit ArdaPaths data
     */
    public ArdaPathsPermissionCheckResponsePacket(boolean hasPermission) {
        this(IRespondablePacket.UNASSIGNED_REQUEST_ID, hasPermission);
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

    @Override
    public FriendlyByteBuf build() {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeUUID(requestId);
        buf.writeBoolean(hasPermission);
        return buf;
    }

    public static ArdaPathsPermissionCheckResponsePacket read(FriendlyByteBuf buf)
    {
        final UUID requestId = buf.readUUID();
        final boolean hasPerm = buf.readBoolean();
        return new ArdaPathsPermissionCheckResponsePacket(requestId, hasPerm);
    }
}

package space.ajcool.ardapaths.core.networking.handlers.server;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import space.ajcool.ardapaths.core.PermissionHelper;
import space.ajcool.ardapaths.core.consumers.networking.RespondablePacketHandler;
import space.ajcool.ardapaths.core.networking.packets.EmptyPacket;
import space.ajcool.ardapaths.core.networking.packets.client.ArdaPathsPermissionCheckResponsePacket;

/**
 * Handles permission check requests from clients and responds with whether the player has edit permissions.
 * Uses {@link PermissionHelper} to determine if the player has the required permissions for editing paths.
 */
public class ArdaPathsPermissionCheckHandler extends RespondablePacketHandler<EmptyPacket, ArdaPathsPermissionCheckResponsePacket> {

    /**
     * Creates the permission check request handler.
     */
    public ArdaPathsPermissionCheckHandler() {
        super(EmptyPacket.PERMISSION_CHECK_TYPE, EmptyPacket.reader(EmptyPacket.PERMISSION_CHECK_TYPE), ArdaPathsPermissionCheckResponsePacket.TYPE, ArdaPathsPermissionCheckResponsePacket::read);
    }

    @Override
    public ArdaPathsPermissionCheckResponsePacket handle(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler, EmptyPacket packet, PacketSender sender) {

        if (player == null) return new ArdaPathsPermissionCheckResponsePacket(false);

        return new ArdaPathsPermissionCheckResponsePacket(PermissionHelper.hasEditPermission(player));
    }
}

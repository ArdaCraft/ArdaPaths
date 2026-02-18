package space.ajcool.ardapaths.core.networking.handlers.server;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import space.ajcool.ardapaths.core.PermissionHelper;
import space.ajcool.ardapaths.core.consumers.networking.RespondablePacketHandler;
import space.ajcool.ardapaths.core.networking.packets.EmptyPacket;
import space.ajcool.ardapaths.core.networking.packets.client.ArdaPathsPermissionCheckResponsePacket;

public class ArdaPathsPermissionCheckHandler extends RespondablePacketHandler<EmptyPacket, ArdaPathsPermissionCheckResponsePacket> {

    private static final String REQUEST_CHANNEL = "ardapaths_permission_check_request";
    private static final String RESPONSE_CHANNEL = "ardapaths_permission_check_response";

    public ArdaPathsPermissionCheckHandler() {
        super(REQUEST_CHANNEL, EmptyPacket::read, RESPONSE_CHANNEL, ArdaPathsPermissionCheckResponsePacket::read);
    }

    @Override
    public ArdaPathsPermissionCheckResponsePacket handle(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, EmptyPacket packet, PacketSender sender) {

        if (player == null) return new ArdaPathsPermissionCheckResponsePacket(false);

        return new ArdaPathsPermissionCheckResponsePacket(PermissionHelper.hasEditPermission(player));
    }
}

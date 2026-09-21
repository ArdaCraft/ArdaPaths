package space.ajcool.ardapaths.core.networking.handlers.server;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import space.ajcool.ardapaths.core.ModConstants;
import space.ajcool.ardapaths.core.PermissionHelper;
import space.ajcool.ardapaths.core.consumers.networking.RespondablePacketHandler;
import space.ajcool.ardapaths.core.networking.packets.EmptyPacket;
import space.ajcool.ardapaths.core.networking.packets.client.DimensionListResponsePacket;

import java.util.List;

/**
 * Handles editor requests for the server's loaded dimension identifiers.
 */
public class DimensionListRequestHandler extends RespondablePacketHandler<EmptyPacket, DimensionListResponsePacket> {
    /**
     * Channel identifier for dimension list requests from clients.
     */
    private static final ResourceLocation REQUEST_CHANNEL = ModConstants.modId("dimension_list_request");

    /**
     * Creates the dimension list request handler.
     */
    public DimensionListRequestHandler() {
        super(REQUEST_CHANNEL, EmptyPacket::read, DimensionListResponsePacket.CHANNEL, DimensionListResponsePacket::read);
    }

    /**
     * Resolves the dimension list for authorized editors.
     *
     * @param server  the Minecraft server
     * @param player  the player who sent the request
     * @param handler the network handler
     * @param packet  the deserialized request packet
     * @param sender  the packet sender
     * @return dimension list response for the requesting player
     */
    @Override
    public DimensionListResponsePacket handle(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler, EmptyPacket packet, PacketSender sender) {
        if (!PermissionHelper.hasEditPermission(player)) {
            return new DimensionListResponsePacket(List.of());
        }

        List<String> dimensions = server.levelKeys().stream()
                .map(key -> key.location().toString())
                .sorted()
                .toList();
        return new DimensionListResponsePacket(dimensions);
    }

    /**
     * Creates an empty response for exceptional request failures.
     *
     * @return empty dimension list response
     */
    @Override
    protected DimensionListResponsePacket errorResponse() {
        return new DimensionListResponsePacket(List.of());
    }
}

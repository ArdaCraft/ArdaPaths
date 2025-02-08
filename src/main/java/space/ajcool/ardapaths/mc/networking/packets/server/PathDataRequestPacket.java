package space.ajcool.ardapaths.mc.networking.packets.server;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.config.ServerConfigManager;
import space.ajcool.ardapaths.mc.networking.PacketRegistry;
import space.ajcool.ardapaths.mc.networking.ServerPacket;

/**
 * A packet sent from the client to the server to request path data.
 */
public class PathDataRequestPacket extends ServerPacket {
    public PathDataRequestPacket() {
        super("path_data_request");
    }

    /**
     * Create a new {@link PathDataRequestPacket} packet.
     */
    public PacketByteBuf create() {
        return PacketByteBufs.create();
    }

    /**
     * Send a {@link PathDataRequestPacket} packet to the server.
     */
    public void sendToServer() {
        super.sendToServer(create());
    }

    @Override
    public void handle(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender sender) {
        server.execute(() -> {
            PacketByteBuf responseBuf = PacketRegistry.PATH_DATA_RESPONSE.create(ServerConfigManager.getInstance().getConfig());
            sender.sendPacket(PacketRegistry.PATH_DATA_RESPONSE.getChannelId(), responseBuf);
        });
    }
}

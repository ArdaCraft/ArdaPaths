package space.ajcool.ardapaths.mc.networking.packets.server;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.mc.networking.RespondableServerPacket;
import space.ajcool.ardapaths.utils.Compression;
import space.ajcool.ardapaths.utils.JsonUtils;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * A packet sent from the client to the server to request path data.
 */
public class PathDataRequestPacket extends RespondableServerPacket {
    public PathDataRequestPacket() {
        super("path_data_request", "path_data_response");
    }

    /**
     * Create a new {@link PathDataRequestPacket} packet.
     */
    public PacketByteBuf create() {
        return PacketByteBufs.create();
    }

    /**
     * Send a {@link PathDataRequestPacket} packet to the server.
     *
     * @param responseConsumer The response consumer
     */
    public void sendToServer(Consumer<PacketByteBuf> responseConsumer) {
        super.sendToServer(create(), responseConsumer);
    }

    @Override
    public void handle(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender sender, UUID requestId) {
        server.execute(() -> {
            PacketByteBuf responseBuf = PacketByteBufs.create();
            responseBuf.writeUuid(requestId);

            String json = JsonUtils.toJson(ArdaPaths.CONFIG.getPaths());
            byte[] compressedJson = Compression.compress(json);
            responseBuf.writeByteArray(compressedJson);

            sender.sendPacket(this.getResponseChannelId(), responseBuf);
        });
    }
}

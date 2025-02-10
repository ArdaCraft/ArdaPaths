package space.ajcool.ardapaths.mc.networking;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.utils.McUtils;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public abstract class RespondableServerPacket implements ServerPacketHandler {
    private static final ConcurrentHashMap<UUID, Consumer<PacketByteBuf>> responseHandlers = new ConcurrentHashMap<>();
    private final Identifier CHANNEL_ID;
    private final Identifier RESPONSE_CHANNEL_ID;

    public RespondableServerPacket(String id, String responseId) {
        CHANNEL_ID = new Identifier(ArdaPaths.MOD_ID, id);
        RESPONSE_CHANNEL_ID = new Identifier(ArdaPaths.MOD_ID, responseId);

        if (McUtils.isClient()) {
            ClientPlayNetworking.registerGlobalReceiver(RESPONSE_CHANNEL_ID, (client, handler, buf, responseSender) -> {
                UUID requestId = buf.readUuid();
                Consumer<PacketByteBuf> responseConsumer = responseHandlers.get(requestId);
                if (responseConsumer != null) {
                    responseConsumer.accept(buf);
                    responseHandlers.remove(requestId);
                }
            });
        }
    }

    protected void sendToServer(PacketByteBuf buf, Consumer<PacketByteBuf> responseConsumer) {
        UUID requestId = UUID.randomUUID();
        responseHandlers.put(requestId, responseConsumer);
        int readableBytes = buf.readableBytes();
        PacketByteBuf requestBuf = PacketByteBufs.create();
        requestBuf.writeUuid(requestId);
        requestBuf.writeBytes(buf, 0, readableBytes);
        ClientPlayNetworking.send(CHANNEL_ID, requestBuf);
    }

    @Override
    public void handle(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender sender) {
        handle(server, player, handler, buf, sender, buf.readUuid());
    }

    public abstract void handle(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender sender, UUID requestId);

    @Override
    public Identifier getChannelId() {
        return CHANNEL_ID;
    }

    public Identifier getResponseChannelId() {
        return RESPONSE_CHANNEL_ID;
    }
}

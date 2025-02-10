package space.ajcool.ardapaths.core.consumers.networking;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import space.ajcool.ardapaths.ArdaPaths;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class RespondablePacketHandler<T extends IPacket, U extends IPacket> extends PacketHandler implements IServerPacketHandler<T>, IClientPacketHandler {
    private final Map<UUID, Consumer<U>> responseConsumers = new HashMap<>();
    private final Function<PacketByteBuf, T> reader;
    private final Identifier responseChannelId;
    private final Function<PacketByteBuf, U> responseReader;

    public RespondablePacketHandler(
            final String channel,
            final Function<PacketByteBuf, T> reader,
            final String responseChannel,
            final Function<PacketByteBuf, U> responseReader
    ) {
        super(channel);
        this.reader = reader;
        responseChannelId = new Identifier(ArdaPaths.MOD_ID, responseChannel);
        this.responseReader = responseReader;
    }

    public Identifier getResponseChannelId() {
        return responseChannelId;
    }

    public void send(final T packet, final Consumer<U> consumer) {
        UUID id = UUID.randomUUID();
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(id);
        PacketByteBuf packetBuf = packet.build();
        buf.writeBytes(packetBuf);
        if (consumer != null){
            responseConsumers.put(id, consumer);
        }
        ClientPlayNetworking.send(getChannelId(), buf);
    }

    @Override
    public void handle(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender sender) {
        UUID requestId = buf.readUuid();
        T packet = reader.apply(buf);
        System.out.println(packet);
        U responsePacket = handle(server, player, handler, packet, sender);
        PacketByteBuf responseBuf = PacketByteBufs.create().writeUuid(requestId);
        PacketByteBuf responsePacketBuf = responsePacket.build();
        responseBuf.writeBytes(responsePacketBuf);
        sender.sendPacket(responseChannelId, responseBuf);
    }

    public abstract U handle(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, T packet, PacketSender sender);

    @Override
    public void handle(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender sender) {
        UUID requestId = buf.readUuid();
        U packet = responseReader.apply(buf);
        Consumer<U> consumer = responseConsumers.remove(requestId);
        if (consumer != null) {
            consumer.accept(packet);
        }
    }
}

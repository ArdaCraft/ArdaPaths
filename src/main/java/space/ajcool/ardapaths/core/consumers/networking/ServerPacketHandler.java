package space.ajcool.ardapaths.core.consumers.networking;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.function.Function;

public abstract class ServerPacketHandler<T extends IPacket> extends PacketHandler implements IServerPacketHandler<T>
{
    private final Function<PacketByteBuf, T> reader;

    public ServerPacketHandler(final String channel, final Function<PacketByteBuf, T> reader)
    {
        super(channel);
        this.reader = reader;
    }

    public void handle(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender sender)
    {
        T packet = reader.apply(buf);
        handle(server, player, handler, packet, sender);
    }

    protected abstract void handle(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, T packet, PacketSender sender);
}

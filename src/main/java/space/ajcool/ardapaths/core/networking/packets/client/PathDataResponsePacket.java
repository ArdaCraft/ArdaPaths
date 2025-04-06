package space.ajcool.ardapaths.core.networking.packets.client;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import space.ajcool.ardapaths.core.consumers.networking.IPacket;

public record PathDataResponsePacket(String json) implements IPacket
{

    @Override
    public PacketByteBuf build()
    {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(json);
        return buf;
    }

    public static PathDataResponsePacket read(PacketByteBuf buf)
    {
        final String json = buf.readString();
        return new PathDataResponsePacket(json);
    }
}

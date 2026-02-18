package space.ajcool.ardapaths.core.networking.packets.server;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import space.ajcool.ardapaths.core.consumers.networking.IPacket;

public record PathDataUpdatePacket(
        String id,
        String name,
        int primaryColor,
        int secondaryColor,
        int tertiaryColor
) implements IPacket
{

    @Override
    public PacketByteBuf build()
    {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(id);
        buf.writeString(name);
        buf.writeInt(primaryColor);
        buf.writeInt(secondaryColor);
        buf.writeInt(tertiaryColor);
        return buf;
    }

    public static space.ajcool.ardapaths.core.networking.packets.server.PathDataUpdatePacket read(PacketByteBuf buf)
    {
        final String pathId = buf.readString();
        final String pathName = buf.readString();
        final int pathPrimaryColor = buf.readInt();
        final int pathSecondaryColor = buf.readInt();
        final int pathTertiaryColor = buf.readInt();
        return new space.ajcool.ardapaths.core.networking.packets.server.PathDataUpdatePacket(pathId, pathName, pathPrimaryColor, pathSecondaryColor, pathTertiaryColor);
    }
}

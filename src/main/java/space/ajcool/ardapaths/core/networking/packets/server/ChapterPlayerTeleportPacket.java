package space.ajcool.ardapaths.core.networking.packets.server;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import space.ajcool.ardapaths.core.consumers.networking.IPacket;

public record ChapterPlayerTeleportPacket(String pathId, String chapterId) implements IPacket
{

    @Override
    public PacketByteBuf build()
    {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(pathId);
        buf.writeString(chapterId);
        return buf;
    }

    public static ChapterPlayerTeleportPacket read(PacketByteBuf buf)
    {
        final String pathId = buf.readString();
        final String chapterId = buf.readString();
        return new ChapterPlayerTeleportPacket(pathId, chapterId);
    }
}

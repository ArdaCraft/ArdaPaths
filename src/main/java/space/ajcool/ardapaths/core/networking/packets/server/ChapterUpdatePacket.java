package space.ajcool.ardapaths.core.networking.packets.server;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import space.ajcool.ardapaths.core.consumers.networking.IPacket;
import space.ajcool.ardapaths.core.data.config.shared.ChapterData;

public record ChapterUpdatePacket(
        String pathId,
        String chapterId,
        String chapterName,
        String chapterDate,
        int chapterIndex,
        String warp
) implements IPacket
{

    public ChapterUpdatePacket(String pathId, ChapterData chapter)
    {
        this(pathId, chapter.getId(), chapter.getName(), chapter.getDate(), chapter.getIndex(), chapter.getWarp());
    }

    @Override
    public PacketByteBuf build()
    {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(pathId);
        buf.writeString(chapterId);
        buf.writeString(chapterName);
        buf.writeString(chapterDate);
        buf.writeInt(chapterIndex);
        buf.writeString(warp);
        return buf;
    }

    public static ChapterUpdatePacket read(PacketByteBuf buf)
    {
        final String pathId = buf.readString();
        final String chapterId = buf.readString();
        final String chapterName = buf.readString();
        final String chapterDate = buf.readString();
        final int chapterIndex = buf.readInt();
        final String warp = buf.readString();
        return new ChapterUpdatePacket(pathId, chapterId, chapterName, chapterDate, chapterIndex, warp);
    }
}

package space.ajcool.ardapaths.core.networking.packets.server;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import space.ajcool.ardapaths.core.consumers.networking.IPacket;

public record ChapterUpdatePacket(
        String pathId,
        String chapterId,
        String chapterName,
        String chapterDate
) implements IPacket {

    @Override
    public PacketByteBuf build() {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(pathId);
        buf.writeString(chapterId);
        buf.writeString(chapterName);
        buf.writeString(chapterDate);
        return buf;
    }

    public static ChapterUpdatePacket read(PacketByteBuf buf) {
        final String pathId = buf.readString();
        final String chapterId = buf.readString();
        final String chapterName = buf.readString();
        final String chapterDate = buf.readString();
        return new ChapterUpdatePacket(pathId, chapterId, chapterName, chapterDate);
    }
}

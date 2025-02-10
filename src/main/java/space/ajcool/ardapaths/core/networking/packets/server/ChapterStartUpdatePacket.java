package space.ajcool.ardapaths.core.networking.packets.server;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;
import space.ajcool.ardapaths.core.consumers.networking.IPacket;

public record ChapterStartUpdatePacket(String pathId, String chapterId, BlockPos position) implements IPacket {

    @Override
    public PacketByteBuf build() {
        return PacketByteBufs.create()
                .writeString(pathId)
                .writeString(chapterId)
                .writeBlockPos(position);
    }

    public static ChapterStartUpdatePacket read(PacketByteBuf buf) {
        final String pathId = buf.readString();
        final String chapterId = buf.readString();
        final BlockPos position = buf.readBlockPos();
        return new ChapterStartUpdatePacket(pathId, chapterId, position);
    }
}

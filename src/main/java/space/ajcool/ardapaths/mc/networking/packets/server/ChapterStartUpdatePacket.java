package space.ajcool.ardapaths.mc.networking.packets.server;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.config.server.PositionData;
import space.ajcool.ardapaths.mc.networking.ServerPacket;

public class ChapterStartUpdatePacket extends ServerPacket {
    public ChapterStartUpdatePacket() {
        super("path_chapter_start_update");
    }

    /**
     * Create a new {@link ChapterStartUpdatePacket} packet.
     *
     * @param pathId The path ID
     * @param chapterId The chapter ID
     * @param start The start position
     */
    public PacketByteBuf create(String pathId, String chapterId, BlockPos start) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(pathId);
        buf.writeString(chapterId);
        if (start == null) {
            buf.writeBoolean(false);
        } else {
            buf.writeBoolean(true);
            buf.writeBlockPos(start);
        }
        return buf;
    }

    /**
     * Send a {@link ChapterStartUpdatePacket} packet to the server.
     *
     * @param pathId The path ID
     * @param chapterId The chapter ID
     * @param start The start position
     */
    public void sendToServer(String pathId, String chapterId, BlockPos start) {
        sendToServer(create(pathId, chapterId, start));
    }

    @Override
    public void handle(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender sender) {
        String pathId = buf.readString();
        String chapterId = buf.readString();
        boolean hasBlockPos = buf.readBoolean();
        System.out.println("hasBlockPos: " + hasBlockPos);
        buf.retain();
        server.execute(() -> {
            if (hasBlockPos) {
                BlockPos start = buf.readBlockPos();
                ArdaPaths.CONFIG.setChapterStart(pathId, chapterId, PositionData.fromBlockPos(start));
            } else {
                ArdaPaths.CONFIG.removeChapterStart(pathId, chapterId);
            }
            ArdaPaths.CONFIG_MANAGER.save();
            buf.release();
        });
    }
}

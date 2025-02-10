package space.ajcool.ardapaths.mc.networking.packets.server;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.config.shared.ChapterData;
import space.ajcool.ardapaths.config.shared.PathData;
import space.ajcool.ardapaths.mc.networking.ServerPacket;

public class ChapterUpdatePacket extends ServerPacket {
    public ChapterUpdatePacket() {
        super("path_chapter_update");
    }

    /**
     * Create a new {@link ChapterUpdatePacket} packet.
     *
     * @param pathId The path ID
     * @param chapterId The chapter ID
     * @param name The chapter name
     * @param date The chapter date
     */
    public PacketByteBuf create(String pathId, String chapterId, String name, String date) {
        PacketByteBuf buf = PacketByteBufs.create();
        NbtCompound nbt = new NbtCompound();
        nbt.putString("path_id", pathId);
        nbt.putString("chapter_id", chapterId);
        nbt.putString("name", name);
        nbt.putString("date", date);
        buf.writeNbt(nbt);
        return buf;
    }

    /**
     * Send a {@link ChapterUpdatePacket} packet to the server.
     *
     * @param pathId The path ID
     * @param chapterId The chapter ID
     * @param name The chapter name
     * @param date The chapter date
     */
    public void sendToServer(String pathId, String chapterId, String name, String date) {
        super.sendToServer(create(pathId, chapterId, name, date));
    }

    @Override
    public void handle(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender sender) {
        NbtCompound nbt = buf.readNbt();
        if (nbt == null || !nbt.contains("path_id") || !nbt.contains("chapter_id")) {
            return;
        }

        server.execute(() -> {
            String pathId = nbt.getString("path_id");
            String chapterId = nbt.getString("chapter_id");
            PathData path = ArdaPaths.CONFIG.getPath(pathId);
            if (path != null) {
                ChapterData chapter = path.getChapter(chapterId);
                if (chapter == null) {
                    chapter = new ChapterData(chapterId);
                }
                if (nbt.contains("name")) {
                    chapter.setName(nbt.getString("name"));
                }
                if (nbt.contains("date")) {
                    chapter.setDate(nbt.getString("date"));
                }
                path.setChapter(chapterId, chapter);
                ArdaPaths.CONFIG_MANAGER.save();
            }
        });
    }
}

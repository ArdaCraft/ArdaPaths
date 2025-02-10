package space.ajcool.ardapaths.mc.networking.packets.server;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.mc.networking.ServerPacket;

public class ChapterPlayerTeleportPacket extends ServerPacket {
    public ChapterPlayerTeleportPacket() {
        super("chapter_player_teleport");
    }

    public PacketByteBuf create(String pathId, String chapterId) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(pathId);
        buf.writeString(chapterId);
        return buf;
    }

    public void sendToServer(String pathId, String chapterId) {
        super.sendToServer(create(pathId, chapterId));
    }

    @Override
    public void handle(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender sender) {
        String pathId = buf.readString();
        String chapterId = buf.readString();

        server.execute(() -> {
            BlockPos start = ArdaPaths.CONFIG.getChapterStart(pathId, chapterId);
            if (start != null) {
                player.requestTeleport(start.getX() - 0.5, start.getY(), start.getZ() - 0.5);
            }
        });
    }
}

package space.ajcool.ardapaths.core.networking.handlers.server;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.core.consumers.networking.ServerPacketHandler;
import space.ajcool.ardapaths.core.conversions.PathMarkerBlockEntityConverter;
import space.ajcool.ardapaths.core.networking.packets.server.PathMarkerLinksUpdatePacket;
import space.ajcool.ardapaths.core.networking.packets.server.PathMarkerUpdatePacket;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;

import java.util.HashMap;
import java.util.Map;

public class PathMarkerLinksUpdateHandler extends ServerPacketHandler<PathMarkerLinksUpdatePacket>
{
    public PathMarkerLinksUpdateHandler()
    {
        super("path_marker_links_update", PathMarkerLinksUpdatePacket::read);
    }

    @Override
    protected void handle(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PathMarkerLinksUpdatePacket packet, PacketSender sender)
    {
        BlockPos blockPos = packet.position();

        server.execute(() ->
        {
            BlockEntity blockEntity = player.getWorld().getBlockEntity(blockPos);

            if (blockEntity instanceof PathMarkerBlockEntity marker)
            {
                marker.applyNbt(syncPathsFromIncoming(marker.toNbt(), packet.data()));
                marker.markUpdated();
            }
        });
    }

    private Map<String, Map<String, NbtCompound>> getPaths(NbtCompound nbt) {
        Map<String, Map<String, NbtCompound>> result = new HashMap<>();

        NbtCompound paths = nbt.getCompound("paths");

        for (String pathKey : paths.getKeys()) {
            NbtCompound chapters = paths.getCompound(pathKey);

            Map<String, NbtCompound> chapterMap = new HashMap<>();
            for (String chapterKey : chapters.getKeys()) {
                chapterMap.put(chapterKey, chapters.getCompound(chapterKey));
            }
            result.put(pathKey, chapterMap);
        }
        return result;
    }

    public NbtCompound syncPathsFromIncoming(NbtCompound existing, NbtCompound incoming) {

        var oldPaths = getPaths(existing);
        var newPaths = getPaths(incoming);

        // 1. Remove entire paths that no longer exist
        oldPaths.keySet().removeIf(path -> !newPaths.containsKey(path));

        // 2. Remove or update chapters inside existing paths
        for (var entry : oldPaths.entrySet()) {
            String path = entry.getKey();
            Map<String, NbtCompound> oldChapters = entry.getValue();
            Map<String, NbtCompound> newChapters = newPaths.get(path);

            // Remove chapters that no longer exist
            oldChapters.keySet().removeIf(ch -> !newChapters.containsKey(ch));

            // Add/update chapters
            for (var chEntry : newChapters.entrySet()) {
                oldChapters.put(chEntry.getKey(), chEntry.getValue().copy());
            }
        }

        // 3. Add entirely new paths
        newPaths.forEach((path, chapters) -> {
            if (!oldPaths.containsKey(path)) {
                oldPaths.put(path, chapters);
            }
        });

        // 4. Rebuild the existing NBT in-place
        NbtCompound rebuilt = new NbtCompound();
        NbtCompound pathsNbt = new NbtCompound();

        for (var pathEntry : oldPaths.entrySet()) {
            NbtCompound chapterNbt = new NbtCompound();

            for (var chapterEntry : pathEntry.getValue().entrySet()) {
                chapterNbt.put(chapterEntry.getKey(), chapterEntry.getValue());
            }

            pathsNbt.put(pathEntry.getKey(), chapterNbt);
        }

        return pathsNbt;
    }
}

package space.ajcool.ardapaths.core.networking.handlers.server;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import space.ajcool.ardapaths.core.consumers.networking.ServerPacketHandler;
import space.ajcool.ardapaths.core.networking.packets.server.PathMarkerLinksUpdatePacket;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles updates to path-chapter links stored in a marker's NBT data.
 * Syncs incoming path and chapter data from a client with the existing marker data on the server,
 * ensuring the marker correctly references the paths and chapters it belongs to.
 */
public class PathMarkerLinksUpdateHandler extends ServerPacketHandler<PathMarkerLinksUpdatePacket>
{
    public PathMarkerLinksUpdateHandler()
    {
        super("path_marker_links_update", PathMarkerLinksUpdatePacket::read);
    }

    /**
     * Requires edit permission because marker link updates mutate marker NBT.
     *
     * @return true because this packet changes editable marker data
     */
    @Override
    protected boolean requiresEditPermission() {
        return true;
    }

    @Override
    protected void handle(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PathMarkerLinksUpdatePacket packet, PacketSender sender)
    {
        BlockPos blockPos = packet.position();
        BlockEntity blockEntity = player.getWorld().getBlockEntity(blockPos);

        if (blockEntity instanceof PathMarkerBlockEntity marker)
        {
            marker.applyNbt(syncPathsFromIncoming(marker.toNbt(), packet.data()));
            marker.markUpdated();
        }
    }

    /**
     * Extracts the paths-chapters structure from an NBT compound.
     * @param nbt the NBT compound containing a "paths" key with nested chapter data
     * @return a map of path IDs to maps of chapter IDs and their NBT data
     */
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

    /**
     * Merges incoming path-chapter data with existing marker data, handling additions, updates, and removals.
     * Ensures the marker's path links are in sync with the client's updates.
     * @param existing the current NBT data from the marker
     * @param incoming the new NBT data from the client
     * @return the merged NBT compound with synced paths and chapters
     */
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

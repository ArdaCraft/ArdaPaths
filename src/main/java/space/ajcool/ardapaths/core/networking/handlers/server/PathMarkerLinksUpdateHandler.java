package space.ajcool.ardapaths.core.networking.handlers.server;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import space.ajcool.ardapaths.core.backup.BackupJobRunner;
import space.ajcool.ardapaths.core.consumers.networking.ServerPacketHandler;
import space.ajcool.ardapaths.core.markers.MarkerResolver;
import space.ajcool.ardapaths.core.networking.packets.server.PathMarkerLinksUpdatePacket;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Handles updates to path-chapter links stored in a marker's NBT data.
 * Syncs incoming path and chapter data from a client with the existing marker data on the server,
 * ensuring the marker correctly references the paths and chapters it belongs to.
 */
public class PathMarkerLinksUpdateHandler extends ServerPacketHandler<PathMarkerLinksUpdatePacket> {

    public PathMarkerLinksUpdateHandler() {
        super(PathMarkerLinksUpdatePacket.TYPE, PathMarkerLinksUpdatePacket::read);
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
    protected void handle(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler, PathMarkerLinksUpdatePacket packet, PacketSender sender) {
        BackupJobRunner.submitMarkerWork(server, gate -> {
            ServerLevel world = gate.call(player::level);
            String dimensionId = world.dimension().identifier().toString();
            MarkerResolver resolver = new MarkerResolver(world, dimensionId);
            BlockPos blockPos = packet.position();

            gate.call(() -> {
                Optional<MarkerResolver.ResolvedMarker> resolved = resolver.resolve(blockPos);
                resolved.ifPresent(marker -> {
                    marker.liveMarker().applyNbt(syncPathsFromIncoming(marker.liveMarker().toNbt(), packet.data()));
                    marker.liveMarker().markUpdated();
                });
                return null;
            });
            return null;
        });
    }

    /**
     * Merges incoming path-chapter data with existing marker data, handling additions, updates, and removals.
     * Ensures the marker's path links are in sync with the client's updates.
     *
     * @param existing the current NBT data from the marker
     * @param incoming the new NBT data from the client
     * @return the merged NBT compound with synced paths and chapters
     */
    public CompoundTag syncPathsFromIncoming(CompoundTag existing, CompoundTag incoming) {

        var oldPaths = getPaths(existing);
        var newPaths = getPaths(incoming);

        // 1. Remove entire paths that no longer exist
        oldPaths.keySet().removeIf(path -> !newPaths.containsKey(path));

        // 2. Remove or update chapters inside existing paths
        for (var entry : oldPaths.entrySet()) {
            String path = entry.getKey();
            Map<String, CompoundTag> oldChapters = entry.getValue();
            Map<String, CompoundTag> newChapters = newPaths.get(path);

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
        CompoundTag pathsNbt = new CompoundTag();

        for (var pathEntry : oldPaths.entrySet()) {
            CompoundTag chapterNbt = new CompoundTag();

            for (var chapterEntry : pathEntry.getValue().entrySet()) {
                chapterNbt.put(chapterEntry.getKey(), chapterEntry.getValue());
            }

            pathsNbt.put(pathEntry.getKey(), chapterNbt);
        }

        return pathsNbt;
    }

    /**
     * Extracts the paths-chapters structure from an NBT compound.
     *
     * @param nbt the NBT compound containing a "paths" key with nested chapter data
     * @return a map of path IDs to maps of chapter IDs and their NBT data
     */
    private Map<String, Map<String, CompoundTag>> getPaths(CompoundTag nbt) {
        Map<String, Map<String, CompoundTag>> result = new HashMap<>();

        CompoundTag paths = nbt.getCompoundOrEmpty("paths");

        for (String pathKey : paths.keySet()) {
            CompoundTag chapters = paths.getCompoundOrEmpty(pathKey);

            Map<String, CompoundTag> chapterMap = new HashMap<>();
            for (String chapterKey : chapters.keySet()) {
                chapterMap.put(chapterKey, chapters.getCompoundOrEmpty(chapterKey));
            }
            result.put(pathKey, chapterMap);
        }
        return result;
    }
}

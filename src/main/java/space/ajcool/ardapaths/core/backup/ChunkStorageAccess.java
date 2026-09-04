package space.ajcool.ardapaths.core.backup;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Narrow access point for vanilla chunk storage operations used by backups.
 */
public interface ChunkStorageAccess {

    /**
     * Finds the region file directory for a server world.
     *
     * @param world world whose region directory is needed
     * @return directory containing region files
     */
    Path regionDirectory(ServerLevel world);

    /**
     * Waits for queued chunk IO for one world to finish before direct file inspection.
     *
     * @param world world whose chunk storage worker should be flushed
     */
    void flushWorker(ServerLevel world);

    /**
     * Reads only the block entity list from a persisted chunk.
     *
     * @param world    world whose chunk storage is scanned
     * @param chunkPos chunk position to inspect
     * @return future containing a partial chunk root, or empty when the requested payload is absent
     */
    CompletableFuture<Optional<CompoundTag>> scanChunkBlockEntities(ServerLevel world, ChunkPos chunkPos);

    /**
     * Reads full persisted chunk NBT without generating or loading terrain.
     *
     * @param world    world whose chunk storage is read
     * @param chunkPos chunk position to inspect
     * @return persisted chunk NBT, or empty when the chunk does not exist
     */
    Optional<CompoundTag> readChunkNbt(ServerLevel world, ChunkPos chunkPos);

    /**
     * Checks whether a chunk is currently loaded in memory.
     *
     * @param world    world whose chunk manager is checked
     * @param chunkPos chunk position to inspect
     * @return true when the chunk is loaded
     */
    boolean isChunkLoaded(ServerLevel world, ChunkPos chunkPos);
}

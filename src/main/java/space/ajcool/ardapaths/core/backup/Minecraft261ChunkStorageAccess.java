package space.ajcool.ardapaths.core.backup;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.visitors.CollectFields;
import net.minecraft.nbt.visitors.FieldSelector;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Minecraft 26.1 implementation of backup access to vanilla chunk storage.
 */
public class Minecraft261ChunkStorageAccess implements ChunkStorageAccess {

    /**
     * NBT query selecting the root chunk block entity list.
     */
    private static final FieldSelector BLOCK_ENTITIES_QUERY = new FieldSelector(ListTag.TYPE, "block_entities");

    @Override
    public Path regionDirectory(ServerLevel world) {
        return DimensionType.getStorageFolder(world.dimension(), world.getServer().getWorldPath(LevelResource.ROOT))
                .resolve("region")
                .normalize();
    }

    @Override
    public void flushWorker(ServerLevel world) {
        world.getChunkSource().chunkMap.synchronize(true).join();
    }

    @Override
    public CompletableFuture<Optional<CompoundTag>> scanChunkBlockEntities(ServerLevel world, ChunkPos chunkPos) {
        CollectFields collector = new CollectFields(BLOCK_ENTITIES_QUERY);
        return world.getChunkSource().chunkScanner().scanChunk(chunkPos, collector)
                .thenApply(ignored -> collector.getResult() instanceof CompoundTag root ? Optional.of(root) : Optional.empty());
    }

    @Override
    public Optional<CompoundTag> readChunkNbt(ServerLevel world, ChunkPos chunkPos) {
        return world.getChunkSource().chunkMap.read(chunkPos).join();
    }

    @Override
    public boolean isChunkLoaded(ServerLevel world, ChunkPos chunkPos) {
        return world.getChunkSource().hasChunk(chunkPos.x(), chunkPos.z());
    }
}

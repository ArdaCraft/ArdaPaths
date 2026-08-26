package space.ajcool.ardapaths.mixin;

import net.minecraft.world.storage.StorageIoWorker;
import net.minecraft.world.storage.VersionedChunkStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accesses the chunk IO worker behind vanilla chunk storage.
 */
@Mixin(VersionedChunkStorage.class)
public interface VersionedChunkStorageAccessor {
    /**
     * Returns the worker used by vanilla chunk storage.
     *
     * @return chunk storage worker
     */
    @Accessor("worker")
    StorageIoWorker ardapaths$getWorker();
}

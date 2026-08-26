package space.ajcool.ardapaths.mixin;

import net.minecraft.world.storage.RegionBasedStorage;
import net.minecraft.world.storage.StorageIoWorker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accesses the region storage held by a vanilla chunk IO worker.
 */
@Mixin(StorageIoWorker.class)
public interface StorageIoWorkerAccessor {
    /**
     * Returns the region-backed storage used by the worker.
     *
     * @return region-backed chunk storage
     */
    @Accessor("storage")
    RegionBasedStorage ardapaths$getStorage();
}

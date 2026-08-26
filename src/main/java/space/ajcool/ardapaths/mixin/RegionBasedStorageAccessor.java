package space.ajcool.ardapaths.mixin;

import net.minecraft.world.storage.RegionBasedStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.nio.file.Path;

/**
 * Accesses the filesystem directory used by vanilla region storage.
 */
@Mixin(RegionBasedStorage.class)
public interface RegionBasedStorageAccessor {
    /**
     * Returns the directory containing the storage's region files.
     *
     * @return region file directory
     */
    @Accessor("directory")
    Path ardapaths$getDirectory();
}

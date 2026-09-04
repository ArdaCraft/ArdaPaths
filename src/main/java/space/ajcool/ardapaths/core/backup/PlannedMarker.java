package space.ajcool.ardapaths.core.backup;

import net.minecraft.nbt.CompoundTag;

/**
 * Restorable marker payload prepared off the server thread.
 *
 * @param dimensionId dimension identifier for the target world
 * @param packedPos   packed target block position
 * @param pathsNbt    marker paths NBT to apply
 */
public record PlannedMarker(String dimensionId, long packedPos, CompoundTag pathsNbt) {

}

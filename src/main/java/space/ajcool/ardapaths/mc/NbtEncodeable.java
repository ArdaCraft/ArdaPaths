package space.ajcool.ardapaths.mc;

import net.minecraft.nbt.NbtCompound;
import org.jetbrains.annotations.Nullable;

public interface NbtEncodeable {
    /**
     * Apply an NBT compound to the object.
     *
     * @param nbt The NBT compound
     */
    void applyNbt(NbtCompound nbt);

    /**
     * Convert an object to an NBT compound.
     *
     * @return The NBT compound
     */
    default NbtCompound toNbt() {
        return toNbt(null);
    }

    /**
     * Convert an object to an NBT compound.
     *
     * @return The NBT compound
     */
    NbtCompound toNbt(@Nullable NbtCompound nbt);

    /**
     * Create an empty object.
     */
    static <T extends NbtEncodeable> T asEmpty() {
        return null;
    }
}

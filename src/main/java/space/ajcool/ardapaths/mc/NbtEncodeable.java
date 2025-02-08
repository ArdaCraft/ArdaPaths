package space.ajcool.ardapaths.mc;

import net.minecraft.nbt.NbtCompound;

public interface NbtEncodeable {
    /**
     * Create an object from an NBT compound.
     *
     * @param nbt The NBT compound
     */
    static <T extends NbtEncodeable> T fromNbt(NbtCompound nbt) {
        return null;
    }

    /**
     * Convert an object to an NBT compound.
     *
     * @return The NBT compound
     */
    NbtCompound toNbt();

    /**
     * Create an empty object.
     */
    static <T extends NbtEncodeable> T asEmpty() {
        return null;
    }

    /**
     * Write the object's data to an existing NBT compound.
     *
     * @param nbt The NBT compound
     * @return The NBT compound
     */
    default NbtCompound writeNbt(NbtCompound nbt) {
        NbtCompound nbtData = toNbt();
        for (String key : nbtData.getKeys()) {
            nbt.put(key, nbtData.get(key));
        }
        return nbt;
    }
}

package space.ajcool.ardapaths.mc;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Interface for objects that can be serialized to and deserialized from NBT (Named Binary Tag).
 */
public interface NbtEncodeable {

    /**
     * Create an empty object.
     *
     * @param <T> the type of encodeable object
     * @return an empty encodeable placeholder
     */
    @SuppressWarnings("unused")
    static <T extends NbtEncodeable> T asEmpty() {
        return null;
    }

    /**
     * Checks whether an NBT compound contains a nested compound at the given key.
     *
     * @param nbt the parent NBT compound
     * @param key the key to inspect
     * @return true when the key contains a nested compound
     */
    static boolean hasCompound(CompoundTag nbt, String key) {
        return nbt.get(key) instanceof CompoundTag;
    }

    /**
     * Gets a nested compound from an NBT object.
     *
     * @param nbt the parent NBT compound
     * @param key the key to read
     * @return the nested compound, or an empty compound when the key is absent
     */
    static CompoundTag getCompound(CompoundTag nbt, String key) {
        return nbt.getCompound(key);
    }

    /**
     * Reads an optional block position from an NBT compound.
     *
     * @param nbt the parent NBT compound
     * @param key the key containing a block position compound
     * @return the decoded position, or empty when no position exists
     */
    static Optional<BlockPos> getBlockPos(CompoundTag nbt, String key) {
        return NbtUtils.readBlockPos(nbt, key);
    }

    /**
     * Writes a block position when one exists.
     *
     * @param nbt the parent NBT compound
     * @param key the key to write
     * @param pos the position to encode, or null when absent
     */
    static void putBlockPosIfPresent(CompoundTag nbt, String key, @Nullable BlockPos pos) {
        if (pos != null) {
            nbt.put(key, NbtUtils.writeBlockPos(pos));
        }
    }

    /**
     * Reads a string value with an empty-string default.
     *
     * @param nbt the parent NBT compound
     * @param key the key to read
     * @return the stored string, or an empty string when absent
     */
    static String getStringOrEmpty(CompoundTag nbt, String key) {
        return nbt.get(key) instanceof StringTag ? nbt.getString(key) : "";
    }

    /**
     * Reads an integer value with a zero default.
     *
     * @param nbt the parent NBT compound
     * @param key the key to read
     * @return the stored integer, or zero when absent
     */
    static int getIntOrZero(CompoundTag nbt, String key) {
        return getIntOrDefault(nbt, key, 0);
    }

    /**
     * Reads an integer value with a supplied default.
     *
     * @param nbt          the parent NBT compound
     * @param key          the key to read
     * @param defaultValue the value to return when the key is absent
     * @return the stored integer, or the supplied default when absent
     */
    static int getIntOrDefault(CompoundTag nbt, String key, int defaultValue) {
        return nbt.get(key) instanceof IntTag ? nbt.getInt(key) : defaultValue;
    }

    /**
     * Reads a boolean value with a supplied default.
     *
     * @param nbt          the parent NBT compound
     * @param key          the key to read
     * @param defaultValue the value to return when the key is absent
     * @return the stored boolean, or the supplied default when absent
     */
    static boolean getBooleanOrDefault(CompoundTag nbt, String key, boolean defaultValue) {
        return nbt.get(key) instanceof ByteTag ? nbt.getBoolean(key) : defaultValue;
    }

    /**
     * Reads a long value with a supplied default.
     *
     * @param nbt          the parent NBT compound
     * @param key          the key to read
     * @param defaultValue the value to return when the key is absent
     * @return the stored long, or the supplied default when absent
     */
    static long getLongOrDefault(CompoundTag nbt, String key, long defaultValue) {
        return nbt.get(key) instanceof LongTag ? nbt.getLong(key) : defaultValue;
    }

    /**
     * Writes a non-empty string value.
     *
     * @param nbt   the parent NBT compound
     * @param key   the key to write
     * @param value the string value to persist
     */
    static void putStringIfNotEmpty(CompoundTag nbt, String key, String value) {
        if (!value.isEmpty()) {
            nbt.putString(key, value);
        }
    }

    /**
     * Writes a non-zero integer value.
     *
     * @param nbt   the parent NBT compound
     * @param key   the key to write
     * @param value the integer value to persist
     */
    static void putIntIfNonZero(CompoundTag nbt, String key, int value) {
        if (value != 0) {
            nbt.putInt(key, value);
        }
    }

    /**
     * Writes an integer value when it differs from a default.
     *
     * @param nbt          the parent NBT compound
     * @param key          the key to write
     * @param value        the integer value to persist
     * @param defaultValue the default value that should not be persisted
     */
    static void putIntIfNonDefault(CompoundTag nbt, String key, int value, int defaultValue) {
        if (value != defaultValue) {
            nbt.putInt(key, value);
        }
    }

    /**
     * Writes a true boolean value.
     *
     * @param nbt   the parent NBT compound
     * @param key   the key to write
     * @param value the boolean value to persist
     */
    static void putBooleanIfTrue(CompoundTag nbt, String key, boolean value) {
        if (value) {
            nbt.putBoolean(key, true);
        }
    }

    /**
     * Writes a false boolean value when the default state is true.
     *
     * @param nbt   the parent NBT compound
     * @param key   the key to write
     * @param value the boolean value to persist
     */
    static void putBooleanIfFalse(CompoundTag nbt, String key, boolean value) {
        if (!value) {
            nbt.putBoolean(key, false);
        }
    }

    /**
     * Writes a long value when it differs from a default.
     *
     * @param nbt          the parent NBT compound
     * @param key          the key to write
     * @param value        the long value to persist
     * @param defaultValue the default value that should not be persisted
     */
    static void putLongIfNonDefault(CompoundTag nbt, String key, long value, long defaultValue) {
        if (value != defaultValue) {
            nbt.putLong(key, value);
        }
    }

    /**
     * Apply an NBT compound to the object.
     *
     * @param nbt The NBT compound
     */
    @SuppressWarnings("unused")
    void applyNbt(CompoundTag nbt);

    /**
     * Convert an object to an NBT compound.
     *
     * @return The NBT compound
     */
    default CompoundTag toNbt() {
        return toNbt(null);
    }

    /**
     * Convert an object to an NBT compound.
     *
     * @param nbt an existing NBT compound to update, or null to create a new one
     * @return the NBT compound representation of this object
     */
    CompoundTag toNbt(@Nullable CompoundTag nbt);
}

package space.ajcool.ardapaths.mc;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.math.BlockPos;

import java.util.Optional;

/**
 * Interface for objects that can be serialized to and deserialized from NBT (Named Binary Tag).
 */
public interface NbtEncodeable {
    /**
     * NBT type id for compound values.
     */
    int COMPOUND_TYPE = 10;

    /**
     * NBT type id for string values.
     */
    int STRING_TYPE = 8;

    /**
     * NBT type id for integer values.
     */
    int INT_TYPE = 3;

    /**
     * NBT type id for long values.
     */
    int LONG_TYPE = 4;

    /**
     * Create an empty object.
     *
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
    static boolean hasCompound(NbtCompound nbt, String key) {
        return nbt.contains(key, COMPOUND_TYPE);
    }

    /**
     * Gets a nested compound from an NBT object.
     *
     * @param nbt the parent NBT compound
     * @param key the key to read
     * @return the nested compound, or an empty compound when the key is absent
     */
    static NbtCompound getCompound(NbtCompound nbt, String key) {
        return nbt.getCompound(key);
    }

    /**
     * Reads an optional block position from an NBT compound.
     *
     * @param nbt the parent NBT compound
     * @param key the key containing a block position compound
     * @return the decoded position, or empty when no position exists
     */
    static Optional<BlockPos> getBlockPos(NbtCompound nbt, String key) {
        if (!hasCompound(nbt, key)) {
            return Optional.empty();
        }

        return Optional.of(NbtHelper.toBlockPos(nbt.getCompound(key)));
    }

    /**
     * Writes a block position when one exists.
     *
     * @param nbt the parent NBT compound
     * @param key the key to write
     * @param pos the position to encode, or null when absent
     */
    static void putBlockPosIfPresent(NbtCompound nbt, String key, @Nullable BlockPos pos) {
        if (pos != null) {
            nbt.put(key, NbtHelper.fromBlockPos(pos));
        }
    }

    /**
     * Reads a string value with an empty-string default.
     *
     * @param nbt the parent NBT compound
     * @param key the key to read
     * @return the stored string, or an empty string when absent
     */
    static String getStringOrEmpty(NbtCompound nbt, String key) {
        return nbt.contains(key, STRING_TYPE) ? nbt.getString(key) : "";
    }

    /**
     * Reads an integer value with a zero default.
     *
     * @param nbt the parent NBT compound
     * @param key the key to read
     * @return the stored integer, or zero when absent
     */
    static int getIntOrZero(NbtCompound nbt, String key) {
        return nbt.contains(key, INT_TYPE) ? nbt.getInt(key) : 0;
    }

    /**
     * Reads a boolean value with a supplied default.
     *
     * @param nbt          the parent NBT compound
     * @param key          the key to read
     * @param defaultValue the value to return when the key is absent
     * @return the stored boolean, or the supplied default when absent
     */
    static boolean getBooleanOrDefault(NbtCompound nbt, String key, boolean defaultValue) {
        return nbt.contains(key) ? nbt.getBoolean(key) : defaultValue;
    }

    /**
     * Reads a long value with a supplied default.
     *
     * @param nbt          the parent NBT compound
     * @param key          the key to read
     * @param defaultValue the value to return when the key is absent
     * @return the stored long, or the supplied default when absent
     */
    static long getLongOrDefault(NbtCompound nbt, String key, long defaultValue) {
        return nbt.contains(key, LONG_TYPE) ? nbt.getLong(key) : defaultValue;
    }

    /**
     * Writes a non-empty string value.
     *
     * @param nbt   the parent NBT compound
     * @param key   the key to write
     * @param value the string value to persist
     */
    static void putStringIfNotEmpty(NbtCompound nbt, String key, String value) {
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
    static void putIntIfNonZero(NbtCompound nbt, String key, int value) {
        if (value != 0) {
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
    static void putBooleanIfTrue(NbtCompound nbt, String key, boolean value) {
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
    static void putBooleanIfFalse(NbtCompound nbt, String key, boolean value) {
        if (!value) {
            nbt.putBoolean(key, false);
        }
    }

    /**
     * Writes a long value when it differs from a default and is not zero.
     *
     * @param nbt          the parent NBT compound
     * @param key          the key to write
     * @param value        the long value to persist
     * @param defaultValue the default value that should not be persisted
     */
    static void putLongIfNonDefault(NbtCompound nbt, String key, long value, long defaultValue) {
        if (value != defaultValue && value != 0) {
            nbt.putLong(key, value);
        }
    }

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
}

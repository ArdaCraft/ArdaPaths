package space.ajcool.ardapaths.mc;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract tests for the shared NBT serialization helper methods.
 */
class NbtEncodeableTest {

    /**
     * Verifies typed readers ignore missing or wrong-type values and return their documented defaults.
     */
    @Test
    void typedReadersReturnDefaultsForMissingOrWrongTypeValues() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("int_as_string", "7");
        nbt.putInt("string_as_int", 7);
        nbt.putString("long_as_string", "42");

        assertFalse(NbtEncodeable.hasCompound(nbt, "missing"));
        assertEquals(new CompoundTag(), NbtEncodeable.getCompound(nbt, "missing"));
        assertEquals(Optional.empty(), NbtEncodeable.getBlockPos(nbt, "missing"));
        assertEquals("", NbtEncodeable.getStringOrEmpty(nbt, "missing"));
        assertEquals("", NbtEncodeable.getStringOrEmpty(nbt, "string_as_int"));
        assertEquals(0, NbtEncodeable.getIntOrZero(nbt, "missing"));
        assertEquals(0, NbtEncodeable.getIntOrZero(nbt, "int_as_string"));
        assertEquals(13, NbtEncodeable.getIntOrDefault(nbt, "missing", 13));
        assertEquals(13, NbtEncodeable.getIntOrDefault(nbt, "int_as_string", 13));
        assertTrue(NbtEncodeable.getBooleanOrDefault(nbt, "missing", true));
        assertEquals(99L, NbtEncodeable.getLongOrDefault(nbt, "missing", 99L));
        assertEquals(99L, NbtEncodeable.getLongOrDefault(nbt, "long_as_string", 99L));
    }

    /**
     * Verifies typed readers return stored values when keys are present with the expected type.
     */
    @Test
    void typedReadersReturnStoredValues() {
        CompoundTag nbt = new CompoundTag();
        BlockPos pos = new BlockPos(12, 64, -5);
        nbt.put("compound", new CompoundTag());
        nbt.put("pos", NbtUtils.writeBlockPos(pos));
        nbt.putString("string", "value");
        nbt.putInt("int", 42);
        nbt.putBoolean("boolean", true);
        nbt.putLong("long", 123456789L);

        assertTrue(NbtEncodeable.hasCompound(nbt, "compound"));
        assertEquals(Optional.of(pos), NbtEncodeable.getBlockPos(nbt, "pos"));
        assertEquals("value", NbtEncodeable.getStringOrEmpty(nbt, "string"));
        assertEquals(42, NbtEncodeable.getIntOrZero(nbt, "int"));
        assertEquals(42, NbtEncodeable.getIntOrDefault(nbt, "int", 13));
        assertTrue(NbtEncodeable.getBooleanOrDefault(nbt, "boolean", false));
        assertEquals(123456789L, NbtEncodeable.getLongOrDefault(nbt, "long", 99L));
    }

    /**
     * Verifies conditional writers only persist meaningful values.
     */
    @Test
    void conditionalWritersOnlyPersistMeaningfulValues() {
        CompoundTag nbt = new CompoundTag();
        BlockPos pos = new BlockPos(1, 2, 3);

        NbtEncodeable.putBlockPosIfPresent(nbt, "present_pos", pos);
        NbtEncodeable.putBlockPosIfPresent(nbt, "missing_pos", null);
        NbtEncodeable.putStringIfNotEmpty(nbt, "string", "value");
        NbtEncodeable.putStringIfNotEmpty(nbt, "empty_string", "");
        NbtEncodeable.putIntIfNonZero(nbt, "int", 5);
        NbtEncodeable.putIntIfNonZero(nbt, "zero_int", 0);
        NbtEncodeable.putIntIfNonDefault(nbt, "custom_int", 9, 7);
        NbtEncodeable.putIntIfNonDefault(nbt, "default_int", 7, 7);
        NbtEncodeable.putBooleanIfTrue(nbt, "true_bool", true);
        NbtEncodeable.putBooleanIfTrue(nbt, "false_bool_as_true", false);
        NbtEncodeable.putBooleanIfFalse(nbt, "false_bool", false);
        NbtEncodeable.putBooleanIfFalse(nbt, "true_bool_as_false", true);
        NbtEncodeable.putLongIfNonDefault(nbt, "custom_long", 10L, 11L);
        NbtEncodeable.putLongIfNonDefault(nbt, "default_long", 11L, 11L);

        assertEquals(Optional.of(pos), NbtEncodeable.getBlockPos(nbt, "present_pos"));
        assertFalse(nbt.contains("missing_pos"));
        assertEquals("value", nbt.getString("string"));
        assertFalse(nbt.contains("empty_string"));
        assertEquals(5, nbt.getInt("int"));
        assertFalse(nbt.contains("zero_int"));
        assertEquals(9, nbt.getInt("custom_int"));
        assertFalse(nbt.contains("default_int"));
        assertTrue(nbt.getBoolean("true_bool"));
        assertFalse(nbt.contains("false_bool_as_true"));
        assertFalse(nbt.getBoolean("false_bool"));
        assertFalse(nbt.contains("true_bool_as_false"));
        assertEquals(10L, nbt.getLong("custom_long"));
        assertFalse(nbt.contains("default_long"));
    }
}

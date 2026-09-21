package space.ajcool.ardapaths.mc.blocks.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import space.ajcool.ardapaths.core.data.TimeActivation;
import space.ajcool.ardapaths.core.data.TimeOfDay;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Serialization tests for path marker chapter NBT payloads.
 */
class ChapterNbtDataTest {
    /**
     * Verifies every persisted chapter field survives a full NBT round trip.
     */
    @Test
    void everyFieldRoundTripsThroughNbt() {
        PathMarkerBlockEntity.ChapterNbtData data = PathMarkerBlockEntity.ChapterNbtData.empty("shire");
        data.setTarget(new BlockPos(-17, 5, 34));
        data.setLookAt(new BlockPos(100, 70, -200));
        data.setTargetMarkerDimension("multiworld:moria_big");
        data.setTargetMarker(new BlockPos(300, 40, -500));
        data.setProximityMessage("Mind the road");
        data.setActivationRange(11);
        data.setChapterStart(true);
        data.setDisplayChapterTitleOnTrail(true);
        data.setDisplayAboveBlocks(false);
        data.setWeather(2);
        data.setTimeOfDay(5_922_000L);
        data.setTimeActivation(TimeActivation.COMPUTED);
        data.setAutoTeleportTarget("bag-end");
        data.setGiveItem("minecraft:bread");
        data.setPackedMessageData(123456789L);

        PathMarkerBlockEntity.ChapterNbtData restored = PathMarkerBlockEntity.ChapterNbtData.fromNbt(data.toNbt(new CompoundTag()));

        assertEquals(data.getTarget(), restored.getTarget());
        assertEquals(data.getLookAt(), restored.getLookAt());
        assertEquals(data.getTargetMarkerDimension(), restored.getTargetMarkerDimension());
        assertEquals(data.getTargetMarker(), restored.getTargetMarker());
        assertTrue(restored.hasTargetMarker());
        assertEquals(data.getProximityMessage(), restored.getProximityMessage());
        assertEquals(data.getActivationRange(), restored.getActivationRange());
        assertEquals(data.getChapterId(), restored.getChapterId());
        assertEquals(data.isChapterStart(), restored.isChapterStart());
        assertEquals(data.isDisplayChapterTitleOnTrail(), restored.isDisplayChapterTitleOnTrail());
        assertEquals(data.isDisplayAboveBlocks(), restored.isDisplayAboveBlocks());
        assertEquals(data.getWeather(), restored.getWeather());
        assertEquals(data.getTimeOfDay(), restored.getTimeOfDay());
        assertEquals(data.getTimeActivation(), restored.getTimeActivation());
        assertEquals(data.getAutoTeleportTarget(), restored.getAutoTeleportTarget());
        assertEquals(data.getGiveItem(), restored.getGiveItem());
        assertEquals(data.getPackedMessageData(), restored.getPackedMessageData());
    }

    /**
     * Verifies default-valued fields are not persisted in compact chapter NBT.
     */
    @Test
    void defaultsAreElidedFromNbt() {
        PathMarkerBlockEntity.ChapterNbtData data = PathMarkerBlockEntity.ChapterNbtData.empty("shire");

        CompoundTag nbt = data.toNbt(new CompoundTag());

        assertEquals("shire", nbt.getString("chapter"));
        assertFalse(nbt.contains("display_above_blocks"));
        assertFalse(nbt.contains("weather"));
        assertFalse(nbt.contains("time_of_day"));
        assertFalse(nbt.contains("time_transition_range"));
        assertFalse(nbt.contains("target_marker_dimension"));
        assertFalse(nbt.contains("target_marker"));
        assertFalse(nbt.contains("packed_message_data"));
        assertEquals(TimeActivation.MARKER_RANGE, PathMarkerBlockEntity.ChapterNbtData.fromNbt(nbt).getTimeActivation());
        assertEquals(PathMarkerBlockEntity.ChapterNbtData.DEFAULT_PACKED_MESSAGE_DATA, PathMarkerBlockEntity.ChapterNbtData.fromNbt(nbt).getPackedMessageData());
    }

    /**
     * Verifies long absolute marker time survives a compact NBT round trip.
     */
    @Test
    void longTimeOfDayRoundTripsThroughNbt() {
        PathMarkerBlockEntity.ChapterNbtData data = PathMarkerBlockEntity.ChapterNbtData.empty("shire");
        data.setTimeOfDay(5_910_000L);

        CompoundTag nbt = data.toNbt(new CompoundTag());
        PathMarkerBlockEntity.ChapterNbtData restored = PathMarkerBlockEntity.ChapterNbtData.fromNbt(nbt);

        assertEquals(5_910_000L, nbt.getLong("time_of_day"));
        assertEquals(5_910_000L, restored.getTimeOfDay());
    }

    /**
     * Verifies old integer time NBT is ignored now that marker times are absolute longs.
     */
    @Test
    void integerTimeOfDayFallsBackToUnset() {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("time_of_day", 6000);

        PathMarkerBlockEntity.ChapterNbtData restored = PathMarkerBlockEntity.ChapterNbtData.fromNbt(nbt);

        assertEquals(TimeOfDay.UNSET, restored.getTimeOfDay());
    }

    /**
     * Verifies legacy numeric transition ranges normalize to marker-range activation.
     */
    @Test
    void legacyNumericTimeRangeNormalizesToMarkerRange() {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("time_transition_range", 64);

        PathMarkerBlockEntity.ChapterNbtData restored = PathMarkerBlockEntity.ChapterNbtData.fromNbt(nbt);

        assertEquals(TimeActivation.MARKER_RANGE, restored.getTimeActivation());
        assertFalse(restored.toNbt(new CompoundTag()).contains("time_transition_range"));
    }

    /**
     * Verifies absent target-like fields remain absent and read back as null.
     */
    @Test
    void nullTargetAndLookAtWriteNoKeysAndReadBackNull() {
        PathMarkerBlockEntity.ChapterNbtData data = PathMarkerBlockEntity.ChapterNbtData.empty("shire");

        CompoundTag nbt = data.toNbt(new CompoundTag());
        PathMarkerBlockEntity.ChapterNbtData restored = PathMarkerBlockEntity.ChapterNbtData.fromNbt(nbt);

        assertFalse(nbt.contains("target"));
        assertFalse(nbt.contains("look_at"));
        assertFalse(nbt.contains("target_marker_dimension"));
        assertFalse(nbt.contains("target_marker"));
        assertNull(restored.getTarget());
        assertNull(restored.getLookAt());
        assertEquals("", restored.getTargetMarkerDimension());
        assertNull(restored.getTargetMarker());
        assertFalse(restored.hasTargetMarker());
    }

    /**
     * Verifies only a default data object is considered empty.
     */
    @Test
    void isEmptyOnlyForDefaultConstructedData() {
        PathMarkerBlockEntity.ChapterNbtData empty = PathMarkerBlockEntity.ChapterNbtData.empty("shire");
        PathMarkerBlockEntity.ChapterNbtData populated = PathMarkerBlockEntity.ChapterNbtData.empty("shire");
        populated.setActivationRange(1);

        assertTrue(empty.isEmpty());
        assertFalse(populated.isEmpty());
    }
}

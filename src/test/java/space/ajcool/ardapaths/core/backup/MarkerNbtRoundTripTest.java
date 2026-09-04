package space.ajcool.ardapaths.core.backup;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import space.ajcool.ardapaths.core.backup.dto.PathNodeDto;
import space.ajcool.ardapaths.core.data.BitPacker;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Integrity tests for the backup exporter and restorer marker NBT contract.
 */
// Instantiated and invoked by the JUnit test runner via reflection.
@SuppressWarnings("unused")
class MarkerNbtRoundTripTest {

    /**
     * Verifies exported nodes restore to the same chapter compound written by marker NBT.
     */
    @Test
    // Instantiated and invoked by the JUnit test runner via reflection.
    @SuppressWarnings("unused")
    void exportedNodeRestoresEquivalentChapterNbt() {
        BlockPos markerPosition = new BlockPos(-33, 70, 48);
        PathMarkerBlockEntity.ChapterNbtData original = PathMarkerBlockEntity.ChapterNbtData.empty("shire");
        original.setTarget(new BlockPos(80, -4, -96));
        original.setLookAt(new BlockPos(-1000, 65, 2048));
        original.setProximityMessage("Mind the road");
        original.setActivationRange(14);
        original.setChapterStart(true);
        original.setDisplayChapterTitleOnTrail(true);
        original.setDisplayAboveBlocks(false);
        original.setWeather(2);
        original.setTimeOfDay(6000);
        original.setTimeTransitionRange(24);
        original.setAutoTeleportTarget("bag-end");
        original.setGiveItem("minecraft:bread");
        original.setPackedMessageData(BitPacker.packFive(7, 120, 9, 3, 11));
        ScannedMarkerData marker = new ScannedMarkerData(
                "minecraft:overworld",
                markerPosition,
                Map.of("frodo", Map.of("shire", original))
        );

        Optional<PathNodeDto> node = new BackupManager().createNode("frodo", "shire", marker);
        CompoundTag restored = new MarkerRestorer().toChapterNbt("shire", node.orElseThrow());

        assertEquals(original.toNbt(new CompoundTag()), restored);
        assertNotNull(original.getTarget(), "target should be set in the test fixture");
        assertEquals(markerPosition.offset(original.getTarget()).asLong(), node.orElseThrow().next());
        assertEquals("-1000 65 2048", node.orElseThrow().lookAt());
    }

    /**
     * Verifies default-valued exported node fields restore to a compact chapter compound.
     */
    @Test
    // Instantiated and invoked by the JUnit test runner via reflection.
    @SuppressWarnings("unused")
    void defaultNodeFieldsRemainElidedAfterRoundTrip() {
        PathMarkerBlockEntity.ChapterNbtData original = PathMarkerBlockEntity.ChapterNbtData.empty("moria");
        original.setProximityMessage("Only non-default");
        ScannedMarkerData marker = new ScannedMarkerData(
                "minecraft:overworld",
                new BlockPos(32, -8, -33),
                Map.of("frodo", Map.of("moria", original))
        );

        PathNodeDto node = new BackupManager().createNode("frodo", "moria", marker).orElseThrow();
        CompoundTag restored = new MarkerRestorer().toChapterNbt("moria", node);

        assertEquals(original.toNbt(new CompoundTag()), restored);
        assertFalse(restored.contains("target"));
        assertFalse(restored.contains("packed_message_data"));
    }
}

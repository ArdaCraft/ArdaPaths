package space.ajcool.ardapaths.core.backup;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.junit.jupiter.api.Test;
import space.ajcool.ardapaths.core.integration.WarpLocation;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests deterministic backup chapter-start candidate selection.
 */
class ChapterStartCandidatePickerTest {

    /**
     * Verifies the nearest same-dimension marker is selected when a warp resolves.
     */
    @Test
    void choosesNearestCandidateInWarpDimension() {
        ScannedMarkerData far = marker("minecraft:overworld", new BlockPos(100, 64, 0));
        ScannedMarkerData near = marker("minecraft:overworld", new BlockPos(12, 64, 0));
        ScannedMarkerData otherDimension = marker("minecraft:the_nether", new BlockPos(10, 64, 0));
        WarpLocation warp = new WarpLocation(
                ResourceKey.create(Registries.DIMENSION, Identifier.parse("minecraft:overworld")),
                new BlockPos(10, 64, 0)
        );

        assertEquals(near, BackupManager.chooseChapterStartCandidate(List.of(far, near, otherDimension), warp).orElseThrow());
    }

    /**
     * Verifies unresolved warps fall back to the lowest packed position.
     */
    @Test
    void choosesLowestPackedPositionWithoutWarp() {
        ScannedMarkerData high = marker("minecraft:overworld", new BlockPos(20, 64, 0));
        ScannedMarkerData low = marker("minecraft:overworld", new BlockPos(1, 64, 0));

        assertEquals(low, BackupManager.chooseChapterStartCandidate(List.of(high, low), null).orElseThrow());
    }

    /**
     * Verifies no candidate is returned for an empty candidate list.
     */
    @Test
    void returnsEmptyForNoCandidates() {
        assertTrue(BackupManager.chooseChapterStartCandidate(List.of(), null).isEmpty());
    }

    /**
     * Creates scanned marker data for tests.
     *
     * @param dimensionId dimension identifier
     * @param position    marker position
     * @return scanned marker data
     */
    private ScannedMarkerData marker(String dimensionId, BlockPos position) {
        PathMarkerBlockEntity.ChapterNbtData data = PathMarkerBlockEntity.ChapterNbtData.empty("chapter");
        data.setChapterStart(true);
        return new ScannedMarkerData(dimensionId, position, Map.of("path", Map.of("chapter", data)));
    }
}

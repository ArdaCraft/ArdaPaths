package space.ajcool.ardapaths.core.backup;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import space.ajcool.ardapaths.core.backup.dto.*;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for restore marker planning.
 */
class MarkerRestorerPlanTest {

    /**
     * Verifies an indexed marker without exported node data is left unplanned.
     */
    @Test
    void indexedMarkerWithNoNodesIsNotPlanned() {
        MarkerIndexDto index = index("minecraft:overworld", pos(1, 64, 1));

        List<PlannedMarker> planned = new MarkerRestorer().plan(index, List.of(path("frodo", chapter("shire", List.of()))));

        assertTrue(planned.isEmpty());
    }

    /**
     * Creates a single-dimension marker index.
     *
     * @param dimensionId indexed dimension
     * @param position    indexed marker position
     * @return marker index DTO
     */
    @SuppressWarnings("SameParameterValue")
    private static MarkerIndexDto index(String dimensionId, BlockPos position) {
        return new MarkerIndexDto(Map.of(dimensionId, Map.of(Long.toString(position.asLong()), coords(position))));
    }

    /**
     * Creates a block position.
     *
     * @param x x coordinate
     * @param y y coordinate
     * @param z z coordinate
     * @return block position
     */
    private static BlockPos pos(int x, int y, int z) {
        return new BlockPos(x, y, z);
    }

    /**
     * Creates a path DTO for plan tests.
     *
     * @param id       path identifier
     * @param chapters path chapters
     * @return path file DTO
     */
    private static PathFileDto path(String id, PathChapterDto... chapters) {
        return new PathFileDto(id, id, null, List.of(chapters), new PathDiagnosticsDto(List.of(), List.of(), List.of(), Map.of()));
    }

    /**
     * Creates a chapter DTO for plan tests.
     *
     * @param id    chapter identifier
     * @param nodes chapter nodes
     * @return path chapter DTO
     */
    private static PathChapterDto chapter(String id, List<PathNodeDto> nodes) {
        return new PathChapterDto(id, id, "0", 0, "", null, null, nodes);
    }

    /**
     * Converts a position to marker index coordinates.
     *
     * @param position block position
     * @return [x, y, z] coordinate array
     */
    private static int[] coords(BlockPos position) {
        return new int[]{position.getX(), position.getY(), position.getZ()};
    }

    /**
     * Verifies nodes outside the marker index are dropped while indexed nodes remain.
     */
    @Test
    void unindexedDimensionOrPositionNodesAreDropped() {
        BlockPos indexed = pos(1, 64, 1);
        MarkerIndexDto index = index("minecraft:overworld", indexed);
        PathNodeDto kept = node("minecraft:overworld", indexed, null, "kept");
        PathNodeDto wrongPosition = node("minecraft:overworld", pos(2, 64, 2), null, "wrong");
        PathNodeDto wrongDimension = node("minecraft:the_nether", indexed, null, "wrong");

        List<PlannedMarker> planned = new MarkerRestorer().plan(index, List.of(path("frodo", chapter("shire", List.of(kept, wrongPosition, wrongDimension)))));

        assertEquals(1, planned.size());
        assertEquals(indexed.asLong(), planned.getFirst().packedPos());
        assertEquals("kept", planned.getFirst().pathsNbt().getCompound("frodo").getCompound("shire").getString("proximity_message"));
    }

    /**
     * Creates a node DTO for plan tests.
     *
     * @param dimensionId node dimension
     * @param position    node marker position
     * @param next        next marker position, or null
     * @param message     proximity message
     * @return path node DTO
     */
    @SuppressWarnings("SameParameterValue")
    private static PathNodeDto node(String dimensionId, BlockPos position, BlockPos next, String message) {
        return new PathNodeDto(
                dimensionId,
                position.asLong(),
                next == null ? null : next.asLong(),
                false,
                false,
                true,
                PathMarkerBlockEntity.ChapterNbtData.UNSET,
                PathMarkerBlockEntity.ChapterNbtData.UNSET,
                null,
                "",
                "",
                "",
                message,
                0,
                new NodeAnimDto(PathMarkerBlockEntity.ChapterNbtData.DEFAULT_PACKED_MESSAGE_DATA, new int[]{5, 100, 5, 2, 8})
        );
    }

    /**
     * Verifies planned markers are ordered by dimension, chunk, and height.
     */
    @Test
    void planOrdersByDimensionChunkAndY() {
        BlockPos overworldHigh = pos(0, 90, 0);
        BlockPos overworldLow = pos(0, 12, 0);
        BlockPos overworldOtherChunk = pos(32, 5, 0);
        BlockPos nether = pos(0, 1, 0);
        MarkerIndexDto index = new MarkerIndexDto(Map.of(
                "minecraft:overworld", Map.of(
                        Long.toString(overworldHigh.asLong()), coords(overworldHigh),
                        Long.toString(overworldLow.asLong()), coords(overworldLow),
                        Long.toString(overworldOtherChunk.asLong()), coords(overworldOtherChunk)
                ),
                "minecraft:the_nether", Map.of(Long.toString(nether.asLong()), coords(nether))
        ));
        List<PathNodeDto> nodes = List.of(
                node("minecraft:overworld", overworldOtherChunk, null, "c"),
                node("minecraft:the_nether", nether, null, "d"),
                node("minecraft:overworld", overworldHigh, null, "b"),
                node("minecraft:overworld", overworldLow, null, "a")
        );

        List<PlannedMarker> planned = new MarkerRestorer().plan(index, List.of(path("frodo", chapter("shire", nodes))));

        assertEquals(List.of(overworldLow.asLong(), overworldHigh.asLong(), overworldOtherChunk.asLong(), nether.asLong()), planned.stream().map(PlannedMarker::packedPos).toList());
    }

    /**
     * Verifies multiple paths and chapters merge into one marker payload.
     */
    @Test
    void planMergesPathsAndChaptersAtOnePosition() {
        BlockPos position = pos(1, 64, 1);
        MarkerIndexDto index = index("minecraft:overworld", position);

        List<PlannedMarker> planned = new MarkerRestorer().plan(index, List.of(
                path("frodo", chapter("shire", List.of(node("minecraft:overworld", position, null, "shire"))), chapter("moria", List.of(node("minecraft:overworld", position, null, "moria")))),
                path("aragorn", chapter("rohan", List.of(node("minecraft:overworld", position, null, "rohan"))), chapter("gondor", List.of(node("minecraft:overworld", position, null, "gondor"))))
        ));

        CompoundTag paths = planned.getFirst().pathsNbt();

        assertEquals("shire", paths.getCompound("frodo").getCompound("shire").getString("proximity_message"));
        assertEquals("moria", paths.getCompound("frodo").getCompound("moria").getString("proximity_message"));
        assertEquals("rohan", paths.getCompound("aragorn").getCompound("rohan").getString("proximity_message"));
        assertEquals("gondor", paths.getCompound("aragorn").getCompound("gondor").getString("proximity_message"));
    }
}

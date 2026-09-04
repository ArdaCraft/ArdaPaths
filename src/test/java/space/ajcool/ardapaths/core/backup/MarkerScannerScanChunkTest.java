package space.ajcool.ardapaths.core.backup;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.MarkerTestSupport;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;

/**
 * Regression tests for extracting path marker data from chunk NBT.
 */
class MarkerScannerScanChunkTest {

    /**
     * Verifies populated, empty, unrelated, and legacy block entities are separated correctly.
     */
    @Test
    void scanChunkSplitsPopulatedEmptyAndIgnoredMarkers() {
        MarkerTestSupport.installServerConfig();
        CompoundTag chunk = new CompoundTag();
        ListTag blockEntities = new ListTag();
        blockEntities.add(populatedMarker(new BlockPos(1, 64, 1)));
        blockEntities.add(emptyMarker(new BlockPos(2, 64, 2)));
        blockEntities.add(unrelatedBlockEntity(new BlockPos(3, 64, 3)));
        blockEntities.add(legacyMarker(new BlockPos(4, 64, 4)));
        chunk.put("block_entities", blockEntities);
        List<ScannedMarkerData> markers = new ArrayList<>();
        List<ScannedMarkerData> emptyMarkers = new ArrayList<>();

        try (MockedStatic<ArdaPaths> ardaPaths = mockStatic(ArdaPaths.class)) {
            ardaPaths.when(ArdaPaths::amITheServer).thenReturn(true);

            new MarkerScanner(null).scanChunk(chunk, "minecraft:overworld", markers, emptyMarkers);

            assertEquals(2, markers.size());
            assertEquals(1, emptyMarkers.size());
            assertEquals(new BlockPos(2, 64, 2), emptyMarkers.getFirst().position());
            assertEquals("current", markers.get(0).pathData().get("frodo").get("shire").getProximityMessage());
            assertEquals(new BlockPos(5, 6, 7), markers.get(1).pathData().get("frodo").get("default").getTarget());
        } finally {
            MarkerTestSupport.clearConfigs();
        }
    }

    /**
     * Creates a current-shape marker with chapter data.
     *
     * @param position marker position
     * @return block entity NBT
     */
    private static CompoundTag populatedMarker(BlockPos position) {
        CompoundTag marker = baseBlockEntity(MarkerScanner.PATH_MARKER_BLOCK_ENTITY_ID, position);
        CompoundTag paths = new CompoundTag();
        CompoundTag path = new CompoundTag();
        CompoundTag chapter = new CompoundTag();
        chapter.putString("proximity_message", "current");
        path.put("shire", chapter);
        paths.put("frodo", path);
        marker.put("paths", paths);
        return marker;
    }

    /**
     * Creates a current-shape marker with no path entries.
     *
     * @param position marker position
     * @return block entity NBT
     */
    private static CompoundTag emptyMarker(BlockPos position) {
        CompoundTag marker = baseBlockEntity(MarkerScanner.PATH_MARKER_BLOCK_ENTITY_ID, position);
        marker.put("paths", new CompoundTag());
        return marker;
    }

    /**
     * Creates an unrelated block entity.
     *
     * @param position block entity position
     * @return block entity NBT
     */
    private static CompoundTag unrelatedBlockEntity(BlockPos position) {
        return baseBlockEntity("minecraft:chest", position);
    }

    /**
     * Creates a legacy marker that should migrate during scan.
     *
     * @param position marker position
     * @return block entity NBT
     */
    private static CompoundTag legacyMarker(BlockPos position) {
        CompoundTag marker = baseBlockEntity(MarkerScanner.PATH_MARKER_BLOCK_ENTITY_ID, position);
        marker.store("targetOffset-0", BlockPos.CODEC, new BlockPos(5, 6, 7));
        marker.putString("proximityMessage", "legacy");
        return marker;
    }

    /**
     * Creates common block entity coordinate fields.
     *
     * @param id       block entity identifier
     * @param position block entity position
     * @return block entity NBT
     */
    private static CompoundTag baseBlockEntity(String id, BlockPos position) {
        CompoundTag blockEntity = new CompoundTag();
        blockEntity.putString("id", id);
        blockEntity.putInt("x", position.getX());
        blockEntity.putInt("y", position.getY());
        blockEntity.putInt("z", position.getZ());
        return blockEntity;
    }
}

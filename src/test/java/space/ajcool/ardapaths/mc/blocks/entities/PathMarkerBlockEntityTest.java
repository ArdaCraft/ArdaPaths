package space.ajcool.ardapaths.mc.blocks.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import space.ajcool.ardapaths.MarkerTestSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Regression tests for path marker block entity NBT loading.
 */
class PathMarkerBlockEntityTest {

    /**
     * Clears global configs after tests that intentionally leave them unset.
     */
    @AfterEach
    void tearDown() {
        MarkerTestSupport.clearConfigs();
    }

    /**
     * Verifies level-less marker loads preserve all path data without consulting configs.
     *
     * @throws ReflectiveOperationException if the marker fixture cannot initialize private state
     */
    @Test
    void applyNbtWithNullLevelAndNoConfigsPreservesEveryEntry() throws ReflectiveOperationException {
        MarkerTestSupport.clearConfigs();
        CompoundTag paths = pathsWithMultipleChapters();

        PathMarkerBlockEntity marker = MarkerTestSupport.markerWithPathData();

        marker.applyNbt(paths);

        PathMarkerBlockEntity.ChapterNbtData shire = marker.getChapterData("frodo", "shire", false);
        PathMarkerBlockEntity.ChapterNbtData moria = marker.getChapterData("frodo", "moria", false);
        PathMarkerBlockEntity.ChapterNbtData rohan = marker.getChapterData("aragorn", "rohan", false);

        assertNotNull(shire);
        assertNotNull(moria);
        assertNotNull(rohan);
        assertEquals("Mind the road", shire.getProximityMessage());
        assertEquals(new BlockPos(4, 5, 6), moria.getTarget());
        assertEquals(9, rohan.getActivationRange());
    }

    /**
     * Creates a multi-path marker NBT fixture with non-empty chapter entries.
     *
     * @return paths compound suitable for {@link PathMarkerBlockEntity#applyNbt(CompoundTag)}
     */
    private CompoundTag pathsWithMultipleChapters() {
        CompoundTag paths = new CompoundTag();
        CompoundTag frodo = new CompoundTag();
        CompoundTag shire = new CompoundTag();
        shire.putString("proximity_message", "Mind the road");
        frodo.put("shire", shire);

        CompoundTag moria = new CompoundTag();
        moria.put("target", NbtUtils.writeBlockPos(new BlockPos(4, 5, 6)));
        frodo.put("moria", moria);
        paths.put("frodo", frodo);

        CompoundTag aragorn = new CompoundTag();
        CompoundTag rohan = new CompoundTag();
        rohan.putInt("activation_range", 9);
        aragorn.put("rohan", rohan);
        paths.put("aragorn", aragorn);
        return paths;
    }

    /**
     * Verifies unknown config IDs are retained when trusted marker data is written back.
     *
     * @throws ReflectiveOperationException if the marker fixture cannot initialize private state
     */
    @Test
    void applyNbtRoundTripsUnknownConfigPathThroughToNbt() throws ReflectiveOperationException {
        MarkerTestSupport.clearConfigs();
        CompoundTag paths = new CompoundTag();
        CompoundTag unknownPath = new CompoundTag();
        CompoundTag unknownChapter = new CompoundTag();
        unknownChapter.putString("proximity_message", "Lost road");
        unknownChapter.putInt("activation_range", 13);
        unknownPath.put("lost", unknownChapter);
        paths.put("unknown_path", unknownPath);

        PathMarkerBlockEntity marker = MarkerTestSupport.markerWithPathData();

        marker.applyNbt(paths);

        assertEquals(paths, marker.toNbt(new CompoundTag()).getCompound("paths"));
    }
}

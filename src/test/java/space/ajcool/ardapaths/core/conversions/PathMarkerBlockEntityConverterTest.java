package space.ajcool.ardapaths.core.conversions;

import com.google.gson.Gson;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.ArdaPathsClient;
import space.ajcool.ardapaths.MarkerTestSupport;
import space.ajcool.ardapaths.core.Client;
import space.ajcool.ardapaths.core.data.config.client.ClientConfig;
import space.ajcool.ardapaths.core.data.config.server.ServerConfig;
import space.ajcool.ardapaths.mc.NbtEncodeable;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;
import space.ajcool.ardapaths.screens.marker.MarkerLinkTracker;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

/**
 * Regression tests for on-disk Path Marker NBT migrations.
 */
class PathMarkerBlockEntityConverterTest {

    /**
     * Gson instance for building config DTOs without touching disk-backed managers.
     */
    private static final Gson GSON = new Gson();

    /**
     * Verifies legacy flat marker NBT is moved into the current paths/chapter structure.
     */
    @Test
    void convertsLegacyFlatMarkerNbtIntoPathsTree() {
        ArdaPaths.CONFIG = GSON.fromJson("""
                {
                  "paths": [
                    {"id":"frodo","name":"Frodo","chapters":{"default":{"id":"default","name":"Default","date":"0","index":0}}},
                    {"id":"aragorn","name":"Aragorn","chapters":{"default":{"id":"default","name":"Default","date":"0","index":0}}}
                  ]
                }
                """, ServerConfig.class);

        CompoundTag legacy = new CompoundTag();
        legacy.putString("proximityMessage", "Mind the road");
        legacy.putInt("activationRange", 9);
        legacy.store("targetOffset-1", BlockPos.CODEC, new BlockPos(4, 5, 6));

        try (MockedStatic<ArdaPaths> ardaPaths = mockStatic(ArdaPaths.class)) {
            ardaPaths.when(ArdaPaths::amITheServer).thenReturn(true);

            CompoundTag converted = PathMarkerBlockEntityConverter.convertNbt(legacy);
            CompoundTag markerData = NbtEncodeable.getCompound(
                    NbtEncodeable.getCompound(
                            NbtEncodeable.getCompound(converted, "paths"),
                            "aragorn"),
                    "default");

            assertSame(legacy, converted);
            assertFalse(converted.contains("proximityMessage"));
            assertFalse(converted.contains("activationRange"));
            assertFalse(converted.contains("targetOffset-0"));
            assertFalse(converted.contains("targetOffset-1"));
            assertInstanceOf(CompoundTag.class, converted.get("paths"));
            assertFalse(NbtEncodeable.getCompound(converted, "paths").contains("frodo"));
            assertEquals(new BlockPos(4, 5, 6), markerData.read("target", BlockPos.CODEC).orElseThrow());
            assertEquals("Mind the road", markerData.getStringOr("proximity_message", ""));
            assertEquals(9, markerData.getIntOr("activation_range", 0));
        } finally {
            ArdaPaths.CONFIG = null;
        }
    }

    /**
     * Verifies current marker NBT is not rewritten when the paths tree already exists.
     */
    @Test
    void leavesCurrentPathsTreeUntouched() {
        CompoundTag current = new CompoundTag();
        CompoundTag paths = new CompoundTag();
        CompoundTag sentinel = new CompoundTag();
        sentinel.putString("value", "keep");
        paths.put("frodo", sentinel);
        current.put("paths", paths);
        current.putString("proximityMessage", "legacy-looking data");

        CompoundTag converted = PathMarkerBlockEntityConverter.convertNbt(current);

        assertSame(current, converted);
        assertEquals("legacy-looking data", converted.getStringOr("proximityMessage", ""));
        assertEquals("keep", NbtEncodeable.getCompound(NbtEncodeable.getCompound(converted, "paths"), "frodo").getStringOr("value", ""));
    }

    /**
     * Verifies a tag with no legacy marker keys is not rewritten as a paths marker tag.
     */
    @Test
    void leavesTagWithNoLegacyMarkerKeysUntouched() {
        ArdaPaths.CONFIG = GSON.fromJson("""
                {
                  "paths": [
                    {"id":"frodo","name":"Frodo","chapters":{"default":{"id":"default","name":"Default","date":"0","index":0}}}
                  ]
                }
                """, ServerConfig.class);

        CompoundTag unrelated = new CompoundTag();
        unrelated.putString("id", "ardapaths:path_marker");
        unrelated.putInt("x", 1);
        unrelated.putInt("y", 2);
        unrelated.putInt("z", 3);
        CompoundTag originalCopy = unrelated.copy();

        try (MockedStatic<ArdaPaths> ardaPaths = mockStatic(ArdaPaths.class)) {
            ardaPaths.when(ArdaPaths::amITheServer).thenReturn(true);

            CompoundTag converted = PathMarkerBlockEntityConverter.convertNbt(unrelated);

            assertSame(unrelated, converted);
            assertFalse(converted.contains("paths"));
            assertEquals(originalCopy, converted);
        } finally {
            ArdaPaths.CONFIG = null;
        }
    }

    /**
     * Verifies level-less remote marker NBT loading preserves data when unknown keys exist.
     */
    @SuppressWarnings({"DataFlowIssue", "ExtractMethodRecommender"})
    @Test
    void applyNbtKeepsKnownPathDataWhenAnotherPathKeyIsUnknown() throws ReflectiveOperationException {
        ArdaPathsClient.CONFIG = GSON.fromJson("""
                {
                  "paths": [
                    {"id":"frodo","name":"Frodo","chapters":{"shire":{"id":"shire","name":"Shire","date":"0","index":0}}}
                  ]
                }
                """, ClientConfig.class);

        CompoundTag paths = new CompoundTag();
        CompoundTag knownPath = new CompoundTag();
        CompoundTag knownChapter = new CompoundTag();
        knownChapter.putString("proximity_message", "Known road");
        knownPath.put("shire", knownChapter);
        paths.put("frodo", knownPath);

        CompoundTag unknownPath = new CompoundTag();
        CompoundTag unknownChapter = new CompoundTag();
        unknownChapter.putString("proximity_message", "Unknown road");
        unknownPath.put("lost", unknownChapter);
        paths.put("unknown_path", unknownPath);

        PathMarkerBlockEntity marker = MarkerTestSupport.markerWithPathData();

        try (MockedStatic<Client> client = mockStatic(Client.class)) {
            client.when(Client::isInSinglePlayer).thenReturn(true);

            marker.applyNbt(paths);

            assertEquals("Known road", marker.getChapterData("frodo", "shire", false).getProximityMessage());
            assertEquals("Unknown road", marker.getChapterData("unknown_path", "lost", false).getProximityMessage());
        } finally {
            ArdaPathsClient.CONFIG = null;
        }
    }

    /**
     * Verifies link counts ignore empty entries created by mutating marker reads.
     */
    @SuppressWarnings("DataFlowIssue")
    @Test
    void markerLinkCountsIgnoreEmptyStubsAndCountNonEmptyEntriesOnce() throws ReflectiveOperationException {
        PathMarkerBlockEntity marker = MarkerTestSupport.markerWithPathData();
        marker.getChapterData("frodo", "shire", true);
        PathMarkerBlockEntity.ChapterNbtData linked = marker.getChapterData("frodo", "moria", true);
        linked.setProximityMessage("Speak friend");
        PathMarkerBlockEntity.ChapterNbtData otherLinked = marker.getChapterData("aragorn", "rohan", true);
        otherLinked.setChapterStart(true);

        MarkerLinkTracker.LinkCounts counts = new MarkerLinkTracker(marker, null).linkCounts();

        assertEquals(2, counts.paths());
        assertEquals(2, counts.chapters());
    }

    /**
     * Verifies fully populated current marker data survives an apply/write round trip.
     */
    @Test
    void applyNbtToNbtPreservesFullyPopulatedMarker() throws ReflectiveOperationException {
        MarkerTestSupport.installClientConfig();

        try {
            CompoundTag paths = new CompoundTag();
            CompoundTag path = new CompoundTag();
            CompoundTag chapter = new CompoundTag();
            chapter.putString("proximity_message", "Speak friend");
            chapter.putInt("activation_range", 12);
            chapter.store("target", BlockPos.CODEC, new BlockPos(-33, 4, 48));
            chapter.store("look_at", BlockPos.CODEC, new BlockPos(20, 70, -15));
            chapter.putString("chapter", "moria");
            chapter.putBoolean("chapter_start", true);
            chapter.putBoolean("display_chapter_title_on_trail", true);
            chapter.putBoolean("display_above_blocks", false);
            chapter.putInt("weather", 2);
            chapter.putInt("time_of_day", 18000);
            chapter.putInt("time_transition_range", 32);
            chapter.putString("auto_teleport_target", "moria-gate");
            chapter.putString("give_item", "minecraft:bread");
            chapter.putLong("packed_message_data", 123456789L);
            path.put("moria", chapter);
            paths.put("frodo", path);

            PathMarkerBlockEntity marker = MarkerTestSupport.markerWithPathData();
            try (MockedStatic<Client> client = mockStatic(Client.class)) {
                client.when(Client::isInSinglePlayer).thenReturn(true);
                marker.applyNbt(paths);
            }

            assertEquals(paths, NbtEncodeable.getCompound(marker.toNbt(new CompoundTag()), "paths"));
        } finally {
            MarkerTestSupport.clearConfigs();
        }
    }

    /**
     * Verifies empty chapter stubs and their parent paths are pruned from marker NBT output.
     */
    @SuppressWarnings("DataFlowIssue")
    @Test
    void toNbtPrunesEmptyChaptersAndPaths() throws ReflectiveOperationException {
        PathMarkerBlockEntity marker = MarkerTestSupport.markerWithPathData();
        marker.getChapterData("frodo", "shire", true);
        PathMarkerBlockEntity.ChapterNbtData populated = marker.getChapterData("aragorn", "rohan", true);
        populated.setProximityMessage("For Rohan");

        CompoundTag written = NbtEncodeable.getCompound(marker.toNbt(new CompoundTag()), "paths");

        assertFalse(written.contains("frodo"));
        assertTrue(written.contains("aragorn"));
        assertEquals("For Rohan", NbtEncodeable.getCompound(NbtEncodeable.getCompound(written, "aragorn"), "rohan").getStringOr("proximity_message", ""));
    }
}

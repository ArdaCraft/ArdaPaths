package space.ajcool.ardapaths.core.networking.handlers.server;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import space.ajcool.ardapaths.core.data.config.shared.ChapterData;
import space.ajcool.ardapaths.core.data.config.shared.PositionData;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests chapter update normalization helpers.
 */
class ChapterUpdateHandlerTest {

    /**
     * Coordinate-shaped warp text is converted into coordinates and cleared from the warp field.
     */
    @Test
    void normalizesCoordinateShapedWarp() {
        ChapterUpdateHandler.NormalizedWarp normalized = ChapterUpdateHandler.normalizeWarp("-56 16 12", null);

        assertEquals("", normalized.warp());
        assertEquals(new BlockPos(-56, 16, 12), normalized.coordinates());
    }

    /**
     * Named warps are preserved with explicit coordinates unchanged.
     */
    @Test
    void preservesNamedWarpAndCoordinates() {
        BlockPos coordinates = new BlockPos(1, 2, 3);
        ChapterUpdateHandler.NormalizedWarp normalized = ChapterUpdateHandler.normalizeWarp("pfThreeisCompany", coordinates);

        assertEquals("pfThreeisCompany", normalized.warp());
        assertEquals(coordinates, normalized.coordinates());
    }

    /**
     * Blank coordinate input stays absent for normal warp-only submissions.
     */
    @Test
    void keepsCoordinatesUnsetWhenAbsent() {
        ChapterUpdateHandler.NormalizedWarp normalized = ChapterUpdateHandler.normalizeWarp("pfThreeisCompany", null);

        assertEquals("pfThreeisCompany", normalized.warp());
        assertNull(normalized.coordinates());
    }

    /**
     * Submitted dimensions have priority over inferred dimensions.
     */
    @Test
    void submittedDimensionWins() {
        ChapterData existing = chapterWithCoordinates("minecraft:overworld");

        assertEquals("minecraft:the_nether", ChapterUpdateHandler.resolveDimension("minecraft:the_nether", existing, "minecraft:end"));
    }

    /**
     * Existing chapter dimensions are reused only when existing coordinates are set.
     */
    @Test
    void existingCoordinateDimensionWinsWhenSubmissionBlank() {
        ChapterData existing = chapterWithCoordinates("minecraft:the_nether");

        assertEquals("minecraft:the_nether", ChapterUpdateHandler.resolveDimension("", existing, "minecraft:overworld"));
    }

    /**
     * Existing chapters without coordinates do not contribute their default dimension.
     */
    @Test
    void existingChapterWithoutCoordinatesUsesPlayerDimension() {
        ChapterData existing = new ChapterData("chapter", "Chapter", 1, "");

        assertEquals("minecraft:the_nether", ChapterUpdateHandler.resolveDimension("", existing, "minecraft:the_nether"));
    }

    /**
     * New chapters use the player dimension when no dimension is submitted.
     */
    @Test
    void nullExistingChapterUsesPlayerDimension() {
        assertEquals("minecraft:end", ChapterUpdateHandler.resolveDimension(null, null, "minecraft:end"));
    }

    /**
     * Creates chapter data with coordinate and dimension fields.
     *
     * @param dimension dimension identifier to set
     * @return chapter data
     */
    private ChapterData chapterWithCoordinates(String dimension) {
        ChapterData chapter = new ChapterData("chapter", "Chapter", 1, "");
        chapter.setCoordinates(PositionData.fromBlockPos(new BlockPos(1, 2, 3)));
        chapter.setDimension(dimension);
        return chapter;
    }
}

package space.ajcool.ardapaths.screens.widgets;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for suggestion ordering and matching helpers.
 */
class SuggestionTextMatcherTest {
    /**
     * Verifies dimension suggestions place the overworld first and remove duplicate identifiers.
     */
    @Test
    void orderDimensionsPutsOverworldFirstAndDeduplicates() {
        List<String> ordered = SuggestionTextMatcher.orderDimensions(List.of(
                "multiworld:moria_big",
                "minecraft:the_end",
                "minecraft:overworld",
                "multiworld:moria_big",
                "minecraft:the_nether"
        ));

        assertEquals(List.of(
                "minecraft:overworld",
                "minecraft:the_end",
                "minecraft:the_nether",
                "multiworld:moria_big"
        ), ordered);
    }

    /**
     * Verifies suggestion filtering uses case-insensitive containment.
     */
    @Test
    void filterContainsMatchesCaseInsensitively() {
        List<String> filtered = SuggestionTextMatcher.filterContains(List.of(
                "minecraft:overworld",
                "multiworld:Moria_Big",
                "multiworld:freebuild"
        ), "moria");

        assertEquals(List.of("multiworld:Moria_Big"), filtered);
    }

    /**
     * Verifies exact matching ignores case and surrounding whitespace.
     */
    @Test
    void containsExactIgnoreCaseTrimsInput() {
        List<String> suggestions = List.of("minecraft:overworld");

        assertTrue(SuggestionTextMatcher.containsExactIgnoreCase(suggestions, " MINECRAFT:OVERWORLD "));
        assertFalse(SuggestionTextMatcher.containsExactIgnoreCase(suggestions, "minecraft:the_end"));
    }
}

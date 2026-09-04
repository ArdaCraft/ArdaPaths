package space.ajcool.ardapaths.core.backup.dto;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import space.ajcool.ardapaths.core.data.config.shared.PositionData;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Gson shape tests for exported backup DTOs.
 */
class BackupDtoGsonShapeTest {

    /**
     * Gson instance matching default backup DTO serialization.
     */
    private static final Gson GSON = new Gson();

    /**
     * Verifies the manifest uses stable snake-case and nested field names.
     */
    @Test
    void manifestUsesPersistedFieldNames() {
        ManifestDto manifest = new ManifestDto(
                1,
                "2026-08-31T12:00:00Z",
                new BackupCountsDto(2, 3, 4, 5, 6),
                Map.of("paths/frodo.json", "abc123")
        );

        JsonObject json = JsonParser.parseString(GSON.toJson(manifest)).getAsJsonObject();

        assertEquals(1, json.get("schema_version").getAsInt());
        assertTrue(json.has("created"));
        assertEquals(2, json.getAsJsonObject("counts").get("dimensions").getAsInt());
        assertEquals("abc123", json.getAsJsonObject("files").get("paths/frodo.json").getAsString());
        assertFalse(json.has("schemaVersion"));
    }

    /**
     * Verifies exported path files retain human-readable path and diagnostic keys.
     */
    @Test
    void pathFileUsesPersistedFieldNames() {
        PathFileDto path = new PathFileDto(
                "frodo",
                "Frodo's Path",
                new PathColorDto(new int[]{255, 215, 0}, new int[]{230, 194, 0}, new int[]{255, 227, 77}),
                List.of(new PathChapterDto("shire", "The Shire", "12 Forelithe", 1, "bag-end", new PositionData(1, 2, 3), "minecraft:overworld", List.of())),
                new PathDiagnosticsDto(List.of(1L), List.of(2L), List.of(List.of(3L, 4L)), Map.of("shire", List.of(5L)))
        );

        JsonObject json = JsonParser.parseString(GSON.toJson(path)).getAsJsonObject();

        assertEquals("frodo", json.get("id").getAsString());
        assertTrue(json.has("colors"));
        JsonObject chapter = json.getAsJsonArray("chapters").get(0).getAsJsonObject();
        assertEquals(1, chapter.getAsJsonObject("coordinates").get("x").getAsInt());
        assertEquals("minecraft:overworld", chapter.get("dimension").getAsString());
        assertFalse(chapter.has("start_pos"));
        assertTrue(json.getAsJsonObject("diagnostics").has("dangling_next"));
        assertTrue(json.getAsJsonObject("diagnostics").has("multi_root"));
        assertFalse(json.getAsJsonObject("diagnostics").has("danglingNext"));
    }

    /**
     * Verifies exported marker nodes retain legacy-compatible snake-case fields.
     */
    @Test
    void pathNodeUsesPersistedFieldNames() {
        PathNodeDto node = new PathNodeDto(
                "minecraft:overworld",
                1L,
                2L,
                true,
                true,
                false,
                1,
                6000,
                12,
                "bag-end",
                "1 2 3",
                "minecraft:bread",
                "Hello",
                8,
                new NodeAnimDto(123L, new int[]{1, 2, 3})
        );

        JsonObject json = JsonParser.parseString(GSON.toJson(node)).getAsJsonObject();

        assertEquals("minecraft:overworld", json.get("dimension").getAsString());
        assertTrue(json.get("chapter_start").getAsBoolean());
        assertTrue(json.get("title_on_trail").getAsBoolean());
        assertFalse(json.get("display_above_blocks").getAsBoolean());
        assertEquals(12, json.get("time_transition_range").getAsInt());
        assertEquals("bag-end", json.get("auto_teleport_target").getAsString());
        assertEquals("minecraft:bread", json.get("give_item").getAsString());
        assertEquals(123L, json.getAsJsonObject("anim").get("packed").getAsLong());
        assertFalse(json.has("chapterStart"));
        assertFalse(json.has("autoTeleportTarget"));
    }

    /**
     * Verifies marker indexes keep dimension and packed-position maps intact.
     */
    @Test
    void markerIndexUsesPersistedFieldNames() {
        MarkerIndexDto index = new MarkerIndexDto(Map.of("minecraft:overworld", Map.of("12", new int[]{1, 2, 3})));

        JsonObject json = JsonParser.parseString(GSON.toJson(index)).getAsJsonObject();

        assertTrue(json.has("markers"));
        assertEquals(1, json.getAsJsonObject("markers").getAsJsonObject("minecraft:overworld").getAsJsonArray("12").get(0).getAsInt());
    }
}

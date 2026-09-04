package space.ajcool.ardapaths.core.data.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests raw server config migration before typed Gson loading.
 */
class ServerConfigMigrationTest {

    /**
     * Temporary directory used for isolated config files.
     */
    @TempDir
    private Path tempDir;

    /**
     * Verifies missing server config files are left alone.
     */
    @Test
    void missingFileIsNoOp() {
        Path config = tempDir.resolve("server.json");

        ServerConfigMigrator.migrate(config.toString());

        assertFalse(Files.exists(config));
        assertFalse(Files.exists(tempDir.resolve("server.json.backup")));
    }

    /**
     * Verifies legacy top-level chapter starts are moved under their chapter.
     *
     * @throws IOException when the fixture cannot be written or read
     */
    @Test
    void legacyChapterStartsMapMigratesToChapterCoordinates() throws IOException {
        Path config = writeConfig("""
                {
                  "paths": [{
                    "id": "frodo",
                    "chapters": {
                      "shire": {"id": "shire", "warp": "bag-end"}
                    }
                  }],
                  "chapter_starts": {"frodo:shire": {"x": 1, "y": 2, "z": 3}}
                }
                """);

        ServerConfigMigrator.migrate(config.toString());

        JsonObject root = read(config);
        JsonObject chapter = chapter(root);
        assertFalse(root.has("chapter_starts"));
        assertEquals(1, chapter.getAsJsonObject("coordinates").get("x").getAsInt());
        assertEquals(2, chapter.getAsJsonObject("coordinates").get("y").getAsInt());
        assertEquals(3, chapter.getAsJsonObject("coordinates").get("z").getAsInt());
        assertEquals("minecraft:overworld", chapter.get("dimension").getAsString());
        assertEquals("bag-end", chapter.get("warp").getAsString());
        assertTrue(Files.exists(tempDir.resolve("server.json.backup")));
    }

    /**
     * Writes a server config fixture.
     *
     * @param json fixture JSON
     * @return config path
     * @throws IOException when the fixture cannot be written
     */
    private Path writeConfig(String json) throws IOException {
        Path config = tempDir.resolve("server.json");
        Files.writeString(config, json);
        return config;
    }

    /**
     * Reads a config fixture as a JSON object.
     *
     * @param path config path
     * @return parsed JSON object
     * @throws IOException when the fixture cannot be read
     */
    private JsonObject read(Path path) throws IOException {
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }

    /**
     * Gets the single chapter object from a parsed config fixture.
     *
     * @param root parsed config root
     * @return chapter JSON object
     */
    private JsonObject chapter(JsonObject root) {
        return root.getAsJsonArray("paths")
                .get(0)
                .getAsJsonObject()
                .getAsJsonObject("chapters")
                .getAsJsonObject("shire");
    }

    /**
     * Verifies coordinate-shaped warp values are moved into coordinates and cleared.
     *
     * @throws IOException when the fixture cannot be written or read
     */
    @Test
    void coordinateWarpMigratesToChapterCoordinates() throws IOException {
        Path config = writeConfig("""
                {
                  "paths": [{
                    "id": "frodo",
                    "chapters": {
                      "shire": {"id": "shire", "warp": "4 5 6"}
                    }
                  }]
                }
                """);

        ServerConfigMigrator.migrate(config.toString());

        JsonObject chapter = chapter(read(config));
        assertEquals("", chapter.get("warp").getAsString());
        assertEquals(4, chapter.getAsJsonObject("coordinates").get("x").getAsInt());
        assertEquals(5, chapter.getAsJsonObject("coordinates").get("y").getAsInt());
        assertEquals(6, chapter.getAsJsonObject("coordinates").get("z").getAsInt());
        assertEquals("minecraft:overworld", chapter.get("dimension").getAsString());
    }

    /**
     * Verifies migrated configs are stable on subsequent migration runs.
     *
     * @throws IOException when the fixture cannot be written or read
     */
    @Test
    void alreadyMigratedConfigIsNoOp() throws IOException {
        Path config = writeConfig("""
                {
                  "paths": [{
                    "id": "frodo",
                    "chapters": {
                      "shire": {
                        "id": "shire",
                        "warp": "bag-end",
                        "coordinates": {"x": 1, "y": 2, "z": 3},
                        "dimension": "minecraft:overworld"
                      }
                    }
                  }]
                }
                """);
        String original = Files.readString(config);

        ServerConfigMigrator.migrate(config.toString());
        ServerConfigMigrator.migrate(config.toString());

        assertEquals(original, Files.readString(config));
        assertFalse(Files.exists(tempDir.resolve("server.json.backup")));
    }

    /**
     * Verifies a config migrated once is unchanged by a second run and keeps one backup.
     *
     * @throws IOException when the fixture cannot be written or read
     */
    @Test
    void migratedConfigDoesNotCreateAdditionalBackupOnSecondRun() throws IOException {
        Path config = writeConfig("""
                {
                  "paths": [{
                    "id": "frodo",
                    "chapters": {
                      "shire": {"id": "shire", "warp": "1 2 3"}
                    }
                  }]
                }
                """);

        ServerConfigMigrator.migrate(config.toString());
        String migrated = Files.readString(config);
        ServerConfigMigrator.migrate(config.toString());

        assertEquals(migrated, Files.readString(config));
        try (var backups = Files.list(tempDir)) {
            assertEquals(1, backups.filter(path -> path.getFileName().toString().startsWith("server.json.backup")).count());
        }
    }

    /**
     * Verifies migration does not overwrite an existing backup file.
     *
     * @throws IOException when the fixture cannot be written or read
     */
    @Test
    void existingBackupIsNotClobbered() throws IOException {
        Path config = writeConfig("""
                {
                  "paths": [{
                    "id": "frodo",
                    "chapters": {
                      "shire": {"id": "shire", "warp": "1 2 3"}
                    }
                  }]
                }
                """);
        Path backup = tempDir.resolve("server.json.backup");
        Files.writeString(backup, "keep me");

        ServerConfigMigrator.migrate(config.toString());

        assertEquals("keep me", Files.readString(backup));
        try (var backups = Files.list(tempDir)) {
            assertEquals(2, backups.filter(path -> path.getFileName().toString().startsWith("server.json.backup")).count());
        }
    }
}

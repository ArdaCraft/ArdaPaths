package space.ajcool.ardapaths.core.data.config;

import com.google.gson.*;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.core.BlockPos;
import space.ajcool.ardapaths.core.data.WarpTarget;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Map;

/**
 * Migrates legacy server configuration JSON before typed config loading can discard old fields.
 */
@Slf4j(topic = "ardapaths")
public final class ServerConfigMigrator {

    /**
     * Pretty printer used to persist migrated server JSON.
     */
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Default dimension for legacy coordinate-only chapter starts.
     */
    private static final String DEFAULT_DIMENSION = "minecraft:overworld";

    /**
     * Prevents construction of this migration utility.
     */
    private ServerConfigMigrator() {

    }

    /**
     * Migrates chapter starts from legacy top-level and coordinate-warp shapes into chapter objects.
     *
     * @param configPath path to the server config file
     */
    public static void migrate(String configPath) {
        Path file = Path.of(configPath);
        if (!Files.exists(file)) return;

        JsonObject root;
        try (Reader reader = Files.newBufferedReader(file)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (!parsed.isJsonObject()) return;
            root = parsed.getAsJsonObject();
        } catch (IOException | JsonSyntaxException | IllegalStateException exception) {
            log.error("Failed to inspect server config for migration at {}; normal config loading will handle it", file, exception);
            return;
        }

        if (!needsMigration(root)) return;

        try {
            backup(file);
            migrateRoot(root);
            write(file, GSON.toJson(root));
            log.info("Migrated ArdaPaths server config chapter starts at {}", file);
        } catch (IOException exception) {
            log.error("Failed to migrate server config at {}; normal config loading will handle it", file, exception);
        }
    }

    /**
     * Checks whether the raw JSON still contains fields or warp values that need conversion.
     *
     * @param root parsed server config root
     * @return true when migration must rewrite the file
     */
    private static boolean needsMigration(JsonObject root) {
        if (root.has("chapter_starts")) return true;
        return hasCoordinateWarp(root);
    }

    /**
     * Checks all chapter objects for legacy coordinate-shaped warp values.
     *
     * @param root parsed server config root
     * @return true when any chapter warp stores raw coordinates
     */
    private static boolean hasCoordinateWarp(JsonObject root) {
        JsonObject chapters;
        JsonElement pathsElement = root.get("paths");
        if (pathsElement == null || !pathsElement.isJsonArray()) return false;

        for (JsonElement pathElement : pathsElement.getAsJsonArray()) {
            if (!pathElement.isJsonObject()) continue;
            chapters = asObject(pathElement.getAsJsonObject().get("chapters"));
            if (chapters == null) continue;
            for (JsonElement chapterElement : chapters.asMap().values()) {
                JsonObject chapter = asObject(chapterElement);
                if (chapter != null && WarpTarget.isCoordinates(asString(chapter.get("warp")))) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Rewrites legacy chapter-start data into each chapter object.
     *
     * @param root parsed server config root to mutate
     */
    private static void migrateRoot(JsonObject root) {
        JsonObject legacyStarts = asObject(root.remove("chapter_starts"));
        JsonElement pathsElement = root.get("paths");
        if (pathsElement == null || !pathsElement.isJsonArray()) return;

        for (JsonElement pathElement : pathsElement.getAsJsonArray()) {
            JsonObject path = asObject(pathElement);
            if (path == null) continue;

            String pathId = asString(path.get("id"));
            JsonObject chapters = asObject(path.get("chapters"));
            if (pathId == null || chapters == null) continue;

            for (Map.Entry<String, JsonElement> chapterEntry : chapters.asMap().entrySet()) {
                JsonObject chapter = asObject(chapterEntry.getValue());
                if (chapter == null) continue;

                String chapterId = chapterEntry.getKey();
                migrateChapterWarp(chapter);
                if (chapter.has("coordinates")) continue;

                JsonObject legacyPosition = legacyStarts == null ? null : asObject(legacyStarts.get(pathId + ":" + chapterId));
                if (legacyPosition != null) {
                    chapter.add("coordinates", legacyPosition.deepCopy());
                    addDefaultDimension(chapter);
                }
            }
        }
    }

    /**
     * Converts one chapter's coordinate-shaped warp into coordinates plus an empty warp.
     *
     * @param chapter chapter JSON object to mutate
     */
    private static void migrateChapterWarp(JsonObject chapter) {
        String warp = asString(chapter.get("warp"));
        if (!WarpTarget.isCoordinates(warp)) return;

        BlockPos coordinates = WarpTarget.parseCoordinates(warp);
        if (coordinates == null) return;

        chapter.add("coordinates", coordinatesJson(coordinates));
        addDefaultDimension(chapter);
        chapter.addProperty("warp", "");
    }

    /**
     * Adds the default dimension to chapters that have no explicit dimension.
     *
     * @param chapter chapter JSON object to mutate
     */
    private static void addDefaultDimension(JsonObject chapter) {
        String dimension = asString(chapter.get("dimension"));
        if (dimension == null || dimension.isBlank()) {
            chapter.addProperty("dimension", DEFAULT_DIMENSION);
        }
    }

    /**
     * Creates the persisted coordinate JSON object for a block position.
     *
     * @param pos position to write
     * @return JSON object with x, y, and z members
     */
    private static JsonObject coordinatesJson(BlockPos pos) {
        JsonObject json = new JsonObject();
        json.addProperty("x", pos.getX());
        json.addProperty("y", pos.getY());
        json.addProperty("z", pos.getZ());
        return json;
    }

    /**
     * Gets an object value only when the element is present and object-shaped.
     *
     * @param element JSON element to inspect
     * @return object value, or null when absent or differently shaped
     */
    private static JsonObject asObject(JsonElement element) {
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    /**
     * Gets a string value only when the element is present and primitive.
     *
     * @param element JSON element to inspect
     * @return string value, or null when absent or not readable as text
     */
    private static String asString(JsonElement element) {
        if (element == null || !element.isJsonPrimitive()) return null;
        try {
            return element.getAsString();
        } catch (ClassCastException | IllegalStateException exception) {
            return null;
        }
    }

    /**
     * Preserves the pre-migration bytes in a non-clobbering backup file.
     *
     * @param file file to back up
     * @throws IOException when the backup cannot be written
     */
    private static void backup(Path file) throws IOException {
        Path backup = file.resolveSibling(file.getFileName() + ".backup");
        if (Files.exists(backup)) {
            backup = file.resolveSibling(file.getFileName() + ".backup-" + Instant.now().toEpochMilli());
        }
        Files.copy(file, backup);
    }

    /**
     * Writes migrated JSON via a temporary file and atomic replacement when supported.
     *
     * @param file migrated config destination
     * @param json migrated JSON text
     * @throws IOException when the migrated file cannot be written
     */
    private static void write(Path file, String json) throws IOException {
        Path parent = file.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        Path tempFile = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(tempFile, json);

        try {
            Files.move(tempFile, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}

package space.ajcool.ardapaths.core.data.config;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import space.ajcool.ardapaths.core.data.config.client.ClientConfig;
import space.ajcool.ardapaths.core.data.config.client.SelectedPathData;
import space.ajcool.ardapaths.core.data.config.server.ServerConfig;
import space.ajcool.ardapaths.core.data.config.shared.ChapterData;
import space.ajcool.ardapaths.core.data.config.shared.Color;
import space.ajcool.ardapaths.core.data.config.shared.PathData;
import space.ajcool.ardapaths.core.data.config.shared.PositionData;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Gson shape tests for client/server configuration DTOs persisted on disk.
 */
class ConfigGsonShapeTest {

    /**
     * Gson instance matching default field naming plus local SerializedName annotations.
     */
    private static final Gson GSON = new Gson();

    /**
     * Verifies client config JSON field names deserialize into their runtime getters.
     */
    @Test
    void clientConfigUsesPersistedFieldNames() {
        ClientConfig config = GSON.fromJson("""
                {
                  "selected_paths": {"server.example":{"path":"frodo","chapter":"shire"}},
                  "proximity_messages": true,
                  "chapter_titles": true,
                  "trail_waypoints": true,
                  "dynamic_environment": true,
                  "proximity_text_speed_multiplier": 1.5,
                  "auto_walk_speed_factor": 0.75,
                  "chapter_title_display_speed": 2500.0,
                  "paths": []
                }
                """, ClientConfig.class);

        assertEquals("frodo", config.getSelectedPathId("server.example"));
        assertEquals("shire", config.getCurrentChapterId("server.example"));
        assertTrue(config.showProximityMessages());
        assertTrue(config.showChapterTitles());
        assertTrue(config.showTrailWaypoints());
        assertTrue(config.useDynamicEnvironment());
        assertEquals(1.5D, config.getProximityTextSpeedMultiplier());
        assertEquals(0.75D, config.getAutoWalkSpeedFactor());
        assertEquals(2500.0F, config.getChapterTitleDisplaySpeed());
        assertTrue(config.getClientPaths().isEmpty());
    }

    /**
     * Verifies selected path entries keep their compact persisted keys.
     */
    @Test
    void selectedPathDataUsesPersistedFieldNames() {
        SelectedPathData selected = new SelectedPathData();
        selected.setPathId("frodo");
        selected.setChapterId("shire");

        JsonObject json = JsonParser.parseString(GSON.toJson(selected)).getAsJsonObject();

        assertEquals("frodo", json.get("path").getAsString());
        assertEquals("shire", json.get("chapter").getAsString());
        assertFalse(json.has("pathId"));
        assertFalse(json.has("chapterId"));
    }

    /**
     * Verifies server config preserves path and chapter-start field names.
     */
    @Test
    void serverConfigUsesPersistedFieldNames() {
        ServerConfig config = GSON.fromJson("""
                {
                  "paths": [
                    {
                      "id": "frodo",
                      "name": "Frodo's Path",
                      "primaryColor": {"red": 255, "green": 215, "blue": 0},
                      "secondaryColor": {"red": 230, "green": 194, "blue": 0},
                      "tertiaryColor": {"red": 255, "green": 227, "blue": 77},
                      "chapters": {
                        "shire": {
                          "id": "shire",
                          "name": "The Shire",
                          "date": "12 Forelithe",
                          "index": 1,
                          "warp": "bag-end",
                          "coordinates": {"x": 1, "y": 2, "z": 3},
                          "dimension": "minecraft:the_nether"
                        }
                      }
                    }
                  ]
                }
                """, ServerConfig.class);

        PathData path = config.getPath("frodo");
        assertNotNull(path);
        assertEquals("Frodo's Path", path.getName());
        assertEquals(0xFFFFD700, path.getPrimaryColor().asHex());
        ChapterData chapter = path.getChapter("shire");
        assertNotNull(chapter);
        assertEquals("The Shire", chapter.getName());
        assertEquals("12 Forelithe", chapter.getDate());
        assertEquals(1, chapter.getIndex());
        assertEquals("bag-end", chapter.getWarp());
        assertEquals(new PositionData(1, 2, 3), chapter.getCoordinates());
        assertEquals("minecraft:the_nether", chapter.getDimension());
        JsonObject json = JsonParser.parseString(GSON.toJson(config)).getAsJsonObject();
        assertFalse(json.has("chapter_starts"));
    }

    /**
     * Verifies shared colour fields keep their expanded persisted names.
     */
    @Test
    void colorUsesExpandedComponentNames() {
        JsonObject json = JsonParser.parseString(GSON.toJson(new Color(1, 2, 3))).getAsJsonObject();

        assertEquals(1, json.get("red").getAsInt());
        assertEquals(2, json.get("green").getAsInt());
        assertEquals(3, json.get("blue").getAsInt());
        assertFalse(json.has("r"));
        assertFalse(json.has("g"));
        assertFalse(json.has("b"));
    }
}

package space.ajcool.ardapaths;

import com.google.gson.Gson;
import net.minecraft.core.BlockPos;
import space.ajcool.ardapaths.core.data.config.client.ClientConfig;
import space.ajcool.ardapaths.core.data.config.server.ServerConfig;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Shared fixtures for Path Marker NBT tests that avoid Minecraft registry bootstrap.
 */
public final class MarkerTestSupport {
    /**
     * Gson instance used for inline config fixture JSON.
     */
    private static final Gson GSON = new Gson();

    /**
     * Prevents construction of the support utility.
     */
    private MarkerTestSupport() {
    }

    /**
     * Creates a marker mock that runs real marker data methods without Minecraft registry bootstrap.
     *
     * @return marker with an initialized path data map and stable block position
     * @throws ReflectiveOperationException if the private marker field cannot be initialized
     */
    public static PathMarkerBlockEntity markerWithPathData() throws ReflectiveOperationException {
        return markerWithPathData(new BlockPos(1, 2, 3));
    }

    /**
     * Creates a marker mock at a supplied position.
     *
     * @param position stable position returned by the marker mock
     * @return marker with an initialized path data map
     * @throws ReflectiveOperationException if the private marker field cannot be initialized
     */
    public static PathMarkerBlockEntity markerWithPathData(BlockPos position) throws ReflectiveOperationException {
        PathMarkerBlockEntity marker = mock(PathMarkerBlockEntity.class, CALLS_REAL_METHODS);
        setField(marker, "pathData", new HashMap<String, Map<String, PathMarkerBlockEntity.ChapterNbtData>>());
        doReturn(position).when(marker).getBlockPos();
        return marker;
    }

    /**
     * Assigns a field declared on a class in the target object's hierarchy.
     *
     * @param target object whose field should be updated
     * @param fieldName field name to find
     * @param value value to assign to the field
     * @throws ReflectiveOperationException if the field cannot be found or written
     */
    public static void setField(Object target, String fieldName, Object value) throws ReflectiveOperationException {
        Field field = findField(target.getClass(), fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    /**
     * Finds a declared field on a class or one of its superclasses.
     *
     * @param type class where the search starts
     * @param fieldName field name to find
     * @return matching field
     * @throws NoSuchFieldException if no field with the supplied name exists
     */
    public static Field findField(Class<?> type, String fieldName) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }

        throw new NoSuchFieldException(fieldName);
    }

    /**
     * Installs a minimal server config with the paths and chapters used by marker NBT tests.
     */
    public static void installServerConfig() {
        ArdaPaths.CONFIG = GSON.fromJson(configJson(), ServerConfig.class);
    }

    /**
     * Installs a minimal client config with the paths and chapters used by marker NBT tests.
     */
    public static void installClientConfig() {
        ArdaPathsClient.CONFIG = GSON.fromJson(configJson(), ClientConfig.class);
    }

    /**
     * Clears static config references used by marker NBT tests.
     */
    public static void clearConfigs() {
        ArdaPaths.CONFIG = null;
        ArdaPathsClient.CONFIG = null;
    }

    /**
     * Creates inline JSON for a two-path config fixture.
     *
     * @return JSON shared by client and server config DTOs
     */
    private static String configJson() {
        return """
                {
                  "paths": [
                    {"id":"frodo","name":"Frodo","chapters":{
                      "shire":{"id":"shire","name":"The Shire","date":"0","index":0},
                      "moria":{"id":"moria","name":"Moria","date":"1","index":1}
                    }},
                    {"id":"aragorn","name":"Aragorn","chapters":{
                      "rohan":{"id":"rohan","name":"Rohan","date":"2","index":0},
                      "gondor":{"id":"gondor","name":"Gondor","date":"3","index":1}
                    }}
                  ]
                }
                """;
    }
}

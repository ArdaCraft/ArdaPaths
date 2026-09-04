package space.ajcool.ardapaths.core.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.experimental.UtilityClass;

import java.lang.reflect.Type;

/**
 * Utility class for JSON serialization and deserialization using Gson.
 */
@UtilityClass
public class Json {

    /**
     * The Gson instance used for all JSON operations.
     */
    private static final Gson GSON = new GsonBuilder().create();

    /**
     * Convert a JSON string to an object.
     *
     * @param <T>  the type of object to deserialize to
     * @param json the JSON string to convert
     * @param type the type of the object
     * @return the deserialized object
     */
    public static <T> T fromJson(String json, Type type) {
        return GSON.fromJson(json, type);
    }

    /**
     * Convert an object to a JSON string.
     *
     * @param object the object to convert
     * @return the JSON string representation
     */
    public static String toJson(Object object) {
        return GSON.toJson(object);
    }
}

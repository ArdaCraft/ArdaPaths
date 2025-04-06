package space.ajcool.ardapaths.core.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.lang.reflect.Type;

public class Json
{
    private static final Gson GSON = new GsonBuilder().create();

    /**
     * Convert a JSON string to an object.
     *
     * @param json The JSON string to convert
     * @param type The type of the object
     */
    public static <T> T fromJson(String json, Class<T> type)
    {
        return GSON.fromJson(json, type);
    }

    /**
     * Convert a JSON string to an object.
     *
     * @param json The JSON string to convert
     * @param type The type of the object
     */
    public static <T> T fromJson(String json, Type type)
    {
        return GSON.fromJson(json, type);
    }

    /**
     * Convert an object to a JSON string.
     *
     * @param object The object to convert
     */
    public static String toJson(Object object)
    {
        return GSON.toJson(object);
    }
}

package space.ajcool.ardapaths.core.integration;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import space.ajcool.ardapaths.core.data.WeatherTypes;

import java.lang.reflect.Method;

/**
 * Client-side facade for optional weather-changing integrations.
 */
@Environment(EnvType.CLIENT)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j(topic = "ardapaths")
public final class Weathers {

    /**
     * Mod id for the optional client-side weather changer.
     */
    private static final String MOD_ID = "weatherchanger";

    /**
     * Fully qualified class name for Weather Changer's client controller.
     */
    private static final String WEATHER_CHANGER_CLASS = "me.lucaslah.weatherchanger.WeatherChanger";

    /**
     * Fully qualified class name for Weather Changer's mode enum.
     */
    private static final String WC_MODE_CLASS = "me.lucaslah.weatherchanger.config.WcMode";

    /**
     * Cached availability state for the optional weather changer mod.
     */
    private static volatile Boolean available;

    /**
     * Cached reflective accessors for Weather Changer internals.
     */
    private static volatile ReflectionAccess reflectionAccess;

    /**
     * Tracks whether a runtime reflection failure has already been logged.
     */
    private static volatile boolean invocationWarningLogged;

    /**
     * @return true when a compatible weather provider is installed on this client
     */
    public static boolean isAvailable() {
        Boolean currentAvailability = available;
        if (currentAvailability == null) {
            synchronized (Weathers.class) {
                currentAvailability = available;
                if (currentAvailability == null) {
                    currentAvailability = resolveReflectionAccess();
                    available = currentAvailability;
                }
            }
        }

        return currentAvailability;
    }

    /**
     * Sets the client-controlled weather through Weather Changer.
     *
     * @param type weather type to display on the client
     */
    public static void setClientWeather(WeatherTypes type) {
        if (!isAvailable() || type == WeatherTypes.DEFAULT) {
            return;
        }

        ReflectionAccess access = reflectionAccess;
        if (access == null) {
            return;
        }

        try {
            String modeName = modeName(type);
            setClientWeatherMode(access, modeName);
            log.debug("[ArdaPaths] Applied dynamic weather mode: {}", modeName);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            logInvocationFailure(exception);
        }
    }

    /**
     * Restores Weather Changer to server-controlled weather rendering.
     */
    public static void resetClientWeather() {
        if (!isAvailable()) {
            return;
        }

        ReflectionAccess access = reflectionAccess;
        if (access == null) {
            return;
        }

        try {
            setClientWeatherMode(access, "OFF");
        } catch (ReflectiveOperationException | RuntimeException exception) {
            logInvocationFailure(exception);
        }
    }

    /**
     * Applies a raw Weather Changer mode through the resolved reflective handle.
     *
     * @param access reflective handles for Weather Changer
     * @param modeName Weather Changer enum constant name to apply
     * @throws ReflectiveOperationException when the reflective invocation fails
     */
    private static void setClientWeatherMode(ReflectionAccess access, String modeName) throws ReflectiveOperationException {
        Enum<?> mode = enumValue(access.wcMode(), modeName);
        access.setMode().invoke(null, mode);
    }

    /**
     * Converts ArdaPaths weather values to Weather Changer mode names.
     *
     * @param type weather type to convert
     * @return enum constant name accepted by Weather Changer
     */
    private static String modeName(WeatherTypes type) {
        return switch (type) {
            case CLEAR, DEFAULT -> "CLEAR";
            case RAIN -> "RAIN";
            case THUNDER -> "THUNDER";
        };
    }

    /**
     * Resolves a Weather Changer enum value from its runtime enum class.
     *
     * @param enumClass Weather Changer enum class
     * @param name enum constant name to resolve
     * @return enum value matching the supplied name
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Enum<?> enumValue(Class<?> enumClass, String name) {
        return Enum.valueOf((Class<? extends Enum>) enumClass, name);
    }

    /**
     * Resolves the weather integration methods and enum type.
     *
     * @return true when every required reflective handle was found
     */
    private static boolean resolveReflectionAccess() {
        if (!FabricLoader.getInstance().isModLoaded(MOD_ID)) {
            return false;
        }

        try {
            Class<?> weatherChanger = Class.forName(WEATHER_CHANGER_CLASS);
            Class<?> wcMode = Class.forName(WC_MODE_CLASS).asSubclass(Enum.class);
            Method setMode = weatherChanger.getMethod("setMode", wcMode);

            reflectionAccess = new ReflectionAccess(setMode, wcMode);
            return true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            log.warn("[ArdaPaths] Weather Changer is installed, but its client API could not be resolved. Dynamic weather changes will be disabled.", exception);
            reflectionAccess = null;
            return false;
        }
    }

    /**
     * Logs a weather invocation failure without repeating the warning every tick.
     *
     * @param exception failure raised while calling Weather Changer
     */
    private static void logInvocationFailure(Exception exception) {
        if (invocationWarningLogged) {
            return;
        }

        synchronized (Weathers.class) {
            available = false;
            reflectionAccess = null;
            if (invocationWarningLogged) {
                return;
            }

            invocationWarningLogged = true;
            log.warn("[ArdaPaths] Weather Changer rejected a dynamic weather update. Dynamic weather changes will be disabled.", exception);
        }
    }

    /**
     * Reflective handles needed to drive Weather Changer without client commands.
     *
     * @param setMode method that applies a Weather Changer mode
     * @param wcMode Weather Changer enum class used by {@code setMode}
     */
    private record ReflectionAccess(Method setMode, Class<?> wcMode) {
    }
}

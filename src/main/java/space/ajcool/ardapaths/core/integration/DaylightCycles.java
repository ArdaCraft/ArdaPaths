package space.ajcool.ardapaths.core.integration;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import space.ajcool.ardapaths.core.Client;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Client-side facade for optional daylight-cycle integrations.
 */
@Environment(EnvType.CLIENT)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j(topic = "ardapaths")
public final class DaylightCycles {

    /**
     * Mod id for the optional client-side daylight changer.
     */
    private static final String MOD_ID = "daylightchangerstruggle";

    /**
     * Key used by DaylightChangerStruggle for its fixed-time cycle.
     */
    private static final String STATIC_TIME_KEY = "statictime";

    /**
     * Fully qualified class name for DaylightChangerStruggle's client entrypoint.
     */
    private static final String TCS_CLIENT_CLASS = "jugglestruggle.timechangerstruggle.client.TimeChangerStruggleClient";

    /**
     * Fully qualified class name for DaylightChangerStruggle's fixed-time cycle.
     */
    private static final String STATIC_TIME_CLASS = "jugglestruggle.timechangerstruggle.daynight.type.StaticTime";

    /**
     * Maximum time between reasserting client time ownership when the target has not changed.
     */
    private static final long CONTROL_REASSERT_NANOS = 1_000_000_000L;

    /**
     * Cached availability state for the optional daylight changer mod.
     */
    private static volatile Boolean available;

    /**
     * Cached reflective accessors for DaylightChangerStruggle internals.
     */
    private static volatile ReflectionAccess reflectionAccess;

    /**
     * Cached fixed-time cycle instance used to avoid reflective lookups on every rendered frame.
     */
    private static volatile Object cachedStaticCycle;

    /**
     * Tracks whether a runtime reflection failure has already been logged.
     */
    private static volatile boolean invocationWarningLogged;

    /**
     * User daylight-cycle settings captured before ArdaPaths takes control.
     */
    private static volatile UserTimeState capturedUserState;

    /**
     * Last absolute tick value sent to DaylightChangerStruggle.
     */
    private static volatile long lastSentTicks = Long.MIN_VALUE;

    /**
     * Day-aligned absolute tick base preserved while ArdaPaths controls the client clock.
     */
    private static volatile long baseDayTicks = Long.MIN_VALUE;

    /**
     * Last monotonic timestamp when ArdaPaths reasserted client time control.
     */
    private static volatile long lastAssertNanos;

    /**
     * Sets the client-controlled time of day through DaylightChangerStruggle.
     *
     * @param ticks daytime ticks to display on the client
     */
    public static void setClientTime(long ticks) {

        if (!isAvailable() || Client.world() == null)
            return;

        ReflectionAccess access = reflectionAccess;

        if (access == null)
            return;

        try {
            long dayTime = Math.floorMod(ticks, 24000L);
            long absoluteTicks = resolveAbsoluteTicks(dayTime);
            if (absoluteTicks == Long.MIN_VALUE)
                return;

            long now = System.nanoTime();

            if (absoluteTicks == lastSentTicks && now - lastAssertNanos < CONTROL_REASSERT_NANOS)
                return;

            Object cycle = getStaticCycle(access);

            if (access.staticTime().isInstance(cycle))
                access.timeSet().setLong(cycle, absoluteTicks);

            access.worldTime().setBoolean(null, false);
            lastSentTicks = absoluteTicks;
            lastAssertNanos = now;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            logInvocationFailure(exception);
        }
    }

    /**
     * @return true when a compatible daylight-cycle provider is installed on this client
     */
    public static boolean isAvailable() {
        Boolean currentAvailability = available;
        if (currentAvailability == null) {
            synchronized (DaylightCycles.class) {
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
     * Resolves a client-visible daytime tick into a continuous absolute world tick.
     *
     * @param dayTime normalized daytime tick to display
     * @return absolute world tick preserving day continuity, or {@link Long#MIN_VALUE} when unavailable
     */
    private static long resolveAbsoluteTicks(long dayTime) {

        long currentBase = baseDay();
        if (currentBase == Long.MIN_VALUE)
            return Long.MIN_VALUE;

        long candidate = currentBase + dayTime;

        if (lastSentTicks == Long.MIN_VALUE)
            return candidate;

        long delta = candidate - lastSentTicks;
        if (delta < -12000L) {
            baseDayTicks = currentBase + 24000L;
            candidate = baseDayTicks + dayTime;
        } else if (delta > 12000L) {
            baseDayTicks = currentBase - 24000L;
            candidate = baseDayTicks + dayTime;
        }

        return candidate;
    }

    /**
     * Returns the active fixed-time cycle, selecting one when DaylightChangerStruggle has switched away.
     *
     * @param access reflective handles for DaylightChangerStruggle
     * @return active fixed-time cycle instance
     * @throws ReflectiveOperationException when a reflective call fails
     */
    private static Object getStaticCycle(ReflectionAccess access) throws ReflectiveOperationException {
        Object cycle = cachedStaticCycle;
        if (access.staticTime().isInstance(cycle)
                && ((Boolean) access.isCycleTypeCurrentCycle().invoke(null, STATIC_TIME_KEY))) {
            return cycle;
        }

        if (!((Boolean) access.isCycleTypeCurrentCycle().invoke(null, STATIC_TIME_KEY))) {
            access.setTimeChanger().invoke(null, STATIC_TIME_KEY);
        }

        cycle = access.getTimeChanger().invoke(null);
        if (access.staticTime().isInstance(cycle)) {
            cachedStaticCycle = cycle;
        }

        return cycle;
    }

    /**
     * Logs a daylight-cycle invocation failure without repeating the warning every tick.
     *
     * @param exception failure raised while calling DaylightChangerStruggle
     */
    private static void logInvocationFailure(Exception exception) {
        if (invocationWarningLogged) {
            return;
        }

        synchronized (DaylightCycles.class) {
            available = false;
            reflectionAccess = null;
            cachedStaticCycle = null;
            capturedUserState = null;
            resetSendGate();
            if (invocationWarningLogged) {
                return;
            }

            invocationWarningLogged = true;
            log.warn("[ArdaPaths] DaylightChangerStruggle rejected a dynamic time update. Dynamic time changes will be disabled.", exception);
        }
    }

    /**
     * Resolves the daylight-cycle integration methods and fields.
     *
     * @return true when every required reflective handle was found
     */
    private static boolean resolveReflectionAccess() {
        if (!FabricLoader.getInstance().isModLoaded(MOD_ID)) {
            return false;
        }

        try {
            Class<?> tcsClient = Class.forName(TCS_CLIENT_CLASS);
            Field worldTime = tcsClient.getField("worldTime");
            Method setTimeChanger = tcsClient.getMethod("setTimeChanger", String.class);
            Method getTimeChanger = tcsClient.getMethod("getTimeChanger");
            Method getTimeChangerKey = tcsClient.getMethod("getTimeChangerKey");
            Method isCycleTypeCurrentCycle = tcsClient.getMethod("isCycleTypeCurrentCycle", String.class);
            Class<?> staticTime = Class.forName(STATIC_TIME_CLASS);
            Field timeSet = staticTime.getField("timeSet");

            reflectionAccess = new ReflectionAccess(
                    worldTime,
                    setTimeChanger,
                    getTimeChanger,
                    getTimeChangerKey,
                    isCycleTypeCurrentCycle,
                    staticTime,
                    timeSet
            );
            return true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            log.warn("[ArdaPaths] DaylightChangerStruggle is installed, but its client API could not be resolved. Dynamic time changes will be disabled.", exception);
            reflectionAccess = null;
            cachedStaticCycle = null;
            capturedUserState = null;
            resetSendGate();
            return false;
        }
    }

    /**
     * Returns the absolute day base used to preserve the current season while changing time of day.
     *
     * @return day-aligned absolute tick base for the active client world
     */
    private static long baseDay() {

        long currentBase = baseDayTicks;

        if (currentBase != Long.MIN_VALUE)
            return currentBase;

        if (Client.world() == null)
            return currentBase;

        long currentTime = Client.world().getDayTime();
        currentBase = Math.floorDiv(currentTime, 24000L) * 24000L;
        baseDayTicks = currentBase;
        return currentBase;
    }

    /**
     * Clears cached send throttling state after ownership changes or integration failures.
     */
    private static void resetSendGate() {
        lastSentTicks = Long.MIN_VALUE;
        baseDayTicks = Long.MIN_VALUE;
        lastAssertNanos = 0L;
    }

    /**
     * Captures the user's active daylight-cycle settings before ArdaPaths takes control.
     */
    public static void captureUserState() {
        if (capturedUserState != null || !isAvailable()) {
            return;
        }

        ReflectionAccess access = reflectionAccess;
        if (access == null) {
            return;
        }

        try {
            boolean worldTime = access.worldTime().getBoolean(null);
            String cycleKey = (String) access.getTimeChangerKey().invoke(null);
            Object cycle = access.getTimeChanger().invoke(null);
            Long staticTimeSet = access.staticTime().isInstance(cycle)
                    ? access.timeSet().getLong(cycle)
                    : null;
            capturedUserState = new UserTimeState(worldTime, cycleKey, staticTimeSet);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            logInvocationFailure(exception);
        }
    }

    /**
     * Enables client-side time control through DaylightChangerStruggle.
     */
    public static void enableClientTimeControl() {
        if (!isAvailable() || Client.world() == null) {
            return;
        }

        ReflectionAccess access = reflectionAccess;
        if (access == null) {
            return;
        }

        try {
            long currentTime = Client.world().getDayTime();
            baseDayTicks = Math.floorDiv(currentTime, 24000L) * 24000L;
            Object cycle = getStaticCycle(access);
            if (access.staticTime().isInstance(cycle)) {
                access.timeSet().setLong(cycle, currentTime);
            }
            access.worldTime().setBoolean(null, false);
            lastSentTicks = Long.MIN_VALUE;
            lastAssertNanos = 0L;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            logInvocationFailure(exception);
        }
    }

    /**
     * Restores the user's daylight-cycle settings captured before ArdaPaths took control.
     */
    public static void restoreUserState() {
        UserTimeState state = capturedUserState;
        if (state == null) {
            disableClientTimeControl();
            return;
        }

        if (!isAvailable()) {
            capturedUserState = null;
            resetSendGate();
            return;
        }

        ReflectionAccess access = reflectionAccess;
        if (access == null) {
            capturedUserState = null;
            resetSendGate();
            return;
        }

        try {
            String currentKey = (String) access.getTimeChangerKey().invoke(null);
            if (!Objects.equals(currentKey, state.cycleKey())) {
                access.setTimeChanger().invoke(null, state.cycleKey());
            }

            if (state.staticTimeSet() != null) {
                Object cycle = access.getTimeChanger().invoke(null);
                if (access.staticTime().isInstance(cycle)) {
                    access.timeSet().setLong(cycle, state.staticTimeSet());
                }
            }

            access.worldTime().setBoolean(null, state.worldTime());
            capturedUserState = null;
            resetSendGate();
        } catch (ReflectiveOperationException | RuntimeException exception) {
            capturedUserState = null;
            logInvocationFailure(exception);
        }
    }

    /**
     * Restores DaylightChangerStruggle to vanilla world time.
     */
    public static void disableClientTimeControl() {
        if (!isAvailable()) {
            return;
        }

        ReflectionAccess access = reflectionAccess;
        if (access == null) {
            return;
        }

        try {
            access.worldTime().setBoolean(null, true);
            resetSendGate();
        } catch (ReflectiveOperationException | RuntimeException exception) {
            logInvocationFailure(exception);
        }
    }

    /**
     * Reflective handles needed to drive DaylightChangerStruggle without client commands.
     *
     * @param worldTime               static flag controlling whether vanilla world time is used
     * @param setTimeChanger          method that selects the active DaylightChangerStruggle cycle
     * @param getTimeChanger          method that returns the active DaylightChangerStruggle cycle
     * @param getTimeChangerKey       method that returns the active DaylightChangerStruggle cycle key
     * @param isCycleTypeCurrentCycle method that checks whether a cycle key is active
     * @param staticTime              fixed-time cycle class
     * @param timeSet                 field storing the fixed client-visible time
     */
    private record ReflectionAccess(
            Field worldTime,
            Method setTimeChanger,
            Method getTimeChanger,
            Method getTimeChangerKey,
            Method isCycleTypeCurrentCycle,
            Class<?> staticTime,
            Field timeSet
    ) {

    }

    /**
     * User daylight-cycle state restored after ArdaPaths releases time control.
     *
     * @param worldTime     whether DaylightChangerStruggle was using vanilla world time
     * @param cycleKey      active DaylightChangerStruggle cycle key
     * @param staticTimeSet static cycle time, or null when the active cycle was not static
     */
    private record UserTimeState(boolean worldTime, String cycleKey, Long staticTimeSet) {

    }
}

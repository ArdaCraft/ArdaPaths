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
import java.text.SimpleDateFormat;
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
     * Fully qualified class name for DaylightChangerStruggle's date formatting utility.
     */
    private static final String DAYLIGHT_UTILS_CLASS = "jugglestruggle.timechangerstruggle.util.DaylightUtils";

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
     * Tracks whether a date-readout override failure has already been logged.
     */
    private static volatile boolean dateFormatWarningLogged;

    /**
     * User daylight-cycle settings captured before ArdaPaths takes control.
     */
    private static volatile UserTimeState capturedUserState;

    /**
     * DaylightChangerStruggle date formatter captured before ArdaPaths installs its year override.
     */
    private static volatile SimpleDateFormat capturedDateFormat;

    /**
     * Last absolute tick value sent to DaylightChangerStruggle.
     */
    private static volatile long lastSentTicks = Long.MIN_VALUE;

    /**
     * Minimum tick value known to be safe for DaylightChangerStruggle static time.
     */
    private static final long MIN_SAFE_TICKS = 0L;

    /**
     * Eight-day moon-cycle span used to shift negative dates without changing moon phase.
     */
    private static final long MOON_PHASE_TICKS = 8L * 24000L;

    /**
     * Sets the client-controlled time of day through DaylightChangerStruggle when the displayed time changes.
     *
     * @param ticks absolute ticks to display on the client
     */
    public static void setClientTime(long ticks) {

        if (!isAvailable() || Client.world() == null)
            return;

        ReflectionAccess access = reflectionAccess;

        if (access == null)
            return;

        try {
            long absoluteTicks = safeClientTicks(DaylightDates.toProviderTicks(ticks));

            if (absoluteTicks == lastSentTicks)
                return;

            Object cycle = getStaticCycle(access);

            if (access.staticTime().isInstance(cycle))
                access.timeSet().setLong(cycle, absoluteTicks);

            access.worldTime().setBoolean(null, false);
            lastSentTicks = absoluteTicks;
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
            capturedDateFormat = null;
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
            Field dateFormat = resolveDateFormatField();

            reflectionAccess = new ReflectionAccess(
                    worldTime,
                    setTimeChanger,
                    getTimeChanger,
                    getTimeChangerKey,
                    isCycleTypeCurrentCycle,
                    staticTime,
                    timeSet,
                    dateFormat
            );
            return true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            log.warn("[ArdaPaths] DaylightChangerStruggle is installed, but its client API could not be resolved. Dynamic time changes will be disabled.", exception);
            reflectionAccess = null;
            cachedStaticCycle = null;
            capturedUserState = null;
            capturedDateFormat = null;
            resetSendGate();
            return false;
        }
    }

    /**
     * Resolves DaylightChangerStruggle's optional date formatter field.
     *
     * @return date formatter field, or null when this DCS version does not expose it
     */
    private static Field resolveDateFormatField() {
        try {
            return Class.forName(DAYLIGHT_UTILS_CLASS).getField("DATE_FORMAT");
        } catch (ReflectiveOperationException | LinkageError exception) {
            log.debug("[ArdaPaths] DaylightChangerStruggle date formatter could not be resolved. Its date readout will use provider years.", exception);
            return null;
        }
    }

    /**
     * Installs a formatter that prints Arda years in DaylightChangerStruggle's date readout.
     *
     * @param access reflective handles for DaylightChangerStruggle
     */
    private static void installDateFormatOverride(ReflectionAccess access) {
        Field dateFormat = access.dateFormat();
        if (dateFormat == null) {
            return;
        }

        try {
            Object formatter = dateFormat.get(null);
            if (!(formatter instanceof SimpleDateFormat simpleDateFormat) || formatter instanceof ArdaDateFormat) {
                return;
            }

            if (capturedDateFormat == null) {
                capturedDateFormat = (SimpleDateFormat) simpleDateFormat.clone();
            }

            dateFormat.set(null, new ArdaDateFormat(simpleDateFormat));
        } catch (ReflectiveOperationException | RuntimeException exception) {
            capturedDateFormat = null;
            logDateFormatFailure(exception);
        }
    }

    /**
     * Restores DaylightChangerStruggle's formatter after ArdaPaths releases time control.
     *
     * @param access reflective handles for DaylightChangerStruggle
     */
    private static void restoreDateFormatOverride(ReflectionAccess access) {
        Field dateFormat = access.dateFormat();
        SimpleDateFormat formatter = capturedDateFormat;
        if (dateFormat == null || formatter == null) {
            return;
        }

        try {
            dateFormat.set(null, formatter);
            capturedDateFormat = null;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            capturedDateFormat = null;
            logDateFormatFailure(exception);
        }
    }

    /**
     * Converts authored ticks to a value safe for the daylight-cycle provider.
     *
     * @param ticks authored absolute ticks
     * @return non-negative absolute ticks preserving time of day and moon phase
     */
    private static long safeClientTicks(long ticks) {
        if (ticks >= MIN_SAFE_TICKS) {
            return ticks;
        }

        // Negative dayTime has not been verified against DaylightChangerStruggle; shift by whole moon cycles for now.
        long cycles = Math.floorDiv(MIN_SAFE_TICKS - ticks + MOON_PHASE_TICKS - 1L, MOON_PHASE_TICKS);
        return ticks + (cycles * MOON_PHASE_TICKS);
    }

    /**
     * Clears cached send state after ownership changes or integration failures.
     */
    private static void resetSendGate() {
        lastSentTicks = Long.MIN_VALUE;
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
     *
     * @param initialArdaTicks ArdaPaths timeline ticks used to seed the provider's fixed-time state
     */
    public static void enableClientTimeControl(long initialArdaTicks) {
        if (!isAvailable() || Client.world() == null) {
            return;
        }

        ReflectionAccess access = reflectionAccess;
        if (access == null) {
            return;
        }

        try {
            Object cycle = getStaticCycle(access);
            if (access.staticTime().isInstance(cycle)) {
                access.timeSet().setLong(cycle, safeClientTicks(DaylightDates.toProviderTicks(initialArdaTicks)));
            }
            access.worldTime().setBoolean(null, false);
            installDateFormatOverride(access);
            lastSentTicks = Long.MIN_VALUE;
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
            restoreDateFormatOverride(access);
            capturedUserState = null;
            resetSendGate();
        } catch (ReflectiveOperationException | RuntimeException exception) {
            capturedUserState = null;
            restoreDateFormatOverride(access);
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
            restoreDateFormatOverride(access);
            resetSendGate();
        } catch (ReflectiveOperationException | RuntimeException exception) {
            restoreDateFormatOverride(access);
            logInvocationFailure(exception);
        }
    }

    /**
     * Logs a date-readout override failure without disabling time control.
     *
     * @param exception failure raised while replacing DaylightChangerStruggle's formatter
     */
    private static void logDateFormatFailure(Exception exception) {
        if (dateFormatWarningLogged) {
            return;
        }

        synchronized (DaylightCycles.class) {
            if (dateFormatWarningLogged) {
                return;
            }

            dateFormatWarningLogged = true;
            log.warn("[ArdaPaths] DaylightChangerStruggle date formatting could not be overridden. Dynamic time changes will continue, but the DCS readout may show years 0004-0007.", exception);
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
     * @param dateFormat              optional field storing DaylightChangerStruggle's date formatter
     */
    private record ReflectionAccess(
            Field worldTime,
            Method setTimeChanger,
            Method getTimeChanger,
            Method getTimeChangerKey,
            Method isCycleTypeCurrentCycle,
            Class<?> staticTime,
            Field timeSet,
            Field dateFormat
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

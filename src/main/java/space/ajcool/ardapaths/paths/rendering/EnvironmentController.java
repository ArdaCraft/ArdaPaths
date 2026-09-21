package space.ajcool.ardapaths.paths.rendering;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import space.ajcool.ardapaths.ArdaPathsClient;
import space.ajcool.ardapaths.core.Client;
import space.ajcool.ardapaths.core.data.TimeActivation;
import space.ajcool.ardapaths.core.data.TimeOfDay;
import space.ajcool.ardapaths.core.data.WeatherTypes;
import space.ajcool.ardapaths.core.integration.DaylightCycles;
import space.ajcool.ardapaths.core.integration.Weathers;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Applies marker-authored client environment changes while the Pathfinder is revealing a trail.
 */
@Environment(EnvType.CLIENT)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EnvironmentController {

    /**
     * Extra distance, in blocks, the player must move beyond an environment marker before it can trigger again.
     */
    private static final double EXIT_BUFFER = 2.0D;

    /**
     * Minimum weather trigger radius for markers without a proximity activation range.
     */
    private static final double MIN_WEATHER_RANGE = 3.0D;

    /**
     * Maximum off-trail distance, in blocks, where trail-authored time can control the client.
     */
    private static final double INFLUENCE_RANGE = 32.0D;

    /**
     * Time constant for frame-rate-independent daylight smoothing.
     */
    private static final double SMOOTHING_SECONDS = 0.25D;

    /**
     * Maximum render-frame duration accepted by the smoothing filter.
     */
    private static final double MAX_FRAME_SECONDS = 0.1D;

    /**
     * Markers whose weather command has already been fired for the current visit.
     */
    private static final Map<BlockPos, WeatherActivation> weatherActivations = new HashMap<>();

    /**
     * Time-capable trail nodes collected during the current world tick.
     */
    private static final Map<BlockPos, TimeNode> timeNodes = new HashMap<>();

    /**
     * Source currently controlling the client-visible time.
     */
    private static TimeSource controllingSource;

    /**
     * Marker-entry time captured for one-way radial interpolation.
     */
    private static Long capturedRadialStartTime;

    /**
     * Highest transition progress reached for the controlling radial source, so approach progress is never given back.
     */
    private static double reachedRadialProgress;

    /**
     * Daytime tick value the frame smoother is approaching.
     */
    private static double desiredTime;

    /**
     * Daytime tick value most recently applied by the frame smoother.
     */
    private static double appliedTime;

    /**
     * Whether {@link #appliedTime} and {@link #desiredTime} contain a valid client-visible time.
     */
    private static boolean hasAppliedTime;

    /**
     * Whether ArdaPaths currently owns DaylightChangerStruggle time settings.
     */
    private static boolean controlActive;

    /**
     * Monotonic timestamp of the last rendered environment frame.
     */
    private static long lastFrameNanos;

    /**
     * Client world key associated with the currently tracked marker positions.
     */
    private static ResourceKey<Level> currentWorldKey;

    /**
     * Processes a followed marker for weather and time effects.
     *
     * @param data      chapter-specific marker data
     * @param markerPos marker block position
     * @param playerPos precise player position
     * @param pathId    currently selected path ID
     */
    public static void processMarker(PathMarkerBlockEntity.ChapterNbtData data,
                                     BlockPos markerPos,
                                     Vec3 playerPos,
                                     String pathId) {
        double squaredDistance = environmentDistanceSquared(playerPos, markerPos);
        processWeather(data, markerPos, squaredDistance, pathId);
        collectTimeNode(data, markerPos);
    }

    /**
     * Measures distance from the player to the marker block volume.
     *
     * @param playerPos precise player position
     * @param markerPos marker block position
     * @return squared distance to the marker block box
     */
    private static double environmentDistanceSquared(Vec3 playerPos, BlockPos markerPos) {
        AABB markerBox = new AABB(markerPos);
        double x = axisOverrun(playerPos.x, markerBox.minX, markerBox.maxX);
        double y = axisOverrun(playerPos.y, markerBox.minY, markerBox.maxY);
        double z = axisOverrun(playerPos.z, markerBox.minZ, markerBox.maxZ);

        return (x * x) + (y * y) + (z * z);
    }

    /**
     * Applies weather when the player enters an armed marker's activation range.
     *
     * @param data            chapter-specific marker data
     * @param markerPos       marker block position
     * @param squaredDistance squared distance from the player to the marker
     * @param pathId          currently selected path ID
     */
    private static void processWeather(PathMarkerBlockEntity.ChapterNbtData data,
                                       BlockPos markerPos,
                                       double squaredDistance,
                                       String pathId) {
        double activationRange = Math.max(data.getActivationRange(), MIN_WEATHER_RANGE);
        if (!ArdaPathsClient.CONFIG.useDynamicEnvironment()
                || !Weathers.isAvailable()
                || data.getWeather() == PathMarkerBlockEntity.ChapterNbtData.UNSET
                || squaredDistance > Mth.square(activationRange)) {
            return;
        }

        WeatherActivation existing = weatherActivations.get(markerPos);
        if (existing != null
                && existing.pathId().equals(pathId)
                && existing.chapterId().equals(data.getChapterId())) {
            return;
        }

        WeatherTypes weather = WeatherTypes.fromInt(data.getWeather());
        if (weather == WeatherTypes.DEFAULT) {
            return;
        }

        Weathers.setClientWeather(weather);
        weatherActivations.put(markerPos.immutable(), new WeatherActivation(
                pathId,
                data.getChapterId(),
                Mth.square(activationRange + EXIT_BUFFER)));
    }

    /**
     * Records marker time data for the current client tick.
     *
     * @param data      chapter-specific marker data
     * @param markerPos marker block position
     */
    private static void collectTimeNode(PathMarkerBlockEntity.ChapterNbtData data, BlockPos markerPos) {
        BlockPos nextPos = data.getTarget() == null
                ? null
                : markerPos.offset(data.getTarget()).immutable();
        timeNodes.put(markerPos.immutable(), new TimeNode(
                data.getTimeOfDay(),
                nextPos,
                data.getActivationRange(),
                data.getTimeActivation()));
    }

    /**
     * Computes how far a coordinate is outside an inclusive range on one axis.
     *
     * @param value coordinate to test
     * @param min   lower range bound
     * @param max   upper range bound
     * @return zero when inside the range, otherwise the distance past the nearest bound
     */
    private static double axisOverrun(double value, double min, double max) {
        if (value < min) {
            return min - value;
        }
        if (value > max) {
            return value - max;
        }

        return 0.0D;
    }

    /**
     * Advances active environment state for the current revealer tick.
     *
     * @param playerPos precise player position
     */
    public static void tick(Vec3 playerPos) {
        ClientLevel world = Client.world();
        if (world != null) {
            ResourceKey<Level> worldKey = world.dimension();
            if (currentWorldKey != null && !currentWorldKey.equals(worldKey)) {
                reset();
            }
            currentWorldKey = worldKey;
        }

        pruneWeatherActivations(playerPos);

        try {
            if (!ArdaPathsClient.CONFIG.useDynamicEnvironment() || !DaylightCycles.isAvailable()) {
                releaseControl();
                return;
            }

            selectTimeSource(playerPos);
        } finally {
            timeNodes.clear();
        }
    }

    /**
     * Applies the dynamic time target once per rendered frame.
     *
     * @param tickDelta fractional progress through the current client tick
     */
    public static void renderFrame(float tickDelta) {
        if (!ArdaPathsClient.CONFIG.useDynamicEnvironment() || !DaylightCycles.isAvailable() || !controlActive || !hasAppliedTime) {
            lastFrameNanos = 0L;
            return;
        }

        if (controllingSource == null && Double.compare(appliedTime, desiredTime) == 0) {
            return;
        }

        LocalPlayer player = Client.player();
        if (player != null && controllingSource != null) {
            desiredTime = desiredTimeFor(controllingSource, player.getPosition(tickDelta));
        }

        double frameSeconds = frameDeltaSeconds(System.nanoTime());
        rebaseAppliedTimeToDesiredDay();
        double delta = desiredTime - appliedTime;

        appliedTime = Math.abs(delta) < 1.0D
                ? desiredTime
                : appliedTime + (delta * (1.0D - Math.exp(-frameSeconds / SMOOTHING_SECONDS)));
        DaylightCycles.setClientTime(Math.round(appliedTime));
    }

    /**
     * Releases client time control and restores the user's daylight-cycle settings.
     */
    public static void releaseControl() {
        if (!controlActive) {
            clearTimeControlState();
            return;
        }

        DaylightCycles.restoreUserState();
        clearTimeControlState();
    }

    /**
     * Clears active environment state, usually because the client left a world.
     */
    public static void reset() {
        weatherActivations.clear();
        timeNodes.clear();
        releaseControl();
        currentWorldKey = null;
        Weathers.resetClientWeather();
    }

    /**
     * Selects and applies the best time source from the nodes collected this tick.
     *
     * @param playerPos precise player position
     */
    private static void selectTimeSource(Vec3 playerPos) {
        if (timeNodes.isEmpty()) {
            releaseControl();
            return;
        }

        double trailDistanceSquared = influenceDistanceSquared(playerPos);
        if (trailDistanceSquared > Mth.square(INFLUENCE_RANGE)) {
            releaseControl();
            return;
        }

        TimeCandidate radial = nearestRadialCandidate(playerPos);
        TimeCandidate segment = nearestSegmentCandidate(playerPos, trailDistanceSquared);
        TimeCandidate arrival = nearestArrivalCandidate(playerPos);
        TimeSource selected = radial == null
                ? segment == null ? arrival == null ? null : arrival.source() : segment.source()
                : radial.source();

        if (selected == null) {
            controllingSource = null;
            capturedRadialStartTime = null;
            reachedRadialProgress = 0.0D;
            return;
        }

        boolean changedSource = !Objects.equals(controllingSource, selected);
        acquireControl();
        if (changedSource) {
            controllingSource = selected;
            capturedRadialStartTime = null;
            reachedRadialProgress = 0.0D;
        }

        desiredTime = desiredTimeFor(selected, playerPos);
    }

    /**
     * Removes weather activations whose marker has been exited.
     *
     * @param playerPos precise player position
     */
    private static void pruneWeatherActivations(Vec3 playerPos) {
        weatherActivations.entrySet().removeIf(entry -> environmentDistanceSquared(playerPos, entry.getKey()) > entry.getValue().exitDistanceSquared());
    }

    /**
     * Finds the closest fixed-range radial time source currently containing the player.
     *
     * @param playerPos precise player position
     * @return nearest radial candidate, or null when none contains the player
     */
    private static TimeCandidate nearestRadialCandidate(Vec3 playerPos) {
        TimeCandidate nearest = null;

        for (Map.Entry<BlockPos, TimeNode> entry : timeNodes.entrySet()) {
            TimeNode node = entry.getValue();
            if (node.timeOfDay() == TimeOfDay.UNSET || node.timeActivation() != TimeActivation.MARKER_RANGE) {
                continue;
            }

            double distanceSquared = environmentDistanceSquared(playerPos, entry.getKey());
            double outerRadius = TimeSourceRules.arrivalRange(node.activationRange());
            if (distanceSquared <= Mth.square(outerRadius)
                    && (nearest == null || distanceSquared < nearest.distanceSquared())) {
                nearest = new TimeCandidate(new TimeSource(TimeSourceType.RADIAL, entry.getKey(), null, node, null), distanceSquared);
            }
        }

        return nearest;
    }

    /**
     * Finds the interpolating source for the trail segment the player is currently walking.
     *
     * @param playerPos            precise player position
     * @param trailDistanceSquared squared distance from the player to the nearest collected trail element
     * @return nearest segment candidate between two timed markers whose end is computed, or null when none is available
     */
    private static TimeCandidate nearestSegmentCandidate(Vec3 playerPos, double trailDistanceSquared) {
        TimeCandidate nearest = null;

        for (Map.Entry<BlockPos, TimeNode> entry : timeNodes.entrySet()) {
            TimeNode startNode = entry.getValue();
            if (startNode.nextPos() == null) {
                continue;
            }

            TimeNode endNode = timeNodes.get(startNode.nextPos());
            if (endNode == null) {
                continue;
            }

            TimeSourceRules.SegmentProjection projection = TimeSourceRules.projectOntoSegment(playerPos, entry.getKey(), startNode.nextPos());
            if (!TimeSourceRules.isComputedSegmentActive(
                    startNode.timeOfDay(),
                    endNode.timeOfDay(),
                    endNode.timeActivation(),
                    projection.overshoot(),
                    TimeSourceRules.arrivalRange(startNode.activationRange()),
                    TimeSourceRules.arrivalRange(endNode.activationRange()))) {
                continue;
            }
            if (!TimeSourceRules.isNearestTrailElement(projection.distanceSquared(), trailDistanceSquared)) {
                continue;
            }

            if (nearest == null || projection.distanceSquared() < nearest.distanceSquared()) {
                nearest = new TimeCandidate(
                        new TimeSource(TimeSourceType.COMPUTED, entry.getKey(), startNode.nextPos(), startNode, endNode),
                        projection.distanceSquared());
            }
        }

        return nearest;
    }

    /**
     * Finds the flat source for a timed computed marker whose arrival radius contains the player.
     *
     * @param playerPos precise player position
     * @return nearest arrival candidate covering chain edges where no segment can interpolate, or null when none contains the player
     */
    private static TimeCandidate nearestArrivalCandidate(Vec3 playerPos) {
        TimeCandidate nearest = null;

        for (Map.Entry<BlockPos, TimeNode> entry : timeNodes.entrySet()) {
            TimeNode node = entry.getValue();
            if (node.timeOfDay() == TimeOfDay.UNSET || node.timeActivation() != TimeActivation.COMPUTED) {
                continue;
            }

            double distanceSquared = environmentDistanceSquared(playerPos, entry.getKey());
            double arrivalRange = TimeSourceRules.arrivalRange(node.activationRange());
            if (distanceSquared <= Mth.square(arrivalRange)
                    && (nearest == null || distanceSquared < nearest.distanceSquared())) {
                nearest = new TimeCandidate(new TimeSource(TimeSourceType.ARRIVAL, entry.getKey(), null, node, null), distanceSquared);
            }
        }

        return nearest;
    }

    /**
     * Computes the smallest distance from the player to any collected trail segment or standalone node.
     *
     * @param playerPos precise player position
     * @return squared distance to the trail's area of influence
     */
    private static double influenceDistanceSquared(Vec3 playerPos) {
        double nearest = Double.MAX_VALUE;

        for (Map.Entry<BlockPos, TimeNode> entry : timeNodes.entrySet()) {
            TimeNode node = entry.getValue();
            double distanceSquared = node.nextPos() == null
                    ? environmentDistanceSquared(playerPos, entry.getKey())
                    : TimeSourceRules.projectOntoSegment(playerPos, entry.getKey(), node.nextPos()).distanceSquared();
            nearest = Math.min(nearest, distanceSquared);
        }

        return nearest;
    }

    /**
     * Computes the daylight target for the selected source and player position.
     *
     * @param source    selected control source
     * @param playerPos precise player position
     * @return desired absolute ticks for the source
     */
    private static long desiredTimeFor(TimeSource source, Vec3 playerPos) {
        return switch (source.type()) {
            case RADIAL -> radialDesiredTime(source, playerPos);
            case COMPUTED -> computedDesiredTime(source, playerPos);
            case ARRIVAL -> arrivalDesiredTime(source);
        };
    }

    /**
     * Computes a marker-arrival source's direct target time.
     *
     * @param source marker source
     * @return marker-authored absolute ticks, or the current applied time if unset
     */
    private static long arrivalDesiredTime(TimeSource source) {
        TimeNode node = source.startNode();
        if (node == null || node.timeOfDay() == TimeOfDay.UNSET) {
            return Math.round(appliedTime);
        }

        return node.timeOfDay();
    }

    /**
     * Computes a radial marker's target time from the closest player approach reached during this visit.
     *
     * @param source    radial source
     * @param playerPos precise player position
     * @return desired absolute ticks for the radial source
     */
    private static long radialDesiredTime(TimeSource source, Vec3 playerPos) {
        TimeNode node = source.startNode();
        if (node == null || node.timeOfDay() == TimeOfDay.UNSET) {
            return Math.round(appliedTime);
        }

        ensureAppliedTime();
        long targetTime = node.timeOfDay();
        if (capturedRadialStartTime == null) {
            capturedRadialStartTime = Math.round(appliedTime);
        }

        double outerRadius = TimeSourceRules.arrivalRange(node.activationRange());
        double distance = Math.sqrt(environmentDistanceSquared(playerPos, source.markerPos()));
        double progress = Mth.clamp((outerRadius - distance) / outerRadius, 0.0D, 1.0D);
        reachedRadialProgress = Math.max(reachedRadialProgress, progress);
        return interpolatedTime(capturedRadialStartTime, targetTime, reachedRadialProgress);
    }

    /**
     * Computes a computed segment's target time from projected player progress.
     *
     * @param source    computed source
     * @param playerPos precise player position
     * @return desired absolute ticks for the computed source
     */
    private static long computedDesiredTime(TimeSource source, Vec3 playerPos) {
        TimeNode startNode = source.startNode();
        TimeNode endNode = source.endNode();
        if (startNode == null || endNode == null || startNode.timeOfDay() == TimeOfDay.UNSET || endNode.timeOfDay() == TimeOfDay.UNSET) {
            return Math.round(appliedTime);
        }

        ensureAppliedTime();
        double progress = TimeSourceRules.projectOntoSegment(playerPos, source.markerPos(), source.nextPos()).progress();
        return TimeSourceRules.segmentTime(startNode.timeOfDay(), endNode.timeOfDay(), progress);
    }

    /**
     * Captures current user time settings and starts client time ownership.
     */
    private static void acquireControl() {
        if (controlActive) {
            ensureAppliedTime();
            return;
        }

        seedAppliedTime();
        DaylightCycles.captureUserState();
        DaylightCycles.enableClientTimeControl(Math.round(appliedTime));
        controlActive = true;
    }

    /**
     * Clears ArdaPaths time-control state without changing external daylight settings.
     */
    private static void clearTimeControlState() {
        controllingSource = null;
        capturedRadialStartTime = null;
        reachedRadialProgress = 0.0D;
        desiredTime = 0.0D;
        appliedTime = 0.0D;
        hasAppliedTime = false;
        controlActive = false;
        lastFrameNanos = 0L;
    }

    /**
     * Seeds the applied time from the current world time if no marker has controlled it yet.
     */
    private static void ensureAppliedTime() {
        if (!hasAppliedTime) {
            seedAppliedTime();
        }
    }

    /**
     * Reads the current client world time into the frame smoother state.
     */
    private static void seedAppliedTime() {
        ClientLevel world = Client.world();
        appliedTime = world == null
                ? 0.0D
                : TimeOfDay.fromDayTime(ArdaPathsClient.CONFIG.getBaselineDate(), world.getOverworldClockTime());
        desiredTime = appliedTime;
        hasAppliedTime = true;
    }

    /**
     * Interpolates along the signed absolute timeline.
     *
     * @param startTicks  starting absolute ticks
     * @param targetTicks target absolute ticks
     * @param progress    interpolation progress in the range {@code [0, 1]}
     * @return interpolated absolute ticks
     */
    private static long interpolatedTime(long startTicks, long targetTicks, double progress) {
        return startTicks + Math.round((targetTicks - startTicks) * Mth.clamp(progress, 0.0D, 1.0D));
    }

    /**
     * Snaps the applied value onto the desired day before smoothing within that day.
     */
    private static void rebaseAppliedTimeToDesiredDay() {
        double delta = desiredTime - appliedTime;
        if (Math.abs(delta) > TimeOfDay.DAY_TICKS / 2.0D) {
            appliedTime += TimeOfDay.DAY_TICKS * Math.round(delta / TimeOfDay.DAY_TICKS);
        }
    }

    /**
     * Calculates elapsed render-frame time used for frame-rate-independent time easing.
     *
     * @param frameNanos the current monotonic frame timestamp
     * @return the clamped elapsed time in seconds
     */
    private static double frameDeltaSeconds(long frameNanos) {
        if (lastFrameNanos == 0L) {
            lastFrameNanos = frameNanos;
            return 1.0D / 60.0D;
        }

        long elapsedNanos = Math.max(0L, frameNanos - lastFrameNanos);
        lastFrameNanos = frameNanos;
        return Math.min(MAX_FRAME_SECONDS, elapsedNanos / 1_000_000_000.0D);
    }

    /**
     * Time source modes supported by marker-authored time.
     */
    private enum TimeSourceType {
        /**
         * Marker-centered interpolation over the marker activation range.
         */
        RADIAL,

        /**
         * Segment interpolation computed from the player's projected position along the trail.
         */
        COMPUTED,

        /**
         * Direct application while inside a computed marker's arrival band.
         */
        ARRIVAL
    }

    /**
     * Visit state for a marker whose weather command has already been triggered.
     *
     * @param pathId              path the activation was triggered for
     * @param chapterId           chapter the activation was triggered for
     * @param exitDistanceSquared squared distance at which the marker can trigger again
     */
    private record WeatherActivation(String pathId, String chapterId, double exitDistanceSquared) {

    }

    /**
     * Time data contributed by one trail marker during a client tick.
     *
     * @param timeOfDay       marker-authored absolute ticks, or {@link TimeOfDay#UNSET}
     * @param nextPos         absolute position of the next marker in the trail, or null when none is configured
     * @param activationRange marker activation radius for radial time control
     * @param timeActivation  activation mode used for marker-authored time
     */
    private record TimeNode(long timeOfDay, BlockPos nextPos, int activationRange, TimeActivation timeActivation) {

    }

    /**
     * Selected trail source for client-visible time control.
     *
     * @param type      interpolation mode selected by the source
     * @param markerPos radial marker position or computed segment start position
     * @param nextPos   computed segment end position, or null for radial sources
     * @param startNode marker data for the radial marker or computed segment start
     * @param endNode   marker data for the computed segment end, or null for radial sources
     */
    private record TimeSource(TimeSourceType type, BlockPos markerPos, BlockPos nextPos, TimeNode startNode,
                              TimeNode endNode) {

    }

    /**
     * Candidate time source and its selection distance.
     *
     * @param source          candidate source
     * @param distanceSquared squared distance used for nearest-source selection
     */
    private record TimeCandidate(TimeSource source, double distanceSquared) {

    }
}

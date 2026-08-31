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
     * Number of ticks in Minecraft's repeating day cycle.
     */
    private static final int DAY_TICKS = 24000;

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
    private static Integer capturedRadialStartTime;

    /**
     * Highest transition progress reached for the controlling radial source, so approach progress is never given back.
     */
    private static double reachedRadialProgress;

    /**
     * Segment start time captured for computed segments whose start marker has no authored time.
     */
    private static Integer capturedSegmentStartTime;

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
     * Visit state for a marker whose weather command has already been triggered.
     *
     * @param pathId              path the activation was triggered for
     * @param chapterId           chapter the activation was triggered for
     * @param exitDistanceSquared squared distance at which the marker can trigger again
     */
    private record WeatherActivation(String pathId, String chapterId, double exitDistanceSquared) {}

    /**
     * Time data contributed by one trail marker during a client tick.
     *
     * @param timeOfDay marker-authored daytime ticks, or {@link TimeOfDay#UNSET}
     * @param nextPos absolute position of the next marker in the trail, or null when none is configured
     * @param activationRange marker activation radius for radial time control
     * @param transitionRange numeric radial transition range or computed-mode sentinel
     */
    private record TimeNode(int timeOfDay, BlockPos nextPos, int activationRange, int transitionRange) {}

    /**
     * Selected trail source for client-visible time control.
     *
     * @param type interpolation mode selected by the source
     * @param markerPos radial marker position or computed segment start position
     * @param nextPos computed segment end position, or null for radial sources
     * @param startNode marker data for the radial marker or computed segment start
     * @param endNode marker data for the computed segment end, or null for radial sources
     */
    private record TimeSource(TimeSourceType type, BlockPos markerPos, BlockPos nextPos, TimeNode startNode, TimeNode endNode) {}

    /**
     * Time interpolation modes supported by marker transition ranges.
     */
    private enum TimeSourceType {
        /**
         * Marker-centered interpolation over a fixed numeric transition range.
         */
        RADIAL,

        /**
         * Segment interpolation computed from the player's projected position along the trail.
         */
        COMPUTED
    }

    /**
     * Projected player position on a marker segment.
     *
     * @param progress clamped progress from start to end
     * @param distanceSquared squared distance from the player to the projected point
     */
    private record SegmentProjection(double progress, double distanceSquared) {}

    /**
     * Candidate time source and its selection distance.
     *
     * @param source candidate source
     * @param distanceSquared squared distance used for nearest-source selection
     */
    private record TimeCandidate(TimeSource source, double distanceSquared) {}

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

        LocalPlayer player = Client.player();
        if (player != null && controllingSource != null) {
            desiredTime = desiredTimeFor(controllingSource, player.getPosition(tickDelta));
        }

        double frameSeconds = frameDeltaSeconds(System.nanoTime());
        double arc = shortestArc(appliedTime, desiredTime);
        double nextTime = Math.abs(arc) < 1.0D
                ? desiredTime
                : appliedTime + (arc * (1.0D - Math.exp(-frameSeconds / SMOOTHING_SECONDS)));

        appliedTime = normalizeTime(nextTime);
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
        if (timeNodes.isEmpty() || influenceDistanceSquared(playerPos) > Mth.square(INFLUENCE_RANGE)) {
            releaseControl();
            return;
        }

        TimeCandidate radial = nearestRadialCandidate(playerPos);
        TimeCandidate computed = nearestComputedCandidate(playerPos);
        TimeSource selected = radial == null
                ? computed == null ? null : computed.source()
                : radial.source();

        if (selected == null) {
            controllingSource = null;
            capturedRadialStartTime = null;
            reachedRadialProgress = 0.0D;
            capturedSegmentStartTime = null;
            return;
        }

        boolean changedSource = !Objects.equals(controllingSource, selected);
        acquireControl();
        if (changedSource) {
            controllingSource = selected;
            capturedRadialStartTime = null;
            reachedRadialProgress = 0.0D;
            capturedSegmentStartTime = null;
        }

        desiredTime = desiredTimeFor(selected, playerPos);
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
     * @param data chapter-specific marker data
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
                data.getTimeTransitionRange()));
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
            if (node.timeOfDay() == TimeOfDay.UNSET || TimeOfDay.isComputed(node.transitionRange())) {
                continue;
            }

            double distanceSquared = environmentDistanceSquared(playerPos, entry.getKey());
            double outerRadius = node.activationRange() + Math.max(TimeOfDay.DEFAULT_TRANSITION_RANGE, node.transitionRange());
            if (distanceSquared <= Mth.square(outerRadius)
                    && (nearest == null || distanceSquared < nearest.distanceSquared())) {
                nearest = new TimeCandidate(new TimeSource(TimeSourceType.RADIAL, entry.getKey(), null, node, null), distanceSquared);
            }
        }

        return nearest;
    }

    /**
     * Finds the closest computed segment whose destination marker requests computed interpolation.
     *
     * @param playerPos precise player position
     * @return nearest computed candidate, or null when none is available
     */
    private static TimeCandidate nearestComputedCandidate(Vec3 playerPos) {
        TimeCandidate nearest = null;

        for (Map.Entry<BlockPos, TimeNode> entry : timeNodes.entrySet()) {
            TimeNode startNode = entry.getValue();
            if (startNode.nextPos() == null) {
                continue;
            }

            TimeNode endNode = timeNodes.get(startNode.nextPos());
            if (endNode == null || endNode.timeOfDay() == TimeOfDay.UNSET || !TimeOfDay.isComputed(endNode.transitionRange())) {
                continue;
            }

            SegmentProjection projection = projectOntoSegment(playerPos, entry.getKey(), startNode.nextPos());
            if (nearest == null || projection.distanceSquared() < nearest.distanceSquared()) {
                nearest = new TimeCandidate(
                        new TimeSource(TimeSourceType.COMPUTED, entry.getKey(), startNode.nextPos(), startNode, endNode),
                        projection.distanceSquared());
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
                    : projectOntoSegment(playerPos, entry.getKey(), node.nextPos()).distanceSquared();
            nearest = Math.min(nearest, distanceSquared);
        }

        return nearest;
    }

    /**
     * Computes the daylight target for the selected source and player position.
     *
     * @param source selected control source
     * @param playerPos precise player position
     * @return desired daytime ticks for the source
     */
    private static int desiredTimeFor(TimeSource source, Vec3 playerPos) {
        return switch (source.type()) {
            case RADIAL -> radialDesiredTime(source, playerPos);
            case COMPUTED -> computedDesiredTime(source, playerPos);
        };
    }

    /**
     * Computes a radial marker's target time from the closest player approach reached during this visit.
     *
     * @param source radial source
     * @param playerPos precise player position
     * @return desired daytime ticks for the radial source
     */
    private static int radialDesiredTime(TimeSource source, Vec3 playerPos) {
        TimeNode node = source.startNode();
        if (node == null || node.timeOfDay() == TimeOfDay.UNSET) {
            return (int) Math.round(appliedTime);
        }

        ensureAppliedTime();
        int targetTime = Math.floorMod(node.timeOfDay(), DAY_TICKS);
        int transitionRange = Math.max(TimeOfDay.DEFAULT_TRANSITION_RANGE, node.transitionRange());
        if (transitionRange == 0) {
            return targetTime;
        }

        if (capturedRadialStartTime == null) {
            capturedRadialStartTime = (int) Math.round(appliedTime);
        }

        double outerRadius = node.activationRange() + transitionRange;
        double distance = Math.sqrt(environmentDistanceSquared(playerPos, source.markerPos()));
        double progress = Mth.clamp((outerRadius - distance) / transitionRange, 0.0D, 1.0D);
        reachedRadialProgress = Math.max(reachedRadialProgress, progress);
        return interpolatedTime(capturedRadialStartTime, targetTime, reachedRadialProgress);
    }

    /**
     * Computes a computed segment's target time from projected player progress.
     *
     * @param source computed source
     * @param playerPos precise player position
     * @return desired daytime ticks for the computed source
     */
    private static int computedDesiredTime(TimeSource source, Vec3 playerPos) {
        TimeNode startNode = source.startNode();
        TimeNode endNode = source.endNode();
        if (startNode == null || endNode == null || endNode.timeOfDay() == TimeOfDay.UNSET) {
            return (int) Math.round(appliedTime);
        }

        ensureAppliedTime();
        int targetTime = Math.floorMod(endNode.timeOfDay(), DAY_TICKS);
        double progress = projectOntoSegment(playerPos, source.markerPos(), source.nextPos()).progress();
        if (startNode.timeOfDay() == TimeOfDay.UNSET) {
            if (capturedSegmentStartTime == null) {
                capturedSegmentStartTime = (int) Math.round(appliedTime);
            }

            return interpolatedTime(capturedSegmentStartTime, targetTime, progress);
        }

        int startTime = Math.floorMod(startNode.timeOfDay(), DAY_TICKS);
        int delta = Math.floorMod(targetTime - startTime, DAY_TICKS);
        return Math.floorMod(startTime + (int) Math.round(delta * progress), DAY_TICKS);
    }

    /**
     * Captures current user time settings and starts client time ownership.
     */
    private static void acquireControl() {
        if (controlActive) {
            ensureAppliedTime();
            return;
        }

        DaylightCycles.captureUserState();
        DaylightCycles.enableClientTimeControl();
        seedAppliedTime();
        controlActive = true;
    }

    /**
     * Clears ArdaPaths time-control state without changing external daylight settings.
     */
    private static void clearTimeControlState() {
        controllingSource = null;
        capturedRadialStartTime = null;
        reachedRadialProgress = 0.0D;
        capturedSegmentStartTime = null;
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
                : Math.floorMod(world.getDayTime(), DAY_TICKS);
        desiredTime = appliedTime;
        hasAppliedTime = true;
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
     * Computes how far a coordinate is outside an inclusive range on one axis.
     *
     * @param value coordinate to test
     * @param min lower range bound
     * @param max upper range bound
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
     * Projects a player position onto a marker-to-marker segment.
     *
     * @param playerPos precise player position
     * @param startPos segment start marker position
     * @param endPos segment end marker position
     * @return clamped projection progress and squared distance to the segment
     */
    private static SegmentProjection projectOntoSegment(Vec3 playerPos, BlockPos startPos, BlockPos endPos) {
        Vec3 start = Vec3.atCenterOf(startPos);
        Vec3 end = Vec3.atCenterOf(endPos);
        Vec3 segment = end.subtract(start);
        double lengthSquared = segment.lengthSqr();
        if (lengthSquared <= 0.0D) {
            double distanceSquared = playerPos.distanceToSqr(start);
            return new SegmentProjection(0.0D, distanceSquared);
        }

        double progress = Mth.clamp(playerPos.subtract(start).dot(segment) / lengthSquared, 0.0D, 1.0D);
        Vec3 projected = start.add(segment.scale(progress));
        return new SegmentProjection(progress, playerPos.distanceToSqr(projected));
    }

    /**
     * Computes shortest-path interpolation across Minecraft's wrapping day cycle.
     *
     * @param startTicks starting daytime ticks
     * @param targetTicks target daytime ticks
     * @param progress interpolation progress in the range {@code [0, 1]}
     * @return interpolated daytime ticks
     */
    private static int interpolatedTime(int startTicks, int targetTicks, double progress) {
        int delta = shortestArc(startTicks, targetTicks);

        return Math.floorMod(startTicks + (int) Math.round(delta * progress), DAY_TICKS);
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
     * Computes the signed shortest arc between two integer daytime values.
     *
     * @param startTicks starting daytime ticks
     * @param targetTicks target daytime ticks
     * @return signed shortest arc across the wrapping day cycle
     */
    private static int shortestArc(int startTicks, int targetTicks) {
        return Math.floorMod(targetTicks - startTicks + (DAY_TICKS + (DAY_TICKS / 2)), DAY_TICKS) - (DAY_TICKS / 2);
    }

    /**
     * Computes the signed shortest arc between two floating-point daytime values.
     *
     * @param startTicks starting daytime ticks
     * @param targetTicks target daytime ticks
     * @return signed shortest arc across the wrapping day cycle
     */
    private static double shortestArc(double startTicks, double targetTicks) {
        double delta = targetTicks - startTicks;
        delta = ((delta + (DAY_TICKS * 1.5D)) % DAY_TICKS) - (DAY_TICKS / 2.0D);

        return delta;
    }

    /**
     * Normalizes a floating-point daytime value into Minecraft's repeating day range.
     *
     * @param ticks daytime ticks to normalize
     * @return normalized daytime ticks
     */
    private static double normalizeTime(double ticks) {
        double normalized = ticks % DAY_TICKS;
        if (normalized < 0.0D) {
            normalized += DAY_TICKS;
        }

        return normalized;
    }
}

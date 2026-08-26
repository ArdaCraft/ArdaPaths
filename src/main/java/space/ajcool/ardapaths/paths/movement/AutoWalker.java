package space.ajcool.ardapaths.paths.movement;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import org.jetbrains.annotations.Nullable;
import space.ajcool.ardapaths.ArdaPathsClient;
import space.ajcool.ardapaths.core.Client;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;
import space.ajcool.ardapaths.mc.items.ModItems;
import space.ajcool.ardapaths.paths.Paths;
import space.ajcool.ardapaths.paths.rendering.objects.AnimatedTrail;

import java.util.*;

/**
 * Client-side automatic walking controller that advances the player along the selected trail.
 */
@Slf4j(topic = "ardapaths")
public class AutoWalker {

    /** Default speed factor corresponding to vanilla walking speed. */
    public static final double DEFAULT_AUTO_WALK_SPEED_FACTOR = 1.0D;

    /** Maximum distance from a trail at which auto-walk can be activated. */
    private static final double ACTIVATION_DISTANCE = 10.0D;

    /** Maximum distance from the active trail before auto-walk cancels. */
    private static final double OFF_TRAIL_DISTANCE = 25.5D;

    /** Radius around the last node that counts as reaching the trail end. */
    private static final double ARRIVAL_DISTANCE = 0.45D;

    /** Small distance used to detect exhausted segments. */
    private static final double MIN_SEGMENT_LENGTH = 0.0001D;

    /** Trail distance used to choose the horizontal steering target without changing movement speed. */
    private static final double STEER_LOOKAHEAD_DISTANCE = 2.0D;

    /** Exponential camera response rate per second while steering toward the trail. */
    private static final double CAMERA_RESPONSIVENESS = 10.0D;

    /** Maximum elapsed frame time used by the camera easing after a pause or hitch. */
    private static final double MAX_CAMERA_FRAME_SECONDS = 0.1D;

    /** Time without mouse look before auto-walk begins recentering the camera. */
    private static final long CAMERA_RECENTER_DELAY_MILLIS = 4_000L;

    /** Wall-clock duration used for the eased camera recenter. */
    private static final long CAMERA_RECENTER_DURATION_MILLIS = 1_000L;

    /** Minimum yaw or pitch delta treated as manual look input. */
    private static final float LOOK_INPUT_EPSILON = 0.05F;

    /** Vanilla jump impulse used when auto-walk meets a one-block obstacle. */
    private static final double AUTO_JUMP_VELOCITY = 0.42D;

    /** Sustained upward velocity used to lift the player over obstacles too tall for a normal jump impulse. */
    private static final double FLOAT_CLIMB_VELOCITY = 0.18D;

    /** Extra feet clearance required before auto-walk stops lifting over a tall obstacle. */
    private static final double CLEARANCE_MARGIN = 0.15D;

    /** Tallest obstacle treated as a normal single-jump obstruction. */
    private static final double JUMP_MAX_STEP = 1.0D;

    /** Distance in front of the player checked for jumpable obstacles. */
    private static final double OBSTACLE_PROBE_DISTANCE = 1.0D;

    /** Vertical range above the player's feet sampled when looking for a forward obstacle. */
    private static final double OBSTACLE_PROBE_HEIGHT = 2.0D;

    /** Maximum lateral distance auto-walk may leave the trail centreline to steer around an obstacle. */
    private static final double MAX_SIDESTEP_DISTANCE = 4.0D;

    /** Lateral spacing between candidate sidestep corridors. */
    private static final double SIDESTEP_PROBE_STEP = 0.5D;

    /** Vertical depth below the target feet position used to detect walkable support on uneven ground. */
    private static final double GROUND_SUPPORT_PROBE_DEPTH = 1.1D;

    /** Forward distance used to place a detour waypoint beyond a narrow obstacle. */
    private static final double DETOUR_FORWARD_CLEARANCE = 2.5D;

    /** Movement ticks between automatic trail-chain rebuilds while auto-walk is active. */
    private static final int REBUILD_INTERVAL_TICKS = 20;

    /** Movement ticks to wait at an unloaded trail boundary before cancelling auto-walk. */
    private static final int TRUNCATED_END_TIMEOUT_TICKS = 200;

    /** Horizontal distance from the active detour target that counts as reaching it. */
    private static final double DETOUR_ARRIVAL_DISTANCE = 0.35D;

    /** Horizontal inset applied to ground probes so adjacent wall faces do not count as floor support. */
    private static final double GROUND_SUPPORT_PROBE_INSET = 0.05D;

    /** Ordered lateral directions tested for each sidestep distance. */
    private static final int[] SIDESTEP_SIDES = {1, -1};

    /** Whether auto-walk is currently controlling the player. */
    @Getter
    private static boolean active = false;

    /** Ordered world-space nodes for the trail being followed. */
    private static List<Vec3d> nodes = List.of();

    /** Whether the current node list ends at a loaded marker rather than an unloaded chunk boundary. */
    private static boolean endIsTerminus = false;

    /** Movement ticks elapsed since the current trail chain was last rebuilt. */
    private static int ticksSinceRebuild = 0;

    /** Consecutive movement ticks spent waiting at an unloaded chain end. */
    private static int truncatedWaitTicks = 0;

    /** Index of the active segment start within {@link #nodes}. */
    private static int segmentIndex = 0;

    /** Current progress along the active segment, in blocks. */
    private static double segmentProgress = 0.0D;

    /** Path ID selected when auto-walk was activated. */
    private static String activePathId = "";

    /** Chapter ID selected when auto-walk was activated. */
    private static String activeChapterId = "";

    /** Trail-derived yaw target most recently computed by the movement tick. */
    private static float targetYaw = 0.0F;

    /** Trail-derived pitch target most recently computed by the movement tick. */
    private static float targetPitch = 0.0F;

    /** Whether a valid camera target is available for the end-of-tick camera driver. */
    private static boolean hasCameraTarget = false;

    /** Last yaw value accepted or written by auto-walk camera handling. */
    private static float lastAppliedYaw = 0.0F;

    /** Last pitch value accepted or written by auto-walk camera handling. */
    private static float lastAppliedPitch = 0.0F;

    /** Whether {@link #lastAppliedYaw} and {@link #lastAppliedPitch} contain player-relative values. */
    private static boolean hasAppliedCamera = false;

    /** Wall-clock time when user mouse look was last detected. */
    private static long lastLookInputTime = 0L;

    /** Wall-clock time when the current camera recenter started, or zero when idle. */
    private static long recenterStartMillis = 0L;

    /** Player yaw captured when the current recenter started. */
    private static float recenterStartYaw = 0.0F;

    /** Player pitch captured when the current recenter started. */
    private static float recenterStartPitch = 0.0F;

    /** Monotonic timestamp of the last rendered camera frame. */
    private static long lastFrameNanos = 0L;

    /** Feet Y target for the current float-over lift, or negative infinity when no lift is active. */
    private static double floatOverTargetY = Double.NEGATIVE_INFINITY;

    /** World-space waypoint being pursued while auto-walk steers around an obstacle. */
    private static @Nullable Vec3d detourTarget = null;

    /**
     * Private constructor for the static movement controller.
     */
    private AutoWalker() {
    }

    /**
     * Toggles auto-walk on or off for the current selected path and chapter.
     */
    public static void toggle() {
        if (active) {
            cancel(StopReason.TOGGLED_OFF);
            return;
        }

        ClientPlayerEntity player = Client.player();
        if (player == null || Client.world() == null || !isHoldingPathfinder(player)) return;

        String pathId = ArdaPathsClient.CONFIG.getSelectedPathId();
        String chapterId = ArdaPathsClient.CONFIG.getCurrentChapterId();
        if (pathId == null || pathId.isBlank() || chapterId == null || chapterId.isBlank()) return;
        if (ArdaPathsClient.CONFIG.getSelectedPath() == null || ArdaPathsClient.CONFIG.getCurrentChapter() == null)
            return;

        engage(pathId, chapterId, player.getPos(), ACTIVATION_DISTANCE);
    }

    /**
     * Requests the next camera frame begin recentering without waiting for the idle delay.
     */
    public static void requestImmediateRecenter() {
        lastLookInputTime = 0L;
        recenterStartMillis = 0L;
    }

    /**
     * Cancels auto-walk and clears the current trail cursor.
     *
     * @param reason the reason auto-walk is stopping
     */
    public static void cancel(StopReason reason) {
        logStop(reason);
        active = false;
        nodes = List.of();
        endIsTerminus = false;
        ticksSinceRebuild = 0;
        truncatedWaitTicks = 0;
        segmentIndex = 0;
        segmentProgress = 0.0D;
        activePathId = "";
        activeChapterId = "";
        floatOverTargetY = Double.NEGATIVE_INFINITY;
        detourTarget = null;
        clearCameraState();
        stopHorizontalVelocity(Client.player());
    }

    /**
     * Checks whether the player is holding the Pathfinder item.
     *
     * @param player the player to inspect
     * @return true when either hand contains the Pathfinder
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean isHoldingPathfinder(ClientPlayerEntity player) {
        return player.getMainHandStack().isOf(ModItems.PATH_REVEALER) || player.getOffHandStack().isOf(ModItems.PATH_REVEALER);
    }

    /**
     * Starts or retargets auto-walk for the selected path and chapter near the supplied position.
     *
     * @param pathId      the selected path ID
     * @param chapterId   the selected chapter ID
     * @param position    the player position to project onto the trail
     * @param maxDistance the maximum horizontal distance allowed from the trail
     * @return true when auto-walk has a usable trail cursor
     */
    private static boolean engage(String pathId, String chapterId, Vec3d position, double maxDistance) {
        TrailChain chain = buildTrailNodes(pathId, chapterId, position);
        TrailProjection projection = findNearestProjection(position, chain.nodes());
        if (projection == null || projection.distance > maxDistance) return false;

        nodes = chain.nodes();
        endIsTerminus = chain.endIsTerminus();
        ticksSinceRebuild = 0;
        truncatedWaitTicks = 0;
        segmentIndex = projection.segmentIndex;
        segmentProgress = projection.segmentLength * projection.segmentT;
        activePathId = pathId;
        activeChapterId = chapterId;
        active = true;
        floatOverTargetY = Double.NEGATIVE_INFINITY;
        detourTarget = null;
        resetCameraState(Client.player());
        logEngaged(pathId, chapterId, chain.nodes());
        return true;
    }

    /**
     * Logs the reason and current cursor state before auto-walk clears its state.
     *
     * @param reason the reason auto-walk is stopping
     */
    private static void logStop(StopReason reason) {
        ClientPlayerEntity player = Client.player();
        int lastSegmentIndex = Math.max(0, nodes.size() - 1);
        if (player == null) {
            log.info(
                    "Auto-walk stopped: {} [position unavailable, path={}, chapter={}, segment={}/{}]",
                    reason,
                    activePathId,
                    activeChapterId,
                    segmentIndex,
                    lastSegmentIndex
            );
            return;
        }

        log.info(
                "Auto-walk stopped at ({}, {}, {}): {} [path={}, chapter={}, segment={}/{}]",
                formatCoordinate(player.getX()),
                formatCoordinate(player.getY()),
                formatCoordinate(player.getZ()),
                reason,
                activePathId,
                activeChapterId,
                segmentIndex,
                lastSegmentIndex
        );
    }

    /**
     * Clears all camera target and free-look state after auto-walk stops.
     */
    private static void clearCameraState() {
        targetYaw = 0.0F;
        targetPitch = 0.0F;
        hasCameraTarget = false;
        lastAppliedYaw = 0.0F;
        lastAppliedPitch = 0.0F;
        hasAppliedCamera = false;
        lastLookInputTime = 0L;
        recenterStartMillis = 0L;
        recenterStartYaw = 0.0F;
        recenterStartPitch = 0.0F;
        lastFrameNanos = 0L;
    }

    /**
     * Removes any horizontal auto-walk velocity while preserving gravity and fall motion.
     *
     * @param player the player whose velocity should be cleared
     */
    private static void stopHorizontalVelocity(@Nullable Entity player) {
        if (player == null) return;

        Vec3d velocity = player.getVelocity();
        player.setVelocity(0.0D, velocity.y, 0.0D);
    }

    /**
     * Builds a forward marker chain from the trail segment nearest to the player.
     *
     * @param pathId    the selected path ID
     * @param chapterId the selected chapter ID
     * @param position  the player position used to choose a chain
     * @return ordered nodes and end classification for the chosen chain
     */
    private static TrailChain buildTrailNodes(String pathId, String chapterId, Vec3d position) {
        Map<BlockPos, PathMarkerBlockEntity> markersByPos = new HashMap<>();
        Map<BlockPos, TrailSegment> segmentsByStart = new HashMap<>();
        List<TrailSegment> segments = new ArrayList<>();

        for (PathMarkerBlockEntity marker : Paths.getTickingMarkers()) {
            BlockPos startBlock = marker.getPos().toImmutable();
            markersByPos.put(startBlock, marker);

            PathMarkerBlockEntity.ChapterNbtData data = marker.getChapterData(pathId, chapterId, false);
            if (data == null || data.getTarget() == null) continue;

            BlockPos endBlock = startBlock.add(data.getTarget());
            TrailSegment segment = new TrailSegment(center(startBlock), center(endBlock), startBlock, endBlock);
            segmentsByStart.put(startBlock, segment);
            segments.add(segment);
        }

        TrailSegment nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (TrailSegment segment : segments) {
            double distance = segment.distanceTo(position);
            if (distance < nearestDistance) {
                nearest = segment;
                nearestDistance = distance;
            }
        }

        if (nearest == null) return new TrailChain(List.of(), false);

        List<Vec3d> orderedNodes = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        TrailSegment current = nearest;
        BlockPos finalEndBlock = nearest.endBlock;
        orderedNodes.add(current.start);

        while (current != null && visited.add(current.startBlock)) {
            orderedNodes.add(current.end);
            finalEndBlock = current.endBlock;
            current = segmentsByStart.get(current.endBlock);
        }

        return new TrailChain(orderedNodes, markersByPos.containsKey(finalEndBlock));
    }

    /**
     * Rebuilds the active trail chain around the player's current position.
     *
     * @param position the player position used to choose the replacement chain
     */
    private static void rebuildTrail(Vec3d position) {
        ticksSinceRebuild = 0;
        TrailChain chain = buildTrailNodes(activePathId, activeChapterId, position);
        if (chain.nodes().size() < 2) return;

        nodes = chain.nodes();
        endIsTerminus = chain.endIsTerminus();
    }

    /**
     * Finds the nearest projection of a position onto a node polyline.
     *
     * @param position   the position to project
     * @param trailNodes the polyline nodes
     * @return the nearest projection, or null when no segment exists
     */
    private static @Nullable TrailProjection findNearestProjection(Vec3d position, List<Vec3d> trailNodes) {
        if (trailNodes.size() < 2) return null;

        TrailProjection nearest = null;
        for (int index = 0; index < trailNodes.size() - 1; index++) {
            Vec3d start = trailNodes.get(index);
            Vec3d end = trailNodes.get(index + 1);
            TrailProjection projection = project(position, start, end, index);
            if (nearest == null || projection.distance < nearest.distance) {
                nearest = projection;
            }
        }

        return nearest;
    }

    /**
     * Resets free-look tracking so a new auto-walk engagement can steer immediately.
     *
     * @param player the player whose current camera should be treated as the baseline
     */
    private static void resetCameraState(@Nullable ClientPlayerEntity player) {
        if (player != null) {
            lastAppliedYaw = player.getYaw();
            lastAppliedPitch = player.getPitch();
            hasAppliedCamera = true;
        } else {
            hasAppliedCamera = false;
        }

        lastLookInputTime = 0L;
        recenterStartMillis = 0L;
        recenterStartYaw = 0.0F;
        recenterStartPitch = 0.0F;
        lastFrameNanos = 0L;
        hasCameraTarget = false;
    }

    /**
     * Logs a successful auto-walk engagement with enough trail context to diagnose truncated marker chains.
     *
     * @param pathId     the path being followed
     * @param chapterId  the chapter being followed
     * @param trailNodes the ordered trail nodes available at engagement time
     */
    private static void logEngaged(String pathId, String chapterId, List<Vec3d> trailNodes) {
        Vec3d firstNode = trailNodes.get(0);
        Vec3d lastNode = trailNodes.get(trailNodes.size() - 1);
        log.info(
                "Auto-walk engaged on path={} chapter={}: {} nodes, trail start at ({}, {}, {}), trail end at ({}, {}, {})",
                pathId,
                chapterId,
                trailNodes.size(),
                formatCoordinate(firstNode.x),
                formatCoordinate(firstNode.y),
                formatCoordinate(firstNode.z),
                formatCoordinate(lastNode.x),
                formatCoordinate(lastNode.y),
                formatCoordinate(lastNode.z)
        );
    }

    /**
     * Formats a world coordinate for compact diagnostic log output.
     *
     * @param coordinate the coordinate value
     * @return the coordinate rounded to one decimal place
     */
    private static String formatCoordinate(double coordinate) {
        return String.format(Locale.ROOT, "%.1f", coordinate);
    }

    /**
     * Creates a centered world-space vector for a block position.
     *
     * @param position the block position
     * @return the centered vector
     */
    private static Vec3d center(BlockPos position) {
        return new Vec3d(position.getX() + 0.5D, position.getY() + 0.5D, position.getZ() + 0.5D);
    }

    /**
     * Projects a position onto a single segment using the path's horizontal footprint.
     *
     * @param position     the position to project
     * @param start        the segment start
     * @param end          the segment end
     * @param segmentIndex the index of the segment
     * @return projection data for the segment
     */
    private static TrailProjection project(Vec3d position, Vec3d start, Vec3d end, int segmentIndex) {

        double t = AnimatedTrail.closestSegmentT(position.x, position.z, start.x, start.z, end.x, end.z);
        Vec3d projected = start.lerp(end, t);

        double segmentLength = start.distanceTo(end);
        double distanceX = position.x - projected.x;
        double distanceZ = position.z - projected.z;

        return new TrailProjection(segmentIndex, t, segmentLength, Math.sqrt((distanceX * distanceX) + (distanceZ * distanceZ)));
    }

    /**
     * Advances the player along the active trail when auto-walk is enabled.
     *
     * @param client the current Minecraft client
     */
    public static void tick(MinecraftClient client) {
        if (!active) return;

        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) {
            cancel(StopReason.PLAYER_GONE);
            return;
        }

        StopReason cancelReason = cancelReason(client, player);
        if (cancelReason != null) {
            cancel(cancelReason);
            return;
        }

        String currentChapterId = ArdaPathsClient.CONFIG.getCurrentChapterId();
        if (currentChapterId == null || currentChapterId.isBlank()) {
            cancel(StopReason.NO_CHAPTER_SELECTED);
            return;
        }

        if (!activeChapterId.equals(currentChapterId) && !engage(activePathId, currentChapterId, player.getPos(), Double.POSITIVE_INFINITY)) {
            cancel(StopReason.CHAPTER_RETARGET_FAILED);
            return;
        }

        ticksSinceRebuild++;
        if (ticksSinceRebuild >= REBUILD_INTERVAL_TICKS) rebuildTrail(player.getPos());

        TrailProjection projection = findNearestProjection(player.getPos(), nodes);
        if (projection == null) {
            cancel(StopReason.NO_TRAIL_PROJECTION);
            return;
        }

        if (projection.distance > OFF_TRAIL_DISTANCE) {
            cancel(StopReason.OFF_TRAIL);
            return;
        }

        double movement = AnimatedTrail.SPEED * ArdaPathsClient.CONFIG.getAutoWalkSpeedFactor();
        segmentIndex = projection.segmentIndex;
        segmentProgress = projection.segmentLength * projection.segmentT;

        if (hasReachedChainEnd(projection, player.getPos())) {
            if (endIsTerminus) {
                cancel(StopReason.ARRIVED_AT_TRAIL_END);
                return;
            }

            if (waitAtTruncatedEnd(player)) return;

            cancel(StopReason.TRAIL_NOT_LOADED);
            return;
        }
        truncatedWaitTicks = 0;

        Vec3d projected = pointAt(segmentIndex, segmentProgress);
        Vec3d current = player.getPos();
        Vec3d steeringTarget = steeringTarget(movement);
        double deltaX = steeringTarget.x - current.x;
        double deltaZ = steeringTarget.z - current.z;
        double horizontalDistance = Math.sqrt((deltaX * deltaX) + (deltaZ * deltaZ));
        if (horizontalDistance < MIN_SEGMENT_LENGTH) return;

        double directionX = deltaX / horizontalDistance;
        double directionZ = deltaZ / horizontalDistance;
        Vec3d steer = movementDirection(player, projected, directionX, directionZ);
        directionX = steer.x;
        directionZ = steer.z;
        double velocityY = player.getVelocity().y;
        velocityY = obstacleAwareVelocityY(player, directionX, directionZ, velocityY);

        player.setVelocity(directionX * movement, velocityY, directionZ * movement);
        updateCameraTarget(projected, steeringTarget);
    }

    /**
     * Smoothly rotates the camera toward the latest trail target once per rendered frame.
     */
    public static void renderCameraFrame() {
        if (!active || !hasCameraTarget) return;

        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        if (FocusController.isEngaged()) {
            lastAppliedYaw = player.getYaw();
            lastAppliedPitch = player.getPitch();
            hasAppliedCamera = true;
            lastLookInputTime = System.currentTimeMillis();
            recenterStartMillis = 0L;
            return;
        }

        float currentYaw = player.getYaw();
        float currentPitch = player.getPitch();
        long now = System.currentTimeMillis();
        long frameNanos = System.nanoTime();
        double frameSeconds = frameDeltaSeconds(frameNanos);

        if (hasAppliedCamera && hasManualLookInput(currentYaw, currentPitch)) {
            lastLookInputTime = now;
            recenterStartMillis = 0L;
            lastAppliedYaw = currentYaw;
            lastAppliedPitch = currentPitch;
            return;
        }

        if (now - lastLookInputTime < CAMERA_RECENTER_DELAY_MILLIS) {
            lastAppliedYaw = currentYaw;
            lastAppliedPitch = currentPitch;
            hasAppliedCamera = true;
            return;
        }

        if (recenterStartMillis == 0L) {
            recenterStartMillis = now;
            recenterStartYaw = currentYaw;
            recenterStartPitch = currentPitch;
        }

        double progress = (now - recenterStartMillis) / (double) CAMERA_RECENTER_DURATION_MILLIS;
        float nextYaw;
        float nextPitch;
        if (progress < 1.0D) {
            float eased = (float) (progress * progress);
            nextYaw = recenterStartYaw + (MathHelper.wrapDegrees(targetYaw - recenterStartYaw) * eased);
            nextPitch = recenterStartPitch + (MathHelper.wrapDegrees(targetPitch - recenterStartPitch) * eased);
        } else {
            float alpha = (float) (1.0D - Math.exp(-CAMERA_RESPONSIVENESS * frameSeconds));
            nextYaw = currentYaw + (MathHelper.wrapDegrees(targetYaw - currentYaw) * alpha);
            nextPitch = currentPitch + (MathHelper.wrapDegrees(targetPitch - currentPitch) * alpha);
        }

        player.setYaw(nextYaw);
        player.bodyYaw = nextYaw;
        player.headYaw = nextYaw;
        player.setPitch(nextPitch);
        player.prevYaw = nextYaw;
        player.prevBodyYaw = nextYaw;
        player.prevHeadYaw = nextYaw;
        player.prevPitch = nextPitch;

        lastAppliedYaw = nextYaw;
        lastAppliedPitch = nextPitch;
        hasAppliedCamera = true;
    }

    /**
     * Calculates the elapsed render-frame time used for frame-rate-independent camera easing.
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
        return Math.min(MAX_CAMERA_FRAME_SECONDS, elapsedNanos / 1_000_000_000.0D);
    }

    /**
     * Determines whether the current view differs from auto-walk's last accepted camera value.
     *
     * @param currentYaw   the player's current yaw
     * @param currentPitch the player's current pitch
     * @return true when the camera changed enough to count as mouse look
     */
    private static boolean hasManualLookInput(float currentYaw, float currentPitch) {
        return Math.abs(MathHelper.wrapDegrees(currentYaw - lastAppliedYaw)) > LOOK_INPUT_EPSILON
                || Math.abs(currentPitch - lastAppliedPitch) > LOOK_INPUT_EPSILON;
    }

    /**
     * Determines whether player input or selection state should stop auto-walk.
     * Movement key input is ignored while the camera is detached from the player, as in freecam.
     *
     * @param client the current Minecraft client
     * @param player the current client player
     * @return the cancellation reason, or null when auto-walk should continue
     */
    private static @Nullable StopReason cancelReason(MinecraftClient client, ClientPlayerEntity player) {
        if (!isHoldingPathfinder(player)) return StopReason.PATHFINDER_NOT_HELD;
        if (client.currentScreen != null) return StopReason.SCREEN_OPEN;
        if (!activePathId.equals(ArdaPathsClient.CONFIG.getSelectedPathId())) return StopReason.PATH_CHANGED;

        if (!isCameraDetached(client, player)
                && (client.options.forwardKey.isPressed()
                || client.options.backKey.isPressed()
                || client.options.leftKey.isPressed()
                || client.options.rightKey.isPressed()
                || client.options.jumpKey.isPressed()
                || client.options.sneakKey.isPressed())) {
            return StopReason.MOVEMENT_KEY_PRESSED;
        }

        return null;
    }

    /**
     * Checks whether the client view is detached from the player, as in freecam or while
     * spectating another entity.
     *
     * @param client the current Minecraft client
     * @param player the current client player
     * @return true when the rendered camera is not the player's own
     */
    private static boolean isCameraDetached(MinecraftClient client, ClientPlayerEntity player) {
        Entity camera = client.getCameraEntity();
        return camera != null && camera != player;
    }

    /**
     * Advances the internal cursor by a distance without mutating active state.
     *
     * @param distance the distance to advance in blocks
     * @return the new cursor, or null when the trail has ended
     */
    private static @Nullable Cursor advanceCursor(double distance) {
        int nextSegmentIndex = segmentIndex;
        double nextSegmentProgress = segmentProgress + distance;

        while (nextSegmentIndex < nodes.size() - 1) {
            double length = segmentLength(nextSegmentIndex);
            if (length < MIN_SEGMENT_LENGTH) {
                nextSegmentIndex++;
                nextSegmentProgress = 0.0D;
                continue;
            }

            if (nextSegmentProgress <= length) {
                return new Cursor(nextSegmentIndex, nextSegmentProgress);
            }

            nextSegmentProgress -= length;
            nextSegmentIndex++;
        }

        return null;
    }

    /**
     * Resolves the trail point used for steering and camera lookahead this tick.
     *
     * @param movement the per-tick movement distance in blocks
     * @return the lookahead point, or the final trail node when the lookahead reaches the end
     */
    private static Vec3d steeringTarget(double movement) {
        Cursor lookaheadCursor = advanceCursor(Math.max(movement, STEER_LOOKAHEAD_DISTANCE));
        if (lookaheadCursor == null) return nodes.get(nodes.size() - 1);

        return pointAt(lookaheadCursor.segmentIndex, lookaheadCursor.segmentProgress);
    }

    /**
     * Determines whether the player is close enough to the final node of the current chain.
     *
     * @param projection the player's current projection onto the trail
     * @param position   the player's current position
     * @return true when the current chain end has been reached
     */
    private static boolean hasReachedChainEnd(TrailProjection projection, Vec3d position) {
        if (projection.segmentIndex < nodes.size() - 2 || projection.segmentT < 0.98D) return false;

        Vec3d end = nodes.get(nodes.size() - 1);
        double distanceX = position.x - end.x;
        double distanceZ = position.z - end.z;
        return Math.sqrt((distanceX * distanceX) + (distanceZ * distanceZ)) <= ARRIVAL_DISTANCE;
    }

    /**
     * Holds position at a truncated chain end while waiting for more trail markers to load.
     *
     * @param player the player waiting at the current chain end
     * @return true while auto-walk should keep waiting
     */
    private static boolean waitAtTruncatedEnd(ClientPlayerEntity player) {
        rebuildTrail(player.getPos());
        truncatedWaitTicks++;
        stopHorizontalVelocity(player);
        return truncatedWaitTicks <= TRUNCATED_END_TIMEOUT_TICKS;
    }

    /**
     * Chooses the vertical velocity needed to handle an obstacle ahead while preserving ordinary stepping.
     *
     * @param player     the player being driven
     * @param directionX normalized horizontal x movement direction
     * @param directionZ normalized horizontal z movement direction
     * @param currentY   the player's current vertical velocity
     * @return vertical velocity to apply this tick
     */
    private static double obstacleAwareVelocityY(ClientPlayerEntity player, double directionX, double directionZ, double currentY) {
        if (player.getWorld() == null) return currentY;
        if (shouldKeepFloating(player)) return FLOAT_CLIMB_VELOCITY;
        if (!player.isOnGround() && floatOverTargetY == Double.NEGATIVE_INFINITY) return currentY;

        double obstacleTop = forwardObstacleTop(player, directionX, directionZ);
        if (obstacleTop == Double.NEGATIVE_INFINITY) {
            return currentY;
        }

        double obstacleHeight = obstacleTop - player.getY();
        if (obstacleHeight <= player.getStepHeight()) {
            return currentY;
        }

        Box forwardBox = forwardProbeBox(player, directionX, directionZ);
        if (canJumpOver(player, directionX, directionZ, obstacleTop)) {
            return AUTO_JUMP_VELOCITY;
        }

        double targetY = obstacleTop + CLEARANCE_MARGIN;
        if (hasClearanceAtFeetY(player, forwardBox, targetY)) {
            floatOverTargetY = Math.max(floatOverTargetY, targetY);
        }

        return shouldKeepFloating(player) ? FLOAT_CLIMB_VELOCITY : currentY;
    }

    /**
     * Checks whether a float-over lift should keep applying upward velocity.
     *
     * @param player the player being lifted
     * @return true when the player's feet have not reached the active lift target
     */
    private static boolean shouldKeepFloating(ClientPlayerEntity player) {
        if (floatOverTargetY == Double.NEGATIVE_INFINITY) return false;
        if (player.getY() < floatOverTargetY) return true;

        floatOverTargetY = Double.NEGATIVE_INFINITY;
        return false;
    }

    /**
     * Chooses the horizontal movement direction, including any committed obstacle detour.
     *
     * @param player      the player being driven
     * @param trailCenter the nearest point on the trail centreline
     * @param directionX  normalized horizontal x movement direction
     * @param directionZ  normalized horizontal z movement direction
     * @return normalized horizontal direction for this tick
     */
    private static Vec3d movementDirection(ClientPlayerEntity player, Vec3d trailCenter, double directionX, double directionZ) {
        Vec3d trailDirection = new Vec3d(directionX, 0.0D, directionZ);
        if (floatOverTargetY != Double.NEGATIVE_INFINITY) return trailDirection;

        if (detourTarget != null) {
            Vec3d detourDirection = directionToTarget(player.getPos(), detourTarget);
            if (detourDirection != null && !hasArrivedAtDetour(player.getPos(), detourTarget) && !isBlockingObstacle(player, forwardObstacleTop(player, detourDirection.x, detourDirection.z))) {
                return detourDirection;
            }

            detourTarget = null;
        }

        double obstacleTop = forwardObstacleTop(player, directionX, directionZ);
        if (player.isOnGround() && isBlockingObstacle(player, obstacleTop)) {
            if (!canJumpOver(player, directionX, directionZ, obstacleTop)) {
                detourTarget = findDetourTarget(player, trailCenter, directionX, directionZ);
                if (detourTarget != null) {
                    Vec3d detourDirection = directionToTarget(player.getPos(), detourTarget);
                    if (detourDirection != null) return detourDirection;
                }
            }
        }

        return trailDirection;
    }

    /**
     * Checks whether a blocking obstacle can be cleared by a normal jump this tick.
     *
     * @param player      the player being driven
     * @param directionX  normalized horizontal x movement direction
     * @param directionZ  normalized horizontal z movement direction
     * @param obstacleTop the measured top of the obstacle ahead
     * @return true when auto-walk should keep the trail direction and issue a jump
     */
    private static boolean canJumpOver(ClientPlayerEntity player, double directionX, double directionZ, double obstacleTop) {
        return player.isOnGround()
                && obstacleTop - player.getY() <= JUMP_MAX_STEP
                && hasClearanceAtFeetY(player, forwardProbeBox(player, directionX, directionZ), player.getY() + 1.0D);
    }

    /**
     * Finds the nearest usable sidestep waypoint around a blocking obstacle.
     *
     * @param player      the player being driven
     * @param trailCenter the nearest point on the trail centreline
     * @param directionX  normalized horizontal x movement direction
     * @param directionZ  normalized horizontal z movement direction
     * @return detour waypoint to pursue, or null when no usable corridor is available
     */
    @SuppressWarnings("UnnecessaryLocalVariable")
    private static @Nullable Vec3d findDetourTarget(ClientPlayerEntity player, Vec3d trailCenter, double directionX, double directionZ) {

        double perpendicularX = -directionZ;
        double perpendicularZ = directionX;
        double currentTrailOffset = ((player.getX() - trailCenter.x) * perpendicularX) + ((player.getZ() - trailCenter.z) * perpendicularZ);

        for (double offset = SIDESTEP_PROBE_STEP; offset <= MAX_SIDESTEP_DISTANCE + 0.000001D; offset += SIDESTEP_PROBE_STEP) {

            for (int side : SIDESTEP_SIDES) {

                double lateralOffset = offset * side;
                if (Math.abs(currentTrailOffset + lateralOffset) > MAX_SIDESTEP_DISTANCE) continue;

                Box approachBox = sidestepProbeBox(player, directionX, directionZ, perpendicularX, perpendicularZ, lateralOffset, DETOUR_FORWARD_CLEARANCE / 2.0D);
                Box waypointBox = sidestepProbeBox(player, directionX, directionZ, perpendicularX, perpendicularZ, lateralOffset, DETOUR_FORWARD_CLEARANCE);
                if (!hasUsableDetourCorridor(player, approachBox) || !hasUsableDetourCorridor(player, waypointBox))
                    continue;

                return player.getPos().add(
                        (directionX * DETOUR_FORWARD_CLEARANCE) + (perpendicularX * lateralOffset),
                        0.0D,
                        (directionZ * DETOUR_FORWARD_CLEARANCE) + (perpendicularZ * lateralOffset)
                );
            }
        }

        return null;
    }

    /**
     * Checks whether an obstacle top is high enough to require detouring or lifting.
     *
     * @param player      the player being driven
     * @param obstacleTop the measured top of the obstacle ahead
     * @return true when the obstacle is taller than the player's step height
     */
    private static boolean isBlockingObstacle(ClientPlayerEntity player, double obstacleTop) {
        return obstacleTop != Double.NEGATIVE_INFINITY && obstacleTop - player.getY() > player.getStepHeight();
    }

    /**
     * Checks whether the player has reached a committed detour waypoint.
     *
     * @param position the player's current position
     * @param target   the detour waypoint
     * @return true when the player is horizontally close enough to the target
     */
    private static boolean hasArrivedAtDetour(Vec3d position, Vec3d target) {
        double distanceX = target.x - position.x;
        double distanceZ = target.z - position.z;
        return Math.sqrt((distanceX * distanceX) + (distanceZ * distanceZ)) < DETOUR_ARRIVAL_DISTANCE;
    }

    /**
     * Builds a normalized horizontal direction from a position to a target.
     *
     * @param position the current position
     * @param target   the target position
     * @return normalized horizontal direction, or null when the target is too close
     */
    private static @Nullable Vec3d directionToTarget(Vec3d position, Vec3d target) {
        double deltaX = target.x - position.x;
        double deltaZ = target.z - position.z;
        double length = Math.sqrt((deltaX * deltaX) + (deltaZ * deltaZ));
        if (length < MIN_SEGMENT_LENGTH) return null;

        return new Vec3d(deltaX / length, 0.0D, deltaZ / length);
    }

    /**
     * Measures the highest collision surface in the forward movement probe.
     *
     * @param player     the player being driven
     * @param directionX normalized horizontal x movement direction
     * @param directionZ normalized horizontal z movement direction
     * @return maximum obstacle top Y, or negative infinity when no obstacle is ahead
     */
    private static double forwardObstacleTop(ClientPlayerEntity player, double directionX, double directionZ) {
        if (player.getWorld() == null) return Double.NEGATIVE_INFINITY;

        Box collisionProbe = lowerForwardProbeBox(player, directionX, directionZ);
        double obstacleTop = Double.NEGATIVE_INFINITY;
        for (VoxelShape shape : player.getWorld().getBlockCollisions(player, collisionProbe)) {
            if (shape.isEmpty()) continue;

            obstacleTop = Math.max(obstacleTop, shape.getBoundingBox().maxY);
        }

        return obstacleTop;
    }

    /**
     * Builds the full-height forward probe box at the player's current position.
     *
     * @param player     the player being driven
     * @param directionX normalized horizontal x movement direction
     * @param directionZ normalized horizontal z movement direction
     * @return player collision box shifted forward by the obstacle probe distance
     */
    private static Box forwardProbeBox(ClientPlayerEntity player, double directionX, double directionZ) {
        return player.getBoundingBox().offset(directionX * OBSTACLE_PROBE_DISTANCE, 0.0D, directionZ * OBSTACLE_PROBE_DISTANCE);
    }

    /**
     * Builds the lower forward probe area used to measure nearby obstacle heights.
     *
     * @param player     the player being driven
     * @param directionX normalized horizontal x movement direction
     * @param directionZ normalized horizontal z movement direction
     * @return forward probe box clipped to the lower obstacle-sampling range
     */
    private static Box lowerForwardProbeBox(ClientPlayerEntity player, double directionX, double directionZ) {
        Box forwardBox = forwardProbeBox(player, directionX, directionZ);
        return new Box(
                forwardBox.minX,
                forwardBox.minY,
                forwardBox.minZ,
                forwardBox.maxX,
                Math.min(forwardBox.maxY, player.getY() + OBSTACLE_PROBE_HEIGHT),
                forwardBox.maxZ
        );
    }

    /**
     * Builds a forward-and-lateral probe box for a possible obstacle detour.
     *
     * @param player         the player being driven
     * @param directionX     normalized horizontal x movement direction
     * @param directionZ     normalized horizontal z movement direction
     * @param perpendicularX normalized horizontal x axis perpendicular to movement
     * @param perpendicularZ normalized horizontal z axis perpendicular to movement
     * @param lateralOffset  signed lateral offset from the trail direction
     * @param forwardOffset  forward distance from the player's current position
     * @return player collision box shifted into the candidate sidestep corridor
     */
    private static Box sidestepProbeBox(ClientPlayerEntity player, double directionX, double directionZ, double perpendicularX, double perpendicularZ, double lateralOffset, double forwardOffset) {
        return player.getBoundingBox().offset(
                (directionX * forwardOffset) + (perpendicularX * lateralOffset),
                0.0D,
                (directionZ * forwardOffset) + (perpendicularZ * lateralOffset)
        );
    }

    /**
     * Checks whether the player's collision box would fit with feet at a target height.
     *
     * @param player    the player being driven
     * @param targetBox the target player collision box at the player's current feet height
     * @param feetY     target world Y for the player's feet
     * @return true when the target player volume is empty
     */
    private static boolean hasClearanceAtFeetY(ClientPlayerEntity player, Box targetBox, double feetY) {
        if (player.getWorld() == null) return false;

        return player.getWorld().isSpaceEmpty(player, targetBox.offset(0.0D, feetY - player.getY(), 0.0D));
    }

    /**
     * Checks whether a candidate sidestep corridor has both player clearance and walkable support.
     *
     * @param player      the player being driven
     * @param corridorBox the candidate sidestep collision box
     * @return true when the corridor can hold the player on nearby terrain height
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean hasUsableDetourCorridor(ClientPlayerEntity player, Box corridorBox) {

        double feetY = player.getY();
        double stepUpFeetY = feetY + player.getStepHeight();

        return (hasClearanceAtFeetY(player, corridorBox, feetY) && hasGroundSupportAtFeetY(player, corridorBox, feetY))
                || (hasClearanceAtFeetY(player, corridorBox, stepUpFeetY) && hasGroundSupportAtFeetY(player, corridorBox, stepUpFeetY));
    }

    /**
     * Checks whether the candidate sidestep corridor has solid ground below the target feet height.
     *
     * @param player      the player being driven
     * @param corridorBox the candidate sidestep collision box
     * @param feetY       target world Y for the player's feet
     * @return true when a collision surface supports the target standing area
     */
    private static boolean hasGroundSupportAtFeetY(ClientPlayerEntity player, Box corridorBox, double feetY) {
        if (player.getWorld() == null) return false;

        Box supportBox = new Box(
                corridorBox.minX + GROUND_SUPPORT_PROBE_INSET,
                feetY - GROUND_SUPPORT_PROBE_DEPTH,
                corridorBox.minZ + GROUND_SUPPORT_PROBE_INSET,
                corridorBox.maxX - GROUND_SUPPORT_PROBE_INSET,
                feetY,
                corridorBox.maxZ - GROUND_SUPPORT_PROBE_INSET
        );
        for (VoxelShape shape : player.getWorld().getBlockCollisions(player, supportBox)) {
            if (!shape.isEmpty()) return true;
        }

        return false;
    }

    /**
     * Resolves the world-space point at a cursor location.
     *
     * @param index    the segment start index
     * @param progress progress along the segment in blocks
     * @return the point at the cursor
     */
    private static Vec3d pointAt(int index, double progress) {
        Vec3d start = nodes.get(index);
        Vec3d end = nodes.get(index + 1);
        double length = start.distanceTo(end);
        double t = length < MIN_SEGMENT_LENGTH ? 1.0D : MathHelper.clamp(progress / length, 0.0D, 1.0D);
        return start.lerp(end, t);
    }

    /**
     * Returns the length of a segment in the active node list.
     *
     * @param index the segment start index
     * @return the segment length in blocks
     */
    private static double segmentLength(int index) {
        return nodes.get(index).distanceTo(nodes.get(index + 1));
    }

    /**
     * Stores a stable trail-tangent camera target for the end-of-tick camera driver.
     *
     * @param from the projected trail point used as the tangent start
     * @param to   the lookahead trail point used as the tangent end
     */
    private static void updateCameraTarget(Vec3d from, Vec3d to) {
        double deltaX = to.x - from.x;
        double deltaZ = to.z - from.z;
        if ((deltaX * deltaX) + (deltaZ * deltaZ) < 0.000001D) return;

        targetYaw = (float) (MathHelper.atan2(deltaZ, deltaX) * (180.0D / Math.PI)) - 90.0F;
        targetPitch = 0.0F;
        hasCameraTarget = true;
    }

    /**
     * Reasons auto-walk can stop controlling the player.
     */
    public enum StopReason {
        /** Auto-walk was disabled by the auto-walk toggle. */
        TOGGLED_OFF,

        /** The client player or world was unavailable during a movement tick. */
        PLAYER_GONE,

        /** The player stopped holding the Pathfinder item. */
        PATHFINDER_NOT_HELD,

        /** A client screen opened while auto-walk was active. */
        SCREEN_OPEN,

        /** The selected path changed while auto-walk was active. */
        PATH_CHANGED,

        /** Manual movement input was pressed while auto-walk was active. */
        MOVEMENT_KEY_PRESSED,

        /** The selected chapter became empty while auto-walk was active. */
        NO_CHAPTER_SELECTED,

        /** Auto-walk could not retarget after the selected chapter changed. */
        CHAPTER_RETARGET_FAILED,

        /** The active node list no longer produced a valid nearest trail projection. */
        NO_TRAIL_PROJECTION,

        /** The player moved too far from the active trail. */
        OFF_TRAIL,

        /** The player reached the last available node in the active trail chain. */
        ARRIVED_AT_TRAIL_END,

        /** The trail chain ended at an unloaded chunk boundary and did not extend before the wait timeout. */
        TRAIL_NOT_LOADED
    }

    /**
     * Immutable trail chain chosen from the loaded marker set.
     *
     * @param nodes          ordered centered trail nodes
     * @param endIsTerminus true when the last node corresponds to a loaded marker
     */
    private record TrailChain(List<Vec3d> nodes, boolean endIsTerminus) {

    }

    /**
     * Immutable marker trail segment used while building an ordered chain.
     *
     * @param start      the centered start position
     * @param end        the centered end position
     * @param startBlock the marker block at the segment start
     * @param endBlock   the block position at the segment end
     */
    private record TrailSegment(Vec3d start, Vec3d end, BlockPos startBlock, BlockPos endBlock) {

        /**
         * Measures the nearest distance from a position to this segment.
         *
         * @param position the position to test
         * @return the nearest distance in blocks
         */
        private double distanceTo(Vec3d position) {
            return project(position, start, end, 0).distance;
        }
    }

    /**
     * Immutable projection of a position onto a trail segment.
     *
     * @param segmentIndex  the segment index in the active node list
     * @param segmentT      the normalized projection parameter
     * @param segmentLength the segment length in blocks
     * @param distance      the distance from the probe position to the segment
     */
    private record TrailProjection(int segmentIndex, double segmentT, double segmentLength, double distance) {

    }

    /**
     * Immutable cursor for a pending movement step.
     *
     * @param segmentIndex    the segment index in the active node list
     * @param segmentProgress progress along the segment in blocks
     */
    private record Cursor(int segmentIndex, double segmentProgress) {

    }
}

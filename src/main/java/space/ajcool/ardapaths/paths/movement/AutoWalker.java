package space.ajcool.ardapaths.paths.movement;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import space.ajcool.ardapaths.ArdaPathsClient;
import space.ajcool.ardapaths.core.Client;
import space.ajcool.ardapaths.mc.items.ModItems;
import space.ajcool.ardapaths.paths.rendering.objects.AnimatedTrail;

import java.util.List;
import java.util.Locale;

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

    /** Small distance used to detect exhausted movement targets. */
    private static final double MIN_SEGMENT_LENGTH = 0.0001D;

    /** Movement ticks between automatic trail-chain rebuilds while auto-walk is active. */
    private static final int REBUILD_INTERVAL_TICKS = 20;

    /** Movement ticks to wait at an unloaded trail boundary before cancelling auto-walk. */
    private static final int TRUNCATED_END_TIMEOUT_TICKS = 200;

    /** Camera driver that tracks the current trail tangent while auto-walk is active. */
    private static final AutoWalkCamera CAMERA = new AutoWalkCamera();

    /** Whether auto-walk is currently controlling the player. */
    @Getter
    // Accessed via Lombok-generated accessor; IntelliJ entry-point analysis can't follow it.
    @SuppressWarnings("unused")
    private static boolean active = false;

    /** Ordered world-space trail followed by the active controller. */
    private static AutoWalkTrail trail = new AutoWalkTrail(List.of(), false);

    /** Movement ticks elapsed since the current trail chain was last rebuilt. */
    private static int ticksSinceRebuild = 0;

    /** Consecutive movement ticks spent waiting at an unloaded chain end. */
    private static int truncatedWaitTicks = 0;

    /** Index of the active segment start within the current trail. */
    private static int segmentIndex = 0;

    /** Current progress along the active segment, in blocks. */
    private static double segmentProgress = 0.0D;

    /** Path ID selected when auto-walk was activated. */
    private static String activePathId = "";

    /** Chapter ID selected when auto-walk was activated. */
    private static String activeChapterId = "";

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

        LocalPlayer player = Client.player();
        if (player == null || Client.world() == null || !isHoldingPathfinder(player)) return;

        String pathId = ArdaPathsClient.CONFIG.getSelectedPathId();
        String chapterId = ArdaPathsClient.CONFIG.getCurrentChapterId();
        if (pathId == null || pathId.isBlank() || chapterId == null || chapterId.isBlank()) return;
        if (ArdaPathsClient.CONFIG.getSelectedPath() == null || ArdaPathsClient.CONFIG.getCurrentChapter() == null)
            return;

        engage(pathId, chapterId, player.position(), ACTIVATION_DISTANCE);
    }

    /**
     * Cancels auto-walk and clears the current trail cursor.
     *
     * @param reason the reason auto-walk is stopping
     */
    public static void cancel(StopReason reason) {
        logStop(reason);
        active = false;
        trail = new AutoWalkTrail(List.of(), false);
        ticksSinceRebuild = 0;
        truncatedWaitTicks = 0;
        segmentIndex = 0;
        segmentProgress = 0.0D;
        activePathId = "";
        activeChapterId = "";
        ObstacleNavigator.reset();
        CAMERA.clear();
        stopHorizontalVelocity(Client.player());
    }

    /**
     * Checks whether the player is holding the Pathfinder item.
     *
     * @param player the player to inspect
     * @return true when either hand contains the Pathfinder
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean isHoldingPathfinder(LocalPlayer player) {
        return player.getMainHandItem().is(ModItems.PATH_REVEALER) || player.getOffhandItem().is(ModItems.PATH_REVEALER);
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
    private static boolean engage(String pathId, String chapterId, Vec3 position, double maxDistance) {
        AutoWalkTrail nextTrail = AutoWalkTrail.build(pathId, chapterId, position);
        AutoWalkTrail.Projection projection = nextTrail.nearestProjection(position);
        if (projection == null || projection.distance() > maxDistance) return false;

        trail = nextTrail;
        ticksSinceRebuild = 0;
        truncatedWaitTicks = 0;
        segmentIndex = projection.segmentIndex();
        segmentProgress = projection.segmentLength() * projection.segmentT();
        activePathId = pathId;
        activeChapterId = chapterId;
        active = true;
        ObstacleNavigator.reset();
        CAMERA.reset(Client.player());
        logEngaged(pathId, chapterId, nextTrail.nodes());
        return true;
    }

    /**
     * Logs the reason and current cursor state before auto-walk clears its state.
     *
     * @param reason the reason auto-walk is stopping
     */
    private static void logStop(StopReason reason) {
        LocalPlayer player = Client.player();
        int lastSegmentIndex = Math.max(0, trail.nodes().size() - 1);
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
     * Removes any horizontal auto-walk velocity while preserving gravity and fall motion.
     *
     * @param player the player whose velocity should be cleared
     */
    private static void stopHorizontalVelocity(@Nullable Entity player) {
        if (player == null) return;

        Vec3 velocity = player.getDeltaMovement();
        player.setDeltaMovement(0.0D, velocity.y, 0.0D);
    }

    /**
     * Logs a successful auto-walk engagement with enough trail context to diagnose truncated marker chains.
     *
     * @param pathId     the path being followed
     * @param chapterId  the chapter being followed
     * @param trailNodes the ordered trail nodes available at engagement time
     */
    private static void logEngaged(String pathId, String chapterId, List<Vec3> trailNodes) {
        Vec3 firstNode = trailNodes.getFirst();
        Vec3 lastNode = trailNodes.getLast();
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
     * Requests the next camera frame begin recentering without waiting for the idle delay.
     */
    public static void requestImmediateRecenter() {
        CAMERA.requestImmediateRecenter();
    }

    /**
     * Advances the player along the active trail when auto-walk is enabled.
     *
     * @param client the current Minecraft client
     */
    public static void tick(Minecraft client) {
        if (!active) return;

        LocalPlayer player = client.player;
        if (player == null || client.level == null) {
            cancel(StopReason.PLAYER_GONE);
            return;
        }

        StopReason reason = cancelReason(client, player);
        if (reason != null) {
            cancel(reason);
            return;
        }

        String currentChapterId = ArdaPathsClient.CONFIG.getCurrentChapterId();
        if (currentChapterId == null || currentChapterId.isBlank()) {
            cancel(StopReason.NO_CHAPTER_SELECTED);
            return;
        }

        if (!activeChapterId.equals(currentChapterId) && !engage(activePathId, currentChapterId, player.position(), Double.POSITIVE_INFINITY)) {
            cancel(StopReason.CHAPTER_RETARGET_FAILED);
            return;
        }

        ticksSinceRebuild++;
        if (ticksSinceRebuild >= REBUILD_INTERVAL_TICKS) rebuildTrail(player.position());

        AutoWalkTrail.Projection projection = trail.nearestProjection(player.position());
        if (projection == null) {
            cancel(StopReason.NO_TRAIL_PROJECTION);
            return;
        }

        if (projection.distance() > OFF_TRAIL_DISTANCE) {
            cancel(StopReason.OFF_TRAIL);
            return;
        }

        double movement = AnimatedTrail.SPEED * ArdaPathsClient.CONFIG.getAutoWalkSpeedFactor();
        segmentIndex = projection.segmentIndex();
        segmentProgress = projection.segmentLength() * projection.segmentT();

        if (trail.hasReachedEnd(projection, player.position())) {
            if (trail.endIsTerminus()) {
                cancel(StopReason.ARRIVED_AT_TRAIL_END);
                return;
            }

            if (waitAtTruncatedEnd(player)) return;

            cancel(StopReason.TRAIL_NOT_LOADED);
            return;
        }
        truncatedWaitTicks = 0;

        AutoWalkTrail.Cursor cursor = new AutoWalkTrail.Cursor(segmentIndex, segmentProgress);
        Vec3 projected = trail.pointAt(segmentIndex, segmentProgress);
        Vec3 current = player.position();
        Vec3 steeringTarget = trail.steeringTarget(cursor, movement);
        double deltaX = steeringTarget.x - current.x;
        double deltaZ = steeringTarget.z - current.z;
        double horizontalDistance = Math.hypot(deltaX, deltaZ);
        if (horizontalDistance < MIN_SEGMENT_LENGTH) return;

        double directionX = deltaX / horizontalDistance;
        double directionZ = deltaZ / horizontalDistance;
        Vec3 steer = ObstacleNavigator.horizontalDirection(player, projected, directionX, directionZ);
        directionX = steer.x;
        directionZ = steer.z;
        double velocityY = ObstacleNavigator.verticalVelocity(player, directionX, directionZ, player.getDeltaMovement().y);

        player.setDeltaMovement(directionX * movement, velocityY, directionZ * movement);
        CAMERA.setTarget(projected, steeringTarget);
    }

    /**
     * Determines whether player input or selection state should stop auto-walk.
     * Movement key input is ignored while the camera is detached from the player, as in freecam.
     *
     * @param client the current Minecraft client
     * @param player the current client player
     * @return the cancellation reason, or null when auto-walk should continue
     */
    @Nullable
    private static StopReason cancelReason(Minecraft client, LocalPlayer player) {
        if (!isHoldingPathfinder(player)) return StopReason.PATHFINDER_NOT_HELD;
        if (client.screen != null) return StopReason.SCREEN_OPEN;
        if (!activePathId.equals(ArdaPathsClient.CONFIG.getSelectedPathId())) return StopReason.PATH_CHANGED;

        if (!isCameraDetached(client, player)
                && (client.options.keyUp.isDown()
                || client.options.keyDown.isDown()
                || client.options.keyLeft.isDown()
                || client.options.keyRight.isDown()
                || client.options.keyJump.isDown()
                || client.options.keyShift.isDown())) {
            return StopReason.MOVEMENT_KEY_PRESSED;
        }

        return null;
    }

    /**
     * Rebuilds the active trail chain around the player's current position.
     *
     * @param position the player position used to choose the replacement chain
     */
    private static void rebuildTrail(Vec3 position) {
        ticksSinceRebuild = 0;
        AutoWalkTrail nextTrail = AutoWalkTrail.build(activePathId, activeChapterId, position);
        if (!nextTrail.isUsable()) return;

        trail = nextTrail;
    }

    /**
     * Holds position at a truncated chain end while waiting for more trail markers to load.
     *
     * @param player the player waiting at the current chain end
     * @return true while auto-walk should keep waiting
     */
    private static boolean waitAtTruncatedEnd(LocalPlayer player) {
        rebuildTrail(player.position());
        truncatedWaitTicks++;
        stopHorizontalVelocity(player);
        return truncatedWaitTicks <= TRUNCATED_END_TIMEOUT_TICKS;
    }

    /**
     * Checks whether the client view is detached from the player, as in freecam or while
     * spectating another entity.
     *
     * @param client the current Minecraft client
     * @param player the current client player
     * @return true when the rendered camera is not the player's own
     */
    private static boolean isCameraDetached(Minecraft client, LocalPlayer player) {
        Entity camera = client.getCameraEntity();
        return camera != null && camera != player;
    }

    /**
     * Smoothly rotates the camera toward the latest trail target once per rendered frame.
     */
    public static void renderCameraFrame() {
        if (active) CAMERA.renderFrame();
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
}

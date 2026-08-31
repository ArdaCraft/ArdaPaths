package space.ajcool.ardapaths.paths.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Camera state machine used while auto-walk is active.
 */
final class AutoWalkCamera {

    /** Time without mouse look before auto-walk begins recentering the camera. */
    private static final long CAMERA_RECENTER_DELAY_MILLIS = 4_000L;

    /** Wall-clock duration used for the eased camera recenter. */
    private static final long CAMERA_RECENTER_DURATION_MILLIS = 1_000L;

    /** Minimum yaw or pitch delta treated as manual look input. */
    private static final float LOOK_INPUT_EPSILON = 0.05F;

    /** Render-frame clock used for exponential camera tracking. */
    private final FrameClock frameClock = new FrameClock();

    /** Trail-derived yaw target most recently computed by the movement tick. */
    private float targetYaw = 0.0F;

    /** Trail-derived pitch target most recently computed by the movement tick. */
    private float targetPitch = 0.0F;

    /** Whether a valid camera target is available for the end-of-tick camera driver. */
    private boolean hasTarget = false;

    /** Last yaw value accepted or written by auto-walk camera handling. */
    private float lastAppliedYaw = 0.0F;

    /** Last pitch value accepted or written by auto-walk camera handling. */
    private float lastAppliedPitch = 0.0F;

    /** Whether the last applied yaw and pitch contain player-relative values. */
    private boolean hasAppliedCamera = false;

    /** Wall-clock time when user mouse look was last detected. */
    private long lastLookInputTime = 0L;

    /** Wall-clock time when the current camera recenter started, or zero when idle. */
    private long recenterStartMillis = 0L;

    /** Player yaw captured when the current recenter started. */
    private float recenterStartYaw = 0.0F;

    /** Player pitch captured when the current recenter started. */
    private float recenterStartPitch = 0.0F;

    /**
     * Creates an idle auto-walk camera controller.
     */
    AutoWalkCamera() {
    }

    /**
     * Stores a stable trail-tangent camera target for the end-of-tick camera driver.
     *
     * @param from projected trail point used as the tangent start
     * @param to   lookahead trail point used as the tangent end
     */
    void setTarget(Vec3 from, Vec3 to) {
        double deltaX = to.x - from.x;
        double deltaZ = to.z - from.z;
        if ((deltaX * deltaX) + (deltaZ * deltaZ) < 0.000001D) return;

        targetYaw = (float) (Mth.atan2(deltaZ, deltaX) * (180.0D / Math.PI)) - 90.0F;
        targetPitch = 0.0F;
        hasTarget = true;
    }

    /**
     * Smoothly rotates the camera toward the latest trail target once per rendered frame.
     */
    void renderFrame() {
        if (!hasTarget) return;

        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null) return;

        if (FocusController.isEngaged()) {
            lastAppliedYaw = player.getYRot();
            lastAppliedPitch = player.getXRot();
            hasAppliedCamera = true;
            lastLookInputTime = System.currentTimeMillis();
            recenterStartMillis = 0L;
            return;
        }

        float currentYaw = player.getYRot();
        float currentPitch = player.getXRot();
        long now = System.currentTimeMillis();
        double frameSeconds = frameClock.deltaSeconds();

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
            float eased = (float) CameraEasing.easeInAlpha(progress);
            nextYaw = CameraEasing.blend(recenterStartYaw, targetYaw, eased);
            nextPitch = CameraEasing.blend(recenterStartPitch, targetPitch, eased);
        } else {
            float alpha = (float) CameraEasing.exponentialAlpha(frameSeconds);
            nextYaw = CameraEasing.blend(currentYaw, targetYaw, alpha);
            nextPitch = CameraEasing.blend(currentPitch, targetPitch, alpha);
        }

        CameraEasing.applyCamera(player, nextYaw, nextPitch);
        lastAppliedYaw = nextYaw;
        lastAppliedPitch = nextPitch;
        hasAppliedCamera = true;
    }

    /**
     * Resets free-look tracking so a new auto-walk engagement can steer immediately.
     *
     * @param player player whose current camera should be treated as the baseline
     */
    void reset(@Nullable LocalPlayer player) {
        if (player != null) {
            lastAppliedYaw = player.getYRot();
            lastAppliedPitch = player.getXRot();
            hasAppliedCamera = true;
        } else {
            hasAppliedCamera = false;
        }

        lastLookInputTime = 0L;
        recenterStartMillis = 0L;
        recenterStartYaw = 0.0F;
        recenterStartPitch = 0.0F;
        frameClock.reset();
        hasTarget = false;
    }

    /**
     * Clears all camera target and free-look state after auto-walk stops.
     */
    void clear() {
        targetYaw = 0.0F;
        targetPitch = 0.0F;
        hasTarget = false;
        lastAppliedYaw = 0.0F;
        lastAppliedPitch = 0.0F;
        hasAppliedCamera = false;
        lastLookInputTime = 0L;
        recenterStartMillis = 0L;
        recenterStartYaw = 0.0F;
        recenterStartPitch = 0.0F;
        frameClock.reset();
    }

    /**
     * Requests the next camera frame begin recentering without waiting for the idle delay.
     */
    void requestImmediateRecenter() {
        lastLookInputTime = 0L;
        recenterStartMillis = 0L;
    }

    /**
     * Determines whether the current view differs from auto-walk's last accepted camera value.
     *
     * @param currentYaw   player's current yaw
     * @param currentPitch player's current pitch
     * @return true when the camera changed enough to count as mouse look
     */
    private boolean hasManualLookInput(float currentYaw, float currentPitch) {
        return Math.abs(Mth.wrapDegrees(currentYaw - lastAppliedYaw)) > LOOK_INPUT_EPSILON
                || Math.abs(currentPitch - lastAppliedPitch) > LOOK_INPUT_EPSILON;
    }
}

package space.ajcool.ardapaths.paths.movement;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import space.ajcool.ardapaths.mc.items.ModItems;

/**
 * Client-side camera controller for authored marker focus targets.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FocusController {

    /** Maximum squared distance from a marker that can offer a focus target. */
    public static final double FOCUS_PROMPT_RANGE_SQUARED = 100.0D;

    /** Wall-clock duration used for focus easing. */
    private static final long FOCUS_EASE_MILLIS = 250;

    /** Render-frame clock used for exponential focus holding. */
    private static final FrameClock FRAME_CLOCK = new FrameClock();

    /** Current phase of the focus camera state machine. */
    private static Phase phase = Phase.IDLE;

    /** Nearest in-range look-at target discovered by trail rendering. */
    @Nullable
    private static BlockPos candidate = null;

    /** Look-at target latched for the current focus engagement. */
    @Nullable
    private static BlockPos focusTarget = null;

    /** Key state observed on the previous client tick. */
    private static boolean held = false;

    /** Wall-clock time when the current phase began. */
    private static long phaseStartMillis = 0L;

    /** Player yaw captured when the current ease began. */
    private static float phaseStartYaw = 0.0F;

    /** Player pitch captured when the current ease began. */
    private static float phaseStartPitch = 0.0F;

    /** Player yaw captured at focus key-down for the return ease. */
    private static float returnYaw = 0.0F;

    /** Player pitch captured at focus key-down for the return ease. */
    private static float returnPitch = 0.0F;

    /**
     * Updates the current marker focus candidate.
     *
     * @param nextCandidate nearest in-range look-at position, or null when none is available
     */
    public static void setCandidate(@Nullable BlockPos nextCandidate) {
        candidate = nextCandidate == null ? null : nextCandidate.immutable();
        if (held && (phase == Phase.EASING_IN || phase == Phase.HOLDING) && !samePosition(candidate, focusTarget)) {
            beginEaseOut();
        }
    }

    /**
     * Compares two nullable block positions by value.
     *
     * @param first  first position
     * @param second second position
     * @return true when both positions are non-null and equal
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean samePosition(@Nullable BlockPos first, @Nullable BlockPos second) {
        return first != null && first.equals(second);
    }

    /**
     * Begins restoring the camera to the view held when Focus was pressed.
     */
    private static void beginEaseOut() {
        if (phase == Phase.IDLE || phase == Phase.EASING_OUT) return;

        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null) {
            reset();
            return;
        }

        phaseStartYaw = player.getYRot();
        phaseStartPitch = player.getXRot();
        phaseStartMillis = System.currentTimeMillis();
        FRAME_CLOCK.reset();
        phase = Phase.EASING_OUT;
    }

    /**
     * Resets focus camera and input state after leaving a world or server.
     */
    public static void reset() {
        phase = Phase.IDLE;
        candidate = null;
        focusTarget = null;
        held = false;
        phaseStartMillis = 0L;
        phaseStartYaw = 0.0F;
        phaseStartPitch = 0.0F;
        returnYaw = 0.0F;
        returnPitch = 0.0F;
        FRAME_CLOCK.reset();
    }

    /**
     * Checks whether an in-range focus target is available.
     *
     * @return true when the player can press Focus for a look-at target
     */
    public static boolean hasCandidate() {
        return candidate != null;
    }

    /**
     * Updates the held Focus key state and reacts to key edges.
     *
     * @param nextHeld true while the Focus key is pressed
     */
    public static void setHeld(boolean nextHeld) {
        if (nextHeld == held) return;

        held = nextHeld;
        if (held) {
            beginFocusOrRecenter();
            return;
        }

        if (phase == Phase.EASING_IN || phase == Phase.HOLDING) {
            beginEaseOut();
        }
    }

    /**
     * Begins either an authored focus or an immediate auto-walk recenter.
     */
    private static void beginFocusOrRecenter() {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null) return;
        if (client.screen != null || !player.isHolding(ModItems.PATH_REVEALER)) return;

        if (candidate != null) {
            returnYaw = player.getYRot();
            returnPitch = player.getXRot();
            focusTarget = candidate.immutable();
            phaseStartYaw = returnYaw;
            phaseStartPitch = returnPitch;
            phaseStartMillis = System.currentTimeMillis();
            FRAME_CLOCK.reset();
            phase = Phase.EASING_IN;
        } else if (AutoWalker.isActive()) {
            AutoWalker.requestImmediateRecenter();
        }
    }

    /**
     * Checks whether focus is currently controlling or restoring the camera.
     *
     * @return true for every state except idle
     */
    public static boolean isEngaged() {
        return phase != Phase.IDLE;
    }

    /**
     * Advances and applies one rendered camera frame for the active focus phase.
     */
    public static void renderCameraFrame() {
        if (phase == Phase.IDLE) return;

        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null) {
            reset();
            return;
        }

        if (shouldReleaseFocus(client, player) && (phase == Phase.EASING_IN || phase == Phase.HOLDING)) {
            beginEaseOut();
        }

        if (focusTarget == null) {
            phase = Phase.IDLE;
            return;
        }

        long now = System.currentTimeMillis();
        double frameSeconds = FRAME_CLOCK.deltaSeconds();
        Aim aim = aimAt(player, focusTarget);
        double progress = (now - phaseStartMillis) / (double) FOCUS_EASE_MILLIS;
        float nextYaw;
        float nextPitch;

        if (phase == Phase.EASING_IN) {
            float eased = (float) CameraEasing.easeInAlpha(progress);
            nextYaw = CameraEasing.blend(phaseStartYaw, aim.yaw(), eased);
            nextPitch = CameraEasing.blend(phaseStartPitch, aim.pitch(), eased);
            if (progress >= 1.0D) {
                phase = Phase.HOLDING;
            }
        } else if (phase == Phase.HOLDING) {
            float alpha = (float) CameraEasing.exponentialAlpha(frameSeconds);
            nextYaw = CameraEasing.blend(player.getYRot(), aim.yaw(), alpha);
            nextPitch = CameraEasing.blend(player.getXRot(), aim.pitch(), alpha);
        } else {
            float eased = (float) CameraEasing.easeOutAlpha(progress);
            nextYaw = CameraEasing.blend(phaseStartYaw, returnYaw, eased);
            nextPitch = CameraEasing.blend(phaseStartPitch, returnPitch, eased);
            if (progress >= 1.0D) {
                CameraEasing.applyCamera(player, nextYaw, nextPitch);
                phase = Phase.IDLE;
                focusTarget = null;
                FRAME_CLOCK.reset();
                return;
            }
        }

        CameraEasing.applyCamera(player, nextYaw, nextPitch);
    }

    /**
     * Checks whether current client state should end an active focus hold.
     *
     * @param client current Minecraft client
     * @param player current client player
     * @return true when focus should ease out
     */
    private static boolean shouldReleaseFocus(Minecraft client, LocalPlayer player) {
        return !held
                || client.screen != null
                || !player.isHolding(ModItems.PATH_REVEALER)
                || !samePosition(candidate, focusTarget);
    }

    /**
     * Computes the yaw and pitch needed to look from the player eye to a block center.
     *
     * @param player current client player
     * @param target target block position
     * @return aim angles for the target
     */
    private static Aim aimAt(LocalPlayer player, BlockPos target) {
        double tx = target.getX() + 0.5D;
        double ty = target.getY() + 0.5D;
        double tz = target.getZ() + 0.5D;
        double dx = tx - player.getX();
        double dy = ty - player.getEyeY();
        double dz = tz - player.getZ();
        double horizontal = Math.sqrt((dx * dx) + (dz * dz));
        float aimYaw = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
        float aimPitch = (float) (-(Mth.atan2(dy, horizontal) * (180.0D / Math.PI)));
        return new Aim(aimYaw, aimPitch);
    }

    /**
     * Focus camera lifecycle phases.
     */
    private enum Phase {
        /** Focus is not controlling the camera. */
        IDLE,

        /** Camera is easing from the original view to the target. */
        EASING_IN,

        /** Camera is held on the target while the Focus key remains pressed. */
        HOLDING,

        /** Camera is easing back to the original view. */
        EASING_OUT
    }

    /**
     * Yaw and pitch pair for a target aim.
     *
     * @param yaw   yaw angle in degrees
     * @param pitch pitch angle in degrees
     */
    private record Aim(float yaw, float pitch) {

    }
}

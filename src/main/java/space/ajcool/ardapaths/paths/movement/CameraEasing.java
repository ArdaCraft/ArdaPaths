package space.ajcool.ardapaths.paths.movement;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

/**
 * Shared camera interpolation helpers for movement-driven camera controllers.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class CameraEasing {

    /** Exponential camera response rate per second while tracking a target. */
    private static final double CAMERA_RESPONSIVENESS = 10.0D;

    /**
     * Applies a camera orientation to the player and matching previous-frame fields.
     *
     * @param player current client player
     * @param yaw    yaw to apply
     * @param pitch  pitch to apply
     */
    static void applyCamera(LocalPlayer player, float yaw, float pitch) {
        player.setYRot(yaw);
        player.yBodyRot = yaw;
        player.yHeadRot = yaw;
        player.setXRot(pitch);
        player.yRotO = yaw;
        player.yBodyRotO = yaw;
        player.yHeadRotO = yaw;
        player.xRotO = pitch;
    }

    /**
     * Blends from one wrapped angle to another.
     *
     * @param from   starting angle in degrees
     * @param to     target angle in degrees
     * @param factor normalized interpolation factor
     * @return blended angle in degrees
     */
    static float blend(float from, float to, float factor) {
        return from + (Mth.wrapDegrees(to - from) * factor);
    }

    /**
     * Computes a quadratic ease-in alpha.
     *
     * @param progress raw transition progress
     * @return eased alpha clamped to the completed transition
     */
    static double easeInAlpha(double progress) {
        double clamped = Math.min(1.0D, progress);
        return clamped * clamped;
    }

    /**
     * Computes a quadratic ease-out alpha.
     *
     * @param progress raw transition progress
     * @return eased alpha clamped to the completed transition
     */
    static double easeOutAlpha(double progress) {
        double remaining = 1.0D - Math.min(1.0D, progress);
        return 1.0D - (remaining * remaining);
    }

    /**
     * Computes a frame-rate-independent tracking alpha.
     *
     * @param frameSeconds elapsed frame time in seconds
     * @return exponential tracking alpha for the elapsed frame
     */
    static double exponentialAlpha(double frameSeconds) {
        return 1.0D - Math.exp(-CAMERA_RESPONSIVENESS * frameSeconds);
    }
}

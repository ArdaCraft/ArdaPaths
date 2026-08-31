package space.ajcool.ardapaths.paths.movement;

/**
 * Per-owner render clock that reports clamped frame deltas for camera easing.
 */
final class FrameClock {

    /** Maximum elapsed frame time used by camera easing after a pause or hitch. */
    private static final double MAX_CAMERA_FRAME_SECONDS = 0.1D;

    /** Monotonic timestamp of the last rendered camera frame. */
    private long lastFrameNanos = 0L;

    /**
     * Creates an idle frame clock with no previous frame timestamp.
     */
    FrameClock() {
    }

    /**
     * Calculates elapsed render-frame time for frame-rate-independent camera easing.
     *
     * @return clamped elapsed time in seconds
     */
    double deltaSeconds() {
        long frameNanos = System.nanoTime();
        if (lastFrameNanos == 0L) {
            lastFrameNanos = frameNanos;
            return 1.0D / 60.0D;
        }

        long elapsedNanos = Math.max(0L, frameNanos - lastFrameNanos);
        lastFrameNanos = frameNanos;
        return Math.min(MAX_CAMERA_FRAME_SECONDS, elapsedNanos / 1_000_000_000.0D);
    }

    /**
     * Clears the previous frame timestamp so the next frame uses the default delta.
     */
    void reset() {
        lastFrameNanos = 0L;
    }
}

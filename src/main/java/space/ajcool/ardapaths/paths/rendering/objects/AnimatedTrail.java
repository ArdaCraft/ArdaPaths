package space.ajcool.ardapaths.paths.rendering.objects;

import lombok.Getter;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.*;
import space.ajcool.ardapaths.core.data.config.shared.Color;
import space.ajcool.ardapaths.mc.blocks.ModBlocks;
import space.ajcool.ardapaths.mc.particles.PathParticleEffect;

import java.util.Arrays;

/**
 * An animated trail that renders particle effects along a path from start to end position.
 * Handles animation timing, colour cycling, and height adjustment for rendering above/below blocks.
 */
public class AnimatedTrail {
    /**
     * Animation speed factor controlling how fast the trail animates per tick.
     */
    public static final double SPEED = 0.21585D;

    /**
     * Multiplier applied to the sampled player speed while the trail head is catching up to its target lead.
     */
    private static final double SPEED_MARGIN = 1.25D;

    /**
     * Desired horizontal distance in blocks from the player to the trail head before speed matching begins.
     */
    private static final double TARGET_LEAD = 8.0D;

    /**
     * Maximum trail head speed in blocks per tick.
     */
    private static final double MAX_SPEED = 1.0D;

    /**
     * Number of player movement samples retained for adaptive speed calculation.
     */
    private static final int SPEED_SAMPLE_TICKS = 10;

    /**
     * Minimum vertical terrain-following adjustment applied per tick.
     */
    private static final double VERTICAL_MIN_STEP = 0.5D;

    /**
     * Ratio between horizontal trail speed and vertical terrain-following speed.
     */
    private static final double VERTICAL_SPEED_RATIO = 3.0D;

    /**
     * Target distance in blocks between adjacent particles emitted by a moving trail head.
     */
    private static final double PARTICLE_SPACING = SPEED;

    /**
     * Maximum number of particles a single trail segment may emit in one tick.
     */
    private static final int MAX_PARTICLES_PER_TICK = 8;

    /**
     * Recent horizontal player speeds in blocks per tick.
     */
    private static final double[] PLAYER_SPEED_SAMPLES = new double[SPEED_SAMPLE_TICKS];

    /**
     * Next index to overwrite in the player speed sample ring.
     */
    private static int playerSpeedSampleIndex = 0;

    /**
     * Rolling maximum player speed in blocks per tick sampled over the recent movement window.
     */
    private static double playerSpeed = 0.0D;

    /**
     * Starting block position of this trail segment.
     */
    @Getter
    private final BlockPos start;

    /**
     * End position (in world coordinates) of this trail segment.
     */
    @Getter
    private final Vec3d end;

    /**
     * Center position of the trail start, reused for interpolation.
     */
    @Getter
    private final Vec3d startPos;

    /**
     * Total immutable distance travelled by this trail.
     */
    private final double totalDistance;

    /**
     * Whether to render this trail above blocks or following terrain.
     */
    private final boolean aboveBlocks;

    /**
     * Primary colour for the trail particles (RGB as integer).
     */
    private final int primaryColor;

    /**
     * Secondary colour for the trail particles (RGB as integer).
     */
    private final int secondaryColor;

    /**
     * Tertiary colour for the trail particles (RGB as integer).
     */
    private final int tertiaryColor;

    /**
     * Current animated position along the trail.
     */
    @Getter
    private Vec3d currentPos;

    /**
     * Current rendered position, accounting for terrain height adjustments.
     */
    @Getter
    private Vec3d currentRenderPos;

    /**
     * Current terrain-following render height for above-block trails.
     */
    private double renderY;

    /**
     * Distance this trail head has travelled along its segment.
     */
    private double distanceTravelled;

    /**
     * Whether above-block terrain height has been resolved at least once.
     */
    private boolean renderYInitialized;

    /**
     * Cached terrain column X for above-block rendering.
     */
    private int cachedColumnX = Integer.MIN_VALUE;

    /**
     * Cached terrain column Z for above-block rendering.
     */
    private int cachedColumnZ = Integer.MIN_VALUE;

    /**
     * Cached render Y resolved for the current terrain column.
     */
    private double cachedGroundY = Double.NaN;

    /**
     * Private constructor for trail creation.
     * Use the static factory method {@link #from} instead.
     *
     * @param start          the starting block position
     * @param end            the ending position
     * @param aboveBlocks    whether to render above blocks
     * @param primaryColor   primary colour as integer
     * @param secondaryColor secondary colour as integer
     * @param tertiaryColor  tertiary colour as integer
     */
    private AnimatedTrail(BlockPos start, Vec3d end, boolean aboveBlocks, int primaryColor, int secondaryColor, int tertiaryColor) {
        this.start = start;
        this.end = end;
        this.startPos = new Vec3d(start.getX(), start.getY(), start.getZ()).add(0.5, 0.5, 0.5);
        this.totalDistance = aboveBlocks ? FlattenedDistance(startPos, end) : startPos.distanceTo(end);
        this.currentPos = startPos;
        this.currentRenderPos = startPos;
        this.renderY = startPos.y;
        this.aboveBlocks = aboveBlocks;
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
        this.tertiaryColor = tertiaryColor;
        this.distanceTravelled = 0.0D;
        this.renderYInitialized = false;
    }

    /**
     * Create a new animated trail.
     *
     * @param start the starting position
     * @param offset the offset from the starting position
     * @param aboveBlocks whether the trail should render above blocks
     * @param colors the colour of the trail
     * @return a new animated trail instance
     */
    public static AnimatedTrail from(BlockPos start, BlockPos offset, boolean aboveBlocks, Color[] colors) {
        if (colors.length != 3) {
            colors = new Color[3];
            colors[0] = colors[1] = colors[2] = Color.fromRgb(100, 100, 100);
        }

        return new AnimatedTrail(
                start,
                new Vec3d(
                        start.getX() + offset.getX(),
                        start.getY() + offset.getY(),
                        start.getZ() + offset.getZ()
                ).add(0.5, 0.5, 0.5),
                aboveBlocks,
                colors[0].asHex(),
                colors[1].asHex(),
                colors[2].asHex()
        );
    }

    /**
     * Updates the adaptive trail speed sample from the player's horizontal displacement this tick.
     *
     * @param player the client player whose achieved movement should drive trail catch-up speed
     */
    public static void updatePlayerSpeed(ClientPlayerEntity player) {
        double horizontalDeltaX = player.getX() - player.prevX;
        double horizontalDeltaZ = player.getZ() - player.prevZ;
        PLAYER_SPEED_SAMPLES[playerSpeedSampleIndex] = Math.hypot(horizontalDeltaX, horizontalDeltaZ);
        playerSpeedSampleIndex = (playerSpeedSampleIndex + 1) % PLAYER_SPEED_SAMPLES.length;

        double sampledMax = 0.0D;
        for (double sample : PLAYER_SPEED_SAMPLES) {
            sampledMax = Math.max(sampledMax, sample);
        }
        playerSpeed = sampledMax;
    }

    /**
     * Clears adaptive trail speed samples after all trail state has been reset.
     */
    public static void resetPlayerSpeed() {
        Arrays.fill(PLAYER_SPEED_SAMPLES, 0.0D);
        playerSpeedSampleIndex = 0;
        playerSpeed = 0.0D;
    }

    /**
     * Returns the normalized projection point on this trail segment nearest to the supplied position.
     *
     * @param px the probe x coordinate
     * @param py the probe y coordinate
     * @param pz the probe z coordinate
     * @return the clamped segment parameter in the range {@code [0, 1]}
     */
    public double closestSegmentT(double px, double py, double pz) {
        double segmentX = end.x - startPos.x;
        double segmentY = end.y - startPos.y;
        double segmentZ = end.z - startPos.z;
        double segmentLengthSquared = (segmentX * segmentX) + (segmentY * segmentY) + (segmentZ * segmentZ);

        if (segmentLengthSquared == 0.0D) {
            return 0.0D;
        }

        double relativeX = px - startPos.x;
        double relativeY = py - startPos.y;
        double relativeZ = pz - startPos.z;
        double projection = ((relativeX * segmentX) + (relativeY * segmentY) + (relativeZ * segmentZ)) / segmentLengthSquared;

        return MathHelper.clamp(projection, 0.0D, 1.0D);
    }

    /**
     * Returns the normalized projection point on a horizontal segment nearest to the supplied position.
     *
     * @param px the probe x coordinate
     * @param pz the probe z coordinate
     * @param sx the segment start x coordinate
     * @param sz the segment start z coordinate
     * @param ex the segment end x coordinate
     * @param ez the segment end z coordinate
     * @return the clamped segment parameter in the range {@code [0, 1]}
     */
    public static double closestSegmentT(double px, double pz, double sx, double sz, double ex, double ez) {
        double segmentX = ex - sx;
        double segmentZ = ez - sz;
        double segmentLengthSquared = (segmentX * segmentX) + (segmentZ * segmentZ);

        if (segmentLengthSquared == 0.0D)
            return 0.0D;

        double relativeX = px - sx;
        double relativeZ = pz - sz;
        double projection = ((relativeX * segmentX) + (relativeZ * segmentZ)) / segmentLengthSquared;

        return MathHelper.clamp(projection, 0.0D, 1.0D);
    }

    /**
     * Render the current trail.
     *
     * @param level     The client world
     * @param playerPos The current player position used to keep the trail head ahead of movement
     */
    public void render(ClientWorld level, Vec3d playerPos) {
        Vec3d previousRenderPos = currentRenderPos;
        double thisTickSpeed = speedFor(horizontalDistance(playerPos, currentRenderPos));

        distanceTravelled += thisTickSpeed;

        double animationPoint = totalDistance == 0 ? 1.0D : distanceTravelled / totalDistance;
        animationPoint = MathHelper.clamp(animationPoint, 0.0D, 1.0D);

        currentPos = startPos.lerp(end, animationPoint);

        if (aboveBlocks) {
            double posY = resolveGroundY(level);

            if (!Double.isNaN(posY)) {
                if (!renderYInitialized) {
                    renderY = posY;
                    renderYInitialized = true;
                    previousRenderPos = new Vec3d(previousRenderPos.x, renderY, previousRenderPos.z);
                } else {
                    renderY = approach(renderY, posY, Math.max(VERTICAL_MIN_STEP, thisTickSpeed * VERTICAL_SPEED_RATIO));
                }
            }

            currentRenderPos = new Vec3d(currentPos.x, renderY, currentPos.z);
        } else {
            currentRenderPos = new Vec3d(currentPos.x, currentPos.y, currentPos.z);
        }

        emitParticles(level, previousRenderPos, currentRenderPos);
    }

    /**
     * Advances this trail by distance carried over from a completed previous segment.
     *
     * @param blocks the number of segment-handoff blocks to add to the travelled distance
     */
    public void advanceBy(double blocks) {
        distanceTravelled += Math.max(0.0D, blocks);
    }

    /**
     * Returns the distance travelled beyond this trail segment's end.
     *
     * @return the excess travelled distance in blocks
     */
    public double overshoot() {
        return Math.max(0.0D, distanceTravelled - totalDistance);
    }

    /**
     * Selects this tick's trail speed based on the lead over the player.
     *
     * @param lead the current horizontal distance from the player to the trail head
     * @return the trail speed to apply this tick in blocks
     */
    private double speedFor(double lead) {
        double desired = lead >= TARGET_LEAD ? playerSpeed : playerSpeed * SPEED_MARGIN;

        return MathHelper.clamp(Math.max(SPEED, desired), SPEED, MAX_SPEED);
    }

    /**
     * Emits enough particles between two rendered positions to preserve trail density at higher speeds.
     *
     * @param level    the client world receiving the particles
     * @param previous the previous rendered head position
     * @param current  the current rendered head position
     */
    private void emitParticles(ClientWorld level, Vec3d previous, Vec3d current) {
        double stepDistance = previous.distanceTo(current);
        int steps = MathHelper.clamp((int) Math.ceil(stepDistance / PARTICLE_SPACING), 1, MAX_PARTICLES_PER_TICK);

        for (int i = 1; i <= steps; i++) {
            Vec3d particlePos = previous.lerp(current, (double) i / steps);
            level.addParticle(new PathParticleEffect(primaryColor, secondaryColor, tertiaryColor),
                    particlePos.x,
                    particlePos.y + 0.3,
                    particlePos.z,
                    0.0, 0.0, 0.0
            );
        }
    }

    /**
     * Moves a scalar value toward a target by no more than the supplied step.
     *
     * @param current the current value
     * @param target  the target value
     * @param step    the maximum distance to move this tick
     * @return the adjusted value
     */
    private double approach(double current, double target, double step) {
        double delta = target - current;
        if (Math.abs(delta) <= step) {
            return target;
        }

        return current + (Math.signum(delta) * step);
    }

    /**
     * Measures horizontal distance between two world positions.
     *
     * @param first  the first world position
     * @param second the second world position
     * @return the distance across the X and Z axes
     */
    private double horizontalDistance(Vec3d first, Vec3d second) {
        return Math.hypot(first.x - second.x, first.z - second.z);
    }

    /**
     * Resolves and caches the terrain render height for the current XZ column.
     *
     * @param level the client world to inspect
     * @return the render Y for the current column, or NaN if no nearby ground was found
     */
    private double resolveGroundY(ClientWorld level) {
        int columnX = MathHelper.floor(currentPos.x);
        int columnY = MathHelper.floor(currentPos.y);
        int columnZ = MathHelper.floor(currentPos.z);

        if (columnX == cachedColumnX && columnZ == cachedColumnZ) {
            return cachedGroundY;
        }

        cachedColumnX = columnX;
        cachedColumnZ = columnZ;
        cachedGroundY = scanGroundY(level, columnX, columnY, columnZ);
        return cachedGroundY;
    }

    /**
     * Scans near the current trail position to find the top of the terrain column.
     *
     * @param level   the client world to inspect
     * @param columnX the current block column x coordinate
     * @param columnY the current block y coordinate
     * @param columnZ the current block column z coordinate
     * @return the render Y for this terrain column, or NaN if no nearby ground was found
     */
    public static double scanGroundY(ClientWorld level, int columnX, int columnY, int columnZ) {
        BlockPos.Mutable mutableScanPos = new BlockPos.Mutable();
        mutableScanPos.set(columnX, columnY, columnZ);

        var currentBlockState = level.getBlockState(mutableScanPos);
        var inAir = currentBlockState.isAir() || currentBlockState.isOf(ModBlocks.PATH_MARKER);

        for (int i = 0; i <= 10; i++) {
            int checkY = columnY + (inAir ? -i : i);
            mutableScanPos.set(columnX, checkY, columnZ);
            var checkBlockState = level.getBlockState(mutableScanPos);

            if (inAir && (checkBlockState.isAir() || checkBlockState.isOf(ModBlocks.PATH_MARKER))) continue;

            if (!inAir) {
                if (!checkBlockState.isAir() && !checkBlockState.isOf(ModBlocks.PATH_MARKER)) continue;

                mutableScanPos.set(columnX, checkY - 1, columnZ);
                checkBlockState = level.getBlockState(mutableScanPos);
            }

            var voxelShape = checkBlockState.getOutlineShape(level, mutableScanPos);
            var max = voxelShape.getMax(Direction.Axis.Y);

            return mutableScanPos.getY() + (max > 0 ? max : 1);
        }

        return Double.NaN;
    }

    /**
     * @return True if the trail has reached the end, otherwise false
     */
    public boolean isAtEnd() {
        return currentPos.equals(end);
    }

    /**
     * Calculate the distance between two Vec3D along only the X and Z axes.
     *
     * @param start the starting position
     * @param end the ending position
     * @return the flattened distance between the two positions
     */
    public double FlattenedDistance(Vec3d start, Vec3d end) {
        var start2D = new Vec2f((float) start.getX(), (float) start.getZ());
        var end2D = new Vec2f((float) end.getX(), (float) end.getZ());

        return Math.sqrt(start2D.distanceSquared(end2D));
    }

}

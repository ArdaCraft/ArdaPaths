package space.ajcool.ardapaths.paths.rendering;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import space.ajcool.ardapaths.mc.particles.ModParticles;
import space.ajcool.ardapaths.config.shared.Color;

public class AnimatedTrail {
    private static final double SPEED = 0.3;
    private final BlockPos start;
    private final Vec3d end;
    private final int color;
    private Vec3d currentPos;
    private double tick;

    private AnimatedTrail(BlockPos start, Vec3d end, int color) {
        this.start = start;
        this.end = end;
        this.currentPos = new Vec3d(start.getX(), start.getY(), start.getZ());
        this.color = color;
        this.tick = 0;
    }

    /**
     * Create a new animated trail.
     *
     * @param start The starting position
     * @param offset The offset from the starting position
     * @param color The color of the trail
     */
    public static AnimatedTrail from(BlockPos start, BlockPos offset, Color color) {
        return new AnimatedTrail(
                start,
                new Vec3d(
                    start.getX() + offset.getX(),
                    start.getY() + offset.getY(),
                    start.getZ() + offset.getZ()
                ).add(0.5, 0.5, 0.5),
                color.asHex()
        );
    }

    /**
     * Render the current trail.
     *
     * @param level The client world
     */
    public void render(ClientWorld level) {
        if (isAtEnd()) {
            tick = 0;
        }

        Vec3d startPos = new Vec3d(start.getX(), start.getY(), start.getZ())
                .add(0.5, 0.5, 0.5);
        double totalDistance = startPos.distanceTo(end);

        double fraction = totalDistance == 0 ? 1.0 : (tick * SPEED) / totalDistance;
        if (fraction > 1.0) {
            fraction = 1.0;
        }

        currentPos = startPos.lerp(end, fraction);

        level.addParticle(ModParticles.PATH,
                currentPos.x,
                currentPos.y + 0.3,
                currentPos.z,
                color, color, color
        );

        tick++;
    }

    /**
     * Get the starting position of the trail.
     */
    public BlockPos getStart() {
        return start;
    }

    /**
     * Get the current position of the trail.
     */
    public Vec3d getCurrentPos() {
        return currentPos;
    }

    /**
     * @return True if the trail has reached the end, otherwise false
     */
    public boolean isAtEnd() {
        return currentPos.equals(end);
    }
}

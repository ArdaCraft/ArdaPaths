package space.ajcool.ardapaths.paths.rendering.objects;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.*;
import space.ajcool.ardapaths.core.data.config.shared.Color;
import space.ajcool.ardapaths.mc.blocks.ModBlocks;
import space.ajcool.ardapaths.mc.particles.ModParticles;

public class AnimatedTrail
{
    private static final double SPEED = 0.21585D;
    private final BlockPos start;
    private final Vec3d end;
    private final boolean aboveBlocks;
    private final int primaryColor;
    private final int secondaryColor;
    private final int tertiaryColor;
    private Vec3d currentPos;
    private Vec3d currentIntermediatePos;
    private Vec3d currentRenderPos;
    private Vec3d targetRenderPos;
    private double ticksAlive;
    private double intermediateTicksAlive;

    private AnimatedTrail(BlockPos start, Vec3d end, boolean aboveBlocks, int primaryColor, int secondaryColor, int tertiaryColor)
    {
        this.start = start;
        this.end = end;
        this.currentPos = new Vec3d(start.getX(), start.getY(), start.getZ());
        this.currentRenderPos = new Vec3d(start.getX(), start.getY(), start.getZ());
        this.currentIntermediatePos = null;
        this.targetRenderPos = null;
        this.aboveBlocks = aboveBlocks;
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
        this.tertiaryColor = tertiaryColor;
        this.ticksAlive = 0;
        this.intermediateTicksAlive = 0;
    }

    /**
     * Create a new animated trail.
     *
     * @param start  The starting position
     * @param offset The offset from the starting position
     * @param colors The color of the trail
     */
    public static AnimatedTrail from(BlockPos start, BlockPos offset, boolean aboveBlocks, Color[] colors)
    {
        if (colors.length != 3)
        {
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
     * Render the current trail.
     *
     * @param level The client world
     */
    public void render(ClientWorld level)
    {
        if (isAtEnd()) ticksAlive = 0;

        Vec3d startPos = new Vec3d(start.getX(), start.getY(), start.getZ()).add(0.5, 0.5, 0.5);

        double totalDistance = aboveBlocks ? FlattenedDistance(startPos, end) : startPos.distanceTo(end);
        double animationPoint = totalDistance == 0 ? 1.0D : (ticksAlive * SPEED) / totalDistance;
        animationPoint = MathHelper.clamp(animationPoint, 0.0D, 1.0D);

        currentPos = startPos.lerp(end, animationPoint);

        if (aboveBlocks && targetRenderPos == null && currentIntermediatePos == null)
        {
            BlockPos currentBlockPos = new BlockPos(
                    (int) Math.floor(currentPos.x),
                    (int) Math.floor(currentPos.y),
                    (int) Math.floor(currentPos.z)
            );

            var currentBlockState = level.getBlockState(currentBlockPos);
            var inAir = currentBlockState.isAir() || currentBlockState.isOf(ModBlocks.PATH_MARKER);

            for (int i = 0; i <= 10; i++)
            {
                var checkPos = currentBlockPos.add(new Vec3i(0, inAir ? -i : i, 0));
                var checkBlockPos = new BlockPos(checkPos);
                var checkBlockState = level.getBlockState(checkBlockPos);

                if (inAir && (checkBlockState.isAir() || checkBlockState.isOf(ModBlocks.PATH_MARKER))) continue;

                if (!inAir)
                {
                    if (!checkBlockState.isAir() && !checkBlockState.isOf(ModBlocks.PATH_MARKER)) continue;

                    checkBlockPos = new BlockPos(currentBlockPos.add(new Vec3i(0, i - 1, 0)));
                    checkBlockState = level.getBlockState(checkBlockPos);
                }

                var voxelShape = checkBlockState.getOutlineShape(level, checkBlockPos);
                var max = voxelShape.getMax(Direction.Axis.Y);

                var posY = (checkBlockPos.getY() + (max > 0 ? max : 1));

                boolean intermediateAnim = animationPoint != 0 && (Math.abs(currentRenderPos.y - posY) > 0.5);

                if (intermediateAnim)
                {
                    currentIntermediatePos = new Vec3d(currentRenderPos.x, currentRenderPos.y, currentRenderPos.z);
                    targetRenderPos = new Vec3d(currentRenderPos.x, posY, currentRenderPos.z);
                }
                else
                {
                    currentRenderPos = new Vec3d(currentPos.x, posY, currentPos.z);
                }

                break;
            }
        }
        else
        {
            currentRenderPos = new Vec3d(currentPos.x, currentPos.y, currentPos.z);
        }

        if (targetRenderPos != null && currentIntermediatePos != null)
        {
            double intermediateDistance = currentIntermediatePos.distanceTo(targetRenderPos);
            double intermediatePoint = intermediateDistance == 0 ? 1.0D : (intermediateTicksAlive * SPEED) / intermediateDistance;
            intermediatePoint = MathHelper.clamp(intermediatePoint, 0.0D, 1.0D);

            currentRenderPos = currentIntermediatePos.lerp(targetRenderPos, intermediatePoint);

            intermediateTicksAlive++;

            if (currentRenderPos.equals(targetRenderPos))
            {
                intermediateTicksAlive = 0;
                targetRenderPos = null;
                currentIntermediatePos = null;

                ticksAlive++;
            }
        }
        else
        {
            ticksAlive++;
        }

        level.addParticle(ModParticles.PATH,
                currentRenderPos.x,
                currentRenderPos.y + 0.3,
                currentRenderPos.z,
                primaryColor, secondaryColor, tertiaryColor
        );
    }

    /**
     * Get the starting position of the trail.
     */
    public BlockPos getStart()
    {
        return start;
    }

    /**
     * Get the current position of the trail.
     */
    public Vec3d getCurrentPos()
    {
        return currentPos;
    }

    public Vec3d getCurrentRenderPos()
    {
        return currentRenderPos;
    }

    /**
     * @return True if the trail has reached the end, otherwise false
     */
    public boolean isAtEnd()
    {
        return currentPos.equals(end);
    }

    /**
     * @return The distance between two Vec3D along only the X and Z axes
     */
    public double FlattenedDistance(Vec3d start, Vec3d end)
    {
        var start2D = new Vec2f((float) start.getX(), (float) start.getZ());
        var end2D = new Vec2f((float) end.getX(), (float) end.getZ());

        return Math.sqrt(start2D.distanceSquared(end2D));
    }
}

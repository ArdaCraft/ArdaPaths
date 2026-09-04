package space.ajcool.ardapaths.paths.movement;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;
import space.ajcool.ardapaths.paths.Paths;
import space.ajcool.ardapaths.paths.rendering.objects.AnimatedTrail;

import java.util.*;

/**
 * Immutable ordered trail chain used by auto-walk movement and projection.
 *
 * @param nodes         ordered centered trail nodes
 * @param endIsTerminus true when the last node corresponds to a loaded marker
 */
record AutoWalkTrail(List<Vec3> nodes, boolean endIsTerminus) {

    /** Empty trail returned when no usable marker chain is loaded. */
    private static final AutoWalkTrail EMPTY = new AutoWalkTrail(List.of(), false);

    /** Radius around the last node that counts as reaching the trail end. */
    private static final double ARRIVAL_DISTANCE = 0.45D;

    /** Small distance used to detect exhausted segments. */
    private static final double MIN_SEGMENT_LENGTH = 0.0001D;

    /** Trail distance used to choose the horizontal steering target without changing movement speed. */
    private static final double STEER_LOOKAHEAD_DISTANCE = 2.0D;

    /**
     * Builds a forward marker chain from the trail segment nearest to the player.
     *
     * @param pathId    selected path ID
     * @param chapterId selected chapter ID
     * @param position  player position used to choose a chain
     * @return ordered trail chain, or an empty trail when no segment is loaded
     */
    static AutoWalkTrail build(String pathId, String chapterId, Vec3 position) {
        Map<BlockPos, PathMarkerBlockEntity> markersByPos = new HashMap<>();
        Map<BlockPos, TrailSegment> segmentsByStart = new HashMap<>();
        List<TrailSegment> segments = new ArrayList<>();

        for (PathMarkerBlockEntity marker : Paths.getTickingMarkers()) {
            BlockPos startBlock = marker.getBlockPos().immutable();
            markersByPos.put(startBlock, marker);

            PathMarkerBlockEntity.ChapterNbtData data = marker.getChapterData(pathId, chapterId, false);
            if (data == null || data.getTarget() == null) continue;

            BlockPos endBlock = startBlock.offset(data.getTarget());
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

        if (nearest == null) return EMPTY;

        List<Vec3> orderedNodes = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        TrailSegment current = nearest;
        BlockPos finalEndBlock = nearest.endBlock();
        orderedNodes.add(current.start());

        while (current != null && visited.add(current.startBlock())) {
            orderedNodes.add(current.end());
            finalEndBlock = current.endBlock();
            current = segmentsByStart.get(current.endBlock());
        }

        return new AutoWalkTrail(List.copyOf(orderedNodes), markersByPos.containsKey(finalEndBlock));
    }

    /**
     * Creates a centered world-space vector for a block position.
     *
     * @param position block position
     * @return centered vector
     */
    private static Vec3 center(BlockPos position) {
        return new Vec3(position.getX() + 0.5D, position.getY() + 0.5D, position.getZ() + 0.5D);
    }

    /**
     * Finds the nearest projection of a position onto this trail.
     *
     * @param position position to project
     * @return nearest projection, or null when no segment exists
     */
    @Nullable
    Projection nearestProjection(Vec3 position) {
        if (!isUsable()) return null;

        Projection nearest = null;
        for (int index = 0; index < nodes.size() - 1; index++) {
            Vec3 start = nodes.get(index);
            Vec3 end = nodes.get(index + 1);
            Projection projection = project(position, start, end, index);
            if (nearest == null || projection.distance() < nearest.distance()) {
                nearest = projection;
            }
        }

        return nearest;
    }

    /**
     * Checks whether this trail has enough nodes to steer movement.
     *
     * @return true when at least one segment exists
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean isUsable() {
        return nodes.size() >= 2;
    }

    /**
     * Projects a position onto a single segment using the path's horizontal footprint.
     *
     * @param position     position to project
     * @param start        segment start
     * @param end          segment end
     * @param segmentIndex index of the segment
     * @return projection data for the segment
     */
    private static Projection project(Vec3 position, Vec3 start, Vec3 end, int segmentIndex) {
        double t = AnimatedTrail.closestSegmentT(position.x, position.z, start.x, start.z, end.x, end.z);
        Vec3 projected = start.lerp(end, t);
        double segmentLength = start.distanceTo(end);
        double distanceX = position.x - projected.x;
        double distanceZ = position.z - projected.z;

        return new Projection(segmentIndex, t, segmentLength, Math.hypot(distanceX, distanceZ));
    }

    /**
     * Resolves the trail point used for steering and camera lookahead this tick.
     *
     * @param from     current trail cursor
     * @param movement per-tick movement distance in blocks
     * @return lookahead point, or the final trail node when the lookahead reaches the end
     */
    Vec3 steeringTarget(Cursor from, double movement) {
        Cursor lookaheadCursor = advance(from, Math.max(movement, STEER_LOOKAHEAD_DISTANCE));
        if (lookaheadCursor == null) return lastNode();

        return pointAt(lookaheadCursor.segmentIndex(), lookaheadCursor.segmentProgress());
    }

    /**
     * Advances a cursor by a distance without mutating this trail.
     *
     * @param from     cursor to advance
     * @param distance distance to advance in blocks
     * @return advanced cursor, or null when the trail has ended
     */
    @Nullable
    Cursor advance(Cursor from, double distance) {
        int nextSegmentIndex = from.segmentIndex();
        double nextSegmentProgress = from.segmentProgress() + distance;

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
     * Returns the final node in this trail.
     *
     * @return final centered node
     */
    Vec3 lastNode() {
        return nodes.getLast();
    }

    /**
     * Resolves the world-space point at a cursor location.
     *
     * @param index    segment start index
     * @param progress progress along the segment in blocks
     * @return point at the cursor
     */
    Vec3 pointAt(int index, double progress) {
        Vec3 start = nodes.get(index);
        Vec3 end = nodes.get(index + 1);
        double length = start.distanceTo(end);
        double t = length < MIN_SEGMENT_LENGTH ? 1.0D : Mth.clamp(progress / length, 0.0D, 1.0D);
        return start.lerp(end, t);
    }

    /**
     * Returns the length of a segment in this trail.
     *
     * @param index segment start index
     * @return segment length in blocks
     */
    double segmentLength(int index) {
        return nodes.get(index).distanceTo(nodes.get(index + 1));
    }

    /**
     * Determines whether the player is close enough to the final node of this trail.
     *
     * @param projection player's current projection onto the trail
     * @param position   player's current position
     * @return true when the current chain end has been reached
     */
    boolean hasReachedEnd(Projection projection, Vec3 position) {
        if (projection.segmentIndex() < nodes.size() - 2 || projection.segmentT() < 0.98D) return false;

        Vec3 end = lastNode();
        double distanceX = position.x - end.x;
        double distanceZ = position.z - end.z;
        return Math.hypot(distanceX, distanceZ) <= ARRIVAL_DISTANCE;
    }

    /**
     * Immutable marker trail segment used while building an ordered chain.
     *
     * @param start      centered start position
     * @param end        centered end position
     * @param startBlock marker block at the segment start
     * @param endBlock   block position at the segment end
     */
    private record TrailSegment(Vec3 start, Vec3 end, BlockPos startBlock, BlockPos endBlock) {

        /**
         * Measures the nearest distance from a position to this segment.
         *
         * @param position position to test
         * @return nearest distance in blocks
         */
        private double distanceTo(Vec3 position) {
            return project(position, start, end, 0).distance();
        }
    }

    /**
     * Immutable projection of a position onto a trail segment.
     *
     * @param segmentIndex  segment index in the active node list
     * @param segmentT      normalized projection parameter
     * @param segmentLength segment length in blocks
     * @param distance      distance from the probe position to the segment
     */
    record Projection(int segmentIndex, double segmentT, double segmentLength, double distance) {

    }

    /**
     * Immutable cursor for a pending movement step.
     *
     * @param segmentIndex    segment index in the active node list
     * @param segmentProgress progress along the segment in blocks
     */
    record Cursor(int segmentIndex, double segmentProgress) {

    }
}

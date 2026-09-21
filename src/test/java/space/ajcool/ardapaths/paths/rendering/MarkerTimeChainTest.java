package space.ajcool.ardapaths.paths.rendering;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import space.ajcool.ardapaths.core.data.TimeActivation;
import space.ajcool.ardapaths.core.data.TimeOfDay;
import space.ajcool.ardapaths.screens.widgets.TextValidationError;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests the authored marker time chain rules independently of client rendering state.
 */
class MarkerTimeChainTest {
    /**
     * Verifies the A-F chain selects no time before B, interpolates B-D, holds through E, and ramps at F.
     */
    @SuppressWarnings("DataFlowIssue")
    @Test
    void chainWalksForwardAsAuthored() {
        Chain chain = referenceChain();
        Long lastApplied = null;

        lastApplied = expectNoSource(chain, pos(5), lastApplied);
        lastApplied = expectTime(chain, pos(10), lastApplied, 2000);
        lastApplied = expectTime(chain, pos(15), lastApplied, 3000);
        lastApplied = expectTime(chain, pos(20), lastApplied, 4000);
        lastApplied = expectTime(chain, pos(25), lastApplied, 5500);
        lastApplied = expectTime(chain, pos(30), lastApplied, 7000);
        lastApplied = expectNoSource(chain, pos(35), lastApplied);
        lastApplied = expectNoSource(chain, pos(40), lastApplied);
        lastApplied = expectTime(chain, pos(50), lastApplied, 9000);

        assertEquals(9000, lastApplied);
    }

    /**
     * Verifies walking back from F holds 15:00 until D, then follows the computed chain back to B.
     */
    @SuppressWarnings("DataFlowIssue")
    @Test
    void chainWalksBackwardAsAuthored() {
        Chain chain = referenceChain();
        Long lastApplied = 9000L;

        lastApplied = expectNoSource(chain, pos(45), lastApplied);
        lastApplied = expectNoSource(chain, pos(40), lastApplied);
        lastApplied = expectTime(chain, pos(30), lastApplied, 7000);
        lastApplied = expectTime(chain, pos(25), lastApplied, 5500);
        lastApplied = expectTime(chain, pos(20), lastApplied, 4000);
        lastApplied = expectTime(chain, pos(15), lastApplied, 3000);
        lastApplied = expectTime(chain, pos(10), lastApplied, 2000);
        lastApplied = expectNoSource(chain, pos(5), lastApplied);

        assertEquals(2000, lastApplied);
    }

    /**
     * Verifies a bent terminal chain holds its last value instead of selecting an earlier off-path segment.
     */
    @SuppressWarnings("DataFlowIssue")
    @Test
    void bentComputedChainHasNoSourcePastTerminalArrivalBand() {
        Chain chain = new Chain();
        chain.add("B", block(0, 0), 2000, TimeActivation.COMPUTED, 0, "C");
        chain.add("C", block(10, 0), 4000, TimeActivation.COMPUTED, 0, "D");
        chain.add("D", block(10, 10), 7000, TimeActivation.COMPUTED, 0, null);

        Long lastApplied = 7000L;

        lastApplied = expectNoSource(chain, pos(10, 14), lastApplied);

        assertEquals(7000, lastApplied);
    }

    /**
     * Verifies a later switchback leg is selected instead of a nearby parallel earlier leg.
     */
    @Test
    void switchbackSelectsCurrentParallelLeg() {
        Chain chain = new Chain();
        chain.add("B", block(0, 0), 2000, TimeActivation.COMPUTED, 0, "C");
        chain.add("C", block(20, 0), 6000, TimeActivation.COMPUTED, 0, "D");
        chain.add("D", block(20, 10), 8000, TimeActivation.COMPUTED, 0, "E");
        chain.add("E", block(0, 10), 12000, TimeActivation.COMPUTED, 0, null);

        assertEquals(10000, chain.selectedTime(pos(10, 10), 8000L));
    }

    /**
     * Asserts that the chain has no selected time source at the supplied player position.
     *
     * @param chain       reference chain
     * @param playerPos   player position to test
     * @param lastApplied previously applied time
     * @return unchanged previously applied time
     */
    private static Long expectNoSource(Chain chain, Vec3 playerPos, Long lastApplied) {
        assertNull(chain.selectedTime(playerPos, lastApplied));
        return lastApplied;
    }

    /**
     * Asserts that the chain selects the expected time at the supplied player position.
     *
     * @param chain       reference chain
     * @param playerPos   player position to test
     * @param lastApplied previously applied time
     * @param expected    expected selected absolute ticks
     * @return expected selected absolute ticks
     */
    private static Long expectTime(Chain chain, Vec3 playerPos, Long lastApplied, long expected) {
        Long selected = chain.selectedTime(playerPos, lastApplied);
        assertEquals(expected, selected);
        return selected;
    }

    /**
     * Verifies computed interpolation crosses midnight without wrapping back to the same day.
     */
    @Test
    void computedSegmentCrossesMidnightForward() throws TextValidationError {
        Chain chain = new Chain();
        chain.add("A", block(0), TimeOfDay.parse("04/09/3006 23:00"), TimeActivation.MARKER_RANGE, 0, "B");
        chain.add("B", block(10), TimeOfDay.parse("05/09/3006 01:00"), TimeActivation.COMPUTED, 0, null);

        assertEquals(TimeOfDay.parse("05/09/3006 00:00"), chain.selectedTime(pos(5), TimeOfDay.parse("04/09/3006 23:00")));
    }

    /**
     * Verifies a bent chain holds the terminal marker's time after leaving the arrival band.
     */
    @Test
    void bentChainDoesNotSelectEarlierLegPastTerminalArrival() {
        Chain chain = new Chain();
        chain.add("B", block(0, 0), 8000, TimeActivation.COMPUTED, 0, "C");
        chain.add("C", block(10, 0), 10000, TimeActivation.COMPUTED, 0, "D");
        chain.add("D", block(10, 10), 13000, TimeActivation.COMPUTED, 0, null);

        assertNull(chain.selectedTime(pos(10, 14), 13000L));
    }

    /**
     * Verifies a nearby earlier parallel leg cannot override the later leg the player is walking.
     */
    @Test
    void switchbackSelectsNearestParallelLeg() {
        Chain chain = new Chain();
        chain.add("B", block(0, 0), 8000, TimeActivation.COMPUTED, 0, "C");
        chain.add("C", block(20, 0), 10000, TimeActivation.COMPUTED, 0, "D");
        chain.add("D", block(20, 10), 12000, TimeActivation.COMPUTED, 0, "E");
        chain.add("E", block(0, 10), 16000, TimeActivation.COMPUTED, 0, null);

        assertEquals(14000, chain.selectedTime(pos(10, 10), 12000L));
    }

    /**
     * Builds the A-F reference marker chain used by the plan.
     *
     * @return reference chain
     */
    private static Chain referenceChain() {
        Chain chain = new Chain();
        chain.add("A", block(0), TimeOfDay.UNSET, TimeActivation.MARKER_RANGE, 0, "B");
        chain.add("B", block(10), 2000, TimeActivation.COMPUTED, 0, "C");
        chain.add("C", block(20), 4000, TimeActivation.COMPUTED, 0, "D");
        chain.add("D", block(30), 7000, TimeActivation.COMPUTED, 0, "E");
        chain.add("E", block(40), TimeOfDay.UNSET, TimeActivation.MARKER_RANGE, 0, "F");
        chain.add("F", block(50), 9000, TimeActivation.MARKER_RANGE, 4, null);
        return chain;
    }

    /**
     * Creates a marker block position along the x axis.
     *
     * @param x x coordinate
     * @return marker block position
     */
    private static BlockPos block(int x) {
        return new BlockPos(x, 0, 0);
    }

    /**
     * Creates a marker block position in the horizontal x-z plane.
     *
     * @param x x coordinate
     * @param z z coordinate
     * @return marker block position
     */
    private static BlockPos block(int x, int z) {
        return new BlockPos(x, 0, z);
    }

    /**
     * Creates a player position at a marker center along the x axis.
     *
     * @param x x coordinate
     * @return precise player position
     */
    private static Vec3 pos(int x) {
        return new Vec3(x + 0.5D, 0.5D, 0.5D);
    }

    /**
     * Creates a player position at a marker center in the horizontal x-z plane.
     *
     * @param x x coordinate
     * @param z z coordinate
     * @return precise player position
     */
    @SuppressWarnings("SameParameterValue")
    private static Vec3 pos(int x, int z) {
        return new Vec3(x + 0.5D, 0.5D, z + 0.5D);
    }

    /**
     * Minimal chain evaluator mirroring {@link EnvironmentController}'s source priority.
     */
    private static final class Chain {
        /**
         * Nodes keyed by marker ID in path order.
         */
        private final Map<String, Node> nodes = new LinkedHashMap<>();

        /**
         * Adds a node to the chain.
         *
         * @param id              node ID
         * @param position        marker position
         * @param timeOfDay       authored absolute ticks
         * @param activation      authored time activation mode
         * @param activationRange marker activation range
         * @param nextId          next node ID
         */
        void add(String id, BlockPos position, long timeOfDay, TimeActivation activation, int activationRange, String nextId) {
            nodes.put(id, new Node(position, timeOfDay, activation, activationRange, nextId));
        }

        /**
         * Selects the desired time at a player position.
         *
         * @param playerPos   player position
         * @param lastApplied last applied time for radial ramp starts
         * @return selected absolute ticks, or null when no source applies
         */
        Long selectedTime(Vec3 playerPos, Long lastApplied) {
            Long radial = radialTime(playerPos, lastApplied);
            if (radial != null) {
                return radial;
            }

            Long segment = segmentTime(playerPos);
            if (segment != null) {
                return segment;
            }

            return markerTime(playerPos);
        }

        /**
         * Selects the closest marker-range radial time source.
         *
         * @param playerPos   player position
         * @param lastApplied last applied time for radial ramp starts
         * @return selected radial time, or null
         */
        private Long radialTime(Vec3 playerPos, Long lastApplied) {
            Candidate<Node> candidate = null;
            for (Node node : nodes.values()) {
                if (node.timeOfDay() == TimeOfDay.UNSET || node.activation() != TimeActivation.MARKER_RANGE) {
                    continue;
                }

                double distanceSquared = playerPos.distanceToSqr(Vec3.atCenterOf(node.position()));
                double range = TimeSourceRules.arrivalRange(node.activationRange());
                if (distanceSquared <= range * range && (candidate == null || distanceSquared < candidate.distanceSquared())) {
                    candidate = new Candidate<>(node, distanceSquared);
                }
            }

            if (candidate == null || lastApplied == null) {
                return null;
            }

            double range = TimeSourceRules.arrivalRange(candidate.value().activationRange());
            double distance = Math.sqrt(playerPos.distanceToSqr(Vec3.atCenterOf(candidate.value().position())));
            double progress = Math.max(0.0D, Math.min(1.0D, (range - distance) / range));
            return shortestTime(lastApplied, candidate.value().timeOfDay(), progress);
        }

        /**
         * Selects the closest active computed segment.
         *
         * @param playerPos player position
         * @return selected segment time, or null
         */
        private Long segmentTime(Vec3 playerPos) {
            Candidate<Segment> candidate = null;
            double trailDistanceSquared = trailDistanceSquared(playerPos);
            for (Node start : nodes.values()) {
                if (start.nextId() == null) {
                    continue;
                }

                Node end = nodes.get(start.nextId());
                if (end == null) {
                    continue;
                }

                TimeSourceRules.SegmentProjection projection = TimeSourceRules.projectOntoSegment(playerPos, start.position(), end.position());
                boolean active = TimeSourceRules.isComputedSegmentActive(
                        start.timeOfDay(),
                        end.timeOfDay(),
                        end.activation(),
                        projection.overshoot(),
                        TimeSourceRules.arrivalRange(start.activationRange()),
                        TimeSourceRules.arrivalRange(end.activationRange()));
                if (active
                        && TimeSourceRules.isNearestTrailElement(projection.distanceSquared(), trailDistanceSquared)
                        && (candidate == null || projection.distanceSquared() < candidate.distanceSquared())) {
                    candidate = new Candidate<>(new Segment(start, end, projection.progress()), projection.distanceSquared());
                }
            }

            return candidate == null
                    ? null
                    : TimeSourceRules.segmentTime(candidate.value().start().timeOfDay(), candidate.value().end().timeOfDay(), candidate.value().progress());
        }

        /**
         * Computes the nearest distance to any collected segment or terminal node.
         *
         * @param playerPos player position
         * @return squared distance to the nearest trail element
         */
        private double trailDistanceSquared(Vec3 playerPos) {
            double nearest = Double.MAX_VALUE;

            for (Node node : nodes.values()) {
                Node next = node.nextId() == null ? null : nodes.get(node.nextId());
                double distanceSquared = next == null
                        ? playerPos.distanceToSqr(Vec3.atCenterOf(node.position()))
                        : TimeSourceRules.projectOntoSegment(playerPos, node.position(), next.position()).distanceSquared();
                nearest = Math.min(nearest, distanceSquared);
            }

            return nearest;
        }

        /**
         * Selects the closest computed marker arrival time.
         *
         * @param playerPos player position
         * @return selected marker time, or null
         */
        private Long markerTime(Vec3 playerPos) {
            Candidate<Node> candidate = null;
            for (Node node : nodes.values()) {
                if (node.timeOfDay() == TimeOfDay.UNSET || node.activation() != TimeActivation.COMPUTED) {
                    continue;
                }

                double distanceSquared = playerPos.distanceToSqr(Vec3.atCenterOf(node.position()));
                double range = TimeSourceRules.arrivalRange(node.activationRange());
                if (distanceSquared <= range * range && (candidate == null || distanceSquared < candidate.distanceSquared())) {
                    candidate = new Candidate<>(node, distanceSquared);
                }
            }

            return candidate == null ? null : candidate.value().timeOfDay();
        }

        /**
         * Interpolates radial time using the controller's shortest-arc ramp.
         *
         * @param startTicks  starting absolute ticks
         * @param targetTicks target absolute ticks
         * @param progress    interpolation progress
         * @return interpolated absolute ticks
         */
        private long shortestTime(long startTicks, long targetTicks, double progress) {
            return startTicks + Math.round((targetTicks - startTicks) * progress);
        }
    }

    /**
     * Chain node data.
     *
     * @param position        marker position
     * @param timeOfDay       authored absolute ticks
     * @param activation      authored time activation mode
     * @param activationRange marker activation range
     * @param nextId          next node ID
     */
    private record Node(BlockPos position, long timeOfDay, TimeActivation activation, int activationRange, String nextId) {
    }

    /**
     * Segment selection data.
     *
     * @param start    segment start node
     * @param end      segment end node
     * @param progress projected progress
     */
    private record Segment(Node start, Node end, double progress) {
    }

    /**
     * Candidate value and selection distance.
     *
     * @param value           candidate payload
     * @param distanceSquared squared selection distance
     * @param <T>             payload type
     */
    private record Candidate<T>(T value, double distanceSquared) {
    }
}

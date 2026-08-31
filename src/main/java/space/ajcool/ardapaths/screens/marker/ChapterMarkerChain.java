package space.ajcool.ardapaths.screens.marker;

import net.minecraft.core.BlockPos;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;
import space.ajcool.ardapaths.paths.Paths;

import java.util.*;

/**
 * Local marker-chain ordering and signature algorithms for one path chapter.
 */
public final class ChapterMarkerChain {
    /**
     * Prevents construction of the static marker-chain helper.
     */
    private ChapterMarkerChain() {
    }

    /**
     * Orders the selected chapter's loaded marker chain that contains the origin marker.
     *
     * @param origin    marker anchoring the local chain
     * @param pathId    path ID whose chapter should be read
     * @param chapterId chapter ID whose marker data should be read
     * @return markers in stable selected-chapter trail order
     */
    public static List<PathMarkerBlockEntity> orderedLocalMarkers(PathMarkerBlockEntity origin, String pathId, String chapterId) {
        Map<BlockPos, PathMarkerBlockEntity> byPos = new LinkedHashMap<>();
        for (PathMarkerBlockEntity marker : Paths.getTickingMarkers()) {
            if (isLinkedToSelectedChapter(origin, marker, pathId, chapterId)) {
                byPos.put(marker.getBlockPos().immutable(), marker);
            }
        }

        BlockPos markerPos = origin.getBlockPos().immutable();
        byPos.put(markerPos, origin);

        Map<BlockPos, BlockPos> nextByPos = buildChapterLinks(byPos, pathId, chapterId);
        Map<BlockPos, List<BlockPos>> prevByPos = new HashMap<>();
        for (Map.Entry<BlockPos, BlockPos> entry : nextByPos.entrySet()) {
            if (byPos.containsKey(entry.getValue())) {
                prevByPos.computeIfAbsent(entry.getValue(), ignored -> new ArrayList<>()).add(entry.getKey());
            }
        }

        List<BlockPos> backward = longestChainBackward(markerPos, prevByPos, new HashMap<>(), new HashSet<>());
        Collections.reverse(backward);
        List<BlockPos> ordered = new ArrayList<>(backward);
        ordered.add(markerPos);
        Set<BlockPos> visited = new HashSet<>(ordered);
        ordered.addAll(chainForward(markerPos, byPos, nextByPos, visited));

        return ordered.stream()
                .map(byPos::get)
                .toList();
    }

    /**
     * Builds a cheap signature for linked marker membership, links, filter state, and selected-chapter content flags.
     *
     * @param origin          marker anchoring the local chain
     * @param pathId          path ID whose chapter should be read
     * @param chapterId       chapter ID whose marker data should be read
     * @param filterSignature marker-list filter signature contribution
     * @return hash-like signature of the current linked local marker state
     */
    public static long signature(PathMarkerBlockEntity origin, String pathId, String chapterId, long filterSignature) {
        long signature = 1125899906842597L;
        Map<BlockPos, PathMarkerBlockEntity> byPos = new HashMap<>();

        for (PathMarkerBlockEntity marker : Paths.getTickingMarkers()) {
            if (isLinkedToSelectedChapter(origin, marker, pathId, chapterId)) {
                byPos.put(marker.getBlockPos().immutable(), marker);
            }
        }

        byPos.put(origin.getBlockPos().immutable(), origin);
        signature = signature * 31 + filterSignature;

        List<BlockPos> positions = new ArrayList<>(byPos.keySet());
        positions.sort(Comparator.comparingLong(BlockPos::asLong));
        for (BlockPos pos : positions) {
            PathMarkerBlockEntity marker = byPos.get(pos);
            PathMarkerBlockEntity.ChapterNbtData data = selectedChapterData(marker, pathId, chapterId);

            signature = signature * 31 + pos.asLong();
            signature = signature * 31 + packedTargetSignature(data.getTarget());
            signature = signature * 31 + (data.isChapterStart() ? 1 : 0);
            signature = signature * 31 + data.getTimeOfDay();
            signature = signature * 31 + data.getWeather();
            signature = signature * 31 + data.getProximityMessage().hashCode();
            signature = signature * 31 + (data.hasMiscData() ? 1 : 0);
        }

        return signature;
    }

    /**
     * Gets selected-chapter marker data without mutating the marker.
     *
     * @param marker    marker whose selected-chapter data should be read
     * @param pathId    path ID whose chapter should be read
     * @param chapterId chapter ID whose marker data should be read
     * @return existing selected-chapter data, or empty data when none exists
     */
    public static PathMarkerBlockEntity.ChapterNbtData selectedChapterData(PathMarkerBlockEntity marker, String pathId, String chapterId) {
        PathMarkerBlockEntity.ChapterNbtData data = marker.getChapterData(pathId, chapterId, false);
        return data == null ? PathMarkerBlockEntity.ChapterNbtData.empty(chapterId) : data;
    }

    /**
     * Builds outgoing links for selected-chapter markers that have target data.
     *
     * @param byPos     selected-chapter marker candidates keyed by block position
     * @param pathId    path ID whose chapter should be read
     * @param chapterId chapter ID whose marker data should be read
     * @return links from each marker position to its absolute target position
     */
    private static Map<BlockPos, BlockPos> buildChapterLinks(Map<BlockPos, PathMarkerBlockEntity> byPos, String pathId, String chapterId) {
        Map<BlockPos, BlockPos> nextByPos = new HashMap<>();

        for (Map.Entry<BlockPos, PathMarkerBlockEntity> entry : byPos.entrySet()) {
            PathMarkerBlockEntity.ChapterNbtData data = selectedChapterData(entry.getValue(), pathId, chapterId);
            if (data.getTarget() != null) {
                nextByPos.put(entry.getKey(), entry.getKey().offset(data.getTarget()));
            }
        }

        return nextByPos;
    }

    /**
     * Walks forward from the edited marker until the selected-chapter target leaves known markers or cycles.
     *
     * @param start     marker position where the forward walk begins
     * @param byPos     selected-chapter marker candidates keyed by block position
     * @param nextByPos outgoing links from marker positions to absolute target positions
     * @param visited   marker positions already claimed by the selected chain
     * @return marker positions after the start in target order
     */
    private static List<BlockPos> chainForward(BlockPos start, Map<BlockPos, PathMarkerBlockEntity> byPos,
                                               Map<BlockPos, BlockPos> nextByPos, Set<BlockPos> visited) {
        List<BlockPos> positions = new ArrayList<>();

        BlockPos current = nextByPos.get(start);
        while (current != null && byPos.containsKey(current) && visited.add(current)) {
            positions.add(current);
            current = nextByPos.get(current);
        }

        return positions;
    }

    /**
     * Finds the longest incoming chain ending at a position, preferring stable position order on ties.
     *
     * @param position  marker position where the backward chain ends
     * @param prevByPos incoming links keyed by target marker position
     * @param memo      previously resolved longest incoming chains
     * @param visiting  positions on the current recursion stack for cycle safety
     * @return predecessor positions from nearest to farthest before the given position
     */
    private static List<BlockPos> longestChainBackward(BlockPos position, Map<BlockPos, List<BlockPos>> prevByPos,
                                                       Map<BlockPos, List<BlockPos>> memo, Set<BlockPos> visiting) {
        if (memo.containsKey(position)) {
            return memo.get(position);
        }

        if (!visiting.add(position)) {
            return List.of();
        }

        List<BlockPos> best = List.of();
        List<BlockPos> predecessors = new ArrayList<>(prevByPos.getOrDefault(position, List.of()));
        predecessors.sort(Comparator.comparingLong(BlockPos::asLong));

        for (BlockPos predecessor : predecessors) {
            if (visiting.contains(predecessor)) {
                continue;
            }

            List<BlockPos> candidate = new ArrayList<>();
            candidate.add(predecessor);
            candidate.addAll(longestChainBackward(predecessor, prevByPos, memo, visiting));
            if (candidate.size() > best.size() || candidate.size() == best.size() && candidate.get(0).asLong() < best.get(0).asLong()) {
                best = candidate;
            }
        }

        visiting.remove(position);
        memo.put(position, best);
        return best;
    }

    /**
     * Packs a target offset into a stable signature value.
     *
     * @param target relative target offset, or null when unset
     * @return stable signature contribution for the target
     */
    private static long packedTargetSignature(BlockPos target) {
        if (target == null) {
            return 0L;
        }

        long signature = 17L;
        signature = signature * 31 + target.getX();
        signature = signature * 31 + target.getY();
        signature = signature * 31 + target.getZ();
        return signature;
    }

    /**
     * Checks whether a marker holds data for the selected path and chapter.
     *
     * @param origin    marker being edited
     * @param marker    marker to inspect
     * @param pathId    path ID whose chapter should be read
     * @param chapterId chapter ID whose marker data should be read
     * @return true if the marker belongs to the chapter currently being edited
     */
    private static boolean isLinkedToSelectedChapter(PathMarkerBlockEntity origin, PathMarkerBlockEntity marker, String pathId, String chapterId) {
        if (marker.getBlockPos().equals(origin.getBlockPos())) return true;

        PathMarkerBlockEntity.ChapterNbtData data = marker.getChapterData(pathId, chapterId, false);
        return data != null && !data.isEmpty();
    }
}

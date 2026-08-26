package space.ajcool.ardapaths.core.backup;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.function.Function;
import java.util.function.ToLongFunction;

/**
 * Shared pacing rules for marker operations that can load cold chunks.
 */
public final class MarkerBatching {
    /** Maximum number of distinct chunks to touch in one server-thread batch. */
    public static final int CHUNKS_PER_BATCH = 4;

    /** Worker-thread pause between marker batches so normal server ticks can run. */
    public static final long BATCH_PACING_MS = 25L;

    /**
     * Prevents utility construction.
     */
    private MarkerBatching() {
    }

    /**
     * Finds a batch endpoint bounded by distinct chunk count.
     *
     * @param markers        ordered marker inputs
     * @param batchStart     first marker index in the batch
     * @param dimensionId    marker dimension extractor
     * @param packedChunkPos marker packed chunk position extractor
     * @param <T>            marker input type
     * @return exclusive batch end index
     */
    public static <T> int findChunkBoundedBatchEnd(List<T> markers, int batchStart, Function<T, String> dimensionId, ToLongFunction<T> packedChunkPos) {
        Set<String> chunks = new HashSet<>();
        int index = batchStart;

        while (index < markers.size()) {
            T marker = markers.get(index);
            String chunk = markerLocation(dimensionId.apply(marker), packedChunkPos.applyAsLong(marker));
            if (!chunks.contains(chunk) && chunks.size() >= CHUNKS_PER_BATCH) break;

            chunks.add(chunk);
            index++;
        }

        return Math.max(index, batchStart + 1);
    }

    /**
     * Pauses the worker between marker batches to give the server room to tick.
     *
     * @param processed processed marker count
     * @param total     total marker count
     */
    public static void paceBetweenBatches(int processed, int total) {
        if (processed >= total) return;

        try {
            Thread.sleep(BATCH_PACING_MS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new CancellationException("marker batch pacing interrupted");
        }
    }

    /**
     * Creates a compact identity for one dimension chunk.
     *
     * @param dimensionId    dimension identifier
     * @param packedChunkPos packed chunk position
     * @return marker chunk identity
     */
    private static String markerLocation(String dimensionId, long packedChunkPos) {
        return dimensionId + ":" + packedChunkPos;
    }
}

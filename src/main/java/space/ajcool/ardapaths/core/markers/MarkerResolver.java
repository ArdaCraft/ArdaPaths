package space.ajcool.ardapaths.core.markers;

import lombok.extern.slf4j.Slf4j;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import space.ajcool.ardapaths.core.data.TimeOfDay;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;

import java.util.*;
import java.util.concurrent.CompletionException;

/**
 * Resolves path markers by loading existing chunks through vanilla chunk storage.
 */
@Slf4j(topic = "ardapaths")
public class MarkerResolver {

    /** World used for live chunk and block-entity lookups. */
    private final ServerLevel world;

    /** Registry identifier for the world being searched. */
    private final String dimensionId;

    /** Request-local cache of persisted chunk existence probes. */
    private final Map<Long, Boolean> chunkExistsCache = new HashMap<>();

    /**
     * Constructs a marker resolver with an empty per-request chunk cache.
     *
     * @param world       world where live markers may be loaded
     * @param dimensionId registry identifier for the searched world
     */
    public MarkerResolver(ServerLevel world, String dimensionId) {
        this.world = world;
        this.dimensionId = dimensionId;
    }

    /**
     * Resolves a marker from loaded world state or an existing persisted chunk.
     *
     * @param pos marker position to resolve
     * @return resolved marker, or empty when absent or unreadable
     */
    public Optional<ResolvedMarker> resolve(BlockPos pos) {
        ChunkPos chunkPos = new ChunkPos(pos);
        boolean loaded = world.getChunkSource().hasChunk(chunkPos.x, chunkPos.z);
        if (!loaded && !chunkExistsOnDisk(chunkPos)) {
            return Optional.empty();
        }

        if (!loaded) {
            world.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.FULL, true);
        }

        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof PathMarkerBlockEntity marker) {
            return Optional.of(ResolvedMarker.loaded(dimensionId, marker));
        }

        return Optional.empty();
    }

    /**
     * Checks whether a chunk has persisted data without creating new terrain.
     *
     * @param chunkPos chunk position to probe
     * @return true when vanilla storage reports saved chunk NBT
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean chunkExistsOnDisk(ChunkPos chunkPos) {
        try {
            return chunkExistsCache.computeIfAbsent(chunkPos.toLong(), ignored ->
                    world.getChunkSource().chunkMap.read(chunkPos).join().isPresent());
        } catch (CompletionException exception) {
            log.warn("Failed to probe ArdaPaths marker chunk {}", chunkPos, exception);
            return false;
        }
    }

    /**
     * Resolves all path markers in a loaded or existing persisted chunk.
     *
     * @param chunkPos chunk position to inspect
     * @return resolved markers present in the chunk
     */
    public List<ResolvedMarker> resolveChunkMarkers(ChunkPos chunkPos) {
        if (!world.getChunkSource().hasChunk(chunkPos.x, chunkPos.z) && !chunkExistsOnDisk(chunkPos)) {
            return List.of();
        }

        ChunkAccess chunk = world.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.FULL, true);
        if (!(chunk instanceof LevelChunk worldChunk)) {
            return List.of();
        }

        List<ResolvedMarker> markers = new ArrayList<>();
        for (BlockEntity blockEntity : worldChunk.getBlockEntities().values()) {
            if (blockEntity instanceof PathMarkerBlockEntity marker) {
                markers.add(ResolvedMarker.loaded(dimensionId, marker));
            }
        }
        return markers;
    }

    /**
     * Marker data resolved from live world state.
     *
     * @param dimensionId dimension identifier containing the marker
     * @param position    marker block position
     * @param liveMarker  loaded marker block entity
     */
    public record ResolvedMarker(String dimensionId, BlockPos position, PathMarkerBlockEntity liveMarker) {

        /**
         * Creates a resolved marker from a loaded block entity.
         *
         * @param dimensionId dimension identifier containing the marker
         * @param marker      loaded marker block entity
         * @return resolved loaded marker
         */
        public static ResolvedMarker loaded(String dimensionId, PathMarkerBlockEntity marker) {
            return new ResolvedMarker(dimensionId, marker.getBlockPos().immutable(), marker);
        }

        /**
         * Applies computed time fields to this marker's selected chapter data.
         *
         * @param timeOfDay           computed marker time
         * @param timeTransitionRange computed transition range
         * @param pathId              path identifier
         * @param chapterId           chapter identifier
         */
        public void apply(int timeOfDay, int timeTransitionRange, String pathId, String chapterId) {
            Map<String, Map<String, PathMarkerBlockEntity.ChapterNbtData>> pathData = liveMarker.getPathData();
            Map<String, PathMarkerBlockEntity.ChapterNbtData> chapters = pathData.computeIfAbsent(pathId, ignored -> new HashMap<>());
            PathMarkerBlockEntity.ChapterNbtData data = chapters.computeIfAbsent(chapterId, PathMarkerBlockEntity.ChapterNbtData::empty);

            data.setTimeOfDay(timeOfDay);
            data.setTimeTransitionRange(timeTransitionRange);
            liveMarker.markUpdated();
        }

        /**
         * Clears selected environment fields from existing chapter data without creating new entries.
         *
         * @param time      whether time fields should be reset
         * @param weather   whether weather fields should be reset
         * @param pathId    path identifier
         * @param chapterId chapter identifier
         */
        public void clear(boolean time, boolean weather, String pathId, String chapterId) {
            PathMarkerBlockEntity.ChapterNbtData data = chapterData(pathId, chapterId);
            if (data == null) {
                return;
            }

            if (time) {
                data.setTimeOfDay(PathMarkerBlockEntity.ChapterNbtData.UNSET);
                data.setTimeTransitionRange(TimeOfDay.DEFAULT_TRANSITION_RANGE);
            }

            if (weather) {
                data.setWeather(PathMarkerBlockEntity.ChapterNbtData.UNSET);
            }
            liveMarker.markUpdated();
        }

        /**
         * Reads chapter data for a path chapter without creating new entries.
         *
         * @param pathId    path identifier
         * @param chapterId chapter identifier
         * @return existing chapter data, or null when absent
         */
        public PathMarkerBlockEntity.ChapterNbtData chapterData(String pathId, String chapterId) {
            Map<String, Map<String, PathMarkerBlockEntity.ChapterNbtData>> pathData = liveMarker.getPathData();
            Map<String, PathMarkerBlockEntity.ChapterNbtData> chapters = pathData.get(pathId);
            return chapters == null ? null : chapters.get(chapterId);
        }
    }
}

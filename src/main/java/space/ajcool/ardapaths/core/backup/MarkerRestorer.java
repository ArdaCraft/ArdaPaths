package space.ajcool.ardapaths.core.backup;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import space.ajcool.ardapaths.core.backup.dto.MarkerIndexDto;
import space.ajcool.ardapaths.core.backup.dto.PathChapterDto;
import space.ajcool.ardapaths.core.backup.dto.PathFileDto;
import space.ajcool.ardapaths.core.backup.dto.PathNodeDto;
import space.ajcool.ardapaths.core.data.TimeOfDay;
import space.ajcool.ardapaths.core.data.WarpTarget;
import space.ajcool.ardapaths.mc.NbtEncodeable;
import space.ajcool.ardapaths.mc.blocks.ModBlocks;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Applies exported marker data back into server worlds.
 */
public class MarkerRestorer {
    /**
     * Builds all marker operations described by the backup files.
     *
     * @param markerIndex exported marker position index
     * @param paths       exported path files
     * @return planned marker payloads
     */
    public List<PlannedMarker> plan(MarkerIndexDto markerIndex, List<PathFileDto> paths) {
        Map<String, Map<Long, NbtCompound>> markerPayloads = buildMarkerPayloads(markerIndex, paths);
        List<PlannedMarker> plannedMarkers = new ArrayList<>();

        for (Map.Entry<String, Map<Long, NbtCompound>> dimensionEntry : markerPayloads.entrySet()) {
            for (Map.Entry<Long, NbtCompound> markerEntry : dimensionEntry.getValue().entrySet()) {
                plannedMarkers.add(new PlannedMarker(dimensionEntry.getKey(), markerEntry.getKey(), markerEntry.getValue()));
            }
        }

        plannedMarkers.sort(Comparator
                .comparing(PlannedMarker::dimensionId)
                .thenComparingLong(marker -> ChunkPos.toLong(BlockPos.fromLong(marker.packedPos())))
                .thenComparingInt(marker -> BlockPos.fromLong(marker.packedPos()).getY()));

        return plannedMarkers;
    }

    /**
     * Applies one marker plan to a target world.
     *
     * @param world      target server world
     * @param position   marker position
     * @param pathsNbt   marker paths NBT to apply
     * @return outcome describing whether the marker was restored
     */
    public static ApplyOutcome apply(ServerWorld world, BlockPos position, NbtCompound pathsNbt) {
        BlockState existingState = world.getBlockState(position);
        if (!existingState.isAir() && !existingState.isOf(ModBlocks.PATH_MARKER)) {
            return ApplyOutcome.CONFLICT;
        }

        if (!existingState.isOf(ModBlocks.PATH_MARKER)) {
            world.setBlockState(position, ModBlocks.PATH_MARKER.getDefaultState(), Block.NOTIFY_LISTENERS);
        }

        if (world.getBlockEntity(position) instanceof PathMarkerBlockEntity markerBlockEntity) {
            markerBlockEntity.applyNbt(pathsNbt);
            markerBlockEntity.markUpdated();
            return ApplyOutcome.PLACED;
        }

        return ApplyOutcome.FAILED;
    }

    /**
     * Deletes one stale marker if it is still an ArdaPaths marker.
     *
     * @param world    target server world
     * @param position stale marker position
     * @return true when a marker block was removed
     */
    public static boolean delete(ServerWorld world, BlockPos position) {
        if (world.getBlockEntity(position) instanceof PathMarkerBlockEntity) {
            return world.removeBlock(position, false);
        }

        return false;
    }

    /**
     * Builds one marker NBT payload per exported marker location.
     *
     * @param markerIndex exported marker position index
     * @param paths       exported path files
     * @return marker payloads grouped by dimension and packed position
     */
    private Map<String, Map<Long, NbtCompound>> buildMarkerPayloads(MarkerIndexDto markerIndex, List<PathFileDto> paths) {
        Map<String, Map<Long, NbtCompound>> markerPayloads = new HashMap<>();

        for (Map.Entry<String, Map<String, int[]>> dimensionEntry : markerIndex.markers().entrySet()) {
            Map<Long, NbtCompound> dimensionMarkers = new HashMap<>();
            for (String packedPosition : dimensionEntry.getValue().keySet()) {
                dimensionMarkers.put(Long.parseLong(packedPosition), new NbtCompound());
            }
            markerPayloads.put(dimensionEntry.getKey(), dimensionMarkers);
        }

        for (PathFileDto path : paths) {
            for (PathChapterDto chapter : path.chapters()) {
                for (PathNodeDto node : chapter.nodes()) {
                    Map<Long, NbtCompound> dimensionMarkers = markerPayloads.get(node.dimension());
                    if (dimensionMarkers == null) continue;

                    NbtCompound markerPathsNbt = dimensionMarkers.get(node.pos());
                    if (markerPathsNbt == null) continue;

                    NbtCompound pathNbt = markerPathsNbt.getCompound(path.id());
                    NbtCompound chapterNbt = toChapterNbt(chapter.id(), node);
                    pathNbt.put(chapter.id(), chapterNbt);
                    markerPathsNbt.put(path.id(), pathNbt);
                }
            }
        }

        return markerPayloads;
    }

    /**
     * Converts an exported node into a marker chapter NBT compound.
     *
     * @param chapterId chapter identifier for the marker payload
     * @param node      exported node
     * @return NBT compound matching {@link PathMarkerBlockEntity.ChapterNbtData}
     */
    private NbtCompound toChapterNbt(String chapterId, PathNodeDto node) {
        NbtCompound chapterNbt = new NbtCompound();

        if (node.next() != null) {
            BlockPos position = BlockPos.fromLong(node.pos());
            BlockPos next = BlockPos.fromLong(node.next());
            NbtEncodeable.putBlockPosIfPresent(chapterNbt, "target", next.subtract(position));
        }

        NbtEncodeable.putStringIfNotEmpty(chapterNbt, "proximity_message", node.message());
        NbtEncodeable.putIntIfNonZero(chapterNbt, "activation_range", node.range());
        NbtEncodeable.putStringIfNotEmpty(chapterNbt, "chapter", chapterId);
        NbtEncodeable.putBooleanIfTrue(chapterNbt, "chapter_start", node.chapterStart());
        NbtEncodeable.putBooleanIfTrue(chapterNbt, "display_chapter_title_on_trail", node.titleOnTrail());
        NbtEncodeable.putBooleanIfFalse(chapterNbt, "display_above_blocks", node.displayAboveBlocks());
        NbtEncodeable.putIntIfNonDefault(chapterNbt, "weather", node.weather() == null ? PathMarkerBlockEntity.ChapterNbtData.UNSET : node.weather(), PathMarkerBlockEntity.ChapterNbtData.UNSET);
        NbtEncodeable.putIntIfNonDefault(chapterNbt, "time_of_day", node.timeOfDay() == null ? PathMarkerBlockEntity.ChapterNbtData.UNSET : node.timeOfDay(), PathMarkerBlockEntity.ChapterNbtData.UNSET);
        NbtEncodeable.putIntIfNonDefault(chapterNbt, "time_transition_range", node.timeTransitionRange() == null ? TimeOfDay.DEFAULT_TRANSITION_RANGE : node.timeTransitionRange(), TimeOfDay.DEFAULT_TRANSITION_RANGE);
        NbtEncodeable.putStringIfNotEmpty(chapterNbt, "auto_teleport_target", node.autoTeleportTarget() == null ? "" : node.autoTeleportTarget());
        NbtEncodeable.putBlockPosIfPresent(chapterNbt, "look_at", WarpTarget.parseCoordinates(node.lookAt()));
        NbtEncodeable.putStringIfNotEmpty(chapterNbt, "give_item", node.giveItem() == null ? "" : node.giveItem());
        NbtEncodeable.putLongIfNonDefault(chapterNbt, "packed_message_data", node.anim().packed(), 360727776182960136L);

        return chapterNbt;
    }

    /**
     * Result of attempting to restore one marker payload.
     */
    public enum ApplyOutcome {
        /** Marker payload was applied to a path marker block entity. */
        PLACED,

        /** A non-marker block occupied the target position. */
        CONFLICT,

        /** The marker block entity could not be obtained after placement. */
        FAILED
    }
}

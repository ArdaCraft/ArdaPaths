package space.ajcool.ardapaths.mc.blocks.entities;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.ArdaPathsClient;
import space.ajcool.ardapaths.core.conversions.PathMarkerBlockEntityConverter;
import space.ajcool.ardapaths.core.data.config.shared.Color;
import space.ajcool.ardapaths.mc.NbtEncodeable;
import space.ajcool.ardapaths.paths.Paths;
import space.ajcool.ardapaths.paths.rendering.TrailRenderer;
import space.ajcool.ardapaths.paths.rendering.objects.AnimatedTrail;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Block entity for Path Marker blocks that stores trail configuration data.
 * Each marker can belong to multiple paths and chapters, storing waypoint offsets
 * and proximity message settings for each combination.
 */
@Slf4j(topic = "ardapaths")
public class PathMarkerBlockEntity extends BlockEntity implements NbtEncodeable {
    /**
     * Map structure: pathId → (chapterId → ChapterNbtData).
     * Allows a single marker to be part of multiple paths and chapters.
     */
    @Getter
    private Map<String, Map<String, ChapterNbtData>> pathData;

    /**
     * Constructs a PathMarkerBlockEntity at the given position.
     *
     * @param blockPos   the block position
     * @param blockState the block state
     */
    public PathMarkerBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(ModBlockEntities.PATH_MARKER, blockPos, blockState);
        this.pathData = new HashMap<>();
    }

    /**
     * Assigns this marker to a world and registers client-side instances for rendering.
     *
     * @param world the world this marker belongs to
     */
    @Override
    public void setWorld(World world) {
        super.setWorld(world);
        if (world.isClient()) {
            Paths.addTickingMarker(this);
        }
    }

    /**
     * Removes this marker from client-side rendering queries when it unloads.
     */
    @Override
    public void markRemoved() {
        if (this.world != null && this.world.isClient()) {
            Paths.removeTickingMarker(this);
        }
        super.markRemoved();
    }

    /**
     * Create a trail using the path's target and the given color.
     *
     * @param pathId The path ID to use when getting the target
     * @param colors The colors of the trail
     */
    public void createTrail(@NotNull String pathId, @NotNull String chapterId, @NotNull Color[] colors) {
        if (!this.pathData.containsKey(pathId)) return;
        if (!this.pathData.get(pathId).containsKey(chapterId)) return;

        ChapterNbtData chapterNbtData = this.pathData.get(pathId).get(chapterId);
        BlockPos target = chapterNbtData.getTarget();

        if (target == null) return;

        AnimatedTrail trail = AnimatedTrail.from(this.getPos(), target, chapterNbtData.isDisplayAboveBlocks(), colors);
        TrailRenderer.registerTrail(trail);
    }

    /**
     * Creates a packet to send the block entity update to clients.
     *
     * @return the update packet
     */
    @Override
    public @Nullable Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    /**
     * Gets the initial NBT data for chunk loading on the client.
     *
     * @return the NBT compound
     */
    @Override
    public @NotNull NbtCompound toInitialChunkDataNbt() {
        return this.createNbt();
    }

    /**
     * Marks this block entity as updated and notifies clients of the change.
     */
    public void markUpdated() {

        if (this.world == null) return;

        this.markDirty();
        this.world.updateListeners(this.getPos(), this.getCachedState(), this.getCachedState(), 3);
    }

    /**
     * Read NBT data from a compound tag and apply it to the entity.
     *
     * @param compoundTag The NBT compound tag
     */
    @Override
    public void readNbt(NbtCompound compoundTag) {
        NbtCompound converted = PathMarkerBlockEntityConverter.convertNbt(compoundTag);

        super.readNbt(converted);

        this.applyNbt(converted.getCompound("paths"));
    }

    /**
     * Apply an NBT compound to the entity. This run on the server.
     *
     * @param nbt The NBT compound
     */
    @Override
    public void applyNbt(NbtCompound nbt) {
        if (nbt == null) {
            log.info("NBT compound is null");

            return;
        }

        Map<String, Map<String, ChapterNbtData>> loadedPathData = new HashMap<>();
        boolean serverSide = this.world != null ? !this.world.isClient() : ArdaPaths.amITheServer();

        for (String pathKey : nbt.getKeys()) {
            var configPath = serverSide ? ArdaPaths.CONFIG.getPath(pathKey) : ArdaPathsClient.CONFIG.getPath(pathKey);

            if (configPath == null && serverSide) {
                log.warn("Refusing to apply marker NBT at {} with unknown path '{}'", this.getPos(), pathKey);
                return;
            }

            var chapterData = new HashMap<String, ChapterNbtData>();

            var nbtEntry = nbt.getCompound(pathKey);

            for (String chapterKey : nbtEntry.getKeys()) {
                if (configPath != null && configPath.getChapter(chapterKey) == null && serverSide) {
                    log.warn("Refusing to apply marker NBT at {} with unknown chapter '{}:{}'", this.getPos(), pathKey, chapterKey);
                    return;
                }

                ChapterNbtData chapterNbtData = ChapterNbtData.fromNbt(nbtEntry.getCompound(chapterKey));
                chapterData.put(chapterKey, chapterNbtData);
            }

            loadedPathData.put(pathKey, chapterData);
        }

        this.pathData = loadedPathData;
    }

    /**
     * Write NBT data to a compound tag.
     *
     * @param compoundTag The NBT compound tag
     */
    @Override
    public void writeNbt(NbtCompound compoundTag) {
        super.writeNbt(compoundTag);
        this.toNbt(compoundTag);
    }

    /**
     * Convert the entity to an NBT compound.
     *
     * @return The NBT compound
     */
    @Override
    public NbtCompound toNbt(@Nullable NbtCompound nbt) {
        if (nbt == null) nbt = new NbtCompound();
        if (this.pathData.isEmpty()) return nbt;

        NbtCompound pathsNbt = new NbtCompound();

        for (Map.Entry<String, Map<String, ChapterNbtData>> pathEntry : this.pathData.entrySet()) {
            NbtCompound pathNbt = new NbtCompound();

            for (Map.Entry<String, ChapterNbtData> chapterEntry : pathEntry.getValue().entrySet()) {
                NbtCompound chapterNbt = chapterEntry.getValue().toNbt();

                if (chapterEntry.getValue().isEmpty() || chapterNbt.isEmpty()) continue;
                pathNbt.put(chapterEntry.getKey(), chapterNbt);
            }

            if (pathNbt.isEmpty()) continue;
            pathsNbt.put(pathEntry.getKey(), pathNbt);
        }

        if (!pathsNbt.isEmpty()) nbt.put("paths", pathsNbt);

        return nbt;
    }

    /**
     * @return The center position of the block entity
     */
    @SuppressWarnings("unused")
    public Vec3d getCenterPos() {
        BlockPos position = this.getPos();
        return new Vec3d(position.getX() + 0.5, position.getY() + 0.5, position.getZ() + 0.5);
    }

    public @Nullable List<ChapterNbtData> getChapters(String pathId, boolean createIfNull) {
        if (!this.pathData.containsKey(pathId) && createIfNull) {
            var newEmpty = new HashMap<String, ChapterNbtData>();
            this.pathData.put(pathId, newEmpty);
        }

        if (!this.pathData.containsKey(pathId)) return null;

        return this.pathData.get(pathId).values().stream().toList();
    }

    /**
     * Get the NBT data for the given path ID.
     *
     * @param pathId The path ID
     */
    public @NotNull PathMarkerBlockEntity.ChapterNbtData getChapterData(String pathId, String chapterId) {
        return Objects.requireNonNull(this.getChapterData(pathId, chapterId, true));
    }

    /**
     * Get the NBT data for the given path ID.
     *
     * @param pathId       The path ID
     * @param createIfNull Whether to create an empty NBT set if no data is found
     */
    public @Nullable PathMarkerBlockEntity.ChapterNbtData getChapterData(String pathId, String chapterId, boolean createIfNull) {
        if (!this.pathData.containsKey(pathId) && createIfNull) {
            var newEmpty = new HashMap<String, ChapterNbtData>();
            newEmpty.put(chapterId, ChapterNbtData.empty(chapterId));

            this.pathData.put(pathId, newEmpty);
        }

        if (!this.pathData.containsKey(pathId)) return null;

        if (!this.pathData.get(pathId).containsKey(chapterId) && createIfNull) {
            this.pathData.get(pathId).put(chapterId, ChapterNbtData.empty(chapterId));
        }

        if (!this.pathData.get(pathId).containsKey(chapterId)) return null;

        return this.pathData.get(pathId).get(chapterId);
    }

    /**
     * Represents the NBT data for a path marker.
     */
    @Getter
    @Setter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ChapterNbtData implements NbtEncodeable {
        /**
         * The proximity message shown when the player enters this marker's activation range.
         */
        @NotNull
        private String proximityMessage;

        /**
         * The distance from this marker at which the proximity message becomes active.
         */
        private int activationRange;

        /**
         * The offset target used to render this marker's outgoing trail, or null when no target is set.
         */
        @Nullable
        private BlockPos target;

        /**
         * The chapter ID this NBT entry belongs to.
         */
        @NotNull
        private String chapterId;

        /**
         * Whether this marker is configured as the start marker for its chapter.
         */
        private boolean isChapterStart;

        /**
         * Whether this marker should trigger the chapter title while following a trail.
         */
        private boolean isDisplayChapterTitleOnTrail;

        /**
         * Whether the rendered trail should rise above terrain instead of following direct block offsets.
         */
        private boolean displayAboveBlocks;

        /**
         * Packed proximity message animation values encoded with {@link space.ajcool.ardapaths.core.data.BitPacker}.
         */
        private long packedMessageData;

        private ChapterNbtData(NbtCompound nbt) {
            this("", 0, null, "", false, false, true, 360727776182960136L);
            this.applyNbt(nbt);
        }

        /**
         * Apply an NBT compound to the entity data.
         *
         * @param nbt The NBT compound
         */
        @Override
        public void applyNbt(NbtCompound nbt) {
            this.target = nbt.contains("target") ? NbtHelper.toBlockPos(nbt.getCompound("target")) : null;
            this.proximityMessage = nbt.getString("proximity_message");
            this.activationRange = nbt.getInt("activation_range");
            this.chapterId = nbt.getString("chapter");
            this.isChapterStart = nbt.getBoolean("chapter_start");
            this.isDisplayChapterTitleOnTrail = nbt.getBoolean("display_chapter_title_on_trail");
            this.displayAboveBlocks = !nbt.contains("display_above_blocks") || nbt.getBoolean("display_above_blocks");
            this.packedMessageData = nbt.contains("packed_message_data") ? nbt.getLong("packed_message_data") : 360727776182960136L;
        }

        /**
         * Create an NBT data object from an NBT compound.
         *
         * @param nbt The NBT compound
         */
        public static ChapterNbtData fromNbt(NbtCompound nbt) {
            return new ChapterNbtData(nbt);
        }

        /**
         * Create an empty NBT data object.
         */
        public static ChapterNbtData empty(String chapterId) {
            return new ChapterNbtData("", 0, null, chapterId, false, false, true, 360727776182960136L);
        }

        /**
         * Remove the target position.
         */
        public void removeTarget() {
            this.target = null;
        }

        /**
         * @return If the data object is "default" and contains no user defined data.
         */
        public boolean isEmpty() {
            return target == null
                    && proximityMessage.isEmpty()
                    && activationRange == 0
                    && !isChapterStart
                    && !isDisplayChapterTitleOnTrail
                    && displayAboveBlocks
                    && packedMessageData == 360727776182960136L; // Default packed value [5,100,5,2,8]
        }

        /**
         * Convert the entity data to an NBT compound.
         *
         * @return The NBT compound
         */
        @Override
        public NbtCompound toNbt(@Nullable NbtCompound nbt) {
            nbt = nbt == null ? new NbtCompound() : nbt;

            if (target != null) nbt.put("target", NbtHelper.fromBlockPos(target));
            if (!proximityMessage.isEmpty()) nbt.putString("proximity_message", proximityMessage);
            if (activationRange != 0) nbt.putInt("activation_range", activationRange);
            if (!chapterId.isEmpty()) nbt.putString("chapter", chapterId);
            if (isChapterStart) nbt.putBoolean("chapter_start", true);
            if (isDisplayChapterTitleOnTrail) nbt.putBoolean("display_chapter_title_on_trail", true);
            if (!displayAboveBlocks) nbt.putBoolean("display_above_blocks", false);
            if (packedMessageData != 360727776182960136L && packedMessageData != 0)
                nbt.putLong("packed_message_data", packedMessageData);

            return nbt;
        }
    }
}

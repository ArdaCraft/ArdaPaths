package space.ajcool.ardapaths.mc.blocks.entities;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.core.conversions.PathMarkerBlockEntityConverter;
import space.ajcool.ardapaths.core.data.TimeOfDay;
import space.ajcool.ardapaths.core.data.config.shared.Color;
import space.ajcool.ardapaths.core.data.config.shared.PathData;
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
    public void setLevel(Level world) {
        super.setLevel(world);
        if (world.isClientSide()) {
            Paths.addTickingMarker(this);
        }
    }

    /**
     * Removes this marker from client-side rendering queries when it unloads.
     */
    @Override
    public void setRemoved() {
        if (this.level != null && this.level.isClientSide()) {
            Paths.removeTickingMarker(this);
        }
        super.setRemoved();
    }

    /**
     * Create a trail using the path's target and the given color.
     *
     * @param pathId    the path ID to use when getting the target
     * @param chapterId the chapter ID to use when getting the target
     * @param colors    the colors of the trail
     */
    public void createTrail(@NotNull String pathId, @NotNull String chapterId, @NotNull Color[] colors) {
        if (!this.pathData.containsKey(pathId)) return;
        if (!this.pathData.get(pathId).containsKey(chapterId)) return;

        ChapterNbtData chapterNbtData = this.pathData.get(pathId).get(chapterId);
        BlockPos target = chapterNbtData.getTarget();

        if (target == null) return;

        AnimatedTrail trail = AnimatedTrail.from(this.getBlockPos(), target, chapterNbtData.isDisplayAboveBlocks(), colors);
        TrailRenderer.registerTrail(trail);
    }

    /**
     * Creates a packet to send the block entity update to clients.
     *
     * @return the update packet
     */
    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /**
     * Gets the initial NBT data for chunk loading on the client.
     *
     * @return the NBT compound
     */
    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        return this.saveWithoutMetadata(provider);
    }

    /**
     * Marks this block entity as updated and notifies clients of the change.
     */
    public void markUpdated() {

        if (this.level == null) return;

        this.setChanged();
        this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
    }

    /**
     * Read NBT data from a compound tag and apply it to the entity.
     *
     * @param compoundTag The NBT compound tag
     * @param provider    registry lookup provider for vanilla serialization
     */
    @Override
    protected void loadAdditional(CompoundTag compoundTag, HolderLookup.Provider provider) {
        CompoundTag converted = PathMarkerBlockEntityConverter.convertNbt(compoundTag);

        super.loadAdditional(converted, provider);

        this.applyNbt(NbtEncodeable.getCompound(converted, "paths"));
    }

    /**
     * Apply a paths NBT compound to the entity without dropping unknown path or chapter data.
     *
     * @param nbt The NBT compound
     */
    @Override
    public void applyNbt(CompoundTag nbt) {
        this.applyNbt(nbt, false);
    }

    /**
     * Apply a paths NBT compound to the entity.
     *
     * @param nbt      the paths NBT compound
     * @param validate whether unknown server-config paths and chapters should be rejected
     */
    private void applyNbt(CompoundTag nbt, boolean validate) {
        if (nbt == null) {
            log.info("NBT compound is null");

            return;
        }

        Map<String, Map<String, ChapterNbtData>> loadedPathData = new HashMap<>();

        for (String pathKey : nbt.getAllKeys()) {
            PathData configPath = ArdaPaths.CONFIG == null ? null : ArdaPaths.CONFIG.getPath(pathKey);

            if (configPath == null && validate) {
                log.warn("Refusing to apply marker NBT at {} with unknown path '{}'", this.getBlockPos(), pathKey);
                continue;
            }

            if (configPath == null && ArdaPaths.CONFIG != null) {
                log.warn("Keeping marker NBT at {} with unknown path '{}'", this.getBlockPos(), pathKey);
            }

            var chapterData = new HashMap<String, ChapterNbtData>();

            var nbtEntry = NbtEncodeable.getCompound(nbt, pathKey);

            for (String chapterKey : nbtEntry.getAllKeys()) {
                if (configPath != null && configPath.getChapter(chapterKey) == null && validate) {
                    log.warn("Refusing to apply marker NBT at {} with unknown chapter '{}:{}'", this.getBlockPos(), pathKey, chapterKey);
                    continue;
                }

                if (configPath != null && configPath.getChapter(chapterKey) == null) {
                    log.warn("Keeping marker NBT at {} with unknown chapter '{}:{}'", this.getBlockPos(), pathKey, chapterKey);
                }

                ChapterNbtData chapterNbtData = ChapterNbtData.fromNbt(NbtEncodeable.getCompound(nbtEntry, chapterKey));
                chapterData.put(chapterKey, chapterNbtData);
            }

            loadedPathData.put(pathKey, chapterData);
        }

        this.pathData = loadedPathData;
    }

    /**
     * Reads NBT data from a remote update and validates it against the server config.
     *
     * @param compoundTag The NBT compound tag
     */
    public void loadValidated(CompoundTag compoundTag) {
        CompoundTag converted = PathMarkerBlockEntityConverter.convertNbt(compoundTag);

        this.applyNbt(NbtEncodeable.getCompound(converted, "paths"), true);
    }

    /**
     * Write NBT data to a compound tag.
     *
     * @param compoundTag The NBT compound tag
     * @param provider    registry lookup provider for vanilla serialization
     */
    @Override
    protected void saveAdditional(CompoundTag compoundTag, HolderLookup.Provider provider) {
        super.saveAdditional(compoundTag, provider);
        this.toNbt(compoundTag);
    }

    /**
     * Convert the entity to an NBT compound.
     *
     * @return The NBT compound
     */
    @Override
    public CompoundTag toNbt(@Nullable CompoundTag nbt) {
        if (nbt == null) nbt = new CompoundTag();
        if (this.pathData.isEmpty()) return nbt;

        CompoundTag pathsNbt = new CompoundTag();

        for (Map.Entry<String, Map<String, ChapterNbtData>> pathEntry : this.pathData.entrySet()) {
            CompoundTag pathNbt = new CompoundTag();

            for (Map.Entry<String, ChapterNbtData> chapterEntry : pathEntry.getValue().entrySet()) {
                CompoundTag chapterNbt = chapterEntry.getValue().toNbt();

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
    public Vec3 getCenterPos() {
        BlockPos position = this.getBlockPos();
        return new Vec3(position.getX() + 0.5, position.getY() + 0.5, position.getZ() + 0.5);
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
     * Get the NBT data for the given path and chapter IDs.
     *
     * @param pathId    the path ID
     * @param chapterId the chapter ID
     * @return the chapter NBT data, never null
     */
    public @NotNull PathMarkerBlockEntity.ChapterNbtData getChapterData(String pathId, String chapterId) {
        return Objects.requireNonNull(this.getChapterData(pathId, chapterId, true));
    }

    /**
     * Get the NBT data for the given path and chapter IDs.
     *
     * @param pathId       the path ID
     * @param chapterId    the chapter ID
     * @param createIfNull whether to create an empty NBT set if no data is found
     * @return the chapter NBT data, or null if not found and createIfNull is false
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
         * Marker value used when optional chapter marker settings are unset.
         */
        public static final int UNSET = -1;

        /**
         * Default packed proximity animation value for [5, 100, 5, 2, 8].
         */
        public static final long DEFAULT_PACKED_MESSAGE_DATA = 360727776182960136L;

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
         * Absolute world position this marker asks the player to look at, or null when unset.
         */
        @Nullable
        private BlockPos lookAt;

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
         * Persisted ordinal of the configured weather type, or {@link #UNSET} when unset.
         */
        private int weather;

        /**
         * Configured time of day in daytime ticks, or {@link #UNSET} when unset.
         */
        private int timeOfDay;

        /**
         * Distance in blocks over which the configured time of day transitions.
         */
        private int timeTransitionRange;

        /**
         * Server-executed teleport target triggered when a player reaches this marker.
         */
        @NotNull
        private String autoTeleportTarget;

        /**
         * Server-executed item grant triggered when a player reaches this marker.
         */
        @NotNull
        private String giveItem;

        /**
         * Packed proximity message animation values encoded with {@link space.ajcool.ardapaths.core.data.BitPacker}.
         */
        private long packedMessageData;

        private ChapterNbtData(CompoundTag nbt) {
            this("", 0, null, null, "", false, false, true, UNSET, UNSET, TimeOfDay.DEFAULT_TRANSITION_RANGE, "", "", DEFAULT_PACKED_MESSAGE_DATA);
            this.applyNbt(nbt);
        }

        /**
         * Apply an NBT compound to the entity data.
         *
         * @param nbt The NBT compound
         */
        @Override
        public void applyNbt(CompoundTag nbt) {
            this.target = NbtEncodeable.getBlockPos(nbt, "target").orElse(null);
            this.lookAt = NbtEncodeable.getBlockPos(nbt, "look_at").orElse(null);
            this.proximityMessage = NbtEncodeable.getStringOrEmpty(nbt, "proximity_message");
            this.activationRange = NbtEncodeable.getIntOrZero(nbt, "activation_range");
            this.chapterId = NbtEncodeable.getStringOrEmpty(nbt, "chapter");
            this.isChapterStart = NbtEncodeable.getBooleanOrDefault(nbt, "chapter_start", false);
            this.isDisplayChapterTitleOnTrail = NbtEncodeable.getBooleanOrDefault(nbt, "display_chapter_title_on_trail", false);
            this.displayAboveBlocks = NbtEncodeable.getBooleanOrDefault(nbt, "display_above_blocks", true);
            this.weather = NbtEncodeable.getIntOrDefault(nbt, "weather", UNSET);
            this.timeOfDay = NbtEncodeable.getIntOrDefault(nbt, "time_of_day", UNSET);
            this.timeTransitionRange = NbtEncodeable.getIntOrDefault(nbt, "time_transition_range", TimeOfDay.DEFAULT_TRANSITION_RANGE);
            this.autoTeleportTarget = NbtEncodeable.getStringOrEmpty(nbt, "auto_teleport_target");
            this.giveItem = NbtEncodeable.getStringOrEmpty(nbt, "give_item");
            this.packedMessageData = NbtEncodeable.getLongOrDefault(nbt, "packed_message_data", DEFAULT_PACKED_MESSAGE_DATA);
        }

        /**
         * Create an NBT data object from an NBT compound.
         *
         * @param nbt the NBT compound to deserialize
         * @return the chapter NBT data loaded from the compound
         */
        public static ChapterNbtData fromNbt(CompoundTag nbt) {
            return new ChapterNbtData(nbt);
        }

        /**
         * Create an empty NBT data object.
         *
         * @param chapterId chapter ID for the empty marker data
         * @return empty marker data for the chapter
         */
        public static ChapterNbtData empty(String chapterId) {
            return new ChapterNbtData("", 0, null, null, chapterId, false, false, true, UNSET, UNSET, TimeOfDay.DEFAULT_TRANSITION_RANGE, "", "", DEFAULT_PACKED_MESSAGE_DATA);
        }

        /**
         * Remove the target position.
         */
        public void removeTarget() {
            this.target = null;
        }

        /**
         * Checks whether this marker has any selected-chapter action data.
         *
         * @return true when an auto teleport, item grant, or look-at target is configured
         */
        public boolean hasMiscData() {
            return !autoTeleportTarget.isEmpty() || !giveItem.isEmpty() || lookAt != null;
        }

        /**
         * @return If the data object is "default" and contains no user defined data.
         */
        public boolean isEmpty() {
            return target == null
                    && lookAt == null
                    && proximityMessage.isEmpty()
                    && activationRange == 0
                    && !isChapterStart
                    && !isDisplayChapterTitleOnTrail
                    && displayAboveBlocks
                    && weather == UNSET
                    && timeOfDay == UNSET
                    && timeTransitionRange == TimeOfDay.DEFAULT_TRANSITION_RANGE
                    && autoTeleportTarget.isEmpty()
                    && giveItem.isEmpty()
                    && packedMessageData == DEFAULT_PACKED_MESSAGE_DATA;
        }

        /**
         * Convert the entity data to an NBT compound.
         *
         * @return The NBT compound
         */
        @Override
        public CompoundTag toNbt(@Nullable CompoundTag nbt) {
            nbt = nbt == null ? new CompoundTag() : nbt;

            NbtEncodeable.putBlockPosIfPresent(nbt, "target", target);
            NbtEncodeable.putBlockPosIfPresent(nbt, "look_at", lookAt);
            NbtEncodeable.putStringIfNotEmpty(nbt, "proximity_message", proximityMessage);
            NbtEncodeable.putIntIfNonZero(nbt, "activation_range", activationRange);
            NbtEncodeable.putStringIfNotEmpty(nbt, "chapter", chapterId);
            NbtEncodeable.putBooleanIfTrue(nbt, "chapter_start", isChapterStart);
            NbtEncodeable.putBooleanIfTrue(nbt, "display_chapter_title_on_trail", isDisplayChapterTitleOnTrail);
            NbtEncodeable.putBooleanIfFalse(nbt, "display_above_blocks", displayAboveBlocks);
            NbtEncodeable.putIntIfNonDefault(nbt, "weather", weather, UNSET);
            NbtEncodeable.putIntIfNonDefault(nbt, "time_of_day", timeOfDay, UNSET);
            NbtEncodeable.putIntIfNonDefault(nbt, "time_transition_range", timeTransitionRange, TimeOfDay.DEFAULT_TRANSITION_RANGE);
            NbtEncodeable.putStringIfNotEmpty(nbt, "auto_teleport_target", autoTeleportTarget);
            NbtEncodeable.putStringIfNotEmpty(nbt, "give_item", giveItem);
            NbtEncodeable.putLongIfNonDefault(nbt, "packed_message_data", packedMessageData, DEFAULT_PACKED_MESSAGE_DATA);

            return nbt;
        }
    }
}

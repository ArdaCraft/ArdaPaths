package space.ajcool.ardapaths.mc.blocks.entities;

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

public class PathMarkerBlockEntity extends BlockEntity implements NbtEncodeable
{
    private Map<String, Map<String, ChapterNbtData>> pathData;

    public PathMarkerBlockEntity(BlockPos blockPos, BlockState blockState)
    {
        super(ModBlockEntities.PATH_MARKER, blockPos, blockState);
        this.pathData = new HashMap<>();
    }

    /**
     * Create a trail using the path's target and the given color.
     *
     * @param pathId The path ID to use when getting the target
     * @param colors The colors of the trail
     */
    public void createTrail(@NotNull String pathId, @NotNull String chapterId, @NotNull Color[] colors)
    {
        if (!this.pathData.containsKey(pathId)) return;
        if (!this.pathData.get(pathId).containsKey(chapterId)) return;

        ChapterNbtData chapterNbtData = this.pathData.get(pathId).get(chapterId);
        if (chapterNbtData.getTarget() == null) return;

        AnimatedTrail trail = AnimatedTrail.from(this.getPos(), chapterNbtData.getTarget(), chapterNbtData.displayAboveBlocks(), colors);
        TrailRenderer.registerTrail(trail);
    }

    public static void tick(World level, BlockPos blockPos, BlockState blockState, PathMarkerBlockEntity pathMarkerBlockEntity)
    {
        Paths.addTickingMarker(pathMarkerBlockEntity);
    }

    @Override
    public @Nullable Packet<ClientPlayPacketListener> toUpdatePacket()
    {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public @NotNull NbtCompound toInitialChunkDataNbt()
    {
        return this.createNbt();
    }

    public void markUpdated()
    {
        this.markDirty();
        this.world.updateListeners(this.getPos(), this.getCachedState(), this.getCachedState(), 3);
    }

    /**
     * Read NBT data from a compound tag and apply it to the entity.
     *
     * @param compoundTag The NBT compound tag
     */
    @Override
    public void readNbt(NbtCompound compoundTag)
    {
        NbtCompound converted = PathMarkerBlockEntityConverter.convertNbt(compoundTag);

        super.readNbt(converted);

        this.applyNbt(converted.getCompound("paths"));
    }

    /**
     * Write NBT data to a compound tag.
     *
     * @param compoundTag The NBT compound tag
     */
    @Override
    public void writeNbt(NbtCompound compoundTag)
    {
        super.writeNbt(compoundTag);
        this.toNbt(compoundTag);
    }

    /**
     * Apply an NBT compound to the entity. This run on the server.
     *
     * @param nbt The NBT compound
     */
    @Override
    public void applyNbt(NbtCompound nbt)
    {
        if (nbt == null)
        {
            ArdaPaths.LOGGER.info("NBT compound is null");

            return;
        }

        this.pathData = new HashMap<>();

        for (String pathKey : nbt.getKeys())
        {
            var configPath = ArdaPaths.amITheServer() ? ArdaPaths.CONFIG.getPath(pathKey) : ArdaPathsClient.CONFIG.getPath(pathKey);

            if (configPath == null) continue;

            var chapterData = new HashMap<String, ChapterNbtData>();

            var nbtEntry = nbt.getCompound(pathKey);

            for (String chapterKey : nbtEntry.getKeys())
            {
                if (configPath.getChapter(chapterKey) == null) continue;

                ChapterNbtData chapterNbtData = ChapterNbtData.fromNbt(nbtEntry.getCompound(chapterKey));
                chapterData.put(chapterKey, chapterNbtData);
            }

            this.pathData.put(pathKey, chapterData);
        }
    }

    /**
     * Convert the entity to an NBT compound.
     *
     * @return The NBT compound
     */
    @Override
    public NbtCompound toNbt(@Nullable NbtCompound nbt)
    {
        if (nbt == null) nbt = new NbtCompound();
        if (this.pathData.isEmpty()) return nbt;

        NbtCompound pathsNbt = new NbtCompound();

        for (Map.Entry<String, Map<String, ChapterNbtData>> pathEntry : this.pathData.entrySet())
        {
            NbtCompound pathNbt = new NbtCompound();

            for (Map.Entry<String, ChapterNbtData> chapterEntry : pathEntry.getValue().entrySet())
            {
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
    public Vec3d getCenterPos()
    {
        BlockPos position = this.getPos();
        return new Vec3d(position.getX() + 0.5, position.getY() + 0.5, position.getZ() + 0.5);
    }

    public @NotNull List<ChapterNbtData> getChapters(String pathId)
    {
        return Objects.requireNonNull(getChapters(pathId, true));
    }

    public @Nullable List<ChapterNbtData> getChapters(String pathId, boolean createIfNull)
    {
        if (!this.pathData.containsKey(pathId) && createIfNull)
        {
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
    public @NotNull PathMarkerBlockEntity.ChapterNbtData getChapterData(String pathId, String chapterId)
    {
        return Objects.requireNonNull(this.getChapterData(pathId, chapterId, true));
    }

    /**
     * Get the NBT data for the given path ID.
     *
     * @param pathId       The path ID
     * @param createIfNull Whether to create an empty NBT set if no data is found
     */
    public @Nullable PathMarkerBlockEntity.ChapterNbtData getChapterData(String pathId, String chapterId, boolean createIfNull)
    {
        if (!this.pathData.containsKey(pathId) && createIfNull)
        {
            var newEmpty = new HashMap<String, ChapterNbtData>();
            newEmpty.put(chapterId, ChapterNbtData.empty(chapterId));

            this.pathData.put(pathId, newEmpty);
        }

        if (!this.pathData.containsKey(pathId)) return null;

        if (!this.pathData.get(pathId).containsKey(chapterId) && createIfNull)
        {
            this.pathData.get(pathId).put(chapterId, ChapterNbtData.empty(chapterId));
        }

        if (!this.pathData.get(pathId).containsKey(chapterId)) return null;

        return this.pathData.get(pathId).get(chapterId);
    }

    /**
     * Represents the NBT data for a path marker.
     */
    public static class ChapterNbtData implements NbtEncodeable
    {
        private String proximityMessage;
        private int activationRange;
        private BlockPos target;
        private String chapterId;
        private boolean isChapterStart;
        private boolean isDisplayChapterTitleOnTrail;
        private boolean displayAboveBlocks;
        private long packedMessageData;

        private ChapterNbtData(NbtCompound nbt)
        {
            this("", 0, null, "", false, false, true, 360727776182960136L);
            this.applyNbt(nbt);
        }

        private ChapterNbtData(
                String proximityMessage,
                int activationRange,
                BlockPos target,
                String chapterId,
                boolean isChapterStart,
                boolean isDisplayChapterTitleOnTrail,
                boolean displayAboveBlocks,
                long packedMessageData
        )
        {
            this.proximityMessage = proximityMessage;
            this.activationRange = activationRange;
            this.target = target;
            this.chapterId = chapterId;
            this.isChapterStart = isChapterStart;
            this.isDisplayChapterTitleOnTrail = isDisplayChapterTitleOnTrail;
            this.displayAboveBlocks = displayAboveBlocks;
            this.packedMessageData = packedMessageData;
        }

        /**
         * Create an NBT data object from an NBT compound.
         *
         * @param nbt The NBT compound
         */
        public static ChapterNbtData fromNbt(NbtCompound nbt)
        {
            return new ChapterNbtData(nbt);
        }

        /**
         * Create an empty NBT data object.
         */
        public static ChapterNbtData empty(String chapterId)
        {
            return new ChapterNbtData("", 0, null, chapterId, false, false, true, 360727776182960136L);
        }

        /**
         * @return The proximity message
         */
        public String getProximityMessage()
        {
            return proximityMessage;
        }

        /**
         * Set the proximity message.
         *
         * @param proximityMessage The proximity message
         */
        public void setProximityMessage(@NotNull String proximityMessage)
        {
            this.proximityMessage = proximityMessage;
        }

        /**
         * @return The activation range
         */
        public int getActivationRange()
        {
            return activationRange;
        }

        /**
         * Set the activation range.
         *
         * @param activationRange The activation range
         */
        public void setActivationRange(int activationRange)
        {
            this.activationRange = activationRange;
        }

        /**
         * @return The target position
         */
        public @Nullable BlockPos getTarget()
        {
            return target;
        }

        /**
         * Set the target position.
         *
         * @param target The target position
         */
        public void setTarget(@NotNull BlockPos target)
        {
            this.target = target;
        }

        /**
         * Remove the target position.
         */
        public void removeTarget()
        {
            this.target = null;
        }

        /**
         * @return The chapter ID
         */
        public String getChapterId() {
            return chapterId;
        }

        /**
         * Set the chapter ID.
         *
         * @param chapterId The chapter ID
         */
        public void setChapterId(@NotNull String chapterId) {
            this.chapterId = chapterId;
        }

        /**
         * @return Whether the path marker is the start of a chapter
         */
        public boolean isChapterStart()
        {
            return isChapterStart;
        }

        /**
         * Set whether the path marker is the start of a chapter.
         *
         * @param chapterStart Whether the path marker is the start of a chapter
         */
        public void setChapterStart(boolean chapterStart)
        {
            isChapterStart = chapterStart;
        }

        public void setDisplayChapterTitleOnTrail(boolean displayChapterTitleOnTrail)
        {
            isDisplayChapterTitleOnTrail = displayChapterTitleOnTrail;
        }

        public boolean isDisplayChapterTitleOnTrail() {
            return isDisplayChapterTitleOnTrail;
        }

        /**
         * @return Whether the path marker should display above blocks
         */
        public boolean displayAboveBlocks()
        {
            return displayAboveBlocks;
        }

        /**
         * Set whether the path marker should display above blocks.
         *
         * @param displayAboveBlocks Whether the path marker should display above blocks
         */
        public void setDisplayAboveBlocks(boolean displayAboveBlocks)
        {
            this.displayAboveBlocks = displayAboveBlocks;
        }

        /**
         * Returns the packed message data as a long value.
         * This value contains multiple integers encoded using bitwise operations.
         *
         * @return the packed message data
         */
        public long getPackedMessageData()
        {
            return packedMessageData;
        }

        /**
         * Sets the packed message data.
         * This method should be used when assigning a new packed long that contains encoded integers.
         *
         * @param packedMessageData the new packed message data to set
         */
        public void setPackedMessageData(long packedMessageData)
        {
            this.packedMessageData = packedMessageData;
        }

        /**
         * @return If the data object is "default" and contains no user defined data.
         */
        public boolean isEmpty()
        {
            if (target != null) return false;
            if (!proximityMessage.isEmpty()) return false;
            if (activationRange != 0) return false;
            if (isChapterStart) return false;
            if (isDisplayChapterTitleOnTrail) return false;
            if (!displayAboveBlocks) return false;
            if (packedMessageData == 360727776182960136L) return false;

            return true;
        }

        /**
         * Apply an NBT compound to the entity data.
         *
         * @param nbt The NBT compound
         */
        @Override
        public void applyNbt(NbtCompound nbt)
        {
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
         * Convert the entity data to an NBT compound.
         *
         * @return The NBT compound
         */
        @Override
        public NbtCompound toNbt(@Nullable NbtCompound nbt)
        {
            nbt = nbt == null ? new NbtCompound() : nbt;

            if (target != null) nbt.put("target", NbtHelper.fromBlockPos(target));
            if (!proximityMessage.isEmpty()) nbt.putString("proximity_message", proximityMessage);
            if (activationRange != 0) nbt.putInt("activation_range", activationRange);
            if (!chapterId.isEmpty()) nbt.putString("chapter", chapterId);
            if (isChapterStart) nbt.putBoolean("chapter_start", true);
            if (isDisplayChapterTitleOnTrail) nbt.putBoolean("display_chapter_title_on_trail", true);
            if (!displayAboveBlocks) nbt.putBoolean("display_above_blocks", false);
            if (packedMessageData != 360727776182960136L && packedMessageData != 0) nbt.putLong("packed_message_data", packedMessageData);

            return nbt;
        }
    }
}

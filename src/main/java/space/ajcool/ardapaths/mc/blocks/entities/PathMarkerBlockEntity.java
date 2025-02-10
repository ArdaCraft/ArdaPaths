package space.ajcool.ardapaths.mc.blocks.entities;

import net.minecraft.nbt.NbtHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.core.data.config.shared.Color;
import space.ajcool.ardapaths.mc.NbtEncodeable;
import space.ajcool.ardapaths.paths.Paths;
import space.ajcool.ardapaths.paths.rendering.TrailRenderer;
import space.ajcool.ardapaths.paths.rendering.objects.AnimatedTrail;
import space.ajcool.ardapaths.core.conversions.PathMarkerBlockEntityConverter;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class PathMarkerBlockEntity extends BlockEntity implements NbtEncodeable {
    private Map<String, NbtData> data;

    public PathMarkerBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(ModBlockEntities.PATH_MARKER, blockPos, blockState);
        this.data = new HashMap<>();
    }

    /**
     * Create a trail using the path's target and the given color.
     *
     * @param pathId The path ID to use when getting the target
     * @param color The color of the trail
     */
    public void createTrail(@NotNull String pathId, @NotNull Color color) {
        if (!this.data.containsKey(pathId)) return;
        NbtData nbtData = this.data.get(pathId);
        if (nbtData.getTarget() == null) return;
        AnimatedTrail trail = AnimatedTrail.from(this.getPos(), nbtData.getTarget(), color);
        TrailRenderer.registerTrail(trail);
    }

    public static void tick(World level, BlockPos blockPos, BlockState blockState, PathMarkerBlockEntity pathMarkerBlockEntity) {
        Paths.addTickingMarker(pathMarkerBlockEntity);
    }

    @Override
    public @Nullable Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public @NotNull NbtCompound toInitialChunkDataNbt() {
        return this.createNbt();
    }

    public void markUpdated() {
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
     * Apply an NBT compound to the entity. This run on the server.
     *
     * @param nbt The NBT compound
     */
    @Override
    public void applyNbt(NbtCompound nbt) {
        this.data = new HashMap<>();
        if (nbt == null) return;
        for (String key : nbt.getKeys()) {
            if (ArdaPaths.CONFIG.getPath(key) == null) {
                continue;
            }
            NbtData nbtData = NbtData.fromNbt(nbt.getCompound(key));
            this.data.put(key, nbtData);
        }
    }

    /**
     * Convert the entity to an NBT compound.
     *
     * @return The NBT compound
     */
    @Override
    public NbtCompound toNbt(@Nullable NbtCompound nbt) {
        if (nbt == null) nbt = new NbtCompound();
        if (this.data.isEmpty()) {
            return nbt;
        }
        NbtCompound paths = new NbtCompound();
        for (Map.Entry<String, NbtData> entry : this.data.entrySet()) {
            NbtCompound pathNbt = entry.getValue().toNbt();
            if (!pathNbt.isEmpty()) {
                paths.put(entry.getKey(), pathNbt);
            }
        }
        if (!paths.isEmpty()) nbt.put("paths", paths);
        return nbt;
    }

    /**
     * @return The center position of the block entity
     */
    public Vec3d getCenterPos() {
        BlockPos position = this.getPos();
        return new Vec3d(position.getX() + 0.5, position.getY() + 0.5, position.getZ() + 0.5);
    }

    /**
     * Get the NBT data for the given path ID.
     *
     * @param pathId The path ID
     */
    public @NotNull NbtData getNbt(String pathId) {
        return Objects.requireNonNull(this.getNbt(pathId, true));
    }

    /**
     * Get the NBT data for the given path ID.
     *
     * @param pathId The path ID
     * @param createIfNull Whether to create an empty NBT set if no data is found
     */
    public @Nullable NbtData getNbt(String pathId, boolean createIfNull) {
        if (!this.data.containsKey(pathId)) {
            if (createIfNull) {
                this.data.put(pathId, NbtData.empty());
            } else {
                return null;
            }
        }
        return this.data.get(pathId);
    }

    /**
     * Represents the NBT data for a path marker.
     */
    public static class NbtData implements NbtEncodeable {
        private String proximityMessage;
        private int activationRange;
        private BlockPos target;
        private String chapterId;
        private boolean isChapterStart;

        private NbtData(NbtCompound nbt) {
            this("", 0, null, "", false);
            this.applyNbt(nbt);
        }

        private NbtData(String proximityMessage, int activationRange, BlockPos target, String chapterId, boolean isChapterStart) {
            this.proximityMessage = proximityMessage;
            this.activationRange = activationRange;
            this.target = target;
            this.chapterId = chapterId;
            this.isChapterStart = isChapterStart;
        }

        /**
         * Create an NBT data object from an NBT compound.
         *
         * @param nbt The NBT compound
         */
        public static NbtData fromNbt(NbtCompound nbt) {
            return new NbtData(nbt);
        }

        /**
         * Create an empty NBT data object.
         */
        public static NbtData empty() {
            return new NbtData("", 0, null, "", false);
        }

        /**
         * @return The proximity message
         */
        public String getProximityMessage() {
            return proximityMessage;
        }

        /**
         * Set the proximity message.
         *
         * @param proximityMessage The proximity message
         */
        public void setProximityMessage(@NotNull String proximityMessage) {
            this.proximityMessage = proximityMessage;
        }

        /**
         * Remove the proximity message.
         */
        public void removeProximityMessage() {
            this.proximityMessage = "";
        }

        /**
         * @return The activation range
         */
        public int getActivationRange() {
            return activationRange;
        }

        /**
         * Set the activation range.
         *
         * @param activationRange The activation range
         */
        public void setActivationRange(int activationRange) {
            this.activationRange = activationRange;
        }

        /**
         * Remove the activation range.
         */
        public void removeActivationRange() {
            this.activationRange = 0;
        }

        /**
         * @return The target position
         */
        public @Nullable BlockPos getTarget() {
            return target;
        }

        /**
         * Set the target position.
         *
         * @param target The target position
         */
        public void setTarget(@NotNull BlockPos target) {
            this.target = target;
        }

        /**
         * Remove the target position.
         */
        public void removeTarget() {
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
         * Remove the chapter ID.
         */
        public void removeChapterId() {
            this.chapterId = "";
        }

        /**
         * @return Whether the path marker is the start of a chapter
         */
        public boolean isChapterStart() {
            return isChapterStart;
        }

        /**
         * Set whether the path marker is the start of a chapter.
         *
         * @param chapterStart Whether the path marker is the start of a chapter
         */
        public void setChapterStart(boolean chapterStart) {
            isChapterStart = chapterStart;
        }

        /**
         * Apply an NBT compound to the entity data.
         *
         * @param nbt The NBT compound
         */
        @Override
        public void applyNbt(NbtCompound nbt) {
            if (nbt.contains("target")) {
                this.target = NbtHelper.toBlockPos(nbt.getCompound("target"));
            } else {
                this.target = null;
            }
            this.proximityMessage = nbt.getString("proximity_message");
            this.activationRange = nbt.getInt("activation_range");
            this.chapterId = nbt.getString("chapter");
            this.isChapterStart = nbt.getBoolean("chapter_start");
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
            return nbt;
        }
    }
}

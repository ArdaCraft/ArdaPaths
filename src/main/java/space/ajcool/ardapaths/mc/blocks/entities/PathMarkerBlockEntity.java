package space.ajcool.ardapaths.mc.blocks.entities;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import space.ajcool.ardapaths.ArdaPaths;

import java.util.HashMap;
import java.util.Map;
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
import space.ajcool.ardapaths.ArdaPathsConfig;
import space.ajcool.ardapaths.trails.Paths;
import space.ajcool.ardapaths.trails.TrailRenderer;
import space.ajcool.ardapaths.trails.rendering.AnimatedTrail;
import space.ajcool.ardapaths.utils.Color;

public class PathMarkerBlockEntity extends BlockEntity {
    public String proximityMessage = "";
    public int activationRange = 0;
    public Map<Integer, BlockPos> targetOffsets = new HashMap<>();

    public PathMarkerBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(ModBlockEntities.PATH_MARKER, blockPos, blockState);
    }

    public void readNbt(NbtCompound compoundTag) {
        super.readNbt(compoundTag);

        targetOffsets = new HashMap<>();

        ArdaPaths.CONFIG.paths.forEach(pathSettings -> {
            if (compoundTag.contains("targetOffset-" + pathSettings.Id, 10)) {
                targetOffsets.put(pathSettings.Id, NbtHelper.toBlockPos(compoundTag.getCompound("targetOffset-" + pathSettings.Id)));
            }
        });

        if (compoundTag.contains("proximityMessage", 8)) proximityMessage = compoundTag.getString("proximityMessage");
        if (compoundTag.contains("activationRange", 3)) activationRange = compoundTag.getInt("activationRange");
    }

    protected void writeNbt(NbtCompound compoundTag) {
        super.writeNbt(compoundTag);

        targetOffsets.forEach((key, value) -> compoundTag.put("targetOffset-" + key, NbtHelper.fromBlockPos(value)));

        compoundTag.putString("proximityMessage", proximityMessage);
        compoundTag.putInt("activationRange", activationRange);
    }

    public void createTrail(int pathId) {
        if (!targetOffsets.containsKey(pathId)) return;
        ArdaPathsConfig.ColorRGB colorOld = ArdaPaths.CONFIG.paths.get(pathId).PrimaryColor;
        Color color = Color.fromRgb(colorOld.Red, colorOld.Green, colorOld.Blue);
        AnimatedTrail trail = AnimatedTrail.from(this.pos, targetOffsets.get(pathId), color);
        TrailRenderer.registerTrail(trail);
    }

    public static void tick(World level, BlockPos blockPos, BlockState blockState, PathMarkerBlockEntity pathMarkerBlockEntity) {
        Paths.addTickingMarker(pathMarkerBlockEntity);
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public @NotNull NbtCompound toInitialChunkDataNbt()
    {
        return this.createNbt();
    }

    public void markUpdated() {
        this.markDirty();
        this.world.updateListeners(this.getPos(), this.getCachedState(), this.getCachedState(), 3);
    }

    public Vec3d position() {
        BlockPos position = this.getPos();
        return new Vec3d(position.getX() + 0.5, position.getY() + 0.5, position.getZ() + 0.5);
    }
}

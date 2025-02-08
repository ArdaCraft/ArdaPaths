package space.ajcool.ardapaths.mc.blocks.entities;

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
import space.ajcool.ardapaths.ArdaPathsClient;
import space.ajcool.ardapaths.paths.Paths;
import space.ajcool.ardapaths.paths.TrailRenderer;
import space.ajcool.ardapaths.paths.rendering.AnimatedTrail;
import space.ajcool.ardapaths.config.shared.Color;

public class PathMarkerBlockEntity extends BlockEntity {
    private PathMarkerNbt data = PathMarkerNbt.asEmpty();

    public PathMarkerBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(ModBlockEntities.PATH_MARKER, blockPos, blockState);
    }

    public void readNbt(NbtCompound compoundTag) {
        super.readNbt(compoundTag);
        this.data = PathMarkerNbt.fromNbt(compoundTag);
    }

    protected void writeNbt(NbtCompound compoundTag) {
        super.writeNbt(compoundTag);
        this.data.writeNbt(compoundTag);
    }

    public void createTrail(int pathId) {
        if (!this.data.hasTargetOffset(pathId)) return;

        BlockPos targetOffset = this.data.getTargetOffset(pathId);
        Color color = ArdaPathsClient.CONFIG.paths.get(pathId).primaryColor;
        AnimatedTrail trail = AnimatedTrail.from(this.pos, targetOffset, color);
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

    public PathMarkerNbt data() {
        return data;
    }
}

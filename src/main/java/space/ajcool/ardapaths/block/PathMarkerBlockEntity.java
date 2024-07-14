package space.ajcool.ardapaths.block;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
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
import space.ajcool.ardapaths.ArdaPathsClient;

import java.util.HashMap;
import java.util.Map;

public class PathMarkerBlockEntity extends BlockEntity
{
    public String proximityMessage = "";
    public int activationRange = 0;
    public Map<Integer, BlockPos> targetOffsets = new HashMap<>();

    public PathMarkerBlockEntity(BlockPos blockPos, BlockState blockState)
    {
        super(ArdaPaths.PATH_MARKER_BLOCK_ENTITY, blockPos, blockState);
    }

    public void load(CompoundTag compoundTag) {
        super.load(compoundTag);

        targetOffsets = new HashMap<>();

        ArdaPaths.CONFIG.paths.forEach(pathSettings -> {
            if (compoundTag.contains("targetOffset-" + pathSettings.Id, 10))
            {
                targetOffsets.put(pathSettings.Id, NbtUtils.readBlockPos(compoundTag.getCompound("targetOffset-" + pathSettings.Id)));
            }
        });

        if (compoundTag.contains("proximityMessage", 8)) proximityMessage = compoundTag.getString("proximityMessage");
        if (compoundTag.contains("activationRange", 3)) activationRange = compoundTag.getInt("activationRange");
    }

    protected void saveAdditional(CompoundTag compoundTag) {
        super.saveAdditional(compoundTag);

        targetOffsets.forEach((key, value) -> compoundTag.put("targetOffset-" + key, NbtUtils.writeBlockPos(value)));

        compoundTag.putString("proximityMessage", proximityMessage);
        compoundTag.putInt("activationRange", activationRange);
    }

    public void createTrail(int pathId)
    {
        if (!targetOffsets.containsKey(pathId)) return;

        ArdaPathsClient.addAnimationTrail(this.worldPosition, targetOffsets.get(pathId), pathId);
    }

    public static void tick(Level level, BlockPos blockPos, BlockState blockState, PathMarkerBlockEntity pathMarkerBlockEntity)
    {
        ArdaPathsClient.addToPathMarkerToTickingList(pathMarkerBlockEntity);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket()
    {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag()
    {
        return this.saveWithoutMetadata();
    }

    public void markUpdated() {
        this.setChanged();
        this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
    }

    public Vec3 position() {
        var position = this.getBlockPos();

        return new Vec3(position.getX() + 0.5, position.getY() + 0.5, position.getZ() + 0.5);
    }
}

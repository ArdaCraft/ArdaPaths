package space.ajcool.ardapaths.mc.blocks.entities;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.util.math.BlockPos;
import space.ajcool.ardapaths.mc.NbtEncodeable;

import java.util.HashMap;
import java.util.Map;

public class PathMarkerNbt implements NbtEncodeable {
    private String proximityMessage;
    private int activationRange;
    private Map<Integer, BlockPos> targetOffsets;
    private String chapterId;

    public PathMarkerNbt(String proximityMessage, int activationRange, Map<Integer, BlockPos> targetOffsets, String chapterId) {
        this.proximityMessage = proximityMessage;
        this.activationRange = activationRange;
        this.targetOffsets = targetOffsets;
        this.chapterId = chapterId;
    }

    public String getProximityMessage() {
        return proximityMessage;
    }

    public void setProximityMessage(String proximityMessage) {
        this.proximityMessage = proximityMessage;
    }

    public boolean hasProximityMessage() {
        return proximityMessage != null && !proximityMessage.isEmpty();
    }

    public int getActivationRange() {
        return activationRange;
    }

    public void setActivationRange(int activationRange) {
        this.activationRange = activationRange;
    }

    public Map<Integer, BlockPos> getTargetOffsets() {
        return targetOffsets;
    }

    public BlockPos getTargetOffset(int pathId) {
        return targetOffsets.get(pathId);
    }

    public void addTargetOffset(int pathId, BlockPos offset) {
        targetOffsets.put(pathId, offset);
    }

    public void removeTargetOffset(int pathId) {
        targetOffsets.remove(pathId);
    }

    public boolean hasTargetOffset(int pathId) {
        return targetOffsets.containsKey(pathId);
    }

    public String getChapterId() {
        return chapterId;
    }

    public void setChapterId(String chapterId) {
        this.chapterId = chapterId;
    }

    public boolean hasChapterId() {
        return chapterId != null && !chapterId.isEmpty();
    }

    public static PathMarkerNbt fromNbt(NbtCompound nbt) {
        Map<Integer, BlockPos> targetOffsets = new HashMap<>();
        nbt.getKeys().forEach(key -> {
            if (key.startsWith("targetOffset-")) {
                int pathId = Integer.parseInt(key.substring(13));
                targetOffsets.put(pathId, NbtHelper.toBlockPos(nbt.getCompound(key)));
            }
        });
        return new PathMarkerNbt(
                nbt.getString("proximityMessage"),
                nbt.getInt("activationRange"),
                targetOffsets,
                nbt.getString("chapterId")
        );
    }

    @Override
    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        if (proximityMessage != null) nbt.putString("proximityMessage", proximityMessage);
        if (proximityMessage != null) nbt.putInt("activationRange", activationRange);
        targetOffsets.forEach((key, value) -> nbt.put("targetOffset-" + key, NbtHelper.fromBlockPos(value)));
        if (chapterId != null) nbt.putString("chapterId", chapterId);
        return nbt;
    }

    public static PathMarkerNbt asEmpty() {
        return new PathMarkerNbt("", 0, new HashMap<>(), null);
    }
}

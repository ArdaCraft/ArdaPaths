package space.ajcool.ardapaths.core.data.config.server;

import com.google.gson.annotations.SerializedName;
import net.minecraft.core.BlockPos;

/**
 * Represents a 3D block position (x, y, z) that can be serialized to/from JSON
 * and converted to Minecraft {@link BlockPos} objects.
 *
 * @param x The X coordinate of this position.
 * @param y The Y coordinate of this position.
 * @param z The Z coordinate of this position.
 */
public record PositionData(@SerializedName("x") int x, @SerializedName("y") int y, @SerializedName("z") int z) {

    /**
     * Create a new {@link PositionData} from a block position.
     *
     * @param pos The block position
     */
    public static PositionData fromBlockPos(BlockPos pos) {
        return new PositionData(pos.getX(), pos.getY(), pos.getZ());
    }

    /**
     * @return The block position
     */
    public BlockPos toBlockPos() {
        return new BlockPos(x, y, z);
    }
}

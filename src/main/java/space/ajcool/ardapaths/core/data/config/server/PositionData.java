package space.ajcool.ardapaths.core.data.config.server;

import com.google.gson.annotations.SerializedName;
import net.minecraft.util.math.BlockPos;

public class PositionData {
    @SerializedName("x")
    private final int x;

    @SerializedName("y")
    private final int y;

    @SerializedName("z")
    private final int z;

    public PositionData(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * @return The X coordinate
     */
    public int getX() {
        return x;
    }

    /**
     * @return The Y coordinate
     */
    public int getY() {
        return y;
    }

    /**
     * @return The Z coordinate
     */
    public int getZ() {
        return z;
    }

    /**
     * @return The block position
     */
    public BlockPos toBlockPos() {
        return new BlockPos(x, y, z);
    }

    /**
     * Create a new {@link PositionData} from a block position.
     *
     * @param pos The block position
     */
    public static PositionData fromBlockPos(BlockPos pos) {
        return new PositionData(pos.getX(), pos.getY(), pos.getZ());
    }
}

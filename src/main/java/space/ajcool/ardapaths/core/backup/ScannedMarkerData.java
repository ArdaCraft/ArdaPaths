package space.ajcool.ardapaths.core.backup;

import net.minecraft.util.math.BlockPos;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;

import java.util.Map;

/**
 * Marker data discovered while scanning saved region files.
 *
 * @param dimensionId dimension identifier containing the marker
 * @param position    absolute marker block position
 * @param pathData    per-path chapter NBT payloads stored on the marker
 */
public record ScannedMarkerData(
        String dimensionId,
        BlockPos position,
        Map<String, Map<String, PathMarkerBlockEntity.ChapterNbtData>> pathData
) {
}

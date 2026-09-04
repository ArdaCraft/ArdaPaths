package space.ajcool.ardapaths.core.integration;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Resolved optional warp destination used as a chapter-start anchor.
 *
 * @param worldKey registry key for the destination world
 * @param position block position nearest the warp destination
 */
public record WarpLocation(ResourceKey<Level> worldKey, BlockPos position) {

}

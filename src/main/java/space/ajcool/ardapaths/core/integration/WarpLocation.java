package space.ajcool.ardapaths.core.integration;

import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Resolved optional warp destination used as a chapter-start anchor.
 *
 * @param worldKey registry key for the destination world
 * @param position block position nearest the warp destination
 */
public record WarpLocation(RegistryKey<World> worldKey, BlockPos position) {
}

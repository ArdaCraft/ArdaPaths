package space.ajcool.ardapaths.mc.blocks.entities;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.entity.BlockEntityType;
import space.ajcool.ardapaths.core.ModConstants;
import space.ajcool.ardapaths.mc.blocks.ModBlocks;

/**
 * Registry for custom block entity types in ArdaPaths.
 * Handles registration of block entity types with the Minecraft registry.
 */
public class ModBlockEntities {

    /**
     * Registry key for the Path Marker block entity type.
     */
    public static final ResourceKey<BlockEntityType<?>> PATH_MARKER_KEY = ResourceKey.create(
            Registries.BLOCK_ENTITY_TYPE,
            ModConstants.modId("path_marker_block_entity")
    );

    /**
     * Register a block entity type.
     *
     * @param <T>  the type of block entity type
     * @param key  the block entity type registry key
     * @param type the block entity type to register
     * @return the registered block entity type
     */
    @SuppressWarnings("SameParameterValue")
    private static <T extends BlockEntityType<?>> T register(final ResourceKey<BlockEntityType<?>> key, final T type) {
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, key, type);
    }

    /**
     * Initializes the block entity type registry by forcing class loading.
     * This method is called during mod initialization.
     */
    @SuppressWarnings("EmptyMethod")
    public static void init() {
    }

    /**
     * The block entity type for Path Marker blocks, used to store and load marker configuration.
     */
    public static final BlockEntityType<PathMarkerBlockEntity> PATH_MARKER = register(
            PATH_MARKER_KEY,
            BlockEntityType.Builder.of(PathMarkerBlockEntity::new, ModBlocks.PATH_MARKER).build(null)
    );

}

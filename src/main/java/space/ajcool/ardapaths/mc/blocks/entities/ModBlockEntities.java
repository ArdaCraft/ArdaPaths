package space.ajcool.ardapaths.mc.blocks.entities;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.mc.blocks.ModBlocks;

/**
 * Registry for custom block entity types in ArdaPaths.
 * Handles registration of block entity types with the Minecraft registry.
 */
public class ModBlockEntities {
    /**
     * Register a block entity type.
     *
     * @param id   The ID of the block entity
     * @param type The block entity type
     */
    @SuppressWarnings("SameParameterValue")
    private static <T extends BlockEntityType<?>> T register(final String id, final T type) {
        return Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(ArdaPaths.MOD_ID, id), type);
    }    /**
     * The block entity type for Path Marker blocks, used to store and load marker configuration.
     */
    public static final BlockEntityType<PathMarkerBlockEntity> PATH_MARKER = register(
            "path_marker_block_entity",
            FabricBlockEntityTypeBuilder.create(PathMarkerBlockEntity::new, ModBlocks.PATH_MARKER).build()
    );

    /**
     * Initializes the block entity type registry by forcing class loading.
     * This method is called during mod initialization.
     */
    public static void init() {
    }


}

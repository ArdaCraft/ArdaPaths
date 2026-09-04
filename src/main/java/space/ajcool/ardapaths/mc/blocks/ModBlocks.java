package space.ajcool.ardapaths.mc.blocks;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import space.ajcool.ardapaths.core.ModConstants;

/**
 * Registry for all custom blocks in ArdaPaths.
 * Handles block registration with the Minecraft registry.
 */
public class ModBlocks {

    /**
     * The registry identifier for the Path Marker block.
     */
    public static final String PATH_MARKER_ID = "path_marker";

    /**
     * Registry key for the Path Marker block.
     */
    public static final ResourceKey<Block> PATH_MARKER_KEY = ResourceKey.create(Registries.BLOCK, ModConstants.modId(PATH_MARKER_ID));

    /**
     * Registry key for the Path Marker block item.
     */
    public static final ResourceKey<Item> PATH_MARKER_ITEM_KEY = ResourceKey.create(Registries.ITEM, ModConstants.modId(PATH_MARKER_ID));

    /**
     * The Path Marker block instance, which is placed to define trail points in the world.
     */
    public static final PathMarkerBlock PATH_MARKER = register(
            PATH_MARKER_KEY,
            new PathMarkerBlock(BlockBehaviour.Properties.of()
                    .setId(PATH_MARKER_KEY)
                    .noOcclusion()
                    .noCollision()
                    .noLootTable()
                    .strength(-1.0f, 3600000.0f)
            )
    );

    /**
     * Register a block and its respective item.
     *
     * @param <T>   the type of block
     * @param key   the block's registry key
     * @param block the block to register
     * @return the registered block
     */
    @SuppressWarnings("SameParameterValue")
    private static <T extends Block> T register(final ResourceKey<Block> key, final T block) {
        Registry.register(BuiltInRegistries.BLOCK, key, block);
        Registry.register(BuiltInRegistries.ITEM, PATH_MARKER_ITEM_KEY, new BlockItem(block, new Item.Properties()
                .setId(PATH_MARKER_ITEM_KEY)
                .useBlockDescriptionPrefix()));
        return block;
    }

    /**
     * Initializes the block registry by forcing class loading.
     * This method is called during mod initialization.
     */
    @SuppressWarnings("EmptyMethod")
    public static void init() {
    }
}

package space.ajcool.ardapaths.mc.blocks;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import space.ajcool.ardapaths.ArdaPaths;

public class ModBlocks
{
    public static final PathMarkerBlock PATH_MARKER = register(
            "path_marker",
            new PathMarkerBlock(FabricBlockSettings.create()
                    .nonOpaque()
                    .collidable(false)
                    .dropsNothing()
                    .strength(-1.0f, 3600000.0f)
            )
    );

    /**
     * Register a block and its respective item.
     *
     * @param id    The block's ID
     * @param block The block to register
     */
    private static <T extends Block> T register(final String id, final T block)
    {
        final Identifier identifier = Identifier.of(ArdaPaths.MOD_ID, id);
        Registry.register(Registries.BLOCK, identifier, block);
        Registry.register(Registries.ITEM, identifier, new BlockItem(block, new FabricItemSettings()));
        return block;
    }

    public static void init()
    {
    }
}

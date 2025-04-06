package space.ajcool.ardapaths.mc.items;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.mc.blocks.ModBlocks;

public class ModItems
{
    public static final PathRevealerItem PATH_REVEALER = register(
            "path_revealer",
            new PathRevealerItem(new FabricItemSettings().maxCount(1).fireproof().rarity(Rarity.EPIC))
    );
    public static final Item PATH_MARKER = ModBlocks.PATH_MARKER.asItem();

    /**
     * Register an item and add it to an item group.
     *
     * @param id   The item's ID
     * @param item The item to register
     */
    private static <T extends Item> T register(final String id, final T item)
    {
        Registry.register(Registries.ITEM, Identifier.of(ArdaPaths.MOD_ID, id), item);
        return item;
    }

    public static void init()
    {
    }
}

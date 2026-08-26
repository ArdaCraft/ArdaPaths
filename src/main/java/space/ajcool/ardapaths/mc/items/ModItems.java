package space.ajcool.ardapaths.mc.items;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.mc.blocks.ModBlocks;

/**
 * Registry for all custom items in ArdaPaths.
 * Handles item registration with the Minecraft registry.
 */
public class ModItems {
    /**
     * The registry identifier for the Path Revealer item.
     */
    public static final String PATH_REVEALER_ID = "path_revealer";

    /**
     * The Path Revealer item, used to activate path rendering and trail visualization.
     */
    public static final PathRevealerItem PATH_REVEALER = register(
            PATH_REVEALER_ID,
            new PathRevealerItem(new FabricItemSettings().maxCount(1).fireproof().rarity(Rarity.EPIC))
    );

    /**
     * The Path Marker item, which is the item form of the Path Marker block.
     */
    public static final Item PATH_MARKER = ModBlocks.PATH_MARKER.asItem();

    /**
     * Register an item and add it to an item group.
     *
     * @param <T> the type of item
     * @param id the item's ID
     * @param item the item to register
     * @return the registered item
     */
    @SuppressWarnings("SameParameterValue")
    private static <T extends Item> T register(final String id, final T item) {
        Registry.register(Registries.ITEM, Identifier.of(ArdaPaths.MOD_ID, id), item);
        return item;
    }

    /**
     * Initializes the item registry by forcing class loading.
     * This method is called during mod initialization.
     */
    public static void init() {
    }
}

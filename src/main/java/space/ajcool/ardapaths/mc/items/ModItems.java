package space.ajcool.ardapaths.mc.items;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import space.ajcool.ardapaths.core.ModConstants;
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
     * Registry key for the Path Revealer item.
     */
    public static final ResourceKey<Item> PATH_REVEALER_KEY = ResourceKey.create(Registries.ITEM, ModConstants.modId(PATH_REVEALER_ID));

    /**
     * Registry key for the Path Marker block item.
     */
    @SuppressWarnings("unused")
    public static final ResourceKey<Item> PATH_MARKER_KEY = ModBlocks.PATH_MARKER_ITEM_KEY;

    /**
     * The Path Revealer item, used to activate path rendering and trail visualization.
     */
    public static final PathRevealerItem PATH_REVEALER = register(
            PATH_REVEALER_KEY,
            new PathRevealerItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC))
    );

    /**
     * The Path Marker item, which is the item form of the Path Marker block.
     */
    public static final Item PATH_MARKER = ModBlocks.PATH_MARKER.asItem();

    /**
     * Register an item and add it to an item group.
     *
     * @param <T> the type of item
     * @param key the item's registry key
     * @param item the item to register
     * @return the registered item
     */
    @SuppressWarnings("SameParameterValue")
    private static <T extends Item> T register(final ResourceKey<Item> key, final T item) {
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    /**
     * Initializes the item registry by forcing class loading.
     * This method is called during mod initialization.
     */
    @SuppressWarnings("EmptyMethod")
    public static void init() {
    }
}

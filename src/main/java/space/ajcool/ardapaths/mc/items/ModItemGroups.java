package space.ajcool.ardapaths.mc.items;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import space.ajcool.ardapaths.core.ModConstants;
import space.ajcool.ardapaths.mc.blocks.ModBlocks;

/**
 * Registry for custom creative mode item groups in ArdaPaths.
 * Handles item group registration and item group membership.
 */
@SuppressWarnings("unused")
public class ModItemGroups {
    /**
     * Registry key for the ArdaPaths creative inventory group.
     */
    public static final ResourceKey<CreativeModeTab> PATH_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, ModConstants.modId("path"));

    /**
     * The primary item group for ArdaPaths items, displayed in the creative inventory.
     * Contains the Path Revealer and Path Marker items.
     */
    public static final CreativeModeTab PATH = register(
            PATH_KEY,
            FabricItemGroup.builder()
                    .icon(ModItems.PATH_REVEALER::getDefaultInstance)
                    .title(Component.translatable("itemGroup.ardapaths.ardapaths"))
                    .build(),
            ModItems.PATH_REVEALER,
            ModBlocks.PATH_MARKER.asItem()
    );

    /**
     * Register an item group.
     *
     * @param key the item group's registry key
     * @param group the item group to register
     * @param items the items to add to the item group
     * @return the registered item group
     */
    @SuppressWarnings("SameParameterValue")
    private static CreativeModeTab register(final ResourceKey<CreativeModeTab> key, final CreativeModeTab group, Item... items) {
        if (BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(key)) {
            return BuiltInRegistries.CREATIVE_MODE_TAB.get(key);
        }

        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, key, group);
        ItemGroupEvents.modifyEntriesEvent(key).register(itemGroup ->
        {
            for (Item item : items) {
                itemGroup.accept(item.getDefaultInstance());
            }
        });

        return group;
    }

    /**
     * Initializes the item group registry by forcing class loading.
     * This method is called during mod initialization.
     */
    @SuppressWarnings("EmptyMethod")
    public static void init() {
    }
}

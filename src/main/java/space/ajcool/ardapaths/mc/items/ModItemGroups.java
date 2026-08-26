package space.ajcool.ardapaths.mc.items;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.mc.blocks.ModBlocks;

/**
 * Registry for custom creative mode item groups in ArdaPaths.
 * Handles item group registration and item group membership.
 */
@SuppressWarnings("unused")
public class ModItemGroups {
    /**
     * The primary item group for ArdaPaths items, displayed in the creative inventory.
     * Contains the Path Revealer and Path Marker items.
     */
    public static final ItemGroup PATH = register(
            "path",
            FabricItemGroup.builder()
                    .icon(ModItems.PATH_REVEALER::getDefaultStack)
                    .displayName(Text.translatable("itemGroup.ardapaths.ardapaths"))
                    .build(),
            ModItems.PATH_REVEALER,
            ModBlocks.PATH_MARKER.asItem()
    );

    /**
     * Register an item group.
     *
     * @param id the item group's ID
     * @param group the item group to register
     * @param items the items to add to the item group
     * @return the registered item group
     */
    @SuppressWarnings("SameParameterValue")
    private static ItemGroup register(final String id, final ItemGroup group, Item... items) {
        RegistryKey<ItemGroup> key = RegistryKey.of(Registries.ITEM_GROUP.getKey(), new Identifier(ArdaPaths.MOD_ID, id));
        if (Registries.ITEM_GROUP.contains(key)) {
            return Registries.ITEM_GROUP.get(key);
        }

        Registry.register(Registries.ITEM_GROUP, key, group);
        ItemGroupEvents.modifyEntriesEvent(key).register(itemGroup ->
        {
            for (Item item : items) {
                itemGroup.add(item.getDefaultStack());
            }
        });

        return group;
    }

    /**
     * Initializes the item group registry by forcing class loading.
     * This method is called during mod initialization.
     */
    public static void init() {
    }
}
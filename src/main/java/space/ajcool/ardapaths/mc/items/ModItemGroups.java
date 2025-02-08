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

public class ModItemGroups {
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
     * @param id The item group's ID
     * @param group The item group to register
     * @param items The items to add to the item group
     */
    private static ItemGroup register(final String id, final ItemGroup group, Item ...items) {
        RegistryKey<ItemGroup> key = RegistryKey.of(Registries.ITEM_GROUP.getKey(), new Identifier(ArdaPaths.MOD_ID, id));
        if (Registries.ITEM_GROUP.contains(key)) {
            return Registries.ITEM_GROUP.get(key);
        }

        Registry.register(Registries.ITEM_GROUP, key, group);
        ItemGroupEvents.modifyEntriesEvent(key).register(itemGroup -> {
            for (Item item : items) {
                itemGroup.add(item.getDefaultStack());
            }
        });

        return group;
    }

    public static void init() {}
}

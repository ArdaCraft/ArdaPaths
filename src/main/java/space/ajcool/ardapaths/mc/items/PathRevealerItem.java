package space.ajcool.ardapaths.mc.items;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import space.ajcool.ardapaths.ArdaPathsClient;
import space.ajcool.ardapaths.core.data.config.shared.PathData;
import space.ajcool.ardapaths.screens.Screens;

import java.util.List;

/**
 * The Path Revealer item used to activate path rendering and trail visualization.
 * When held, displays animated trails and proximity messages.
 * Right-clicking opens the path selection screen.
 */
public class PathRevealerItem extends Item {
    /**
     * Constructs a Path Revealer item with the given properties.
     *
     * @param properties the item settings (max count 1, fireproof, epic rarity)
     */
    public PathRevealerItem(Settings properties) {
        super(properties);
    }

    /**
     * Opens the path selection screen when the player uses this item.
     *
     * @param level           the world
     * @param player          the player using the item
     * @param interactionHand the hand used
     * @return the action result with the item stack
     */
    @Environment(EnvType.CLIENT)
    @Override
    public TypedActionResult<ItemStack> use(World level, PlayerEntity player, Hand interactionHand) {
        if (level.isClient()) {
            Screens.openSelectionScreen();
        }

        return super.use(level, player, interactionHand);
    }

    /**
     * Appends tooltip information showing the current path and usage instructions.
     *
     * @param itemStack   the item stack
     * @param level       the world, or null in inventory
     * @param list        the tooltip lines to append to
     * @param tooltipFlag the tooltip context
     */
    @Override
    public void appendTooltip(ItemStack itemStack, @Nullable World level, List<Text> list, TooltipContext tooltipFlag) {
        super.appendTooltip(itemStack, level, list, tooltipFlag);

        PathData path = ArdaPathsClient.CONFIG.getSelectedPath();
        if (path != null) {
            var text = Text.literal("You are currently on ").formatted(Formatting.GRAY).append(Text.literal(path.getName()).fillStyle(Style.EMPTY.withColor(path.getPrimaryColor().asHex())));
            list.add(text);
        }

        list.add(Text.literal(" "));
        list.add(Text.literal("Hold ").formatted(Formatting.AQUA).append(Text.literal("this item to start pathfinding.").formatted(Formatting.GRAY)));
        list.add(Text.literal("Right Click ").formatted(Formatting.AQUA).append(Text.literal("to change your path.").formatted(Formatting.GRAY)));
    }
}

package space.ajcool.ardapaths.mc.items;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.NonNull;
import space.ajcool.ardapaths.ArdaPathsClient;
import space.ajcool.ardapaths.core.data.config.shared.PathData;
import space.ajcool.ardapaths.screens.Screens;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

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
    public PathRevealerItem(Properties properties) {
        super(properties);
    }

    /**
     * Opens the path selection screen when the player uses this item.
     *
     * @param level           the world
     * @param player          the player using the item
     * @param interactionHand the hand used
     * @return the action result
     */
    @Environment(EnvType.CLIENT)
    @Override
    public @NonNull InteractionResult use(@NonNull Level level, @NonNull Player player, @NonNull InteractionHand interactionHand) {
        openSelectionScreen(level);

        return super.use(level, player, interactionHand);
    }

    /**
     * Opens the selection screen when the item is used on a client world.
     *
     * @param level the world where the item was used
     */
    @Environment(EnvType.CLIENT)
    private static void openSelectionScreen(Level level) {

        if (!level.isClientSide())
            return;

        Screens.openSelectionScreen();
    }

    /**
     * Appends tooltip information showing the current path and usage instructions.
     *
     * @param itemStack   the item stack
     * @param context     the item tooltip context
     * @param tooltipDisplay tooltip component visibility settings
     * @param tooltip     tooltip line sink
     * @param tooltipFlag the tooltip context
     */
    // Dynamic client-config tooltip still uses Minecraft's item override in 26.1.2.
    @SuppressWarnings("deprecation")
    @Override
    public void appendHoverText(@NonNull ItemStack itemStack, @NonNull TooltipContext context, @NonNull TooltipDisplay tooltipDisplay, @NonNull Consumer<Component> tooltip, @NonNull TooltipFlag tooltipFlag) {
        super.appendHoverText(itemStack, context, tooltipDisplay, tooltip, tooltipFlag);
        createTooltipLines().forEach(tooltip);
    }

    /**
     * Builds the tooltip lines for the Path Revealer item.
     *
     * @return tooltip lines to append
     */
    private static List<Component> createTooltipLines() {
        List<Component> lines = new ArrayList<>();
        PathData path = ArdaPathsClient.CONFIG.getSelectedPath();
        if (path != null) {
            var text = Component.literal("You are currently on ").withStyle(ChatFormatting.GRAY).append(Component.literal(path.getName()).withStyle(Style.EMPTY.withColor(path.getPrimaryColor().asHex())));
            lines.add(text);
        }

        lines.add(Component.literal(" "));
        lines.add(Component.literal("Hold ").withStyle(ChatFormatting.AQUA).append(Component.literal("this item to start pathfinding.").withStyle(ChatFormatting.GRAY)));
        lines.add(Component.literal("Right Click ").withStyle(ChatFormatting.AQUA).append(Component.literal("to change your path.").withStyle(ChatFormatting.GRAY)));
        return lines;
    }
}

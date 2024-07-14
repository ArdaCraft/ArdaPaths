package space.ajcool.ardapaths.item;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.ArdaPathsClient;
import space.ajcool.ardapaths.ArdaPathsConfig;
import space.ajcool.ardapaths.screen.PathSelectionScreen;

import java.util.List;

public class PathRevealerItem extends Item
{
    public PathRevealerItem(Properties properties)
    {
        super(properties);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand interactionHand)
    {
        if (level.isClientSide()) ArdaPathsClient.openSelectionScreen();

        return super.use(level, player, interactionHand);
    }

    @Override
    public void appendHoverText(ItemStack itemStack, @Nullable Level level, List<Component> list, TooltipFlag tooltipFlag)
    {
        super.appendHoverText(itemStack, level, list, tooltipFlag);

        for (ArdaPathsConfig.PathSettings path : ArdaPaths.CONFIG.paths)
        {
            if (PathSelectionScreen.selectedPathId != path.Id) continue;

            var text = Component.literal("You are currently on ").withStyle(ChatFormatting.GRAY).append(Component.literal(path.Name).withStyle(Style.EMPTY.withColor(path.PrimaryColor.encodedColor())));

            list.add(text);
        }

        list.add(Component.literal(" "));
        list.add(Component.literal("Hold ").withStyle(ChatFormatting.AQUA).append(Component.literal("this item to start pathfinding.").withStyle(ChatFormatting.GRAY)));
        list.add(Component.literal("Right Click ").withStyle(ChatFormatting.AQUA).append(Component.literal("to change your path.").withStyle(ChatFormatting.GRAY)));
    }
}

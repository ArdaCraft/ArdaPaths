package space.ajcool.ardapaths.paths.rendering;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import space.ajcool.ardapaths.ArdaPathsClient;
import space.ajcool.ardapaths.core.ModConstants;
import space.ajcool.ardapaths.mc.items.ModItems;
import space.ajcool.ardapaths.paths.movement.FocusController;
import space.ajcool.ardapaths.screens.GuiTextures;

/**
 * Persistent HUD prompt shown when a nearby marker offers an authored focus target.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FocusPromptRenderer {

    /**
     * Texture used to indicate the focus action.
     */
    private static final Identifier EYE_ICON = ModConstants.modId("eye-icon");

    /**
     * Drawn icon size in screen pixels.
     */
    private static final int ICON_SIZE = 12;

    /**
     * Horizontal gap between the icon and label.
     */
    private static final int ICON_TEXT_GAP = 4;

    /**
     * Vertical clearance above the hotbar.
     */
    private static final int HOTBAR_CLEARANCE = 22;

    /**
     * Renders the focus prompt when a candidate is available and focus is idle.
     *
     * @param context      the drawing context used for HUD rendering
     * @param ignoredDelta the frame delta supplied by Fabric
     */
    public static void render(GuiGraphicsExtractor context, DeltaTracker ignoredDelta) {
        if (!FocusController.hasCandidate() || FocusController.isEngaged() || ArdaPathsClient.FOCUS_KEY == null) return;

        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || !player.isHolding(ModItems.PATH_REVEALER)) return;

        Font textRenderer = client.font;
        Component label = Component.translatable("ardapaths.client.focus.prompt", ArdaPathsClient.FOCUS_KEY.getTranslatedKeyMessage());
        int textWidth = textRenderer.width(label);
        int rowWidth = ICON_SIZE + ICON_TEXT_GAP + textWidth;
        int x = (context.guiWidth() - rowWidth) / 2;
        int y = context.guiHeight() - HOTBAR_CLEARANCE - ICON_TEXT_GAP - ICON_SIZE;

        GuiTextures.blitSprite(context, EYE_ICON, x, y, ICON_SIZE, ICON_SIZE);
        context.text(textRenderer, label, x + ICON_SIZE + ICON_TEXT_GAP, y + 2, 0xFFFFFFFF, true);
    }
}

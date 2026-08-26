package space.ajcool.ardapaths.paths.rendering;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.ArdaPathsClient;
import space.ajcool.ardapaths.mc.items.ModItems;
import space.ajcool.ardapaths.paths.movement.FocusController;

/**
 * Persistent HUD prompt shown when a nearby marker offers an authored focus target.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FocusPromptRenderer {
    /**
     * Texture used to indicate the focus action.
     */
    private static final Identifier EYE_ICON = new Identifier(ArdaPaths.MOD_ID, "textures/gui/eye-icon.png");

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
     * @param context the drawing context used for HUD rendering
     * @param ignoredDelta the frame delta supplied by Fabric
     */
    public static void render(DrawContext context, float ignoredDelta) {
        if (!FocusController.hasCandidate() || FocusController.isEngaged() || ArdaPathsClient.FOCUS_KEY == null) return;

        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || !player.isHolding(ModItems.PATH_REVEALER)) return;

        TextRenderer textRenderer = client.textRenderer;
        Text label = Text.translatable("ardapaths.client.focus.prompt", ArdaPathsClient.FOCUS_KEY.getBoundKeyLocalizedText());
        int textWidth = textRenderer.getWidth(label);
        int rowWidth = ICON_SIZE + ICON_TEXT_GAP + textWidth;
        int x = (context.getScaledWindowWidth() - rowWidth) / 2;
        int y = context.getScaledWindowHeight() - HOTBAR_CLEARANCE - ICON_TEXT_GAP - ICON_SIZE;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        context.drawTexture(EYE_ICON, x, y, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        context.drawText(textRenderer, label, x + ICON_SIZE + ICON_TEXT_GAP, y + 2, 0xFFFFFF, true);

        RenderSystem.disableBlend();
    }
}

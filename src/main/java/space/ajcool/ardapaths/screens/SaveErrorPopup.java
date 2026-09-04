package space.ajcool.ardapaths.screens;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Error popup shown when a marker save is rejected or cannot be applied by the server.
 */
public class SaveErrorPopup extends ArdaPathsScreen {

    /** Message text displayed in the popup. */
    private final Component message;

    /** Screen to return to after the popup closes. */
    private final Screen parentScreen;

    /** Callback invoked once when the popup is dismissed. */
    private final Runnable onDismiss;

    /** Whether dismissal work has already been run. */
    private boolean dismissed;

    /**
     * Constructs a save error popup.
     *
     * @param message      message displayed in the popup
     * @param parentScreen screen to return to after dismissal
     * @param onDismiss    callback invoked after dismissal
     */
    public SaveErrorPopup(Component message, Screen parentScreen, Runnable onDismiss) {
        super(Component.literal("Save Error"));
        this.message = message;
        this.parentScreen = parentScreen;
        this.onDismiss = onDismiss;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        this.addRenderableWidget(Button.builder(Component.translatable("ardapaths.generic.ok"), button -> onClose())
                .bounds(centerX - 25, centerY + 10, 50, 20)
                .build());
    }

    @Override
    public void onClose() {
        if (!dismissed) {
            dismissed = true;
            onDismiss.run();
        }

        if (minecraft != null) {
            minecraft.setScreen(parentScreen);
        }
    }

    /**
     * Renders the save error message after the modal blur pass has completed.
     *
     * @param context drawing context
     * @param mouseX  current mouse x coordinate
     * @param mouseY  current mouse y coordinate
     * @param delta   partial tick delta
     */
    @Override
    protected void renderModContent(GuiGraphics context, int mouseX, int mouseY, float delta) {
        context.drawCenteredString(this.font, this.message, this.width / 2, this.height / 2 - 20, 0xFFFFFF);
    }
}

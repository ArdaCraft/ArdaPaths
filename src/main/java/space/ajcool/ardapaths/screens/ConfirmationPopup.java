package space.ajcool.ardapaths.screens;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * A confirmation dialogue popup screen with yes/no buttons.
 * Displays a centered message and executes callbacks when the user confirms or cancels.
 */
public class ConfirmationPopup extends ArdaPathsScreen {

    /**
     * Callback invoked when the player confirms.
     */
    private final Runnable onConfirm;

    /**
     * Callback invoked when the player cancels.
     */
    private final Runnable onCancel;

    /**
     * The message text displayed in the confirmation dialogue.
     */
    private final Component message;

    /**
     * The screen to return to after the dialogue closes.
     */
    private final Screen parentScreen;

    /**
     * Text for the confirmation button.
     */
    private final Component confirmButtonText = Component.translatable("ardapaths.generic.yes");

    /**
     * Text for the cancel button.
     */
    private final Component cancelButtonText = Component.translatable("ardapaths.generic.no");

    /**
     * Whether to close completely or return to parent screen.
     */
    private final boolean closeOnValidate;

    /**
     * Constructs a ConfirmationPopup with default behaviour (return to parent).
     *
     * @param message      the confirmation message
     * @param onConfirm    callback when confirmed
     * @param onCancel     callback when cancelled
     * @param parentScreen the screen to return to
     */
    public ConfirmationPopup(Component message, Runnable onConfirm, Runnable onCancel, Screen parentScreen) {
        this(message, onConfirm, onCancel, parentScreen, false);
    }

    /**
     * Constructs a ConfirmationPopup with custom close behaviour.
     *
     * @param message         the confirmation message
     * @param onConfirm       callback when confirmed
     * @param onCancel        callback when cancelled
     * @param parentScreen    the screen to return to
     * @param closeOnValidate whether to close completely instead of returning to parent
     */
    public ConfirmationPopup(Component message, Runnable onConfirm, Runnable onCancel, Screen parentScreen, boolean closeOnValidate) {
        super(Component.literal("Confirm"));
        this.message = message;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
        this.parentScreen = parentScreen;
        this.closeOnValidate = closeOnValidate;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        this.addRenderableWidget(Button.builder(confirmButtonText, button -> {
            onConfirm.run();
            onClose();
        }).bounds(centerX - 60, centerY + 10, 50, 20).build());

        this.addRenderableWidget(Button.builder(cancelButtonText, button -> {
            onCancel.run();
            onClose();
        }).bounds(centerX + 10, centerY + 10, 50, 20).build());
    }

    @Override
    public void onClose() {

        if (closeOnValidate) {
            super.onClose();
            return;
        }

        if (minecraft == null) return;

        minecraft.setScreen(parentScreen);
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        this.renderModBackground(context);

        // Draw centered text
        context.drawCenteredString(
                this.font,
                this.message,
                this.width / 2,
                this.height / 2 - 20,
                0xFFFFFF
        );

        super.render(context, mouseX, mouseY, delta);
    }
}

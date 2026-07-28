package space.ajcool.ardapaths.screens;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * A confirmation dialogue popup screen with yes/no buttons.
 * Displays a centered message and executes callbacks when the user confirms or cancels.
 */
public class ConfirmationPopup extends Screen {

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
    private final Text message;

    /**
     * The screen to return to after the dialogue closes.
     */
    private final Screen parentScreen;

    /**
     * Text for the confirmation button.
     */
    private final Text confirmButtonText = Text.translatable("ardapaths.generic.yes");

    /**
     * Text for the cancel button.
     */
    private final Text cancelButtonText = Text.translatable("ardapaths.generic.no");

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
    public ConfirmationPopup(Text message, Runnable onConfirm, Runnable onCancel, Screen parentScreen) {
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
    public ConfirmationPopup(Text message, Runnable onConfirm, Runnable onCancel, Screen parentScreen, boolean closeOnValidate) {
        super(Text.literal("Confirm"));
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

        this.addDrawableChild(ButtonWidget.builder(confirmButtonText, button -> {
            onConfirm.run();
            close();
        }).dimensions(centerX - 60, centerY + 10, 50, 20).build());

        this.addDrawableChild(ButtonWidget.builder(cancelButtonText, button -> {
            onCancel.run();
            close();
        }).dimensions(centerX + 10, centerY + 10, 50, 20).build());
    }

    @Override
    public void close() {

        if (closeOnValidate) {
            super.close();
            return;
        }

        if (client == null) return;

        client.setScreen(parentScreen);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);

        // Draw centered text
        context.drawCenteredTextWithShadow(
                this.textRenderer,
                this.message,
                this.width / 2,
                this.height / 2 - 20,
                0xFFFFFF
        );

        super.render(context, mouseX, mouseY, delta);
    }
}
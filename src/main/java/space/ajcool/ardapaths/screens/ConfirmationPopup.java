package space.ajcool.ardapaths.screens;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class ConfirmationPopup extends Screen {

    private final Runnable onConfirm;
    private final Runnable onCancel;
    private final Text message;
    private final Screen parentScreen;

    private Text confirmButtonText = Text.translatable("ardapaths.generic.yes");
    private Text cancelButtonText = Text.translatable("ardapaths.generic.no");

    public ConfirmationPopup(Text message, Runnable onConfirm, Runnable onCancel, Screen parentScreen) {
        super(Text.literal("Confirm"));
        this.message = message;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
        this.parentScreen = parentScreen;
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

    @Override
    public void close() {
        client.setScreen(parentScreen);
    }

    public Text getConfirmButtonText() {
        return confirmButtonText;
    }

    public void setConfirmButtonText(Text confirmButtonText) {
        this.confirmButtonText = confirmButtonText;
    }

    public Text getCancelButtonText() {
        return cancelButtonText;
    }

    public void setCancelButtonText(Text cancelButtonText) {
        this.cancelButtonText = cancelButtonText;
    }
}
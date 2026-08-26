package space.ajcool.ardapaths.screens;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Blocking progress popup used while waiting for a server response.
 */
public class BusyPopup extends Screen {
    /**
     * Number of screen ticks to wait before timing out the operation.
     */
    private static final int TIMEOUT_TICKS = 30 * 20;

    /**
     * Message displayed in the middle of the popup.
     */
    private final Text message;

    /**
     * Screen to restore if the request times out.
     */
    private final Screen parentScreen;

    /**
     * Callback invoked when the wait reaches its timeout.
     */
    private final Runnable onTimeout;

    /**
     * Number of ticks spent waiting for the response.
     */
    private int ticksWaiting;

    /**
     * Constructs a blocking popup.
     *
     * @param message      message displayed while waiting
     * @param parentScreen screen to restore when timing out
     * @param onTimeout    callback used to report timeout state to the parent
     */
    public BusyPopup(Text message, Screen parentScreen, Runnable onTimeout) {
        super(Text.literal("Busy"));
        this.message = message;
        this.parentScreen = parentScreen;
        this.onTimeout = onTimeout;
    }

    /**
     * Advances the wait timer and restores the parent screen if no response arrives in time.
     */
    @Override
    public void tick() {
        ticksWaiting++;
        if (ticksWaiting >= TIMEOUT_TICKS && client != null) {
            onTimeout.run();
            client.setScreen(parentScreen);
        }
    }

    /**
     * Keeps the popup blocking so Escape cannot dismiss an in-flight request.
     *
     * @return false because only code may close this popup
     */
    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    /**
     * Ignores ordinary close requests while the server operation is pending.
     */
    @Override
    public void close() {
    }

    /**
     * Renders the blocking overlay and wait message.
     *
     * @param context drawing context
     * @param mouseX  current mouse x coordinate
     * @param mouseY  current mouse y coordinate
     * @param delta   partial tick delta
     */
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        context.drawCenteredTextWithShadow(this.textRenderer, this.message, this.width / 2, this.height / 2 - 5, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }
}

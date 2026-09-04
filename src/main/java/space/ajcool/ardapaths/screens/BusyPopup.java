package space.ajcool.ardapaths.screens;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Blocking progress popup used while waiting for a server response.
 */
public class BusyPopup extends ArdaPathsScreen {

    /**
     * Number of screen ticks to wait before timing out the operation.
     */
    private static final int TIMEOUT_TICKS = 30 * 20;

    /**
     * Message displayed in the middle of the popup.
     */
    private final Component message;

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
    public BusyPopup(Component message, Screen parentScreen, Runnable onTimeout) {
        super(Component.literal("Busy"));
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
        if (ticksWaiting >= TIMEOUT_TICKS) {
            onTimeout.run();
            minecraft.setScreen(parentScreen);
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
    public void onClose() {
    }

    /**
     * Renders the wait message after the modal blur pass has completed.
     *
     * @param context drawing context
     * @param mouseX  current mouse x coordinate
     * @param mouseY  current mouse y coordinate
     * @param partialTick   partial tick delta
     */
    @Override
    protected void extractModContent(GuiGraphicsExtractor context, int mouseX, int mouseY, float partialTick) {
        context.centeredText(this.font, this.message, this.width / 2, this.height / 2 - 5, 0xFFFFFFFF);
    }
}

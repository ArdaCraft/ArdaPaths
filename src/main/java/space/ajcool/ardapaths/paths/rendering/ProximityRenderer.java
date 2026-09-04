package space.ajcool.ardapaths.paths.rendering;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.InteractionHand;
import org.jetbrains.annotations.NotNull;
import space.ajcool.ardapaths.core.Client;
import space.ajcool.ardapaths.core.data.config.shared.Color;
import space.ajcool.ardapaths.mc.items.ModItems;
import space.ajcool.ardapaths.paths.rendering.objects.AnimatedMessage;
import space.ajcool.ardapaths.paths.rendering.objects.AnimatedTitle;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Abstract base class for managing queued rendering of text-based UI elements.
 * <p>
 * Handles sequential display of {@link TextRenderable} items, ensuring only one item
 * is shown at a time. When an item finishes animating, the next item in the queue
 * is automatically displayed. Duplicate items are prevented from being queued.
 *
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ProximityRenderer {

    /** Singleton instance of the proximity renderer */
    private static final ProximityRenderer INSTANCE = new ProximityRenderer();

    /** Queue of renderables waiting to be displayed */
    private final Queue<TextRenderable> renderQueue = new ArrayDeque<>();

    /** The renderable currently being displayed, or null if none */
    private AnimatedMessage currentDisplayedMessage;

    /** The title currently being displayed, or null if none */
    private AnimatedTitle currentDisplayedTitle;

    public static void render(GuiGraphics context, DeltaTracker delta) {
        INSTANCE.renderNextItem(context, delta.getGameTimeDeltaTicks());
        updateVisualMessageStack(context);
    }

    /**
     * Renders the next item in the queue or continues rendering the current item.
     * <p>
     * If the current item has finished or no item is displayed, the next item from
     * the queue is polled and rendered. If the queue is empty, no rendering occurs.
     *
     * @param context      the drawing context for rendering
     * @param ignoredDelta the time delta since last frame
     */
    private void renderNextItem(GuiGraphics context, float ignoredDelta) {

        var nextItem = renderQueue.peek();

        if (nextItem instanceof AnimatedTitle) {

            if (currentDisplayedTitle == null || currentDisplayedTitle.isFinished())
                currentDisplayedTitle = (AnimatedTitle) renderQueue.poll();

        } else if (nextItem instanceof AnimatedMessage) {

            if (currentDisplayedMessage == null || currentDisplayedMessage.isFinished())
                currentDisplayedMessage = (AnimatedMessage) renderQueue.poll();
        }

        // Render current items if available
        if (currentDisplayedMessage != null) currentDisplayedMessage.render(context);
        if (currentDisplayedTitle != null) currentDisplayedTitle.render(context);
    }

    /**
     * Updates the visual representation of the message stack in the player's hand.
     * <p>
     * If the player is holding the Path Revealer item, its stack count is updated
     * to reflect the number of messages currently queued for display, clamped
     * between 1 and 64.
     *
     * @param context the drawing context for rendering
     */
    private static void updateVisualMessageStack(GuiGraphics context) {

        var count = (INSTANCE.currentDisplayedMessage != null && !INSTANCE.currentDisplayedMessage.isFinished()) ? 1 : 0;
        count += (INSTANCE.currentDisplayedTitle != null && !INSTANCE.currentDisplayedTitle.isFinished()) ? 1 : 0;

        count = Math.max(count, INSTANCE.renderQueue.size());

        if (count == 0) return;

        var player = Client.player();
        if (player == null) return;

        var activeHand = player.getUsedItemHand();

        var stackInHand = player.getItemInHand(activeHand);
        if (!stackInHand.is(ModItems.PATH_REVEALER)) return;

        // Get the slot index for the active hand item
        int slotIndex = activeHand == InteractionHand.MAIN_HAND ? player.getInventory().selected : 40;

        // Calculate screen position (assuming hotbar rendering)
        int x = context.guiWidth() / 2 - 90 + slotIndex * 20 + 18;
        int y = context.guiHeight() - 10;

        // Draw the count
        String countText = Integer.toString(count);
        context.drawString(
                Minecraft.getInstance().font,
                countText,
                x - Minecraft.getInstance().font.width(countText),
                y,
                0xFFFFFF,
                true
        );
    }

    /**
     * Queues a new message for display with default animation parameters.
     *
     * @param animatedMessage the message to display
     */
    public static void addMessage(@NotNull AnimatedMessage animatedMessage) {

        if (INSTANCE.currentDisplayedMessage != null
                && !INSTANCE.currentDisplayedMessage.isFinished()
                && INSTANCE.currentDisplayedMessage.equals(animatedMessage)) return;

        INSTANCE.addToQueue(animatedMessage);
    }

    /**
     * Adds a renderable item to the display queue.
     * <p>
     * Duplicate prevention is enforced: items already in the queue or currently
     * displayed are ignored. Before adding, the item is reset to its initial state.
     *
     * @param item the renderable to queue for display
     */
    private void addToQueue(TextRenderable item) {

        if (INSTANCE.renderQueue.contains(item)) return;
        item.reset();
        INSTANCE.renderQueue.add(item);
    }

    /**
     * Queues a new title for display with default animation parameters.
     *
     * @param title the text content to display
     * @param color the color of the title text
     */
    public static void addTitle(String title, Color color) {

        var newTitle = new AnimatedTitle(title, color);

        if (INSTANCE.currentDisplayedTitle != null
                && !INSTANCE.currentDisplayedTitle.isFinished()
                && INSTANCE.currentDisplayedTitle.equals(newTitle)) return;

        INSTANCE.addToQueue(newTitle);
    }

    /**
     * Checks if a title is currently being displayed.
     *
     * @return true if a title is in the process of being displayed, false otherwise
     */
    public static boolean isDisplayingTitle() {

        return INSTANCE.currentDisplayedTitle != null && !INSTANCE.currentDisplayedTitle.isFinished();
    }

    /**
     * Clears all queued and currently displayed items.
     * <p>
     * After calling this method, no renderables will be shown until new items are added.
     */
    public static void clear() {
        INSTANCE.currentDisplayedTitle = null;
        INSTANCE.currentDisplayedMessage = null;
        INSTANCE.renderQueue.clear();
    }
}

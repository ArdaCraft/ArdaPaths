package space.ajcool.ardapaths.paths.rendering;

import lombok.Getter;
import net.minecraft.client.gui.DrawContext;

/**
 * Base class for text-based UI elements that can be rendered, animated, and managed.
 * Provides common lifecycle state management for renderable text components.
 */
public abstract class TextRenderable {

    /**
     * The system time (in milliseconds) when this renderable was started, or -1 if not yet started.
     */
    protected long startTime;

    /**
     * Indicates whether this renderable is currently visible on screen.
     */
    @Getter
    protected boolean showing;

    /**
     * Indicates whether this renderable has completed its lifecycle
     */
    @Getter
    protected boolean done;

    /**
     * Constructs a new TextRenderable in its initial state.
     * The renderable starts showing and not done.
     */
    public TextRenderable() {
        this.showing = true;
        this.done = false;
        this.startTime = -1;
    }

    /**
     * Renders this text element to the screen.
     *
     * @param drawContext the drawing context used for rendering
     */
    public abstract void render(DrawContext drawContext);

    /**
     * Checks if this renderable has finished its animation or display cycle.
     *
     * @return true if the renderable is finished, false otherwise
     */
    public abstract boolean isFinished();

    /**
     * Resets the animation to its initial state for reuse.
     * Clears the start time and marks the message as showing and not done.
     */
    public void reset() {
        this.startTime = -1;
        this.showing = true;
        this.done = false;
    }

    /**
     * Immediately stops the renderable and marks it as done.
     * Sets showing to false and done to true.
     */
    @SuppressWarnings("unused")
    public void stop() {
        showing = false;
        done = true;
    }

}

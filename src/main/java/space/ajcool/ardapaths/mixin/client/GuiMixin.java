package space.ajcool.ardapaths.mixin.client;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import space.ajcool.ardapaths.paths.rendering.InterfaceVisibility;

/**
 * Suppresses selected vanilla HUD elements while the Pathfinder hide-interface mode is active.
 */
@Mixin(Gui.class)
public class GuiMixin {
    /**
     * Cancels hotbar-adjacent HUD rendering when the Pathfinder interface is hidden.
     *
     * @param graphics     the HUD graphics context
     * @param deltaTracker the frame delta tracker
     * @param ci           the mixin callback
     */
    @Inject(method = "renderHotbarAndDecorations", at = @At("HEAD"), cancellable = true)
    private void ardaPaths$renderHotbarAndDecorations(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (InterfaceVisibility.isInterfaceHidden()) ci.cancel();
    }

    /**
     * Cancels crosshair rendering when the Pathfinder interface is hidden.
     *
     * @param graphics     the HUD graphics context
     * @param deltaTracker the frame delta tracker
     * @param ci           the mixin callback
     */
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void ardaPaths$renderCrosshair(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (InterfaceVisibility.isInterfaceHidden()) ci.cancel();
    }

    /**
     * Cancels mob-effect icon rendering when the Pathfinder interface is hidden.
     *
     * @param graphics     the HUD graphics context
     * @param deltaTracker the frame delta tracker
     * @param ci           the mixin callback
     */
    @Inject(method = "renderEffects", at = @At("HEAD"), cancellable = true)
    private void ardaPaths$renderEffects(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (InterfaceVisibility.isInterfaceHidden()) ci.cancel();
    }
}

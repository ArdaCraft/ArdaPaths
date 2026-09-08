package space.ajcool.ardapaths.mixin.client;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.PlayerRideableJumping;
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
     * Cancels hotbar rendering when the Pathfinder interface is hidden.
     *
     * @param tickDelta the frame tick delta
     * @param graphics  the HUD graphics context
     * @param ci        the mixin callback
     */
    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
    private void ardaPaths$renderHotbar(float tickDelta, GuiGraphics graphics, CallbackInfo ci) {
        if (InterfaceVisibility.isInterfaceHidden()) ci.cancel();
    }

    /**
     * Cancels crosshair rendering when the Pathfinder interface is hidden.
     *
     * @param graphics the HUD graphics context
     * @param ci       the mixin callback
     */
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void ardaPaths$renderCrosshair(GuiGraphics graphics, CallbackInfo ci) {
        if (InterfaceVisibility.isInterfaceHidden()) ci.cancel();
    }

    /**
     * Cancels held-item name rendering when the Pathfinder interface is hidden.
     *
     * @param graphics the HUD graphics context
     * @param ci       the mixin callback
     */
    @Inject(method = "renderSelectedItemName", at = @At("HEAD"), cancellable = true)
    private void ardaPaths$renderSelectedItemName(GuiGraphics graphics, CallbackInfo ci) {
        if (InterfaceVisibility.isInterfaceHidden()) ci.cancel();
    }

    /**
     * Cancels player health, armor, hunger, and air rendering when the Pathfinder interface is hidden.
     *
     * @param graphics the HUD graphics context
     * @param ci       the mixin callback
     */
    @Inject(method = "renderPlayerHealth", at = @At("HEAD"), cancellable = true)
    private void ardaPaths$renderPlayerHealth(GuiGraphics graphics, CallbackInfo ci) {
        if (InterfaceVisibility.isInterfaceHidden()) ci.cancel();
    }

    /**
     * Cancels vehicle health rendering when the Pathfinder interface is hidden.
     *
     * @param graphics the HUD graphics context
     * @param ci       the mixin callback
     */
    @Inject(method = "renderVehicleHealth", at = @At("HEAD"), cancellable = true)
    private void ardaPaths$renderVehicleHealth(GuiGraphics graphics, CallbackInfo ci) {
        if (InterfaceVisibility.isInterfaceHidden()) ci.cancel();
    }

    /**
     * Cancels the experience bar when the Pathfinder interface is hidden.
     *
     * @param graphics the HUD graphics context
     * @param x        the left x coordinate used by vanilla
     * @param ci       the mixin callback
     */
    @Inject(method = "renderExperienceBar", at = @At("HEAD"), cancellable = true)
    private void ardaPaths$renderExperienceBar(GuiGraphics graphics, int x, CallbackInfo ci) {
        if (InterfaceVisibility.isInterfaceHidden()) ci.cancel();
    }

    /**
     * Cancels the mount jump meter when the Pathfinder interface is hidden.
     *
     * @param mount    the mounted entity exposing jump charge
     * @param graphics the HUD graphics context
     * @param x        the left x coordinate used by vanilla
     * @param ci       the mixin callback
     */
    @Inject(method = "renderJumpMeter", at = @At("HEAD"), cancellable = true)
    private void ardaPaths$renderJumpMeter(PlayerRideableJumping mount, GuiGraphics graphics, int x, CallbackInfo ci) {
        if (InterfaceVisibility.isInterfaceHidden()) ci.cancel();
    }

    /**
     * Cancels mob-effect icon rendering when the Pathfinder interface is hidden.
     *
     * @param graphics the HUD graphics context
     * @param ci       the mixin callback
     */
    @Inject(method = "renderEffects", at = @At("HEAD"), cancellable = true)
    private void ardaPaths$renderEffects(GuiGraphics graphics, CallbackInfo ci) {
        if (InterfaceVisibility.isInterfaceHidden()) ci.cancel();
    }
}

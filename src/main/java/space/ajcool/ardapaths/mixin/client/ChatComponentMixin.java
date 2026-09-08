package space.ajcool.ardapaths.mixin.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import space.ajcool.ardapaths.paths.rendering.InterfaceVisibility;

/**
 * Suppresses the passive chat overlay while preserving the normal chat screen.
 */
@Mixin(ChatComponent.class)
public class ChatComponentMixin {
    /**
     * Cancels chat overlay rendering when the Pathfinder interface is hidden.
     *
     * @param graphics the HUD graphics context
     * @param tick     the client tick count used by vanilla
     * @param mouseX   the current mouse x coordinate
     * @param mouseY   the current mouse y coordinate
     * @param focused  whether the chat component is focused
     * @param ci       the mixin callback
     */
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void ardaPaths$render(GuiGraphics graphics, int tick, int mouseX, int mouseY, boolean focused, CallbackInfo ci) {
        if (InterfaceVisibility.isInterfaceHidden()) ci.cancel();
    }
}

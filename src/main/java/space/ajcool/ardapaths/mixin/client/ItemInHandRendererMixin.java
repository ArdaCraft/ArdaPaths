package space.ajcool.ardapaths.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import space.ajcool.ardapaths.paths.rendering.InterfaceVisibility;

/**
 * Suppresses first-person hands and held items while the Pathfinder hide-interface mode is active.
 */
@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {
    /**
     * Cancels first-person hand rendering when the Pathfinder interface is hidden.
     *
     * @param partialTick the frame tick delta
     * @param poseStack   the active pose stack
     * @param collector   the render submit node collector
     * @param player      the local player being rendered
     * @param packedLight the packed light value
     * @param ci          the mixin callback
     */
    @Inject(method = "renderHandsWithItems", at = @At("HEAD"), cancellable = true)
    private void ardaPaths$renderHandsWithItems(
            float partialTick,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            LocalPlayer player,
            int packedLight,
            CallbackInfo ci
    ) {
        if (InterfaceVisibility.isInterfaceHidden()) ci.cancel();
    }
}

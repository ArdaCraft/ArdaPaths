package space.ajcool.ardapaths.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
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
     * @param tickDelta the frame tick delta
     * @param matrices  the active pose stack
     * @param buffers   the render buffer source
     * @param player    the local player being rendered
     * @param light     the packed light value
     * @param ci        the mixin callback
     */
    @Inject(method = "renderHandsWithItems", at = @At("HEAD"), cancellable = true)
    private void ardaPaths$renderHandsWithItems(
            float tickDelta,
            PoseStack matrices,
            MultiBufferSource.BufferSource buffers,
            LocalPlayer player,
            int light,
            CallbackInfo ci
    ) {
        if (InterfaceVisibility.isInterfaceHidden()) ci.cancel();
    }
}

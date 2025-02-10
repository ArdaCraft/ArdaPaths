package space.ajcool.ardapaths;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.Item;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.*;
import space.ajcool.ardapaths.config.ClientConfigManager;
import space.ajcool.ardapaths.config.client.ClientConfig;
import space.ajcool.ardapaths.mc.blocks.PathMarkerBlock;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;
import space.ajcool.ardapaths.mc.items.ModItems;
import space.ajcool.ardapaths.mc.particles.ModParticles;
import space.ajcool.ardapaths.mc.networking.PacketRegistry;
import space.ajcool.ardapaths.paths.ProximityMessageRenderer;
import space.ajcool.ardapaths.paths.Paths;
import space.ajcool.ardapaths.paths.TrailRenderer;
import space.ajcool.ardapaths.screens.PathSelectionScreen;

import java.util.*;

public class ArdaPathsClient implements ClientModInitializer {
    public static ClientConfigManager CONFIG_MANAGER;
    public static ClientConfig CONFIG;
    public static boolean callingForTeleport = false;

    @Override
    public void onInitializeClient() {
        CONFIG_MANAGER = new ClientConfigManager("./config/arda-paths/config.json");
        CONFIG = CONFIG_MANAGER.getConfig();

        ModParticles.initClient();

        HudRenderCallback.EVENT.register(ProximityMessageRenderer::render);
        ClientTickEvents.END_WORLD_TICK.register(TrailRenderer::render);
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            CONFIG_MANAGER.updatePathData();
        });

        List<Item> markerSet = new ArrayList<>(ClientWorld.BLOCK_MARKER_ITEMS);
        markerSet.add(ModItems.PATH_MARKER);
        ClientWorld.BLOCK_MARKER_ITEMS = Set.copyOf(markerSet);
        ClientTickEvents.START_WORLD_TICK.register(level -> {
            if (PathMarkerBlock.selectedBlockPosition != null && MinecraftClient.getInstance().player != null && !MinecraftClient.getInstance().player.getMainHandStack().isOf(ModItems.PATH_MARKER)) {
                PathMarkerBlock.selectedBlockPosition = null;

                var message = Text.empty()
                        .append(Text.literal("ArdaPaths: ").formatted(Formatting.DARK_AQUA))
                        .append(Text.literal("Deselected origin block.").formatted(Formatting.RED));

                MinecraftClient.getInstance().player.sendMessage(message);

            }
            else if (PathMarkerBlock.selectedBlockPosition != null) {
                var random = level.random;
                level.addParticle(ParticleTypes.COMPOSTER, PathMarkerBlock.selectedBlockPosition.getX() + random.nextDouble(), PathMarkerBlock.selectedBlockPosition.getY()+ random.nextDouble(), PathMarkerBlock.selectedBlockPosition.getZ() + random.nextDouble(), 0.0, 0.0, 0.0);
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (callingForTeleport && MinecraftClient.getInstance().player != null) {
                var playerPosition = MinecraftClient.getInstance().player.getPos();

                Vec3d closestPosition = null;
                var closestDistance = Double.MAX_VALUE;
                String selectedPathId = CONFIG.getSelectedPathId();

                for (PathMarkerBlockEntity tickingPathMarker : Paths.getTickingMarkers()) {
                    PathMarkerBlockEntity.NbtData data = tickingPathMarker.getNbt(selectedPathId);
                    if (data.getTarget() == null) continue;

                    var dist = tickingPathMarker.getCenterPos().distanceTo(playerPosition);

                    if (dist < closestDistance)
                    {
                        closestPosition = tickingPathMarker.getCenterPos();
                        closestDistance = dist;
                    }
                }

                if (closestPosition != null)
                {
                    PacketRegistry.PATH_PLAYER_TELEPORT.sendToServer(closestPosition.x, closestPosition.y, closestPosition.z);
                }

                callingForTeleport = false;
            }

            Paths.clearTickingMarkers();
        });
    }
}

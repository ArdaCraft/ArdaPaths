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
import net.minecraft.util.math.Vec3d;
import space.ajcool.ardapaths.core.data.config.ClientConfigManager;
import space.ajcool.ardapaths.core.data.config.client.ClientConfig;
import space.ajcool.ardapaths.core.networking.PacketRegistry;
import space.ajcool.ardapaths.core.networking.packets.server.PlayerTeleportPacket;
import space.ajcool.ardapaths.mc.blocks.PathMarkerBlock;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;
import space.ajcool.ardapaths.mc.items.ModItems;
import space.ajcool.ardapaths.mc.particles.ModParticles;
import space.ajcool.ardapaths.paths.Paths;
import space.ajcool.ardapaths.paths.rendering.ProximityMessageRenderer;
import space.ajcool.ardapaths.paths.rendering.TrailRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ArdaPathsClient implements ClientModInitializer {
    public static ClientConfigManager CONFIG_MANAGER;
    public static ClientConfig CONFIG;
    public static boolean callingForTeleport = false;

    @Override
    public void onInitializeClient() {
        CONFIG_MANAGER = new ClientConfigManager("./config/arda-paths/config.json");
        CONFIG = CONFIG_MANAGER.getConfig();

        List<Item> markerSet = new ArrayList<>(ClientWorld.BLOCK_MARKER_ITEMS);
        markerSet.add(ModItems.PATH_MARKER);
        ClientWorld.BLOCK_MARKER_ITEMS = Set.copyOf(markerSet);

        ModParticles.initClient();

        HudRenderCallback.EVENT.register(ProximityMessageRenderer::render);
        ClientTickEvents.END_WORLD_TICK.register(TrailRenderer::render);

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            CONFIG_MANAGER.updatePathData();
        });

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
                String selectedChapterId = CONFIG.getCurrentChapterId();

                for (PathMarkerBlockEntity tickingPathMarker : Paths.getTickingMarkers()) {
                    PathMarkerBlockEntity.ChapterNbtData data = tickingPathMarker.getChapterData(selectedPathId, selectedChapterId);
                    if (data.getTarget() == null) continue;

                    var dist = tickingPathMarker.getCenterPos().distanceTo(playerPosition);

                    if (dist < closestDistance)
                    {
                        closestPosition = tickingPathMarker.getCenterPos();
                        closestDistance = dist;
                    }
                }

                if (closestPosition != null) {
                    PlayerTeleportPacket packet = new PlayerTeleportPacket(closestPosition.x + 0.5, closestPosition.y, closestPosition.z + 0.5);
                    PacketRegistry.PLAYER_TELEPORT.send(packet);
                }

                callingForTeleport = false;
            }

            Paths.clearTickingMarkers();
        });
    }
}

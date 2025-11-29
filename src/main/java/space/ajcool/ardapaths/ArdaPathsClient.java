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
import space.ajcool.ardapaths.core.data.LastVisitedTrailNodeData;
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

public class ArdaPathsClient implements ClientModInitializer
{
    public static ClientConfigManager CONFIG_MANAGER;
    public static ClientConfig CONFIG;
    public static boolean callingForTeleport = false;
    public static LastVisitedTrailNodeData lastVisitedTrailNodeData;

    @Override
    public void onInitializeClient()
    {
        CONFIG_MANAGER = new ClientConfigManager("./config/arda-paths/config.json");
        CONFIG = CONFIG_MANAGER.getConfig();

        List<Item> markerSet = new ArrayList<>(ClientWorld.BLOCK_MARKER_ITEMS);
        markerSet.add(ModItems.PATH_MARKER);
        ClientWorld.BLOCK_MARKER_ITEMS = Set.copyOf(markerSet);

        ModParticles.initClient();

        HudRenderCallback.EVENT.register(ProximityMessageRenderer::render);
        ClientTickEvents.END_WORLD_TICK.register(TrailRenderer::render);

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
        {
            CONFIG_MANAGER.updatePathData();
        });

        ClientTickEvents.START_WORLD_TICK.register(level ->
        {
            if (PathMarkerBlock.selectedBlockPosition != null && MinecraftClient.getInstance().player != null && !MinecraftClient.getInstance().player.getMainHandStack().isOf(ModItems.PATH_MARKER))
            {
                PathMarkerBlock.selectedBlockPosition = null;

                var message = Text.empty()
                        .append(Text.literal("ArdaPaths: ").formatted(Formatting.DARK_AQUA))
                        .append(Text.literal("Deselected origin block.").formatted(Formatting.RED));

                MinecraftClient.getInstance().player.sendMessage(message);

            }
            else if (PathMarkerBlock.selectedBlockPosition != null)
            {
                var random = level.random;
                level.addParticle(ParticleTypes.COMPOSTER, PathMarkerBlock.selectedBlockPosition.getX() + random.nextDouble(), PathMarkerBlock.selectedBlockPosition.getY() + random.nextDouble(), PathMarkerBlock.selectedBlockPosition.getZ() + random.nextDouble(), 0.0, 0.0, 0.0);
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client ->
        {
            if (callingForTeleport && MinecraftClient.getInstance().player != null)
            {
                String currentSelectedChapterId = ArdaPathsClient.CONFIG.getCurrentChapterId() != null ? ArdaPathsClient.CONFIG.getCurrentChapterId() : "";

                if(lastVisitedTrailNodeData != null) {

                    String lastVisitedNodeChapterId = lastVisitedTrailNodeData.getSelectedChapterId() != null ? lastVisitedTrailNodeData.getSelectedChapterId() : "";

                    if (!currentSelectedChapterId.isBlank() && currentSelectedChapterId.equals(lastVisitedNodeChapterId)) {

                        PlayerTeleportPacket packet = new PlayerTeleportPacket(lastVisitedTrailNodeData.getPosX() + 0.5, lastVisitedTrailNodeData.getPosY(), lastVisitedTrailNodeData.getPosZ() + 0.5, lastVisitedTrailNodeData.getWorldId());
                        PacketRegistry.PLAYER_TELEPORT.send(packet);
                        callingForTeleport = false;
                        return;
                    } else {
                        var message = Text.empty()
                                .append(Text.literal("Last visited trail does not belong to the current chapter. Teleporting to chapter start instead...").formatted(Formatting.DARK_AQUA));
                        MinecraftClient.getInstance().player.sendMessage(message);
                    }
                } else {

                    var message = Text.empty()
                            .append(Text.literal("No last visited trail node data found, teleporting to chapter start instead...").formatted(Formatting.DARK_AQUA));
                    MinecraftClient.getInstance().player.sendMessage(message);
                    ArdaPaths.LOGGER.info("");
                }

                if (!currentSelectedChapterId.isBlank())
                    Paths.gotoChapter(currentSelectedChapterId, true);
                else {

                    var message = Text.empty()
                            .append(Text.literal("No chapter selected, cannot teleport.").formatted(Formatting.DARK_AQUA));
                    MinecraftClient.getInstance().player.sendMessage(message);
                }

                callingForTeleport = false;
            }

            Paths.clearTickingMarkers();
        });
    }
}

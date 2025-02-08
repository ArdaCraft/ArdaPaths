package space.ajcool.ardapaths;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.*;
import space.ajcool.ardapaths.config.ClientConfigManager;
import space.ajcool.ardapaths.config.ServerConfigManager;
import space.ajcool.ardapaths.config.client.ClientConfig;
import space.ajcool.ardapaths.config.server.ServerConfig;
import space.ajcool.ardapaths.config.shared.Color;
import space.ajcool.ardapaths.config.shared.PathSettings;
import space.ajcool.ardapaths.mc.blocks.PathMarkerBlock;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;
import space.ajcool.ardapaths.mc.items.ModItems;
import space.ajcool.ardapaths.mc.particles.ModParticles;
import space.ajcool.ardapaths.mc.networking.PacketRegistry;
import space.ajcool.ardapaths.paths.ProximityMessageRenderer;
import space.ajcool.ardapaths.paths.Paths;
import space.ajcool.ardapaths.paths.TrailRenderer;
import space.ajcool.ardapaths.screen.PathMarkerEditScreen;
import space.ajcool.ardapaths.screen.PathSelectionScreen;

import java.util.*;

public class ArdaPathsClient implements ClientModInitializer {
    public static ClientConfigManager CONFIG_MANAGER;
    public static ClientConfig CONFIG;

    @Override
    public void onInitializeClient() {
        CONFIG_MANAGER = ClientConfigManager.getInstance();
        CONFIG = CONFIG_MANAGER.getConfig();

        ModParticles.initClient();

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (client.isInSingleplayer()) {
                Paths.clearPaths();
                ServerConfig serverConfig = ServerConfigManager.getInstance().getConfig();
                for (PathSettings path : serverConfig.paths) {
                    Paths.addPath(path);
                }
                onPathDataInitialized();
            } else {
                PacketRegistry.PATH_DATA_REQUEST.sendToServer();
            }
        });

        var markerSet = new ArrayList<>(ClientWorld.BLOCK_MARKER_ITEMS);
        markerSet.add(ModItems.PATH_MARKER);
        ClientWorld.BLOCK_MARKER_ITEMS = Set.copyOf(markerSet);

        // TICKS

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

        ClientTickEvents.END_CLIENT_TICK.register(client ->
        {
            if (PathSelectionScreen.callingForTeleport && MinecraftClient.getInstance().player != null)
            {
                var playerPosition = MinecraftClient.getInstance().player.getPos();

                Vec3d closestPosition = null;
                var closestDistance = Double.MAX_VALUE;

                for (PathMarkerBlockEntity tickingPathMarker : Paths.getTickingMarkers())
                {
                    if (!tickingPathMarker.data().hasTargetOffset(PathSelectionScreen.selectedPathId)) continue;

                    var dist = tickingPathMarker.position().distanceTo(playerPosition);

                    if (dist < closestDistance)
                    {
                        closestPosition = tickingPathMarker.position();
                        closestDistance = dist;
                    }
                }

                if (closestPosition != null)
                {
                    PacketRegistry.PATH_PLAYER_TELEPORT.sendToServer(closestPosition.x, closestPosition.y, closestPosition.z);
                }

                PathSelectionScreen.callingForTeleport = false;
            }

            Paths.clearTickingMarkers();
        });


    }

    public static void onPathDataInitialized() {
        HudRenderCallback.EVENT.register(ProximityMessageRenderer::render);
        ClientTickEvents.END_WORLD_TICK.register(TrailRenderer::render);

        ColorProviderRegistry.ITEM.register((itemStack, i) ->
        {
            for (PathSettings path : Paths.getPaths())
            {
                if (path.id != PathSelectionScreen.selectedPathId) continue;

                return path.primaryColor.asHex();
            }

            return Color.fromRgb(100, 100, 100).asHex();
        }, ModItems.PATH_REVEALER);
    }

    public static boolean checkCtrlHeld()
    {
        var level = MinecraftClient.getInstance().world;

        return level != null && level.isClient() && Screen.hasControlDown();
    }

    public static void openEditorScreen(PathMarkerBlockEntity pathMarkerBlockEntity)
    {
        var level = MinecraftClient.getInstance().world;

        if (level != null && level.isClient()) MinecraftClient.getInstance().setScreen(new PathMarkerEditScreen(pathMarkerBlockEntity));
    }

    public static void openSelectionScreen()
    {
        var level = MinecraftClient.getInstance().world;

        if (level != null && level.isClient()) MinecraftClient.getInstance().setScreen(new PathSelectionScreen());
    }

    public static int selectedTrailId()
    {
        var level = MinecraftClient.getInstance().world;

        if (level != null && level.isClient()) return PathSelectionScreen.selectedPathId;

        return 0;
    }
}

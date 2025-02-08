package space.ajcool.ardapaths;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.*;
import space.ajcool.ardapaths.mc.blocks.PathMarkerBlock;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;
import space.ajcool.ardapaths.mc.items.ModItems;
import space.ajcool.ardapaths.mc.particles.ModParticles;
import space.ajcool.ardapaths.networking.PacketRegistry;
import space.ajcool.ardapaths.trails.ProximityMessageRenderer;
import space.ajcool.ardapaths.trails.Paths;
import space.ajcool.ardapaths.trails.TrailRenderer;
import space.ajcool.ardapaths.screen.PathMarkerEditScreen;
import space.ajcool.ardapaths.screen.PathSelectionScreen;
import space.ajcool.ardapaths.mc.sounds.TrailSoundInstance;

import java.util.*;

public class ArdaPathsClient implements ClientModInitializer {
    public static TrailSoundInstance trailSoundInstance = null;

    @Override
    public void onInitializeClient()
    {
        ModParticles.initClient();

        var markerSet = new ArrayList<>(ClientWorld.BLOCK_MARKER_ITEMS);
        markerSet.add(ModItems.PATH_MARKER);
        ClientWorld.BLOCK_MARKER_ITEMS = Set.copyOf(markerSet);

        ColorProviderRegistry.ITEM.register((itemStack, i) ->
        {
            for (ArdaPathsConfig.PathSettings path : ArdaPaths.CONFIG.paths)
            {
                if (path.Id != PathSelectionScreen.selectedPathId) continue;

                return path.PrimaryColor.encodedColor();
            }

            return new ArdaPathsConfig.ColorRGB(100, 100, 100).encodedColor();
        }, ModItems.PATH_REVEALER);

        // TICKS

        HudRenderCallback.EVENT.register(ProximityMessageRenderer::render);

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

        ClientTickEvents.END_WORLD_TICK.register(TrailRenderer::render);

        ClientTickEvents.END_CLIENT_TICK.register(client ->
        {
            if (PathSelectionScreen.callingForTeleport && MinecraftClient.getInstance().player != null)
            {
                var playerPosition = MinecraftClient.getInstance().player.getPos();

                Vec3d closestPosition = null;
                var closestDistance = Double.MAX_VALUE;

                for (PathMarkerBlockEntity tickingPathMarker : Paths.getTickingMarkers())
                {
                    if (!tickingPathMarker.targetOffsets.containsKey(PathSelectionScreen.selectedPathId)) continue;

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

package space.ajcool.ardapaths;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import org.lwjgl.glfw.GLFW;
import space.ajcool.ardapaths.core.PermissionHelper;
import space.ajcool.ardapaths.core.consumers.networking.RespondablePacketHandler;
import space.ajcool.ardapaths.core.data.LastVisitedTrailNodeData;
import space.ajcool.ardapaths.core.data.config.ClientConfigManager;
import space.ajcool.ardapaths.core.data.config.client.ClientConfig;
import space.ajcool.ardapaths.core.data.config.shared.Color;
import space.ajcool.ardapaths.core.data.config.shared.PathData;
import space.ajcool.ardapaths.core.networking.PacketRegistry;
import space.ajcool.ardapaths.core.networking.packets.server.PlayerTeleportPacket;
import space.ajcool.ardapaths.mc.blocks.PathMarkerBlock;
import space.ajcool.ardapaths.mc.items.ModItems;
import space.ajcool.ardapaths.mc.particles.ModParticles;
import space.ajcool.ardapaths.paths.Paths;
import space.ajcool.ardapaths.paths.movement.AutoWalker;
import space.ajcool.ardapaths.paths.movement.FocusController;
import space.ajcool.ardapaths.paths.rendering.EnvironmentController;
import space.ajcool.ardapaths.paths.rendering.FocusPromptRenderer;
import space.ajcool.ardapaths.paths.rendering.ProximityRenderer;
import space.ajcool.ardapaths.paths.rendering.TrailRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Client-side initialization for ArdaPaths.
 * Handles UI rendering, client-side state management, and input handling.
 */
public class ArdaPathsClient implements ClientModInitializer {

    /**
     * Manager for client-side configuration, handles loading and saving config.json.
     */
    public static ClientConfigManager CONFIG_MANAGER;

    /**
     * The current client-side path configuration, loaded from config.json.
     */
    public static ClientConfig CONFIG;

    /**
     * Flag indicating whether a teleport to the last visited trail node has been requested.
     */
    public static boolean callingForTeleport = false;

    /**
     * Data about the last trail node visited by the player, or null if not yet visited.
     */
    public static LastVisitedTrailNodeData lastVisitedTrailNodeData;

    /**
     * Keybinding used to toggle automatic trail walking.
     */
    public static KeyMapping AUTO_WALK_KEY;

    /**
     * Keybinding used to focus the view on an authored look-at target, or recentre auto-walk.
     */
    public static KeyMapping FOCUS_KEY;

    /**
     * Fabric client mod initialization entry point.
     * Initializes client-side systems including UI rendering, event listeners, and particle effects.
     */
    @Override
    public void onInitializeClient() {
        CONFIG_MANAGER = new ClientConfigManager("./config/arda-paths/config.json");
        CONFIG = CONFIG_MANAGER.getConfig();

        List<Item> markerSet = new ArrayList<>(ClientLevel.MARKER_PARTICLE_ITEMS);
        markerSet.add(ModItems.PATH_MARKER);
        ClientLevel.MARKER_PARTICLE_ITEMS = Set.copyOf(markerSet);

        ModParticles.initClient();
        registerPathfinderColorProvider();
        AUTO_WALK_KEY = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.ardapaths.auto_walk",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_NUM_LOCK,
                "key.category.ardapaths"
        ));
        FOCUS_KEY = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.ardapaths.focus",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_ALT,
                "key.category.ardapaths"
        ));

        HudRenderCallback.EVENT.register(ProximityRenderer::render);
        HudRenderCallback.EVENT.register(FocusPromptRenderer::render);
        WorldRenderEvents.START.register(context ->
        {
            FocusController.renderCameraFrame();
            AutoWalker.renderCameraFrame();
            EnvironmentController.renderFrame(context.tickCounter().getGameTimeDeltaTicks());
        });

        ClientTickEvents.END_WORLD_TICK.register(TrailRenderer::render);

        ClientTickEvents.START_CLIENT_TICK.register(AutoWalker::tick);

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
        {
            CONFIG_MANAGER.updatePathData();
            PermissionHelper.hasEditPermission(client.player);
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
        {
            RespondablePacketHandler.clearAllResponseConsumers();
            PermissionHelper.resetClientCache();
            Paths.clearTickingMarkers();
            EnvironmentController.reset();
            TrailRenderer.clearTrails();
            FocusController.reset();
        });

        ClientTickEvents.START_WORLD_TICK.register(level ->
        {
            if (PathMarkerBlock.selectedBlockPosition != null && Minecraft.getInstance().player != null && !Minecraft.getInstance().player.getMainHandItem().is(ModItems.PATH_MARKER)) {
                PathMarkerBlock.selectedBlockPosition = null;

                var message = Component.empty()
                        .append(Component.translatable("ardapaths.client.message.ardapaths").withStyle(ChatFormatting.DARK_AQUA))
                        .append(Component.translatable("ardapaths.client.message.deselected_origin_block").withStyle(ChatFormatting.RED));

                Minecraft.getInstance().player.sendSystemMessage(message);

            } else if (PathMarkerBlock.selectedBlockPosition != null) {
                var random = level.random;
                level.addParticle(ParticleTypes.COMPOSTER, PathMarkerBlock.selectedBlockPosition.getX() + random.nextDouble(), PathMarkerBlock.selectedBlockPosition.getY() + random.nextDouble(), PathMarkerBlock.selectedBlockPosition.getZ() + random.nextDouble(), 0.0, 0.0, 0.0);
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client ->
        {
            while (AUTO_WALK_KEY.consumeClick()) {
                AutoWalker.toggle();
            }

            FocusController.setHeld(FOCUS_KEY.isDown());

            if (callingForTeleport && Minecraft.getInstance().player != null) {
                String currentSelectedChapterId = ArdaPathsClient.CONFIG.getCurrentChapterId() != null ? ArdaPathsClient.CONFIG.getCurrentChapterId() : "";

                if (lastVisitedTrailNodeData != null) {

                    String lastVisitedNodeChapterId = lastVisitedTrailNodeData.selectedChapterId() != null ? lastVisitedTrailNodeData.selectedChapterId() : "";

                    if (!currentSelectedChapterId.isBlank() && currentSelectedChapterId.equals(lastVisitedNodeChapterId)) {
                        ProximityRenderer.clear();
                        PlayerTeleportPacket packet = new PlayerTeleportPacket(lastVisitedTrailNodeData.posX() + 0.5, lastVisitedTrailNodeData.posY(), lastVisitedTrailNodeData.posZ() + 0.5, lastVisitedTrailNodeData.worldId());
                        PacketRegistry.PLAYER_TELEPORT.send(packet);
                        callingForTeleport = false;
                        return;
                    } else {
                        var message = Component.empty()
                                .append(Component.translatable("ardapaths.client.message.trail_does_not_belong_to_chapter").withStyle(ChatFormatting.DARK_AQUA));
                        Minecraft.getInstance().player.sendSystemMessage(message);
                    }
                } else {

                    var message = Component.empty()
                            .append(Component.translatable("ardapaths.client.message.no_trail_data").withStyle(ChatFormatting.DARK_AQUA));
                    Minecraft.getInstance().player.sendSystemMessage(message);
                }

                if (!currentSelectedChapterId.isBlank()) {
                    ProximityRenderer.clear();
                    Paths.gotoChapter(currentSelectedChapterId, true);
                } else {

                    var message = Component.empty()
                            .append(Component.translatable("ardapaths.client.message.no_chapter_selected").withStyle(ChatFormatting.DARK_AQUA));
                    Minecraft.getInstance().player.sendSystemMessage(message);
                }

                callingForTeleport = false;
            }
        });
    }

    /**
     * Registers the Pathfinder item colour provider once for the client process.
     */
    private void registerPathfinderColorProvider() {
        ColorProviderRegistry.ITEM.register((itemStack, tintIndex) ->
        {
            PathData selectedPath = CONFIG.getSelectedPath();
            if (selectedPath != null) {
                return selectedPath.getPrimaryColor().asHex();
            }
            return Color.fromRgb(100, 100, 100).asHex();
        }, ModItems.PATH_REVEALER);
    }
}

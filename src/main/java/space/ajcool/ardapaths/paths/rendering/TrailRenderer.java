package space.ajcool.ardapaths.paths.rendering;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;
import space.ajcool.ardapaths.ArdaPathsClient;
import space.ajcool.ardapaths.core.Client;
import space.ajcool.ardapaths.core.data.Journal;
import space.ajcool.ardapaths.core.data.LastVisitedTrailNodeData;
import space.ajcool.ardapaths.core.data.config.shared.ChapterData;
import space.ajcool.ardapaths.core.data.config.shared.Color;
import space.ajcool.ardapaths.core.data.config.shared.PathData;
import space.ajcool.ardapaths.core.networking.packets.server.PlayerTeleportPacket;
import space.ajcool.ardapaths.mc.blocks.entities.ModBlockEntities;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;
import space.ajcool.ardapaths.mc.items.ModItems;
import space.ajcool.ardapaths.mc.sounds.TrailSoundInstance;
import space.ajcool.ardapaths.paths.Paths;
import space.ajcool.ardapaths.paths.rendering.objects.AnimatedMessage;
import space.ajcool.ardapaths.paths.rendering.objects.AnimatedTrail;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Responsible for rendering trails in the client world.
 */
public class TrailRenderer {

    private static final List<AnimatedTrail> trails = new ArrayList<>();
    public static TrailSoundInstance trailSoundInstance = null;

    /**
     * Render all registered trails.
     *
     * @param level The client world
     */
    public static void render(ClientWorld level)
    {

        ClientPlayerEntity player = Client.player();
        if (player == null) return;

        PathData selectedPath = ArdaPathsClient.CONFIG.getSelectedPath();
        if (selectedPath == null) return;

        boolean isHoldingRevealer = player.isHolding(ModItems.PATH_REVEALER);
        boolean isHoldingMarker = player.isHolding(ModItems.PATH_MARKER);

        // If the player is not holding either item, clear trails and messages
        if (!isHoldingRevealer && !isHoldingMarker) {

            clearTrails();
            ProximityMessageRenderer.clearMessage();
            ProximityTitleRenderer.clearMessage();

        // Else, render trails based on the held item
        } else {

            String currentPathId = selectedPath.getId();
            Color[] currentPathColors = selectedPath.getColors();
            String currentChapterId = ArdaPathsClient.CONFIG.getCurrentChapterId();

            if (isHoldingMarker)
                renderPathMarkerMode(currentPathId, currentChapterId, currentPathColors);
            else
                renderPathRevealerMode(player, currentPathId, currentChapterId, currentPathColors);

            renderTrails(level, player, selectedPath, currentChapterId);
        }
    }

    /**
     * Render the trails and remove those that are out of range or at the end.
     *
     * @param level           The client world
     * @param player          The client player entity
     * @param selectedPath    The currently selected path data
     * @param currentChapterId The current chapter ID
     */
    private static void renderTrails(ClientWorld level, ClientPlayerEntity player, PathData selectedPath, String currentChapterId) {

        List<AnimatedTrail> trailsSnapshot = new ArrayList<>(trails);
        List<AnimatedTrail> trailsToRemove = new ArrayList<>();

        for (AnimatedTrail trail : trailsSnapshot) {

            var playerPosition = player.getPos();
            var distanceToTrail = playerPosition.squaredDistanceTo(trail.getCurrentPos());

            if (distanceToTrail > (player.isHolding(ModItems.PATH_REVEALER) ? 225 : 10000) || trail.isAtEnd()) {

                trailsToRemove.add(trail);

                if (trail.isAtEnd() && player.isHolding(ModItems.PATH_REVEALER)) {

                    var stopPos = BlockPos.ofFloored(trail.getCurrentPos());
                    var optionalMarkerAtPos = level.getBlockEntity(stopPos, ModBlockEntities.PATH_MARKER);

                    optionalMarkerAtPos.ifPresent(marker -> {
                        marker.createTrail(selectedPath.getId(), currentChapterId, selectedPath.getColors());
                        trail.render(level);
                    });
                }
                continue;
            }

            trail.render(level);
        }

        trails.removeAll(trailsToRemove);
    }

    /**
     * Render trails in Path Marker mode. IE, when the player is holding a Path Marker, displaying trails from all
     * surrounding markers given the current selected Path and Chapter ID.
     *
     * @param currentPathId     The current path ID
     * @param currentChapterId  The current chapter ID
     * @param currentPathColors The colors of the current path
     */
    private static void renderPathMarkerMode(String currentPathId, String currentChapterId, Color[] currentPathColors)
    {
        Paths.getTickingMarkers().forEach(marker ->
        {
            PathMarkerBlockEntity.ChapterNbtData data = marker.getChapterData(currentPathId, currentChapterId, false);
            if (data == null) return;

            boolean trailExists = trails.stream().anyMatch(trail -> trail.getStart().equals(marker.getPos()));
            if (!trailExists)
            {
                marker.createTrail(currentPathId, currentChapterId, currentPathColors);
            }
        });
    }

    /**
     * Render trails in Path Revealer mode. IE, when the player is holding a Path Revealer, displaying trails from the current Path and Chapter.
     * Determines the closest valid path marker and creates a trail from it if within range.
     * Switches chapters if the player is within range of a chapter start marker.
     *
     * @param player            The client player entity
     * @param currentPathId     The current path ID
     * @param currentChapterId  The current chapter ID
     * @param currentPathColors The colors of the current path
     */
    private static void renderPathRevealerMode(ClientPlayerEntity player, String currentPathId,
                                               String currentChapterId, Color[] currentPathColors)
    {
        BlockPos playerPos = player.getBlockPos();
        PathMarkerBlockEntity closestValidMarker = null;
        double closestSquaredDistance = Double.MAX_VALUE;

        for (PathMarkerBlockEntity marker : Paths.getTickingMarkers()) {

            double squaredDistance = playerPos.getSquaredDistance(marker.getPos());
            PathMarkerBlockEntity.ChapterNbtData currentChapterData = marker.getChapterData(currentPathId, currentChapterId, false);

            if (currentChapterData != null) {

                displayAnimatedText(squaredDistance, currentChapterData, player, playerPos, currentPathColors);

                if (currentChapterData.getTarget() != null && squaredDistance < closestSquaredDistance) {
                    closestValidMarker = marker;
                    closestSquaredDistance = squaredDistance;
                }
            }

            processChapterSwitching(marker, currentPathId, squaredDistance);
        }

        if (trails.isEmpty() && closestValidMarker != null && closestSquaredDistance <= 100) {

            updateLastVisitedTrailNode(currentChapterId, closestValidMarker);
            closestValidMarker.createTrail(currentPathId, currentChapterId, currentPathColors);
        }
    }

    /**
     * Process chapter switching based on the player's proximity to chapter start markers.
     * When the player is within the activation range of a chapter start marker, switch to that chapter
     * if it is the next chapter in sequence. If the current chapter is "default", switch to the first available chapter.
     * @param marker          The path marker block entity
     * @param currentPathId   The current path ID
     * @param squaredDistance The squared distance between the player and the path marker
     */
    private static void processChapterSwitching(PathMarkerBlockEntity marker, String currentPathId, double squaredDistance) {

        // Here selected path is guaranteed to be non-null
        var selectedPath = ArdaPathsClient.CONFIG.getSelectedPath();
        assert selectedPath != null;

        ChapterData currentChapter = ArdaPathsClient.CONFIG.getCurrentChapter();

        // Filter chapters that are valid for switching - ie chapter start, within range, and not empty ID
        List<PathMarkerBlockEntity.ChapterNbtData> filteredChapters = marker.getChapters(currentPathId).stream()
                .filter(data -> !data.getChapterId().isEmpty())
                .filter(PathMarkerBlockEntity.ChapterNbtData::isChapterStart)
                .filter(data -> squaredDistance <= MathHelper.square(data.getActivationRange()))
                .toList();

        if (!filteredChapters.isEmpty()) {

            for (var otherChapterData : filteredChapters) {

                String otherChapterId = otherChapterData.getChapterId();
                ChapterData chapter = selectedPath.getChapter(otherChapterId);

                if (currentChapter == null || chapter == null) continue;

                if (!"default".equalsIgnoreCase(currentChapter.getName())) {
                    if (chapter.getIndex() <= currentChapter.getIndex()) continue;
                    if ((chapter.getIndex() - currentChapter.getIndex()) > 1) continue;
                } else {

                    otherChapterId = filteredChapters.stream()
                            .map(otherChapterIdentifier -> selectedPath.getChapter(otherChapterIdentifier.getChapterId()))
                            .min(Comparator.comparingInt(chapterData -> chapterData != null ? chapterData.getIndex() : 0))
                            .map(ChapterData::getId)
                            .orElse(otherChapterId);
                }

                ArdaPathsClient.CONFIG.setCurrentChapter(otherChapterId);
                ArdaPathsClient.CONFIG_MANAGER.save();
            }
        }
    }

    /**
     * Display animated text (Chapter Title or Proximity message) based on the player's proximity to a path marker.
     *
     * @param squaredDistance     The squared distance between the player and the path marker
     * @param currentChapterData  The chapter data of the path marker
     * @param player              The player entity
     * @param playerPos           The position of the player
     * @param currentPathColors   The colors of the current path
     */
    private static void displayAnimatedText(double squaredDistance,
                                            PathMarkerBlockEntity.ChapterNbtData currentChapterData,
                                            ClientPlayerEntity player,
                                            BlockPos playerPos,
                                            Color[] currentPathColors) {

        var renderMessages      = ArdaPathsClient.CONFIG.showProximityMessages();
        var renderChapterTitles = ArdaPathsClient.CONFIG.showChapterTitles();
        var selectedPath        = ArdaPathsClient.CONFIG.getSelectedPath();
        assert selectedPath != null;

        // If we are within activation range
        if (squaredDistance <= MathHelper.square(currentChapterData.getActivationRange())) {

            // Render proximity message
            if (!currentChapterData.getProximityMessage().isEmpty() && renderMessages) {

                Journal.addProximityMessage(selectedPath.getId(),
                        currentChapterData.getChapterId(),
                        currentChapterData.getProximityMessage(),
                        getPlayerTeleportPacket(player, playerPos));

                ProximityMessageRenderer.setMessage(AnimatedMessage.getAnimatedMessage(currentChapterData));
            }

            // Render chapter title
            ChapterData currentChapterInfo = selectedPath.getChapter(currentChapterData.getChapterId());
            if (currentChapterInfo != null && currentChapterData.isChapterStart() && currentChapterData.isDisplayChapterTitleOnTrail()) {

                // Add chapter start to journal either way to ensure it's recorded
                Journal.addChapterStart(selectedPath.getId(),
                        currentChapterData.getChapterId(),
                        currentChapterInfo.getName(),
                        getPlayerTeleportPacket(player, playerPos),
                        currentPathColors[0].asHex());

                if (renderChapterTitles)
                    ProximityTitleRenderer.setTitle(currentChapterInfo.getName(), currentPathColors[0]);

            }
        }
    }

    /**
     * Create a PlayerTeleportPacket for the given player and position.
     *
     * @param player    The player to teleport
     * @param playerPos The position to teleport to
     * @return A PlayerTeleportPacket for the given player and position
     */
    private static @NotNull PlayerTeleportPacket getPlayerTeleportPacket(ClientPlayerEntity player, BlockPos playerPos) {

        var worldId = player.getWorld()
                .getRegistryKey()
                .getValue();

        return new PlayerTeleportPacket(playerPos.getX(), playerPos.getY(), playerPos.getZ(), worldId);
    }

    /**
     * Update the last visited trail node data. This is used for respawning the player at the last visited trail node if
     * the return to path button is pressed.
     *
     * @param currentChapterId     The current chapter ID
     * @param closestValidMarker   The closest valid path marker
     */
    private static void updateLastVisitedTrailNode(String currentChapterId, PathMarkerBlockEntity closestValidMarker){

        Identifier worldId = null;

        if (closestValidMarker.getWorld() != null)
            worldId = closestValidMarker.getWorld()
                    .getRegistryKey()
                    .getValue();

        ArdaPathsClient.lastVisitedTrailNodeData = new LastVisitedTrailNodeData(currentChapterId,
                closestValidMarker.getPos().getX(),
                closestValidMarker.getPos().getY(),
                closestValidMarker.getPos().getZ(),
                worldId);
    }

    /**
     * Register a new trail to render.
     *
     * @param trail The trail to render
     */
    public static void registerTrail(AnimatedTrail trail) {

        trails.add(trail);

        if (trailSoundInstance != null) return;

        trailSoundInstance = new TrailSoundInstance(trail);
        MinecraftClient.getInstance().getSoundManager().play(trailSoundInstance);
    }

    /**
     * Clear all registered trails.
     */
    public static void clearTrails() {
        trails.clear();
        trailSoundInstance = null;
    }
}
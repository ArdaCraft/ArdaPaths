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
import space.ajcool.ardapaths.core.integration.Waypoints;
import space.ajcool.ardapaths.core.networking.packets.server.PlayerTeleportPacket;
import space.ajcool.ardapaths.mc.blocks.entities.ModBlockEntities;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;
import space.ajcool.ardapaths.mc.items.ModItems;
import space.ajcool.ardapaths.mc.sounds.TrailSoundInstance;
import space.ajcool.ardapaths.paths.Paths;
import space.ajcool.ardapaths.paths.rendering.objects.AnimatedMessage;
import space.ajcool.ardapaths.paths.rendering.objects.AnimatedTrail;

import java.util.*;

/**
 * Responsible for rendering trails in the client world.
 */
public class TrailRenderer {

    private static final List<AnimatedTrail> trails = new ArrayList<>();

    /**
     * Active trails keyed by their starting marker position.
     */
    private static final Map<BlockPos, AnimatedTrail> trailsByStart = new HashMap<>();

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
            ProximityRenderer.clear();

        // Else, render trails based on the held item
        } else {

            String currentPathId = selectedPath.getId();
            Color[] currentPathColors = selectedPath.getColors();
            String currentChapterId = ArdaPathsClient.CONFIG.getCurrentChapterId();

            if (isHoldingMarker)
                renderPathMarkerMode(currentPathId, currentChapterId, currentPathColors);
            else
                renderPathRevealerMode(player, currentPathId, currentChapterId, currentPathColors);

            renderTrails(level, player, selectedPath, currentChapterId, isHoldingRevealer);
        }
    }

    /**
     * Render the trails and remove those that are out of range or at the end.
     *
     * @param level           The client world
     * @param player          The client player entity
     * @param selectedPath    The currently selected path data
     * @param currentChapterId The current chapter ID
     * @param isHoldingRevealer whether the player is holding the Pathfinder
     */
    private static void renderTrails(ClientWorld level, ClientPlayerEntity player, PathData selectedPath, String currentChapterId, boolean isHoldingRevealer) {

        Iterator<AnimatedTrail> iterator = trails.iterator();
        var playerPosition = player.getPos();
        List<PathMarkerBlockEntity> markersToStart = null;

        while (iterator.hasNext()) {
            AnimatedTrail trail = iterator.next();

            var distanceToTrail = playerPosition.squaredDistanceTo(trail.getCurrentPos());

            if (distanceToTrail > (isHoldingRevealer ? 225 : 10000) || trail.isAtEnd()) {

                iterator.remove();
                trailsByStart.remove(trail.getStart());

                if (trail.isAtEnd() && isHoldingRevealer) {

                    var stopPos = BlockPos.ofFloored(trail.getCurrentPos());
                    var optionalMarkerAtPos = level.getBlockEntity(stopPos, ModBlockEntities.PATH_MARKER);

                    if (optionalMarkerAtPos.isPresent()) {
                        if (markersToStart == null) {
                            markersToStart = new ArrayList<>();
                        }
                        markersToStart.add(optionalMarkerAtPos.get());
                    }
                }
                continue;
            }

            trail.render(level);
        }

        if (markersToStart != null) {
            for (PathMarkerBlockEntity marker : markersToStart) {
                marker.createTrail(selectedPath.getId(), currentChapterId, selectedPath.getColors());
            }
        }
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

            if (!trailsByStart.containsKey(marker.getPos()))
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

                displayAnimatedText(squaredDistance, currentChapterData, player, marker.getPos(), currentPathColors);

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

        setWaypointToNextTrailNode(closestValidMarker);
    }

    /**
     * Adds an ardamap waypoint on the next node
     * @param closestValidMarker the closest valid marker
     */
    private static void setWaypointToNextTrailNode(PathMarkerBlockEntity closestValidMarker) {

        if (closestValidMarker != null) {
            AnimatedTrail trail = trailsByStart.get(closestValidMarker.getPos());
            if (trail != null) {
                Waypoints.setNextTrailNode(trail.getEnd());
            }
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
        if (currentChapter == null) return;

        List<PathMarkerBlockEntity.ChapterNbtData> chapters = marker.getChapters(currentPathId, false);
        if (chapters == null || chapters.isEmpty()) return;

        ChapterData selectedChapter = null;
        for (var otherChapterData : chapters) {
            String otherChapterId = otherChapterData.getChapterId();
            if (otherChapterId.isEmpty() || !otherChapterData.isChapterStart()) continue;
            if (squaredDistance > MathHelper.square(otherChapterData.getActivationRange())) continue;

            ChapterData chapter = selectedPath.getChapter(otherChapterId);
            if (chapter == null) continue;

            if ("default".equalsIgnoreCase(currentChapter.getName())) {

                var targetChapterIsDefault = "default".equalsIgnoreCase(chapter.getName());

                if (targetChapterIsDefault) continue;

                var currentSelectedChapterIsNull = selectedChapter == null;
                var selectedChapterIsAfter       = !currentSelectedChapterIsNull && (chapter.getIndex() < selectedChapter.getIndex());

                if (currentSelectedChapterIsNull || selectedChapterIsAfter) {
                    selectedChapter = chapter;
                }

                continue;
            }

            if (chapter.getIndex() <= currentChapter.getIndex()) continue;
            if ((chapter.getIndex() - currentChapter.getIndex()) > 1) continue;

            selectedChapter = chapter;
            break;
        }

        if (selectedChapter == null) return;

        String selectedChapterId = selectedChapter.getId();
        if (!selectedChapterId.equals(ArdaPathsClient.CONFIG.getCurrentChapterId())) {
            ArdaPathsClient.CONFIG.setCurrentChapter(selectedChapterId);
            ArdaPathsClient.CONFIG_MANAGER.save();
        }
    }

    /**
     * Display animated text (Chapter Title or Proximity message) based on the player's proximity to a path marker.
     *
     * @param squaredDistance     The squared distance between the player and the path marker
     * @param currentChapterData  The chapter data of the path marker
     * @param player              The player entity
     * @param markerPos           The position of the marker
     * @param currentPathColors   The colors of the current path
     */
    private static void displayAnimatedText(double squaredDistance,
                                            PathMarkerBlockEntity.ChapterNbtData currentChapterData,
                                            ClientPlayerEntity player,
                                            BlockPos markerPos,
                                            Color[] currentPathColors) {

        var renderMessages      = ArdaPathsClient.CONFIG.showProximityMessages();
        var renderChapterTitles = ArdaPathsClient.CONFIG.showChapterTitles();
        var selectedPath        = ArdaPathsClient.CONFIG.getSelectedPath();
        assert selectedPath != null;

        // If we are within activation range
        if (squaredDistance <= MathHelper.square(currentChapterData.getActivationRange())) {

            // Render proximity message
            String proximityMessage = currentChapterData.getProximityMessage();
            if (!proximityMessage.isEmpty() && renderMessages && !ProximityRenderer.isShowingOrQueuedMessage(proximityMessage)) {

                Journal.addProximityMessage(selectedPath.getId(),
                        currentChapterData.getChapterId(),
                        proximityMessage,
                        getPlayerTeleportPacket(player, markerPos));

                ProximityRenderer.addMessage(AnimatedMessage.getAnimatedMessage(currentChapterData));
            }

            // Render chapter title
            ChapterData currentChapterInfo = selectedPath.getChapter(currentChapterData.getChapterId());
            if (currentChapterInfo != null && currentChapterData.isChapterStart() && currentChapterData.isDisplayChapterTitleOnTrail()) {

                // Add chapter start to journal either way to ensure it's recorded
                Journal.addChapterStart(selectedPath.getId(),
                        currentChapterData.getChapterId(),
                        currentChapterInfo.getName(),
                        getPlayerTeleportPacket(player, markerPos),
                        currentPathColors[0].asHex());

                String chapterName = currentChapterInfo.getName();
                if (renderChapterTitles && !ProximityRenderer.isShowingOrQueuedTitle(chapterName))
                    ProximityRenderer.addTitle(chapterName, currentPathColors[0]);

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
        if (trailsByStart.containsKey(trail.getStart())) return;

        trails.add(trail);
        trailsByStart.put(trail.getStart(), trail);

        if (trailSoundInstance != null && !trailSoundInstance.isDone()) return;

        trailSoundInstance = new TrailSoundInstance(trail);
        MinecraftClient.getInstance().getSoundManager().play(trailSoundInstance);
    }

    /**
     * Clear all registered trails.
     */
    public static void clearTrails() {

        Waypoints.clearWaypoints();
        trails.clear();
        trailsByStart.clear();
        trailSoundInstance = null;
    }
}

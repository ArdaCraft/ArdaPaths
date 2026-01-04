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

public class TrailRenderer
{
    private static final double DESIRED_SPACING = 15.0;
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

        String currentPathId = selectedPath.getId();
        Color[] currentPathColors = selectedPath.getColors();
        String currentChapterId = ArdaPathsClient.CONFIG.getCurrentChapterId();

        if (!player.isHolding(ModItems.PATH_REVEALER) && !player.isHolding(ModItems.PATH_MARKER))
        {
            clearTrails();
            ProximityMessageRenderer.clearMessage();
            ProximityTitleRenderer.clearMessage();
            return;
        }

        if (player.isHolding(ModItems.PATH_MARKER))
        {
            Paths.getTickingMarkers().forEach(marker ->
            {
                PathMarkerBlockEntity.ChapterNbtData data = marker.getChapterData(currentPathId, currentChapterId, false);

                if (data == null) return;
                if (trails.stream().anyMatch(trail -> trail.getStart().equals(marker.getPos()))) return;

                marker.createTrail(currentPathId, currentChapterId, currentPathColors);
            });
        }
        else
        {
            PathMarkerBlockEntity closestValidMarker = null;
            var closestSquaredDistance = Double.MAX_VALUE;

            for (PathMarkerBlockEntity marker : Paths.getTickingMarkers())
            {
                BlockPos markerPos = marker.getPos();
                BlockPos playerPos = player.getBlockPos();
                var squaredDistance = playerPos.getSquaredDistance(markerPos);

                PathMarkerBlockEntity.ChapterNbtData currentChapterData = marker.getChapterData(currentPathId, currentChapterId, false);

                if (currentChapterData != null)
                {
                    displayAnimatedText(squaredDistance, currentChapterData, player, playerPos, currentPathColors);

                    if (currentChapterData.getTarget() != null && squaredDistance < closestSquaredDistance)
                    {
                        closestValidMarker = marker;
                        closestSquaredDistance = squaredDistance;
                    }
                }

                List<PathMarkerBlockEntity.ChapterNbtData> filteredChapters =
                        marker.getChapters(currentPathId).stream()
                                // Must have a non-empty chapter ID
                                .filter(data -> !data.getChapterId().isEmpty())
                                // Must be a chapter start
                                .filter(PathMarkerBlockEntity.ChapterNbtData::isChapterStart)
                                // Must be within activation range
                                .filter(data -> squaredDistance <= MathHelper.square(data.getActivationRange()))
                                .toList();

                ChapterData currentChapter = ArdaPathsClient.CONFIG.getCurrentChapter();

                shouldSwitchToNewChapter(filteredChapters, currentChapter);
            }

            if (trails.isEmpty() && closestValidMarker != null && closestSquaredDistance <= 100)
            {
                updateLastVisitedTrailNode(currentChapterId, closestValidMarker);
                closestValidMarker.createTrail(currentPathId, currentChapterId, currentPathColors);
            }
        }

        for (AnimatedTrail trail : List.copyOf(trails))
        {
            renderTrail(level, trail, player, selectedPath, currentChapterId);
        }
    }

    /**
     * Display animated text based on the player's proximity to a path marker.
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

        var renderMessages = ArdaPathsClient.CONFIG.showProximityMessages();
        var renderChapterTitles = ArdaPathsClient.CONFIG.showChapterTitles();
        var selectedPath = ArdaPathsClient.CONFIG.getSelectedPath();

        if (squaredDistance <= MathHelper.square(currentChapterData.getActivationRange()))
        {
            if (!currentChapterData.getProximityMessage().isEmpty() && renderMessages) {

                var animatedMessage = AnimatedMessage.getAnimatedMessage(currentChapterData);
                Journal.addProximityMessage(currentChapterData.getProximityMessage(), getPlayerTeleportPacket(player, playerPos));
                ProximityMessageRenderer.setMessage(animatedMessage);
            }

            if (selectedPath != null) {

                ChapterData currentChapterInfo = selectedPath.getChapter(currentChapterData.getChapterId());
                if (currentChapterInfo != null && currentChapterData.isChapterStart() && currentChapterData.isDisplayChapterTitleOnTrail() && renderChapterTitles) {

                    Journal.addChapterStart(currentChapterInfo.getName(), getPlayerTeleportPacket(player, playerPos));
                    ProximityTitleRenderer.setTitle(currentChapterInfo.getName(), currentPathColors[0]);
                }
            }
        }
    }

    /**
     * Determine if the current chapter should be switched to a new chapter based on proximity to path markers.
     *
     * @param filteredChapters List of chapter data from nearby path markers
     * @param currentChapter   The current chapter data
     */
    private static void shouldSwitchToNewChapter(List<PathMarkerBlockEntity.ChapterNbtData> filteredChapters, ChapterData currentChapter) {

        for (var otherChapterData : filteredChapters)
        {
            String otherChapterId = otherChapterData.getChapterId();

            ChapterData chapter = ArdaPathsClient.CONFIG.getSelectedPath().getChapter(otherChapterId);

            if (currentChapter == null || chapter == null) continue;

            if (!"default".equalsIgnoreCase(currentChapter.getName())) {
                if (chapter.getIndex() <= currentChapter.getIndex()) continue;
                if ((chapter.getIndex() - currentChapter.getIndex()) > 1) continue;
            } else {

                otherChapterId = filteredChapters.stream()
                        .map(otherChapterIdentifier -> ArdaPathsClient.CONFIG.getSelectedPath().getChapter(otherChapterIdentifier.getChapterId()))
                        .min(Comparator.comparingInt(chapterData -> chapterData != null ? chapterData.getIndex() : 0))
                        .map(ChapterData::getId)
                        .orElse(otherChapterId);
            }

            ArdaPathsClient.CONFIG.setCurrentChapter(otherChapterId);
            ArdaPathsClient.CONFIG_MANAGER.save();
        }
    }

    /**
     * Render a single trail. Remove it if the player is too far away or if it has reached the end of the trail.
     *
     * @param level           The client world
     * @param trail           The trail to render
     * @param player          The player entity
     * @param selectedPath   The selected path data
     * @param currentChapterId The current chapter ID
     */
    private static void renderTrail(ClientWorld level, AnimatedTrail trail, ClientPlayerEntity player, PathData selectedPath, String currentChapterId) {

        var playerPosition = player.getPos();
        var distanceToTrail = playerPosition.squaredDistanceTo(trail.getCurrentPos());

        if (distanceToTrail > (player.isHolding(ModItems.PATH_REVEALER) ? 225 : 10000))
        {
            removeTrail(trail);
            return;
        }

        if (trail.isAtEnd())
        {
            if (player.isHolding(ModItems.PATH_REVEALER))
            {
                var stopPos = BlockPos.ofFloored(trail.getCurrentPos());
                var optionalMarkerAtPos = level.getBlockEntity(stopPos, ModBlockEntities.PATH_MARKER);

                optionalMarkerAtPos.ifPresent(marker -> marker.createTrail(selectedPath.getId(), currentChapterId, selectedPath.getColors()));
            }

            removeTrail(trail);
            return;
        }

        trail.render(level);
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
     * Update the last visited trail node data.
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
    public static void registerTrail(AnimatedTrail trail)
    {
        trails.add(trail);

        if (trailSoundInstance != null) return;

        trailSoundInstance = new TrailSoundInstance(trail);
        MinecraftClient.getInstance().getSoundManager().play(trailSoundInstance);
    }

    /**
     * Remove a trail from the list of trails to render.
     *
     * @param start The starting position of the trail to remove
     */
    public static void removeTrail(BlockPos start)
    {
        trails.removeIf(trail -> trail.getStart().equals(start));
    }

    /**
     * Remove a trail from the list of trails to render.
     *
     * @param trail The trail to remove
     */
    public static void removeTrail(AnimatedTrail trail)
    {
        trails.remove(trail);
    }

    /**
     * Clear all registered trails.
     */
    public static void clearTrails()
    {
        trails.clear();
        trailSoundInstance = null;
    }
}
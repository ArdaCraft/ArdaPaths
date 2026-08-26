package space.ajcool.ardapaths.paths.rendering;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import space.ajcool.ardapaths.ArdaPathsClient;
import space.ajcool.ardapaths.core.Client;
import space.ajcool.ardapaths.core.data.Journal;
import space.ajcool.ardapaths.core.data.LastVisitedTrailNodeData;
import space.ajcool.ardapaths.core.data.config.shared.ChapterData;
import space.ajcool.ardapaths.core.data.config.shared.Color;
import space.ajcool.ardapaths.core.data.config.shared.PathData;
import space.ajcool.ardapaths.core.integration.Waypoints;
import space.ajcool.ardapaths.core.networking.PacketRegistry;
import space.ajcool.ardapaths.core.networking.packets.server.MarkerActionTriggerPacket;
import space.ajcool.ardapaths.core.networking.packets.server.PlayerTeleportPacket;
import space.ajcool.ardapaths.mc.blocks.entities.ModBlockEntities;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;
import space.ajcool.ardapaths.mc.items.ModItems;
import space.ajcool.ardapaths.mc.sounds.TrailSoundInstance;
import space.ajcool.ardapaths.paths.Paths;
import space.ajcool.ardapaths.paths.movement.FocusController;
import space.ajcool.ardapaths.paths.rendering.objects.AnimatedMessage;
import space.ajcool.ardapaths.paths.rendering.objects.AnimatedTrail;

import java.util.*;

/**
 * Responsible for rendering trails in the client world.
 */
public class TrailRenderer {

    /**
     * Extra distance, in blocks, the player must move beyond a marker's activation range before its text can play again.
     */
    private static final double PROXIMITY_EXIT_BUFFER = 2.0;

    /**
     * Minimum distance, in blocks, at which marker actions may trigger even without proximity text range.
     */
    private static final int MIN_ACTION_TRIGGER_RANGE = 3;

    /**
     * Maximum squared distance at which revealer-mode trail sound remains relevant.
     */
    private static final double REVEALER_SOUND_CULL_DISTANCE_SQUARED = 225.0D;

    /**
     * Distance from trail geometry at which the player is considered on the trail.
     */
    private static final double ON_TRAIL_OFFSET = 3.0D;

    /**
     * Distance from trail geometry at which the off-trail guidance loop reaches full volume.
     */
    private static final double OFF_TRAIL_FULL_VOLUME_DISTANCE = 7.0D;

    /**
     * Maximum number of completed revealer trail segments retained for sound anchoring.
     */
    private static final int MAX_AUDIBLE_SEGMENTS = 32;

    /**
     * Animated trail segments currently being rendered.
     */
    private static final List<AnimatedTrail> trails = new ArrayList<>();

    /**
     * Active trails keyed by their starting marker position.
     */
    private static final Map<BlockPos, AnimatedTrail> trailsByStart = new HashMap<>();

    /**
     * Recently completed revealer trail geometry keyed by starting marker position.
     */
    private static final LinkedHashMap<BlockPos, AudibleSegment> audibleSegments = new LinkedHashMap<>();

    /**
     * Markers whose proximity text or chapter title has already played for the current visit.
     */
    private static final Map<BlockPos, ProximityActivation> proximityActivations = new HashMap<>();

    /**
     * Markers whose server-executed actions have already triggered for the current visit.
     */
    private static final Map<BlockPos, ProximityActivation> markerActionActivations = new HashMap<>();

    /**
     * Sound instance for the currently audible trail rendering effect.
     */
    private static TrailSoundInstance trailSoundInstance = null;

    /**
     * Visit state for a marker whose proximity text or chapter title has already been triggered.
     *
     * @param pathId              The path the activation was triggered for
     * @param chapterId           The chapter the activation was triggered for
     * @param exitDistanceSquared The squared distance at which the marker can trigger again
     */
    private record ProximityActivation(String pathId, String chapterId, double exitDistanceSquared) {}

    /**
     * Completed trail geometry retained as part of the audible revealer polyline.
     *
     * @param sx the segment start x coordinate
     * @param sy the segment start y coordinate
     * @param sz the segment start z coordinate
     * @param ex the segment end x coordinate
     * @param ey the segment end y coordinate
     * @param ez the segment end z coordinate
     */
    private record AudibleSegment(double sx, double sy, double sz, double ex, double ey, double ez) {
        /**
         * Returns the normalized projection point on this segment nearest to the supplied position.
         *
         * @param px the probe x coordinate
         * @param py the probe y coordinate
         * @param pz the probe z coordinate
         * @return the clamped segment parameter in the range {@code [0, 1]}
         */
        private double closestSegmentT(double px, double py, double pz) {
            double segmentX = ex - sx;
            double segmentY = ey - sy;
            double segmentZ = ez - sz;
            double segmentLengthSquared = (segmentX * segmentX) + (segmentY * segmentY) + (segmentZ * segmentZ);

            if (segmentLengthSquared == 0.0D) {
                return 0.0D;
            }

            double relativeX = px - sx;
            double relativeY = py - sy;
            double relativeZ = pz - sz;
            double projection = ((relativeX * segmentX) + (relativeY * segmentY) + (relativeZ * segmentZ)) / segmentLengthSquared;

            return MathHelper.clamp(projection, 0.0D, 1.0D);
        }

        /**
         * Computes the x coordinate at a normalized segment position.
         *
         * @param t the normalized segment position
         * @return the x coordinate at {@code t}
         */
        private double xAt(double t) {
            return sx + ((ex - sx) * t);
        }

        /**
         * Computes the y coordinate at a normalized segment position.
         *
         * @param t the normalized segment position
         * @return the y coordinate at {@code t}
         */
        private double yAt(double t) {
            return sy + ((ey - sy) * t);
        }

        /**
         * Computes the z coordinate at a normalized segment position.
         *
         * @param t the normalized segment position
         * @return the z coordinate at {@code t}
         */
        private double zAt(double t) {
            return sz + ((ez - sz) * t);
        }
    }

    /**
     * Render all registered trails.
     *
     * @param level The client world
     */
    public static void render(ClientWorld level)
    {

        ClientPlayerEntity player = Client.player();
        if (player == null) return;

        AnimatedTrail.updatePlayerSpeed(player);

        PathData selectedPath = ArdaPathsClient.CONFIG.getSelectedPath();
        if (selectedPath == null) {
            FocusController.setCandidate(null);
            return;
        }

        boolean isHoldingRevealer = player.isHolding(ModItems.PATH_REVEALER);
        boolean isHoldingMarker = player.isHolding(ModItems.PATH_MARKER);

        // If the player is not holding either item, clear trails and messages
        if (!isHoldingRevealer && !isHoldingMarker) {

            pruneProximityActivations(player.getBlockPos());
            EnvironmentController.releaseControl();
            clearTrails();
            ProximityRenderer.clear();
            FocusController.setCandidate(null);

        // Else, render trails based on the held item
        } else {

            String currentPathId = selectedPath.getId();
            Color[] currentPathColors = selectedPath.getColors();
            String currentChapterId = ArdaPathsClient.CONFIG.getCurrentChapterId();

            if (isHoldingMarker) {
                EnvironmentController.releaseControl();
                FocusController.setCandidate(null);
                renderPathMarkerMode(currentPathId, currentChapterId, currentPathColors);
            } else {
                renderPathRevealerMode(level, player, currentPathId, currentChapterId, currentPathColors);
            }

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
        double playerX = player.getX();
        double playerY = player.getY();
        double playerZ = player.getZ();
        double nearestDistSq = Double.MAX_VALUE;
        double nearestX = 0.0D;
        double nearestY = 0.0D;
        double nearestZ = 0.0D;
        boolean hasSoundTarget = false;
        List<PathMarkerBlockEntity> markersToStart = null;
        Map<BlockPos, Double> segmentOvershoots = null;
        Vec3d playerPos = new Vec3d(playerX, playerY, playerZ);

        while (iterator.hasNext()) {
            AnimatedTrail trail = iterator.next();

            if (trail.isAtEnd()) {
                if (isHoldingRevealer) {
                    rememberAudibleSegment(trail);
                }

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
                        if (segmentOvershoots == null) {
                            segmentOvershoots = new HashMap<>();
                        }
                        segmentOvershoots.put(stopPos.toImmutable(), trail.overshoot());
                    }
                }
                continue;
            }

            Vec3d currentPos = trail.getCurrentPos();
            double currentDistSq = squaredDistance(playerX, playerY, playerZ, currentPos.x, currentPos.y, currentPos.z);

            if (currentDistSq > (isHoldingRevealer ? REVEALER_SOUND_CULL_DISTANCE_SQUARED : 10000)) {
                iterator.remove();
                trailsByStart.remove(trail.getStart());
                continue;
            }

            trail.render(level, playerPos);

            hasSoundTarget = true;

            Vec3d startPos = trail.getStartPos();
            Vec3d endPos = trail.getEnd();
            double segmentT = trail.closestSegmentT(playerX, playerY, playerZ);
            double candidateX = startPos.x + ((endPos.x - startPos.x) * segmentT);
            double candidateY = startPos.y + ((endPos.y - startPos.y) * segmentT);
            double candidateZ = startPos.z + ((endPos.z - startPos.z) * segmentT);
            double candidateDistSq = squaredDistance(playerX, playerY, playerZ, candidateX, candidateY, candidateZ);

            if (candidateDistSq < nearestDistSq) {
                nearestDistSq = candidateDistSq;
                nearestX = candidateX;
                nearestY = candidateY;
                nearestZ = candidateZ;
            }

            if (trail.isAtEnd()) {
                if (isHoldingRevealer) {
                    rememberAudibleSegment(trail);
                }

                iterator.remove();
                trailsByStart.remove(trail.getStart());

                if (isHoldingRevealer) {
                    BlockPos stopPos = BlockPos.ofFloored(trail.getCurrentPos());
                    var optionalMarkerAtPos = level.getBlockEntity(stopPos, ModBlockEntities.PATH_MARKER);

                    if (optionalMarkerAtPos.isPresent()) {
                        if (markersToStart == null) {
                            markersToStart = new ArrayList<>();
                        }
                        markersToStart.add(optionalMarkerAtPos.get());
                        if (segmentOvershoots == null) {
                            segmentOvershoots = new HashMap<>();
                        }
                        segmentOvershoots.put(stopPos.toImmutable(), trail.overshoot());
                    }
                }
            }
        }

        if (markersToStart != null) {
            for (PathMarkerBlockEntity marker : markersToStart) {
                marker.createTrail(selectedPath.getId(), currentChapterId, selectedPath.getColors());

                AnimatedTrail createdTrail = trailsByStart.get(marker.getPos());
                if (createdTrail == null) {
                    continue;
                }

                createdTrail.advanceBy(segmentOvershoots.getOrDefault(marker.getPos(), 0.0D));
                createdTrail.render(level, playerPos);

                Vec3d startPos = createdTrail.getStartPos();
                Vec3d endPos = createdTrail.getEnd();
                double segmentT = createdTrail.closestSegmentT(playerX, playerY, playerZ);
                double candidateX = startPos.x + ((endPos.x - startPos.x) * segmentT);
                double candidateY = startPos.y + ((endPos.y - startPos.y) * segmentT);
                double candidateZ = startPos.z + ((endPos.z - startPos.z) * segmentT);
                double candidateDistSq = squaredDistance(playerX, playerY, playerZ, candidateX, candidateY, candidateZ);

                if (candidateDistSq < nearestDistSq) {
                    nearestDistSq = candidateDistSq;
                    nearestX = candidateX;
                    nearestY = candidateY;
                    nearestZ = candidateZ;
                }

                hasSoundTarget = true;
            }
        }

        if (isHoldingRevealer) {
            Iterator<Map.Entry<BlockPos, AudibleSegment>> audibleIterator = audibleSegments.entrySet().iterator();
            while (audibleIterator.hasNext()) {
                AudibleSegment segment = audibleIterator.next().getValue();
                double segmentT = segment.closestSegmentT(playerX, playerY, playerZ);
                double candidateX = segment.xAt(segmentT);
                double candidateY = segment.yAt(segmentT);
                double candidateZ = segment.zAt(segmentT);
                double candidateDistSq = squaredDistance(playerX, playerY, playerZ, candidateX, candidateY, candidateZ);

                if (candidateDistSq > REVEALER_SOUND_CULL_DISTANCE_SQUARED) {
                    audibleIterator.remove();
                    continue;
                }

                hasSoundTarget = true;

                if (candidateDistSq < nearestDistSq) {
                    nearestDistSq = candidateDistSq;
                    nearestX = candidateX;
                    nearestY = candidateY;
                    nearestZ = candidateZ;
                }
            }
        }

        if (hasSoundTarget) {
            double nearestDistance = Math.sqrt(nearestDistSq);
            float targetEnvelope = proximityEnvelope(nearestDistance);
            ensureTrailSound(nearestX, nearestY, nearestZ);
            trailSoundInstance.setTargetEnvelope(targetEnvelope);
        } else if (trailSoundInstance != null && !trailSoundInstance.isDone()) {
            trailSoundInstance.release();
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
     * @param ignoredLevel             The client world
     * @param player            The client player entity
     * @param currentPathId     The current path ID
     * @param currentChapterId  The current chapter ID
     * @param currentPathColors The colors of the current path
     */
    private static void renderPathRevealerMode(ClientWorld ignoredLevel, ClientPlayerEntity player, String currentPathId,
                                               String currentChapterId, Color[] currentPathColors)
    {
        BlockPos playerPos = player.getBlockPos();
        Vec3d precisePlayerPos = player.getPos();
        pruneProximityActivations(playerPos);

        PathMarkerBlockEntity closestValidMarker = null;
        double closestSquaredDistance = Double.MAX_VALUE;
        BlockPos closestFocusTarget = null;
        double closestFocusSquaredDistance = Double.MAX_VALUE;

        for (PathMarkerBlockEntity marker : Paths.getTickingMarkers()) {

            double squaredDistance = playerPos.getSquaredDistance(marker.getPos());
            PathMarkerBlockEntity.ChapterNbtData currentChapterData = marker.getChapterData(currentPathId, currentChapterId, false);

            if (currentChapterData != null) {

                displayAnimatedText(squaredDistance, currentChapterData, player, marker.getPos(), currentPathColors);
                processMarkerActions(squaredDistance, currentChapterData, marker.getPos(), currentPathId);
                EnvironmentController.processMarker(currentChapterData, marker.getPos(), precisePlayerPos, currentPathId);

                if (currentChapterData.getTarget() != null && squaredDistance < closestSquaredDistance) {
                    closestValidMarker = marker;
                    closestSquaredDistance = squaredDistance;
                }

                if (currentChapterData.getLookAt() != null
                        && squaredDistance <= FocusController.FOCUS_PROMPT_RANGE_SQUARED
                        && squaredDistance < closestFocusSquaredDistance) {
                    closestFocusTarget = currentChapterData.getLookAt();
                    closestFocusSquaredDistance = squaredDistance;
                }
            }

            processChapterSwitching(marker, currentPathId, squaredDistance);
        }

        FocusController.setCandidate(closestFocusTarget);
        EnvironmentController.tick(precisePlayerPos);

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

            ProximityActivation existing = proximityActivations.get(markerPos);
            if (existing != null
                    && existing.pathId().equals(selectedPath.getId())
                    && existing.chapterId().equals(currentChapterData.getChapterId())) return;

            proximityActivations.put(markerPos.toImmutable(), new ProximityActivation(
                    selectedPath.getId(),
                    currentChapterData.getChapterId(),
                    MathHelper.square(currentChapterData.getActivationRange() + PROXIMITY_EXIT_BUFFER)));

            // Render proximity message
            String proximityMessage = currentChapterData.getProximityMessage();
            if (!proximityMessage.isEmpty() && renderMessages) {

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
                if (renderChapterTitles)
                    ProximityRenderer.addTitle(chapterName, currentPathColors[0]);

            }
        }
    }

    /**
     * Sends a server action trigger once when the player enters an authored marker action range.
     *
     * @param squaredDistance    The squared distance between the player and the path marker
     * @param currentChapterData The chapter data of the path marker
     * @param markerPos          The position of the marker
     * @param currentPathId      The ID of the selected path
     */
    private static void processMarkerActions(double squaredDistance,
                                             PathMarkerBlockEntity.ChapterNbtData currentChapterData,
                                             BlockPos markerPos,
                                             String currentPathId) {
        boolean hasActions = !currentChapterData.getAutoTeleportTarget().isEmpty() || !currentChapterData.getGiveItem().isEmpty();
        if (!hasActions) return;

        int activationRange = Math.max(currentChapterData.getActivationRange(), MIN_ACTION_TRIGGER_RANGE);
        if (squaredDistance > MathHelper.square(activationRange)) return;

        ProximityActivation existing = markerActionActivations.get(markerPos);
        if (existing != null
                && existing.pathId().equals(currentPathId)
                && existing.chapterId().equals(currentChapterData.getChapterId())) return;

        markerActionActivations.put(markerPos.toImmutable(), new ProximityActivation(
                currentPathId,
                currentChapterData.getChapterId(),
                MathHelper.square(activationRange + PROXIMITY_EXIT_BUFFER)));

        PacketRegistry.MARKER_ACTION_TRIGGER.send(new MarkerActionTriggerPacket(markerPos, currentPathId, currentChapterData.getChapterId()));
    }

    /**
     * Re-arm proximity text for any triggered markers the player has moved away from.
     *
     * @param playerPos The player's current block position
     */
    private static void pruneProximityActivations(BlockPos playerPos) {

        proximityActivations.entrySet().removeIf(entry -> playerPos.getSquaredDistance(entry.getKey()) > entry.getValue().exitDistanceSquared());
        markerActionActivations.entrySet().removeIf(entry -> playerPos.getSquaredDistance(entry.getKey()) > entry.getValue().exitDistanceSquared());
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

        audibleSegments.remove(trail.getStart());
        trails.add(trail);
        trailsByStart.put(trail.getStart(), trail);
    }

    /**
     * Clears all registered trails and releases the active trail sound for a graceful fade-out.
     */
    public static void clearTrails() {

        Waypoints.clearWaypoints();
        trails.clear();
        trailsByStart.clear();
        audibleSegments.clear();
        AnimatedTrail.resetPlayerSpeed();

        if (trailSoundInstance != null && !trailSoundInstance.isDone()) {
            trailSoundInstance.release();
        }
    }

    /**
     * Ensures the trail sound exists and is anchored at the supplied position.
     *
     * @param x the world x coordinate to emit from
     * @param y the world y coordinate to emit from
     * @param z the world z coordinate to emit from
     */
    private static void ensureTrailSound(double x, double y, double z) {
        if (trailSoundInstance == null
                || trailSoundInstance.isDone()
                || !MinecraftClient.getInstance().getSoundManager().isPlaying(trailSoundInstance)) {
            trailSoundInstance = new TrailSoundInstance(x, y, z);
            MinecraftClient.getInstance().getSoundManager().play(trailSoundInstance);
            return;
        }

        trailSoundInstance.updatePosition(x, y, z);
    }

    /**
     * Computes the sound envelope target from the player's distance to the nearest trail geometry.
     *
     * @param distance the shortest distance from the player to the trail
     * @return the target sound envelope in the range {@code [0, 1]}
     */
    private static float proximityEnvelope(double distance) {
        double ramp = (distance - ON_TRAIL_OFFSET) / (OFF_TRAIL_FULL_VOLUME_DISTANCE - ON_TRAIL_OFFSET);

        return (float) MathHelper.clamp(ramp, 0.0D, 1.0D);
    }

    /**
     * Retains a completed revealer trail segment as sound-anchor geometry.
     *
     * @param trail the completed trail whose static segment should remain audible
     */
    private static void rememberAudibleSegment(AnimatedTrail trail) {
        Vec3d startPos = trail.getStartPos();
        Vec3d endPos = trail.getEnd();

        audibleSegments.put(trail.getStart().toImmutable(), new AudibleSegment(
                startPos.x,
                startPos.y,
                startPos.z,
                endPos.x,
                endPos.y,
                endPos.z));

        while (audibleSegments.size() > MAX_AUDIBLE_SEGMENTS) {
            Iterator<BlockPos> iterator = audibleSegments.keySet().iterator();
            if (!iterator.hasNext()) {
                return;
            }
            iterator.next();
            iterator.remove();
        }
    }

    /**
     * Computes the squared distance between two world positions.
     *
     * @param x1 the first position x coordinate
     * @param y1 the first position y coordinate
     * @param z1 the first position z coordinate
     * @param x2 the second position x coordinate
     * @param y2 the second position y coordinate
     * @param z2 the second position z coordinate
     * @return the squared distance between the supplied positions
     */
    private static double squaredDistance(double x1, double y1, double z1, double x2, double y2, double z2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        double dz = z1 - z2;

        return (dx * dx) + (dy * dy) + (dz * dz);
    }
}

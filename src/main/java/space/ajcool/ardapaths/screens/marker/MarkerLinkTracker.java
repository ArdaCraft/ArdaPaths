package space.ajcool.ardapaths.screens.marker;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import space.ajcool.ardapaths.ArdaPathsClient;
import space.ajcool.ardapaths.core.data.config.shared.ChapterData;
import space.ajcool.ardapaths.core.data.config.shared.PathData;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;

import java.util.*;

/**
 * Tracks the path and chapter associations that existed when marker editing began.
 */
public class MarkerLinkTracker {

    /** Captured marker positions for original-entry sets that are passed between same-marker screens. */
    private static final Map<Set<AbstractMap.SimpleEntry<String, String>>, BlockPos> CAPTURED_POSITIONS = Collections.synchronizedMap(new IdentityHashMap<>());

    /** Marker whose path and chapter links are tracked. */
    private final PathMarkerBlockEntity marker;

    /** Original non-empty path and chapter associations. */
    private final Set<AbstractMap.SimpleEntry<String, String>> originalEntries;

    /**
     * Creates link tracking for a marker, optionally seeded from an existing same-marker editor round-trip.
     *
     * @param marker          marker whose associations are tracked
     * @param originalEntries existing original associations for this same marker, or null to capture current marker state
     */
    public MarkerLinkTracker(PathMarkerBlockEntity marker, Set<AbstractMap.SimpleEntry<String, String>> originalEntries) {
        this.marker = marker;
        this.originalEntries = belongsToMarker(marker, originalEntries) ? originalEntries : trackInitialPathAndChapterData();
        CAPTURED_POSITIONS.put(this.originalEntries, marker.getBlockPos().immutable());
    }

    /**
     * Checks whether a supplied original-entry set was captured for the same marker position.
     *
     * @param marker          marker currently being edited
     * @param originalEntries candidate original-entry set from a previous screen
     * @return true when the candidate set belongs to the same marker
     */
    private static boolean belongsToMarker(PathMarkerBlockEntity marker, Set<AbstractMap.SimpleEntry<String, String>> originalEntries) {
        if (originalEntries == null) return false;

        BlockPos capturedPosition = CAPTURED_POSITIONS.get(originalEntries);
        return marker.getBlockPos().equals(capturedPosition);
    }

    /**
     * Captures the current non-empty path/chapter associations for change tracking.
     *
     * @return the initial set of path/chapter entries linked to this marker
     */
    private Set<AbstractMap.SimpleEntry<String, String>> trackInitialPathAndChapterData() {
        Set<AbstractMap.SimpleEntry<String, String>> pathAndChapterData = new HashSet<>();

        var pathData = marker.getPathData();

        for (String pathEntryKey : pathData.keySet()) {
            for (String chapterEntryKey : pathData.get(pathEntryKey).keySet()) {
                boolean isDefault = marker.getPathData().get(pathEntryKey).get(chapterEntryKey) == null || marker.getPathData().get(pathEntryKey).get(chapterEntryKey).isEmpty();
                if (!isDefault) pathAndChapterData.add(new AbstractMap.SimpleEntry<>(pathEntryKey, chapterEntryKey));
            }
        }

        return pathAndChapterData;
    }

    /**
     * Returns the original association set used by screens that preserve the edit session.
     *
     * @return original path and chapter association entries
     */
    public Set<AbstractMap.SimpleEntry<String, String>> originalEntries() {
        return originalEntries;
    }

    /**
     * Counts currently non-empty linked paths and chapters.
     *
     * @return linked path and chapter counts
     */
    public LinkCounts linkCounts() {
        int linkedChapters = 0;
        int linkedPaths = 0;

        if (marker.getPathData() != null) {
            for (String pathEntry : marker.getPathData().keySet()) {
                int linkedChaptersForPath = 0;
                var chapters = marker.getPathData().get(pathEntry);

                for (String chapter : chapters.keySet()) {
                    var chapterNbtData = marker.getChapterData(pathEntry, chapter, false);
                    if (chapterNbtData != null && !chapterNbtData.isEmpty()) {
                        linkedChaptersForPath++;
                    }
                }

                if (linkedChaptersForPath > 0) {
                    linkedPaths++;
                    linkedChapters += linkedChaptersForPath;
                }
            }
        }

        return new LinkCounts(linkedPaths, linkedChapters);
    }

    /**
     * Checks if a path/chapter combination was originally linked to this marker.
     *
     * @param pathId    path ID to check
     * @param chapterId chapter ID to check
     * @return true if the path/chapter was in the original marker data
     */
    public boolean isPathAndChapterLinked(String pathId, String chapterId) {
        return originalEntries.contains(new AbstractMap.SimpleEntry<>(pathId, chapterId));
    }

    /**
     * Checks whether this marker was originally linked to any chapter in the path.
     *
     * @param pathId path ID to check
     * @return true if any original association used the path
     */
    public boolean isPathLinked(String pathId) {
        return originalEntries.stream()
                .anyMatch(entry -> entry.getKey().equals(pathId));
    }

    /**
     * Returns chapter IDs originally linked for a path.
     *
     * @param pathId path ID whose linked chapter IDs should be returned
     * @return linked chapter IDs for the path
     */
    public List<String> linkedChapterIds(String pathId) {
        return originalEntries.stream()
                .filter(entry -> entry.getKey().equals(pathId))
                .map(AbstractMap.SimpleEntry::getValue)
                .distinct()
                .toList();
    }

    /**
     * Generates a formatted text summary of newly added path/chapter associations.
     *
     * @param selectedPathId       currently selected path ID
     * @param selectedChapterId    currently selected chapter ID
     * @param selectedFormModified whether the selected form has unsaved edits
     * @return text listing the modified path and chapter entries
     */
    public Component listModifiedPathAndChapterData(String selectedPathId, String selectedChapterId, boolean selectedFormModified) {
        MutableComponent modifiedEntries = Component.empty();

        var pathData = marker.getPathData();

        for (String pathEntryKey : pathData.keySet()) {
            for (String chapterEntryKey : pathData.get(pathEntryKey).keySet()) {
                var comparedEntry = new AbstractMap.SimpleEntry<>(pathEntryKey, chapterEntryKey);
                var chapterData = marker.getChapterData(pathEntryKey, chapterEntryKey, false);
                boolean isSelectedPathAndChapter = pathEntryKey.equals(selectedPathId) && chapterEntryKey.equals(selectedChapterId) && selectedFormModified;
                boolean hasStoredData = chapterData != null && !chapterData.isEmpty();

                if (!isSelectedPathAndChapter && !hasStoredData) continue;

                if (!originalEntries.contains(comparedEntry)) {
                    PathData configuredPath = ArdaPathsClient.CONFIG.getPath(pathEntryKey);
                    ChapterData chapter = configuredPath != null ? configuredPath.getChapter(chapterEntryKey) : null;

                    MutableComponent pathName = configuredPath != null
                            ? Component.literal(configuredPath.getName()).withStyle(style -> style.withColor(configuredPath.getPrimaryColor().asHex()))
                            : Component.literal(pathEntryKey).withStyle(style -> style.withColor(0x888888).withItalic(true));
                    MutableComponent chapterName = chapter != null
                            ? Component.literal(chapter.getName()).withStyle(style -> style.withColor(configuredPath.getSecondaryColor().asHex()))
                            : Component.literal(chapterEntryKey).withStyle(style -> style.withColor(0x888888).withItalic(true));

                    modifiedEntries.append(pathName)
                            .append(Component.literal(" - "))
                            .append(chapterName)
                            .append(Component.literal(" "));
                }
            }
        }

        return modifiedEntries;
    }

    /**
     * Removes any path/chapter associations added since editing began.
     */
    public void discardChapterAndPathDataChanges() {
        var pathData = marker.getPathData();

        for (String pathEntryKey : pathData.keySet()) {
            var chapters = pathData.get(pathEntryKey);
            var iterator = chapters.keySet().iterator();

            while (iterator.hasNext()) {
                String chapterEntryKey = iterator.next();
                var comparedEntry = new AbstractMap.SimpleEntry<>(pathEntryKey, chapterEntryKey);

                if (!originalEntries.contains(comparedEntry)) {
                    iterator.remove();
                }
            }
        }
        marker.markUpdated();
    }

    /**
     * Current number of linked paths and linked chapters.
     *
     * @param paths    number of linked paths
     * @param chapters number of linked chapters
     */
    public record LinkCounts(int paths, int chapters) {

    }
}

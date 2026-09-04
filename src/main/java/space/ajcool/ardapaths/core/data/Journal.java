package space.ajcool.ardapaths.core.data;

import space.ajcool.ardapaths.core.networking.packets.server.PlayerTeleportPacket;

import java.util.*;

/**
 * Journal class to log proximity messages and chapter starts.
 */
public class Journal {

    /** Maximum size of the journal log */
    private static final int MAX_SIZE = 50;

    /** Set to store journal entries */
    private static final Set<Entry> LOG = new LinkedHashSet<>();

    /**
     * Adds a proximity message to the journal.
     *
     * @param pathId         the ID of the path this entry belongs to
     * @param chapterId      the ID of the chapter this entry belongs to
     * @param text           The proximity message text
     * @param teleportPacket The teleport packet associated with the message
     */
    public static void addProximityMessage(String pathId, String chapterId, String text, PlayerTeleportPacket teleportPacket) {
        if (LOG.add(new Entry(pathId, chapterId, text, teleportPacket, EntryType.PROXIMITY_MESSAGE))) {
            trimToMaxSize();
        }
    }

    /**
     * Ensures the journal does not exceed its maximum size by removing the oldest entry if necessary.
     */
    private static void trimToMaxSize() {

        // Remove eldest entry if we exceed max size
        while (LOG.size() > MAX_SIZE) {

            Iterator<Entry> it = LOG.iterator();
            if (it.hasNext()) {
                it.next();
                it.remove();
            }
        }
    }

    /**
     * Adds a chapter start entry to the journal.
     *
     * @param pathId         the ID of the path this entry belongs to
     * @param chapterId      the ID of the chapter this entry belongs to
     * @param text           The chapter start text
     * @param teleportPacket The teleport packet associated with the chapter start
     * @param color          The colour associated with the chapter start
     */
    public static void addChapterStart(String pathId, String chapterId, String text, PlayerTeleportPacket teleportPacket, int color) {
        if (LOG.add(new Entry(pathId, chapterId, text, teleportPacket, EntryType.CHAPTER_START, color))) {
            trimToMaxSize();
        }
    }

    /**
     * Retrieves an unmodifiable set of journal entries.
     *
     * @return An unmodifiable set of journal entries
     */
    public static Set<Entry> getEntries() {
        return Collections.unmodifiableSet(LOG);
    }

    /**
     * Enum representing the type of journal entry.
     */
    public enum EntryType {
        /** Entry type for proximity messages */
        PROXIMITY_MESSAGE,
        /** Entry type for chapter starts */
        CHAPTER_START,
    }

    /**
     * Record representing a journal entry.
     *
     * @param text           The entry text
     * @param teleportPacket The teleport packet associated with the entry
     * @param type           The type of the entry
     * @param color          The colour associated with the entry
     */
    public record Entry(String pathId, String chapterId, String text, PlayerTeleportPacket teleportPacket,
                        EntryType type, int color) {

        /**
         * Constructs an Entry with the default colour (light grey).
         *
         * @param pathId         the ID of the path
         * @param chapterId      the ID of the chapter
         * @param text           the entry text
         * @param teleportPacket the teleport packet associated with the entry
         * @param type           the type of the entry
         */
        public Entry(String pathId, String chapterId, String text, PlayerTeleportPacket teleportPacket, EntryType type) {
            this(pathId, chapterId, text, teleportPacket, type, 0xFFDDDDDD);
        }

        /**
         * Overrides equals method for proper comparison of Entry objects.
         * Do not include teleportPacket in equality check as minor difference in player position can occur.
         *
         * @param obj The object to compare with
         * @return true if the objects are equal, false otherwise
         */
        @Override
        public boolean equals(Object obj) {

            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;

            Entry entry = (Entry) obj;

            return Objects.equals(pathId, entry.pathId) &&
                    Objects.equals(chapterId, entry.chapterId) &&
                    Objects.equals(text, entry.text) &&
                    type == entry.type;
        }

        /**
         * Overrides hashCode method for proper hashing of Entry objects.
         * Do not include teleportPacket in hash code calculation as minor difference in player position can occur.
         *
         * @return The hash code of the Entry object
         */
        @Override
        public int hashCode() {
            return Objects.hash(text, type);
        }
    }
}

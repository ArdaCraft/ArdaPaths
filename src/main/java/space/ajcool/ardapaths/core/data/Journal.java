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
     * @param text            The proximity message text
     * @param teleportPacket  The teleport packet associated with the message
     */
    public static void addProximityMessage(String text, PlayerTeleportPacket teleportPacket) {
        rotateJournal();
        LOG.add(new Entry(text, teleportPacket,EntryType.PROXIMITY_MESSAGE));
    }

    /**
     * Adds a chapter start entry to the journal.
     *
     * @param text            The chapter start text
     * @param teleportPacket  The teleport packet associated with the chapter start
     * @param color           The color associated with the chapter start
     */
    public static void addChapterStart(String text, PlayerTeleportPacket teleportPacket, int color) {
        rotateJournal();
        LOG.add(new Entry(text, teleportPacket, EntryType.CHAPTER_START, color));
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
     * Ensures the journal does not exceed its maximum size by removing the oldest entry if necessary.
     */
    private static void rotateJournal(){

        // Remove eldest entry if we exceed max size
        if (LOG.size() + 1 > MAX_SIZE) {

            Iterator<Entry> it = LOG.iterator();
            if (it.hasNext()) {
                it.next();
                it.remove();
            }
        }
    }

    /**
     * Record representing a journal entry.
     *
     * @param text            The entry text
     * @param teleportPacket  The teleport packet associated with the entry
     * @param type            The type of the entry
     * @param color           The color associated with the entry
     */
    public record Entry(String text, PlayerTeleportPacket teleportPacket, EntryType type, int color) {

        public Entry(String text, PlayerTeleportPacket teleportPacket, EntryType type) {
            this(text, teleportPacket, type, 0xDDDDDD);
        }

        /**
         * Overrides equals method for proper comparison of Entry objects.
         * Do not include teleportPacket in equality check as minor difference in player position can occur.
         * @param obj The object to compare with
         * @return true if the objects are equal, false otherwise
         */
        @Override
        public boolean equals(Object obj) {

            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;

            Entry entry = (Entry) obj;

            return Objects.equals(text, entry.text) && type == entry.type;
        }

        /**
         * Overrides hashCode method for proper hashing of Entry objects.
         * Do not include teleportPacket in hash code calculation as minor difference in player position can occur.
         * @return The hash code of the Entry object
         */
        @Override
        public int hashCode() {
            return Objects.hash(text, type);
        }
    }

    /**
     * Enum representing the type of journal entry.
     */
    public enum EntryType{
        PROXIMITY_MESSAGE,
        CHAPTER_START,
    }
}
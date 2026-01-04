package space.ajcool.ardapaths.core.data;

import space.ajcool.ardapaths.core.networking.packets.server.PlayerTeleportPacket;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

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
     */
    public static void addChapterStart(String text, PlayerTeleportPacket teleportPacket) {
        rotateJournal();
        LOG.add(new Entry(text, teleportPacket, EntryType.CHAPTER_START));
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

        // Remove oldest entry if we exceed max size
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
     */
    public record Entry(String text, PlayerTeleportPacket teleportPacket, EntryType type) {}

    /**
     * Enum representing the type of journal entry.
     */
    public enum EntryType{
        PROXIMITY_MESSAGE,
        CHAPTER_START,
    }
}
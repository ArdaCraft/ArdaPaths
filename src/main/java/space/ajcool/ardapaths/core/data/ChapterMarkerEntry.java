package space.ajcool.ardapaths.core.data;

import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;

/**
 * Wire representation of one row in a chapter marker list.
 *
 * @param packedPos        packed marker block position
 * @param timeOfDay        configured marker time of day
 * @param weather          configured marker weather type
 * @param proximityMessage truncated proximity tooltip text
 * @param hasMiscData      whether the marker has action data
 * @param chapterStart     whether the marker is a chapter-start marker
 * @param chainBreak       whether this entry is a visual separator, not a marker
 */
public record ChapterMarkerEntry(long packedPos, int timeOfDay, int weather, String proximityMessage, boolean hasMiscData, boolean chapterStart, boolean chainBreak) {
    /**
     * Maximum proximity tooltip payload length.
     */
    public static final int MAX_PROXIMITY_MESSAGE_LENGTH = 256;

    /**
     * Creates a marker row from resolved chapter NBT.
     *
     * @param packedPos packed marker block position
     * @param data      marker chapter data
     * @return row for the marker
     */
    public static ChapterMarkerEntry marker(long packedPos, PathMarkerBlockEntity.ChapterNbtData data) {
        return new ChapterMarkerEntry(
                packedPos,
                data.getTimeOfDay(),
                data.getWeather(),
                truncate(data.getProximityMessage()),
                data.hasMiscData(),
                data.isChapterStart(),
                false
        );
    }

    /**
     * Creates a visual separator row between disconnected chains.
     *
     * @return chain-break row
     */
    public static ChapterMarkerEntry breakEntry() {
        return new ChapterMarkerEntry(0L, TimeOfDay.UNSET, WeatherTypes.DEFAULT.ordinal(), "", false, false, true);
    }

    /**
     * Truncates proximity text for bounded tooltip transport.
     *
     * @param message proximity text from marker NBT
     * @return bounded non-null proximity text
     */
    private static String truncate(String message) {
        if (message == null) return "";
        if (message.length() <= MAX_PROXIMITY_MESSAGE_LENGTH) return message;
        return message.substring(0, MAX_PROXIMITY_MESSAGE_LENGTH);
    }
}

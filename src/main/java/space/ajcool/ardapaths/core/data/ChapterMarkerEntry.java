package space.ajcool.ardapaths.core.data;

import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;

/**
 * Wire representation of one row in a chapter marker list.
 *
 * @param packedPos        packed marker block position
 * @param dimensionId      dimension identifier containing the marker, or the destination dimension for dimension breaks
 * @param timeOfDay        configured marker time of day
 * @param weather          configured marker weather type
 * @param proximityMessage truncated proximity tooltip text
 * @param hasMiscData      whether the marker has action data
 * @param chapterStart     whether the marker is a chapter-start marker
 * @param chainBreak       whether this entry is a visual separator, not a marker
 * @param dimensionBreak   whether this entry is a cross-dimension separator, not a marker
 */
public record ChapterMarkerEntry(long packedPos, String dimensionId, long timeOfDay, int weather, String proximityMessage, boolean hasMiscData, boolean chapterStart, boolean chainBreak, boolean dimensionBreak) {
    /**
     * Maximum proximity tooltip payload length.
     */
    public static final int MAX_PROXIMITY_MESSAGE_LENGTH = 256;

    /**
     * Creates a marker row from resolved chapter NBT.
     *
     * @param packedPos packed marker block position
     * @param dimensionId dimension identifier containing the marker
     * @param data        marker chapter data
     * @return row for the marker
     */
    public static ChapterMarkerEntry marker(long packedPos, String dimensionId, PathMarkerBlockEntity.ChapterNbtData data) {
        return new ChapterMarkerEntry(
                packedPos,
                dimensionId,
                data.getTimeOfDay(),
                data.getWeather(),
                truncate(data.getProximityMessage()),
                data.hasMiscData(),
                data.isChapterStart(),
                false,
                false
        );
    }

    /**
     * Creates a visual separator row between disconnected chains.
     *
     * @return chain-break row
     */
    public static ChapterMarkerEntry breakEntry() {
        return new ChapterMarkerEntry(0L, "", TimeOfDay.UNSET, WeatherTypes.DEFAULT.ordinal(), "", false, false, true, false);
    }

    /**
     * Creates a visual separator where a chapter chain continues in another dimension.
     *
     * @param dimensionId destination dimension identifier
     * @return dimension-break row
     */
    public static ChapterMarkerEntry dimensionBreakEntry(String dimensionId) {
        return new ChapterMarkerEntry(0L, dimensionId == null ? "" : dimensionId, TimeOfDay.UNSET, WeatherTypes.DEFAULT.ordinal(), "", false, false, true, true);
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

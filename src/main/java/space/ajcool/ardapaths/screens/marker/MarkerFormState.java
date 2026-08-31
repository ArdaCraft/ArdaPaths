package space.ajcool.ardapaths.screens.marker;

import lombok.Getter;
import lombok.Setter;
import space.ajcool.ardapaths.core.data.BitPacker;
import space.ajcool.ardapaths.core.data.TimeOfDay;
import space.ajcool.ardapaths.core.data.WarpTarget;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;

import java.util.Objects;

/**
 * Mutable form values edited by the marker editor.
 */
@Getter
@Setter
public class MarkerFormState {
    /** Whether this marker marks the start of a chapter. */
    private boolean chapterStart;

    /** Whether to display the chapter title on the trail when this is a chapter start. */
    private boolean showChapterStartTitle;

    /** Text displayed to the player when they trigger this marker's proximity zone. */
    private String proximityMessage;

    /** Distance in blocks from the marker that triggers the proximity message. */
    private int activationRange;

    /** Whether the proximity message and marker should render above block surfaces. */
    private boolean displayAboveBlocks;

    /** Speed at which characters are revealed in the proximity message. */
    private int charRevealSpeed;

    /** Offset applied to the fade delay calculation. */
    private int fadeDelayOffset;

    /** Factor applied to the fade delay calculation. */
    private int fadeDelayFactor;

    /** Speed at which text fades out. */
    private int fadeSpeed;

    /** Minimum opacity of the text when fully faded. */
    private int minOpacity;

    /** Weather ordinal selected for this marker, or unset when the player should keep current weather. */
    private int weather;

    /** Time-of-day ticks selected for this marker, or unset when the player should keep current time. */
    private int timeOfDay;

    /** Distance in blocks selected for transitioning to the marker's time of day. */
    private int timeTransitionRange;

    /** Target coordinates or warp name triggered when a player reaches this marker. */
    private String autoTeleportTarget;

    /** Target coordinates focused by the client while the Focus key is held. */
    private String lookAt;

    /** Item identifier granted when a player reaches this marker. */
    private String giveItem;

    /**
     * Populates this form state from persisted marker chapter data.
     *
     * @param data marker chapter data to load
     */
    public void loadFrom(PathMarkerBlockEntity.ChapterNbtData data) {
        chapterStart = data.isChapterStart();
        showChapterStartTitle = data.isDisplayChapterTitleOnTrail();
        proximityMessage = data.getProximityMessage();
        activationRange = data.getActivationRange();
        displayAboveBlocks = data.isDisplayAboveBlocks();

        int[] unpackedMessageData = BitPacker.unpackFive(data.getPackedMessageData());

        charRevealSpeed = unpackedMessageData[0];
        fadeDelayOffset = unpackedMessageData[1];
        fadeDelayFactor = unpackedMessageData[2];
        fadeSpeed = unpackedMessageData[3];
        minOpacity = unpackedMessageData[4];
        weather = data.getWeather();
        timeOfDay = data.getTimeOfDay();
        timeTransitionRange = data.getTimeTransitionRange();
        autoTeleportTarget = data.getAutoTeleportTarget();
        lookAt = WarpTarget.formatCoordinates(data.getLookAt());
        giveItem = data.getGiveItem();
    }

    /**
     * Applies this form state to persisted marker chapter data.
     *
     * @param data marker chapter data to update
     */
    public void applyTo(PathMarkerBlockEntity.ChapterNbtData data) {
        data.setProximityMessage(proximityMessage);
        data.setActivationRange(activationRange);
        data.setChapterStart(chapterStart);
        data.setDisplayChapterTitleOnTrail(chapterStart && showChapterStartTitle);
        data.setDisplayAboveBlocks(displayAboveBlocks);
        data.setWeather(weather);
        data.setTimeOfDay(timeOfDay);
        data.setTimeTransitionRange(timeTransitionRange);
        data.setAutoTeleportTarget(autoTeleportTarget);
        data.setLookAt(WarpTarget.parseCoordinates(lookAt));
        data.setGiveItem(giveItem);
        data.setPackedMessageData(BitPacker.packFive(charRevealSpeed, fadeDelayOffset, fadeDelayFactor, fadeSpeed, minOpacity));
    }

    /**
     * Calculates a normalized hash of the current form values.
     *
     * @return hash of the editable form values
     */
    public int hash() {
        return Objects.hash(
                proximityMessage,
                activationRange,
                displayAboveBlocks,
                chapterStart,
                showChapterStartTitle,
                charRevealSpeed,
                fadeDelayOffset,
                fadeDelayFactor,
                fadeSpeed,
                minOpacity,
                weather,
                TimeOfDay.snap(timeOfDay),
                timeTransitionRange,
                autoTeleportTarget,
                lookAt,
                giveItem
        );
    }

    /**
     * Creates a detached copy of this form state.
     *
     * @return copied form state
     */
    public MarkerFormState copy() {
        MarkerFormState copy = new MarkerFormState();
        copy.chapterStart = chapterStart;
        copy.showChapterStartTitle = showChapterStartTitle;
        copy.proximityMessage = proximityMessage;
        copy.activationRange = activationRange;
        copy.displayAboveBlocks = displayAboveBlocks;
        copy.charRevealSpeed = charRevealSpeed;
        copy.fadeDelayOffset = fadeDelayOffset;
        copy.fadeDelayFactor = fadeDelayFactor;
        copy.fadeSpeed = fadeSpeed;
        copy.minOpacity = minOpacity;
        copy.weather = weather;
        copy.timeOfDay = timeOfDay;
        copy.timeTransitionRange = timeTransitionRange;
        copy.autoTeleportTarget = autoTeleportTarget;
        copy.lookAt = lookAt;
        copy.giveItem = giveItem;
        return copy;
    }
}

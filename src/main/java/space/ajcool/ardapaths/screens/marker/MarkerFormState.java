package space.ajcool.ardapaths.screens.marker;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import space.ajcool.ardapaths.core.data.BitPacker;
import space.ajcool.ardapaths.core.data.TimeActivation;
import space.ajcool.ardapaths.core.data.TimeOfDay;
import space.ajcool.ardapaths.core.data.WarpTarget;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;

import java.util.Objects;

/**
 * Mutable form values edited by the marker editor.
 */
@Getter
@Setter
// Accessed via Lombok-generated accessor; IntelliJ entry-point analysis can't follow it.
@SuppressWarnings("unused")
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

    /** Absolute date-time ticks selected for this marker, or unset when the player should keep current time. */
    private long timeOfDay;

    /** Activation mode selected for transitioning to the marker's time of day. */
    private TimeActivation timeActivation;

    /** Target coordinates or warp name triggered when a player reaches this marker. */
    private String autoTeleportTarget;

    /** Target coordinates focused by the client while the Focus key is held. */
    private String lookAt;

    /** Dimension identifier for the marker that continues this chapter chain. */
    private String targetMarkerDimension;

    /** X coordinate of the marker that continues this chapter chain. */
    private String targetMarkerX;

    /** Y coordinate of the marker that continues this chapter chain. */
    private String targetMarkerY;

    /** Z coordinate of the marker that continues this chapter chain. */
    private String targetMarkerZ;

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
        timeActivation = data.getTimeActivation();
        autoTeleportTarget = data.getAutoTeleportTarget();
        lookAt = WarpTarget.formatCoordinates(data.getLookAt());
        targetMarkerDimension = data.getTargetMarkerDimension();
        targetMarkerX = data.getTargetMarker() == null ? "" : String.valueOf(data.getTargetMarker().getX());
        targetMarkerY = data.getTargetMarker() == null ? "" : String.valueOf(data.getTargetMarker().getY());
        targetMarkerZ = data.getTargetMarker() == null ? "" : String.valueOf(data.getTargetMarker().getZ());
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
        data.setTimeActivation(timeActivation);
        data.setAutoTeleportTarget(autoTeleportTarget);
        data.setLookAt(WarpTarget.parseCoordinates(lookAt));
        if (hasCompleteTargetMarker()) {
            data.setTargetMarkerDimension(targetMarkerDimension.trim());
            data.setTargetMarker(new BlockPos(Integer.parseInt(targetMarkerX.trim()), Integer.parseInt(targetMarkerY.trim()), Integer.parseInt(targetMarkerZ.trim())));
        } else {
            data.setTargetMarkerDimension("");
            data.setTargetMarker(null);
        }
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
                timeActivation,
                autoTeleportTarget,
                lookAt,
                targetMarkerDimension,
                targetMarkerX,
                targetMarkerY,
                targetMarkerZ,
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
        copy.timeActivation = timeActivation;
        copy.autoTeleportTarget = autoTeleportTarget;
        copy.lookAt = lookAt;
        copy.targetMarkerDimension = targetMarkerDimension;
        copy.targetMarkerX = targetMarkerX;
        copy.targetMarkerY = targetMarkerY;
        copy.targetMarkerZ = targetMarkerZ;
        copy.giveItem = giveItem;
        return copy;
    }

    /**
     * Checks whether all target-marker fields are populated.
     *
     * @return true when the target-marker dimension and coordinates are all present
     */
    private boolean hasCompleteTargetMarker() {
        return targetMarkerDimension != null && !targetMarkerDimension.trim().isEmpty()
                && targetMarkerX != null && !targetMarkerX.trim().isEmpty()
                && targetMarkerY != null && !targetMarkerY.trim().isEmpty()
                && targetMarkerZ != null && !targetMarkerZ.trim().isEmpty();
    }
}

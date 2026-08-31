package space.ajcool.ardapaths.core.data;

import org.junit.jupiter.api.Test;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * Tests for packed marker animation settings.
 */
class BitPackerTest {
    /**
     * Verifies ordinary packed animation values unpack in the original order.
     */
    @Test
    void packFiveRoundTripsValues() {
        int[] values = {5, 100, 5, 2, 8};

        assertArrayEquals(values, BitPacker.unpackFive(BitPacker.packFive(values[0], values[1], values[2], values[3], values[4])));
    }

    /**
     * Verifies field-width boundary values survive packing.
     */
    @Test
    void packFiveRoundTripsBoundaries() {
        int[] values = {BitPacker.MAX_8_BIT_VALUE, BitPacker.MAX_14_BIT_VALUE, 0, BitPacker.MAX_14_BIT_VALUE, 1};

        assertArrayEquals(values, BitPacker.unpackFive(BitPacker.packFive(values[0], values[1], values[2], values[3], values[4])));
    }

    /**
     * Pins the shared default animation constant to its decoded values.
     */
    @Test
    void defaultPackedMessageDataUnpacksToExpectedValues() {
        assertArrayEquals(new int[]{5, 100, 5, 2, 8}, BitPacker.unpackFive(PathMarkerBlockEntity.ChapterNbtData.DEFAULT_PACKED_MESSAGE_DATA));
    }
}

package space.ajcool.ardapaths.core.data;

import lombok.experimental.UtilityClass;

/**
 * Utility class for packing and unpacking multiple integers into a single long value.
 * Uses bit shifting to store multiple values in a space-efficient manner.
 */
@UtilityClass
public class BitPacker {

    /**
     * Bit width used for packing most values.
     */
    private static final int BIT_WIDTH = 14;

    /**
     * Bitmask for 14-bit values.
     */
    private static final long MASK = (1L << BIT_WIDTH) - 1; // 0b11111111111111

    /**
     * Maximum value that can be stored in each 14-bit field.
     */
    public static final int MAX_14_BIT_VALUE = (int) MASK;

    /**
     * Bitmask for 8-bit values.
     */
    private static final long MASK_8 = (1L << 8) - 1;

    /**
     * Maximum value that can be stored in the first 8-bit field.
     */
    public static final int MAX_8_BIT_VALUE = (int) MASK_8;

    /**
     * Packs five integers into a single long value.
     * The first value uses 8 bits, the remaining four use 14 bits each.
     *
     * @param a the first integer (8 bits)
     * @param b the second integer (14 bits)
     * @param c the third integer (14 bits)
     * @param d the fourth integer (14 bits)
     * @param e the fifth integer (14 bits)
     * @return the packed long value
     */
    public static long packFive(int a, int b, int c, int d, int e) {
        return ((long) a & MASK_8) << 56
                | ((long) b & MASK) << 42
                | ((long) c & MASK) << 28
                | ((long) d & MASK) << 14
                | ((long) e & MASK);
    }

    /**
     * Unpacks five integers from a long value previously packed with {@link #packFive(int, int, int, int, int)}.
     *
     * @param packed the packed long value
     * @return an array of 5 integers in the same order they were packed
     */
    public static int[] unpackFive(long packed) {
        int[] result = new int[5];

        result[0] = (int) ((packed >> 56) & MASK_8);
        result[1] = (int) ((packed >> 42) & MASK);
        result[2] = (int) ((packed >> 28) & MASK);
        result[3] = (int) ((packed >> 14) & MASK);
        result[4] = (int) (packed & MASK);

        return result;
    }
}

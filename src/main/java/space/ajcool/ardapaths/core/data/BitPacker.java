package space.ajcool.ardapaths.core.data;

public class BitPacker
{
    private static final int BIT_WIDTH = 14;
    private static final long MASK = (1L << BIT_WIDTH) - 1; // 0b11111111111111
    private static final long MASK_8 = (1L << 8) - 1;

    public static long packFive(int a, int b, int c, int d, int e)
    {
        return ((long) a << 56) | ((long) b << 42) | ((long) c << 28) | ((long) d << 14) | (long) e;
    }

    public static int[] unpackFive(long packed)
    {
        int[] result = new int[5];

        result[0] = (int) ((packed >> 56) & MASK_8);
        result[1] = (int) ((packed >> 42) & MASK);
        result[2] = (int) ((packed >> 28) & MASK);
        result[3] = (int) ((packed >> 14) & MASK);
        result[4] = (int) (packed & MASK);

        return result;
    }
}

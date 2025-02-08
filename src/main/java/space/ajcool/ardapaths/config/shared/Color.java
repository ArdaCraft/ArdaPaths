package space.ajcool.ardapaths.config.shared;

import com.google.gson.annotations.SerializedName;

public class Color {
    @SerializedName("red")
    public int r;
    @SerializedName("green")
    public int g;
    @SerializedName("blue")
    public int b;

    public Color(int r, int g, int b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }

    /**
     * Create a color from RGB values.
     *
     * @param r Red
     * @param g Green
     * @param b Blue
     */
    public static Color fromRgb(int r, int g, int b) {
        return new Color(r, g, b);
    }

    /**
     * Create a color from a hex value.
     *
     * @param hex The hex value
     */
    public static Color fromHex(int hex) {
        return new Color((hex >> 16) & 0xFF, (hex >> 8) & 0xFF, hex & 0xFF);
    }

    /**
     * Convert the color to a hex value.
     */
    public int asHex() {
        return (r << 16) | (g << 8) | b;
    }
}

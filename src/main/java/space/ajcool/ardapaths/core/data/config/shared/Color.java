package space.ajcool.ardapaths.core.data.config.shared;

import com.google.gson.annotations.SerializedName;

import java.util.regex.Pattern;

public class Color
{
    @SerializedName("red")
    public int r;

    @SerializedName("green")
    public int g;

    @SerializedName("blue")
    public int b;

    public Color(int r, int g, int b)
    {
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
    public static Color fromRgb(int r, int g, int b)
    {
        return new Color(r, g, b);
    }

    /**
     * Create a color from a hex value.
     *
     * @param hex The hex value
     */
    public static Color fromHex(int hex)
    {
        return new Color((hex >> 16) & 0xFF, (hex >> 8) & 0xFF, hex & 0xFF);
    }

    /**
     * Convert the color to a hex value.
     */
    public int asHex()
    {
        return (r << 16) | (g << 8) | b;
    }

    /**
     * Convert the color to a hex string value.
     */
    public String asHexString() {
        return String.format("#%02X%02X%02X", r, g, b);
    }

    /**
     * Create a color from a hex string.
     * @param hex The hex string
     */
    public static Color fromHexString(String hex) {

        if (hex != null && !hex.isBlank() && hex.matches("^#([a-fA-F0-9]{6})$")) {

            // Remove the leading '#'
            int rgb = Integer.parseInt(hex.substring(1), 16);

            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int b = rgb & 0xFF;

            return new Color(r, g, b);
        }

        return new Color(255, 255, 255);
    }
}

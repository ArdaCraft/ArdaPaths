package space.ajcool.ardapaths.core.data.config.shared;

import com.google.gson.annotations.SerializedName;

/**
 * Represents an RGB colour with conversion methods for hex values and hex strings.
 * This class is used for serializing colours to JSON with expanded field names.
 */
public class Color {

    /**
     * Red component of the colour (0-255).
     */
    @SerializedName("red")
    public int r;

    /**
     * Green component of the colour (0-255).
     */
    @SerializedName("green")
    public int g;

    /**
     * Blue component of the colour (0-255).
     */
    @SerializedName("blue")
    public int b;

    /**
     * Constructs a Colour with the specified RGB components.
     *
     * @param r the red component (0-255)
     * @param g the green component (0-255)
     * @param b the blue component (0-255)
     */
    public Color(int r, int g, int b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }

    /**
     * Create a colour from RGB values.
     *
     * @param r Red
     * @param g Green
     * @param b Blue
     * @return a new Color instance
     */
    public static Color fromRgb(int r, int g, int b) {
        return new Color(r, g, b);
    }

    /**
     * Create a colour from a hex value.
     *
     * @param hex The hex value
     * @return a new Color instance
     */
    public static Color fromHex(int hex) {
        return new Color((hex >> 16) & 0xFF, (hex >> 8) & 0xFF, hex & 0xFF);
    }

    /**
     * Create a colour from a hex string.
     *
     * @param hex The hex string
     * @return a new Color instance
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

    /**
     * Convert the colour to a hex value.
     *
     * @return the opaque ARGB hex representation as an integer
     */
    public int asHex() {
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /**
     * Convert the colour to a hex string value.
     *
     * @return the hex representation as a string (e.g., "#RRGGBB")
     */
    public String asHexString() {
        return String.format("#%02X%02X%02X", r, g, b);
    }
}

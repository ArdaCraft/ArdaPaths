package space.ajcool.ardapaths;

import com.google.gson.GsonBuilder;
import dev.isxander.yacl3.config.ConfigEntry;
import dev.isxander.yacl3.config.ConfigInstance;
import dev.isxander.yacl3.config.GsonConfigInstance;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("deprecation")
public class ArdaPathsConfig {
    public static final ConfigInstance<ArdaPathsConfig> INSTANCE = new GsonConfigInstance<>(ArdaPathsConfig.class, Path.of("./config/ardapaths.json"), new GsonBuilder().setPrettyPrinting().create());

    @ConfigEntry
    public boolean markerText = true;

    @ConfigEntry
    public List<PathSettings> paths = List.of(
            new PathSettings(
                    0,
                    "Frodo's Path",
                    new ColorRGB(255, 215, 0), new ColorRGB(230, 194, 0), new ColorRGB(255, 227, 77)
            ),
            new PathSettings(
                    1,
                    "Aragorn's Path",
                    new ColorRGB(0, 158, 96), new ColorRGB(0, 126, 77), new ColorRGB(77, 187, 144)
            ),
            new PathSettings(
                    2,
                    "Bilbo's Path",
                    new ColorRGB(125, 249, 255), new ColorRGB(88, 174, 179), new ColorRGB(164, 251, 255)
            ),
            new PathSettings(
                    3,
                    "Merry's Path",
                    new ColorRGB(227, 66, 52), new ColorRGB(159, 46, 36), new ColorRGB(235, 123, 113)
            )
    );

    public static class ColorRGB {
        @ConfigEntry
        public int Red;

        @ConfigEntry
        public int Green;

        @ConfigEntry
        public int Blue;

        public ColorRGB(int red, int green, int blue)
        {
            Red = red;
            Green = green;
            Blue = blue;
        }
        public int encodedColor()
        {
            return (((Red & 0x0ff) << 16) | ((Green & 0x0ff) << 8) | (Blue & 0x0ff));
        }
    }

    public static class PathSettings {
        @ConfigEntry
        public int Id;

        @ConfigEntry
        public String Name;

        @ConfigEntry
        public ColorRGB PrimaryColor;

        @ConfigEntry
        public ColorRGB SecondaryColor;

        @ConfigEntry
        public ColorRGB TertiaryColor;

        public PathSettings(int id, String name, ColorRGB primaryColor, ColorRGB secondaryColor, ColorRGB tertiaryColor)
        {
            Id = id;
            Name = name;
            PrimaryColor = primaryColor;
            SecondaryColor = secondaryColor;
            TertiaryColor = tertiaryColor;
        }
    }
}

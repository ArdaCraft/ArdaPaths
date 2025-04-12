package space.ajcool.ardapaths.core.data.config;

import space.ajcool.ardapaths.core.data.config.server.ServerConfig;
import space.ajcool.ardapaths.core.data.config.shared.ChapterData;
import space.ajcool.ardapaths.core.data.config.shared.Color;
import space.ajcool.ardapaths.core.data.config.shared.PathData;

public class ServerConfigManager extends ConfigManager<ServerConfig>
{
    public ServerConfigManager(String configPath)
    {
        super(configPath);
    }

    @Override
    protected ServerConfig createDefault()
    {
        ServerConfig config = new ServerConfig();
        config.addPath(new PathData()
                .setId("frodo")
                .setName("Frodo's Path")
                .setPrimaryColor(new Color(255, 215, 0))
                .setSecondaryColor(new Color(230, 194, 0))
                .setTertiaryColor(new Color(255, 227, 77))
                .setChapter(new ChapterData("default", "Default", "0", 0))
        );
        config.addPath(new PathData()
                .setId("aragorn")
                .setName("Aragorn's Path")
                .setPrimaryColor(new Color(0, 158, 96))
                .setSecondaryColor(new Color(0, 126, 77))
                .setTertiaryColor(new Color(77, 187, 144))
                .setChapter(new ChapterData("default", "Default", "0", 0))
        );
        config.addPath(new PathData()
                .setId("bilbo")
                .setName("Bilbo's Path")
                .setPrimaryColor(new Color(125, 249, 255))
                .setSecondaryColor(new Color(88, 174, 179))
                .setTertiaryColor(new Color(164, 251, 255))
                .setChapter(new ChapterData("default", "Default", "0", 0))
        );
        config.addPath(new PathData()
                .setId("merry")
                .setName("Merry's Path")
                .setPrimaryColor(new Color(227, 66, 52))
                .setSecondaryColor(new Color(159, 46, 36))
                .setTertiaryColor(new Color(235, 123, 113))
                .setChapter(new ChapterData("default", "Default", "0", 0))
        );
        config.addPath(new PathData()
                .setId("info")
                .setName("Project Info")
                .setPrimaryColor(new Color(255, 255, 255))
                .setSecondaryColor(new Color(255, 255, 255))
                .setTertiaryColor(new Color(255, 255, 255))
                .setChapter(new ChapterData("info", "Info", "0", 0))
        );
        return config;
    }
}

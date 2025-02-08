package space.ajcool.ardapaths.config;

import space.ajcool.ardapaths.config.shared.Color;
import space.ajcool.ardapaths.config.shared.PathSettings;
import space.ajcool.ardapaths.config.server.ServerConfig;

import java.util.ArrayList;

public class ServerConfigManager extends ConfigManager<ServerConfig> {
    private static ServerConfigManager INSTANCE;

    public ServerConfigManager(String configPath) {
        super(configPath);
    }

    /**
     * @return The instance of the config manager.
     */
    public static ServerConfigManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ServerConfigManager("./config/arda-paths.json");
        }
        return INSTANCE;
    }

    @Override
    protected ServerConfig createDefault() {
        ServerConfig config = new ServerConfig();
        config.paths = new ArrayList<>();
        config.paths.add(new PathSettings(
                0,
                "Frodo's Path",
                null,
                new Color(255, 215, 0),
                new Color(230, 194, 0),
                new Color(255, 227, 77)
        ));
        config.paths.add(new PathSettings(
                1,
                "Aragorn's Path",
                null,
                new Color(0, 158, 96),
                new Color(0, 126, 77),
                new Color(77, 187, 144)
        ));
        config.paths.add(new PathSettings(
                2,
                "Bilbo's Path",
                null,
                new Color(125, 249, 255),
                new Color(88, 174, 179),
                new Color(164, 251, 255)
        ));
        config.paths.add(new PathSettings(
                3,
                "Merry's Path",
                null,
                new Color(227, 66, 52),
                new Color(159, 46, 36),
                new Color(235, 123, 113)
        ));
        return config;
    }
}

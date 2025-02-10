package space.ajcool.ardapaths.core.data.config;

import space.ajcool.ardapaths.core.data.config.shared.Color;
import space.ajcool.ardapaths.core.data.config.shared.PathData;
import space.ajcool.ardapaths.core.data.config.server.ServerConfig;

public class ServerConfigManager extends ConfigManager<ServerConfig> {
    public ServerConfigManager(String configPath) {
        super(configPath);
    }

    @Override
    protected ServerConfig createDefault() {
        ServerConfig config = new ServerConfig();
        config.addPath(new PathData()
                .setId("frodo")
                .setName("Frodo's Path")
                .setColor(new Color(255, 215, 0))
        );
        config.addPath(new PathData()
                .setId("aragorn")
                .setName("Aragorn's Path")
                .setColor(new Color(0, 158, 96))
        );
        config.addPath(new PathData()
                .setId("bilbo")
                .setName("Bilbo's Path")
                .setColor(new Color(125, 249, 255))
        );
        config.addPath(new PathData()
                .setId("merry")
                .setName("Merry's Path")
                .setColor(new Color(227, 66, 52))
        );
        return config;
    }
}

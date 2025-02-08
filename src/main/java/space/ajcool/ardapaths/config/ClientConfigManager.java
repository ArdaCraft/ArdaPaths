package space.ajcool.ardapaths.config;

import space.ajcool.ardapaths.config.client.ClientConfig;

public class ClientConfigManager extends ConfigManager<ClientConfig> {
    private static ClientConfigManager INSTANCE;

    public ClientConfigManager(String configPath) {
        super(configPath);
    }

    /**
     * @return The instance of the config manager.
     */
    public static ClientConfigManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ClientConfigManager("./config/arda-paths-client.json");
        }
        return INSTANCE;
    }

    @Override
    protected ClientConfig createDefault() {
        ClientConfig config = new ClientConfig();
        config.proximityMessages = true;
        config.selectedPath = -1;
        config.currentChapter = null;
        return config;
    }
}

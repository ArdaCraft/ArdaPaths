package space.ajcool.ardapaths.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public abstract class ConfigManager<T> {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path file;
    protected T config;

    public ConfigManager(String configPath) {
        this.file = Path.of(configPath);
        this.load();
    }

    /**
     * Load the config from file.
     */
    @SuppressWarnings("unchecked")
    public void load() {
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file)) {
                T defaultConfig = createDefault();
                T loadedConfig = GSON.fromJson(reader, (Class<T>) defaultConfig.getClass());
                config = Objects.requireNonNullElse(loadedConfig, defaultConfig);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            config = createDefault();
        }
        save();
    }

    /**
     * Save the config to file.
     */
    public void save() {
        try {
            if (!Files.exists(file.getParent())) {
                Files.createDirectories(file.getParent());
            }
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates the default configuration object.
     */
    protected abstract T createDefault();

    /**
     * @return The configuration object
     */
    public T getConfig() {
        return config;
    }
}

package space.ajcool.ardapaths.core.data.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Abstract base class for managing JSON-based configuration files.
 * Handles automatic loading and saving of configuration objects.
 *
 * @param <T> the type of configuration object to manage
 */
public abstract class ConfigManager<T> {
    /**
     * Gson instance configured to output pretty-printed JSON.
     */
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Path to the configuration file on disk.
     */
    private final Path file;

    /**
     * The current configuration object in memory.
     */
    @Getter
    protected T config;

    /**
     * Constructs a ConfigManager and loads the configuration from disk.
     * If the file doesn't exist, creates a default configuration.
     *
     * @param configPath the file path to the configuration file
     */
    public ConfigManager(String configPath) {
        this.file = Path.of(configPath);
        this.load();
    }

    /**
     * Load the config from file.
     */
    @SuppressWarnings({"unchecked", "CallToPrintStackTrace"})
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
     * Creates the default configuration object.
     */
    protected abstract T createDefault();

    /**
     * Save the config to file.
     */
    @SuppressWarnings("CallToPrintStackTrace")
    public void save() {
        new Thread(() ->
        {
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
        }).start();
    }

}

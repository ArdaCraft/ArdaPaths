package space.ajcool.ardapaths.core.data.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.concurrent.*;

/**
 * Abstract base class for managing JSON-based configuration files.
 * Handles automatic loading and saving of configuration objects.
 *
 * @param <T> the type of configuration object to manage
 */
@Slf4j(topic = "ardapaths")
public abstract class ConfigManager<T> {
    /**
     * Gson instance configured to output pretty-printed JSON.
     */
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Delay used to coalesce bursts of config saves into one disk write.
     */
    private static final long SAVE_DEBOUNCE_MILLIS = 1000L;

    /**
     * Shared daemon writer that serializes config file writes across all config managers.
     */
    private static final ScheduledExecutorService SAVE_EXECUTOR = Executors.newSingleThreadScheduledExecutor(new ConfigThreadFactory());

    /**
     * Path to the configuration file on disk.
     */
    private final Path file;

    /**
     * Coordinates pending config snapshots and scheduled writes for this file.
     */
    private final Object saveLock = new Object();

    /**
     * Serializes immediate flushes with background writes for this file.
     */
    private final Object writeLock = new Object();

    /**
     * Most recent JSON snapshot waiting to be written to disk.
     */
    private String pendingJson;

    /**
     * Scheduled save task for the current debounce window, or null when none is pending.
     */
    private ScheduledFuture<?> pendingSave;

    /**
     * JVM shutdown hook that forces the newest config snapshot to disk.
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final Thread shutdownHook;

    /**
     * The current configuration object in memory.
     */
    @Setter
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
        this.shutdownHook = new Thread(this::flush, "ardapaths-config-flush-" + file.getFileName());
        Runtime.getRuntime().addShutdownHook(shutdownHook);
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
            } catch (IOException | JsonSyntaxException e) {
                log.error("Failed to load config from {}; falling back to defaults", file, e);
                config = createDefault();
            }
        } else {
            config = createDefault();
        }
        save();
    }

    /**
     * Creates the default configuration object.
     *
     * @return the default config used when no valid file exists
     */
    protected abstract T createDefault();

    /**
     * Saves a snapshot of the current config after a short debounce window.
     */
    public void save() {
        String snapshot = GSON.toJson(config);

        synchronized (saveLock) {
            pendingJson = snapshot;
            if (pendingSave == null || pendingSave.isDone()) {
                pendingSave = SAVE_EXECUTOR.schedule(this::flushPending, SAVE_DEBOUNCE_MILLIS, TimeUnit.MILLISECONDS);
            }
        }
    }

    /**
     * Writes any pending snapshot immediately, or snapshots and writes the current config if none is pending.
     */
    public void flush() {
        String snapshot;

        synchronized (saveLock) {
            if (pendingSave != null) {
                pendingSave.cancel(false);
                pendingSave = null;
            }
            snapshot = pendingJson;
            pendingJson = null;
        }

        if (snapshot == null) {
            snapshot = GSON.toJson(config);
        }

        write(snapshot);
    }

    /**
     * Flushes the newest queued JSON snapshot from the save executor.
     */
    private void flushPending() {
        String snapshot;

        synchronized (saveLock) {
            snapshot = pendingJson;
            pendingJson = null;
            pendingSave = null;
        }

        if (snapshot != null) {
            write(snapshot);
        }
    }

    /**
     * Writes a JSON snapshot through a temporary file before replacing the live config.
     *
     * @param json the serialized config snapshot to persist
     */
    private void write(String json) {
        synchronized (writeLock) {
            try {
                Path parent = file.getParent();
                if (parent != null && !Files.exists(parent)) {
                    Files.createDirectories(parent);
                }

                Path tempFile = file.resolveSibling(file.getFileName() + ".tmp");
                Files.writeString(tempFile, json);

                try {
                    Files.move(tempFile, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                log.error("Failed to save config to {}", file, e);
            }
        }
    }

    /**
     * Creates daemon threads for background config writes.
     */
    private static class ConfigThreadFactory implements ThreadFactory {
        /**
         * Builds a daemon writer thread.
         *
         * @param runnable the save task to run
         * @return the daemon thread that will run config saves
         */
        @Override
        public Thread newThread(@NotNull Runnable runnable) {
            Thread thread = new Thread(runnable, "ardapaths-config-writer");
            thread.setDaemon(true);
            return thread;
        }
    }

}

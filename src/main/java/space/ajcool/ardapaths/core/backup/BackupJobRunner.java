package space.ajcool.ardapaths.core.backup;

import lombok.extern.slf4j.Slf4j;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import space.ajcool.ardapaths.core.backup.progress.OperationKind;
import space.ajcool.ardapaths.core.backup.progress.OperationProgress;
import space.ajcool.ardapaths.core.backup.progress.ProgressReporter;
import space.ajcool.ardapaths.core.backup.progress.ProgressSnapshot;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Runs backup and restore operations asynchronously with a single active-job guard.
 */
@Slf4j(topic = "ardapaths")
public class BackupJobRunner {
    /**
     * Single background worker for file scanning and JSON work.
     */
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(new BackupThreadFactory());

    /**
     * Backup logic executed by worker jobs.
     */
    private final BackupManager backupManager = new BackupManager();

    /**
     * Active mutually exclusive backup or restore job.
     */
    private static final AtomicReference<ActiveJob> ACTIVE = new AtomicReference<>();

    /**
     * Starts a backup when no other backup job is active.
     *
     * @param source command source that requested the backup
     * @return launch result with current progress when rejected
     */
    public JobStartResult tryStartBackup(ServerCommandSource source) {
        OperationProgress progress = new OperationProgress(OperationKind.BACKUP);
        ActiveJob job = new ActiveJob(OperationKind.BACKUP, progress);

        if (!ACTIVE.compareAndSet(null, job)) {
            return JobStartResult.rejected(ACTIVE.get().progress().snapshot());
        }

        MinecraftServer server = source.getServer();
        WORKER.submit(() -> runBackupJob(server, source, job));
        return JobStartResult.started(progress.snapshot());
    }

    /**
     * Starts a restore when no other backup job is active.
     *
     * @param source command source that requested the restore
     * @param zipName optional backup zip file
     * @param hard whether stale markers should be deleted
     * @return launch result with current progress when rejected
     */
    public JobStartResult tryStartRestore(ServerCommandSource source, String zipName, boolean hard) {
        OperationProgress progress = new OperationProgress(OperationKind.RESTORE);
        ActiveJob job = new ActiveJob(OperationKind.RESTORE, progress);

        if (!ACTIVE.compareAndSet(null, job)) {
            return JobStartResult.rejected(ACTIVE.get().progress().snapshot());
        }

        MinecraftServer server = source.getServer();
        WORKER.submit(() -> runRestoreJob(server, source, zipName, hard, job));
        return JobStartResult.started(progress.snapshot());
    }

    /**
     * Runs marker work on the shared backup worker with a server-thread gate.
     *
     * @param server target server
     * @param work   worker task that may use the supplied server gate
     * @param <T>    task result type
     * @return future completed by the shared worker
     */
    public static <T> CompletableFuture<T> submitMarkerWork(MinecraftServer server, Function<ServerGate, T> work) {
        return CompletableFuture.supplyAsync(() -> work.apply(new SubmitServerGate(server)), WORKER);
    }

    /**
     * Checks whether a backup or restore command is currently running.
     *
     * @return true when marker work should report a busy status instead of queueing
     */
    public static boolean isJobActive() {
        return ACTIVE.get() != null;
    }

    /**
     * Lists historical backup zip names for command suggestions.
     *
     * @return sorted zip names
     */
    public java.util.List<String> listBackupZipNames() {
        return backupManager.listBackupZipNames();
    }

    /**
     * Runs one backup job and reports completion.
     *
     * @param server target server
     * @param source originating command source
     * @param job active job state
     */
    private void runBackupJob(MinecraftServer server, ServerCommandSource source, ActiveJob job) {
        long start = System.currentTimeMillis();
        log.info("ArdaPaths backup started");

        try {
            BackupResult result = backupManager.runBackup(server, new LoggingReporter(job.progress()), new SubmitServerGate(server));
            long duration = System.currentTimeMillis() - start;
            log.info("ArdaPaths backup completed in {} ms: {}", duration, describeBackup(result));
            sendFeedback(server, source, describeBackup(result));
        } catch (IOException | CancellationException | CompletionException exception) {
            log.warn("ArdaPaths backup failed", exception);
            sendError(server, source, "ArdaPaths backup failed: " + exception.getMessage());
        } finally {
            ACTIVE.compareAndSet(job, null);
        }
    }

    /**
     * Runs one restore job and reports completion.
     *
     * @param server target server
     * @param source originating command source
     * @param zipName optional backup zip file
     * @param hard whether stale markers should be deleted
     * @param job active job state
     */
    private void runRestoreJob(MinecraftServer server, ServerCommandSource source, String zipName, boolean hard, ActiveJob job) {
        long start = System.currentTimeMillis();
        log.info("ArdaPaths restore started{}", hard ? " in hard mode" : "");

        try {
            RestoreResult result = backupManager.runRestore(server, zipName, hard, new LoggingReporter(job.progress()), new SubmitServerGate(server));
            long duration = System.currentTimeMillis() - start;
            log.info("ArdaPaths restore completed in {} ms: {}", duration, describeRestore(result));
            sendFeedback(server, source, describeRestore(result));
        } catch (IOException | CancellationException | CompletionException exception) {
            log.warn("ArdaPaths restore failed", exception);
            sendError(server, source, "ArdaPaths restore failed: " + exception.getMessage());
        } finally {
            ACTIVE.compareAndSet(job, null);
        }
    }

    /**
     * Formats a backup result for operator feedback.
     *
     * @param result backup result
     * @return feedback message
     */
    private String describeBackup(BackupResult result) {
        BackupStats stats = result.counts();

        if (!result.changed()) {
            return "ArdaPaths backup unchanged; no files written." + skippedDimensionsSuffix(result);
        }

        return "ArdaPaths backup wrote "
                + stats.paths() + " paths, "
                + stats.chapters() + " chapters, "
                + stats.markers() + " markers, "
                + stats.nodes() + " nodes."
                + (result.rotated() ? " Previous data saved as " + result.backupZipName() + "." : "")
                + skippedDimensionsSuffix(result);
    }

    /**
     * Formats skipped dimension feedback for partial backups.
     *
     * @param result backup result
     * @return empty string or skipped-dimension sentence
     */
    private String skippedDimensionsSuffix(BackupResult result) {
        if (result.skippedDimensions().isEmpty()) {
            return "";
        }

        return " Skipped " + result.skippedDimensions().size() + " dimension(s) with unreadable world data: " + String.join(", ", result.skippedDimensions()) + ".";
    }

    /**
     * Formats a restore result for operator feedback.
     *
     * @param result restore result
     * @return feedback message
     */
    private String describeRestore(RestoreResult result) {
        BackupStats stats = result.counts();
        String hardSuffix = result.hard() ? " Deleted " + result.markersDeleted() + " stale markers." : "";
        String skippedSuffix = result.markersSkipped() > 0
                ? " Skipped " + result.markersSkipped() + " markers; missing chunks: " + result.missingChunks() + ", occupied blocks: " + result.markerConflicts() + ". See the server log."
                : "";

        return "ArdaPaths restored "
                + stats.paths() + " paths, "
                + stats.chapters() + " chapters, and placed "
                + result.markersPlaced() + " markers from " + result.source() + "."
                + hardSuffix
                + skippedSuffix;
    }

    /**
     * Sends final command feedback on the server thread.
     *
     * @param server  target server
     * @param source  command source
     * @param message feedback message
     */
    private void sendFeedback(MinecraftServer server, ServerCommandSource source, String message) {
        if (!server.isRunning()) return;
        server.execute(() -> source.sendFeedback(() -> Text.literal(message), true));
    }

    /**
     * Sends final command failure feedback on the server thread.
     *
     * @param server target server
     * @param source command source
     * @param message failure message
     */
    private void sendError(MinecraftServer server, ServerCommandSource source, String message) {
        if (!server.isRunning()) return;
        server.execute(() -> source.sendError(Text.literal(message)));
    }

    /**
     * Runs work that must execute on the server thread.
     */
    public interface ServerGate {
        /**
         * Runs server-thread-only work.
         *
         * @param runnable work to run
         */
        void run(Runnable runnable);

        /**
         * Calls server-thread-only work and returns its value.
         *
         * @param supplier work to call
         * @param <T>      supplier result type
         * @return supplier result
         */
        <T> T call(Supplier<T> supplier);
    }

    /**
     * Server gate backed by {@link MinecraftServer#submit(Supplier)}.
     */
    private static class SubmitServerGate implements ServerGate {
        /**
         * Server receiving gated tasks.
         */
        private final MinecraftServer server;

        /**
         * Creates a gate for one server.
         *
         * @param server server receiving gated tasks
         */
        SubmitServerGate(MinecraftServer server) {
            this.server = server;
        }

        /**
         * Runs work on the server thread.
         *
         * @param runnable work to run
         */
        @Override
        public void run(Runnable runnable) {
            call(() -> {
                runnable.run();
                return null;
            });
        }

        /**
         * Calls work on the server thread.
         *
         * @param supplier work to call
         * @return supplier result
         */
        @Override
        public <T> T call(Supplier<T> supplier) {
            if (!server.isRunning()) {
                throw new CancellationException("server is stopping");
            }

            if (server.isOnThread()) {
                return supplier.get();
            }

            return server.submit(supplier).join();
        }
    }

    /**
     * Progress reporter that updates shared progress and logs throttled progress lines.
     */
    private static class LoggingReporter implements ProgressReporter {
        /**
         * Minimum milliseconds between repeated progress log lines.
         */
        private static final long LOG_THROTTLE_MS = 1_000L;

        /**
         * Shared mutable progress state.
         */
        private final OperationProgress progress;

        /**
         * Last logged phase label.
         */
        private String lastPhase = "";

        /**
         * Last progress log wall-clock time.
         */
        private long lastLogAt = 0L;

        /**
         * Creates a logging reporter.
         *
         * @param progress shared progress state
         */
        LoggingReporter(OperationProgress progress) {
            this.progress = progress;
        }

        /**
         * Updates and logs a phase change.
         *
         * @param phase the current operation phase
         */
        @Override
        public void phase(String phase) {
            progress.phase(phase);
            logSnapshot(true);
        }

        /**
         * Updates and logs throttled numeric progress.
         *
         * @param done completed units
         * @param total total units, or zero when unknown
         */
        @Override
        public void advance(int done, int total) {
            progress.advance(done, total);
            logSnapshot(false);
        }

        /**
         * Logs the current progress snapshot when the throttle permits it.
         *
         * @param force whether to ignore the time throttle
         */
        private void logSnapshot(boolean force) {
            ProgressSnapshot snapshot = progress.snapshot();
            long now = System.currentTimeMillis();

            if (force || !snapshot.phase().equals(lastPhase) || now - lastLogAt >= LOG_THROTTLE_MS) {
                lastPhase = snapshot.phase();
                lastLogAt = now;
                log.info("ArdaPaths {} progress: {}", snapshot.kind().name().toLowerCase(Locale.ROOT), snapshot.format());
            }
        }
    }

    /**
     * Active job state held by the concurrency guard.
     *
     * @param kind operation kind
     * @param progress mutable progress state
     */
    private record ActiveJob(OperationKind kind, OperationProgress progress) {
    }

    /**
     * Result of attempting to start a backup job.
     *
     * @param started whether a new job was started
     * @param snapshot started job or already-active job progress
     */
    public record JobStartResult(boolean started, ProgressSnapshot snapshot) {
        /**
         * Creates a successful launch result.
         *
         * @param snapshot started job progress
         * @return launch result
         */
        public static JobStartResult started(ProgressSnapshot snapshot) {
            return new JobStartResult(true, snapshot);
        }

        /**
         * Creates a rejected launch result.
         *
         * @param snapshot already-active job progress
         * @return launch result
         */
        public static JobStartResult rejected(ProgressSnapshot snapshot) {
            return new JobStartResult(false, snapshot);
        }
    }

    /**
     * Thread factory for the backup worker.
     */
    private static class BackupThreadFactory implements ThreadFactory {
        /**
         * Creates a daemon worker thread.
         *
         * @param runnable worker task
         * @return daemon worker thread
         */
        @Override
        public Thread newThread(@NotNull Runnable runnable) {
            Thread thread = new Thread(runnable, "ardapaths-backup");
            thread.setDaemon(true);
            return thread;
        }
    }
}

package space.ajcool.ardapaths.core.backup;

import lombok.extern.slf4j.Slf4j;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;
import space.ajcool.ardapaths.core.backup.progress.OperationKind;
import space.ajcool.ardapaths.core.backup.progress.OperationProgress;
import space.ajcool.ardapaths.core.backup.progress.ProgressReporter;
import space.ajcool.ardapaths.core.backup.progress.ProgressSnapshot;

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
     * Active mutually exclusive backup or restore job.
     */
    private static final AtomicReference<ActiveJob> ACTIVE = new AtomicReference<>();

    /**
     * Backup logic executed by worker jobs.
     */
    private final BackupManager backupManager = new BackupManager();

    /**
     * Runs marker work on the shared backup worker with a server-thread gate.
     *
     * @param server target server
     * @param work   worker task that may use the supplied server gate
     * @param <T>    task result type
     * @return future completed by the shared worker
     */
    public static <T> CompletableFuture<T> submitMarkerWork(MinecraftServer server, Function<ServerGate, T> work) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return work.apply(new SubmitServerGate(server));
            } catch (Throwable throwable) {
                log.error("ArdaPaths marker work failed", throwable);
                throw throwable;
            }
        }, WORKER);
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
     * Starts a backup when no other backup job is active.
     *
     * @param source command source that requested the backup
     * @return launch result with current progress when rejected
     */
    public JobStartResult tryStartBackup(CommandSourceStack source) {
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
     * Runs one backup job and reports completion.
     *
     * @param server target server
     * @param source originating command source
     * @param job    active job state
     */
    private void runBackupJob(MinecraftServer server, CommandSourceStack source, ActiveJob job) {
        long start = System.currentTimeMillis();
        log.info("ArdaPaths backup started");

        try {
            BackupResult result = backupManager.runBackup(server, new LoggingReporter(job.progress()), new SubmitServerGate(server));
            long duration = System.currentTimeMillis() - start;
            log.info("ArdaPaths backup completed in {} ms: {}", duration, describeBackup(result));
            sendFeedback(server, source, describeBackup(result));
        } catch (Throwable throwable) {
            log.error("ArdaPaths backup failed", throwable);
            sendError(server, source, "ArdaPaths backup failed: " + describeThrowable(throwable));
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
     * Sends final command feedback on the server thread.
     *
     * @param server  target server
     * @param source  command source
     * @param message feedback message
     */
    private void sendFeedback(MinecraftServer server, CommandSourceStack source, String message) {
        if (!server.isRunning()) return;
        server.execute(() -> source.sendSuccess(() -> Component.literal(message), true));
    }

    /**
     * Sends final command failure feedback on the server thread.
     *
     * @param server  target server
     * @param source  command source
     * @param message failure message
     */
    private void sendError(MinecraftServer server, CommandSourceStack source, String message) {
        if (!server.isRunning()) return;
        server.execute(() -> source.sendFailure(Component.literal(message)));
    }

    /**
     * Formats a throwable with type information for operator-facing failures.
     *
     * @param throwable failure to describe
     * @return class name and message suitable for command feedback
     */
    private String describeThrowable(Throwable throwable) {
        String message = throwable.getMessage();
        String type = throwable.getClass().getSimpleName();
        return message == null || message.isBlank() ? type : type + ": " + message;
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
     * Starts a restore when no other backup job is active.
     *
     * @param source  command source that requested the restore
     * @param zipName optional backup zip file
     * @param hard    whether stale markers should be deleted
     * @return launch result with current progress when rejected
     */
    public JobStartResult tryStartRestore(CommandSourceStack source, String zipName, boolean hard) {
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
     * Runs one restore job and reports completion.
     *
     * @param server  target server
     * @param source  originating command source
     * @param zipName optional backup zip file
     * @param hard    whether stale markers should be deleted
     * @param job     active job state
     */
    private void runRestoreJob(MinecraftServer server, CommandSourceStack source, String zipName, boolean hard, ActiveJob job) {
        long start = System.currentTimeMillis();
        log.info("ArdaPaths restore started{}", hard ? " in hard mode" : "");

        try {
            RestoreResult result = backupManager.runRestore(server, zipName, hard, new LoggingReporter(job.progress()), new SubmitServerGate(server));
            long duration = System.currentTimeMillis() - start;
            log.info("ArdaPaths restore completed in {} ms: {}", duration, describeRestore(result));
            sendFeedback(server, source, describeRestore(result));
        } catch (Throwable throwable) {
            log.error("ArdaPaths restore failed", throwable);
            sendError(server, source, "ArdaPaths restore failed: " + describeThrowable(throwable));
        } finally {
            ACTIVE.compareAndSet(job, null);
        }
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
     * Lists historical backup zip names for command suggestions.
     *
     * @return sorted zip names
     */
    public java.util.List<String> listBackupZipNames() {
        return backupManager.listBackupZipNames();
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
     *
     * @param server server receiving gated tasks
     */
    private record SubmitServerGate(MinecraftServer server) implements ServerGate {

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
        @SuppressWarnings("resource")
        @Override
        public <T> T call(Supplier<T> supplier) {
            if (!server().isRunning()) {
                throw new CancellationException("server is stopping");
            }

            if (server().isSameThread()) {
                return supplier.get();
            }

            return server().submit(supplier).join();
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

        /**
         * Updates and logs throttled numeric progress.
         *
         * @param done  completed units
         * @param total total units, or zero when unknown
         */
        @Override
        public void advance(int done, int total) {
            progress.advance(done, total);
            logSnapshot(false);
        }
    }

    /**
     * Active job state held by the concurrency guard.
     *
     * @param kind     operation kind
     * @param progress mutable progress state
     */
    private record ActiveJob(OperationKind kind, OperationProgress progress) {

    }

    /**
     * Result of attempting to start a backup job.
     *
     * @param started  whether a new job was started
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

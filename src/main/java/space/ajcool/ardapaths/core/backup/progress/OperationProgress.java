package space.ajcool.ardapaths.core.backup.progress;

/**
 * Thread-safe mutable progress state for one active backup job.
 */
public class OperationProgress implements ProgressReporter {
    /**
     * Kind of job being tracked.
     */
    private final OperationKind kind;

    /**
     * Wall-clock job start time in milliseconds.
     */
    private final long startedAt;

    /**
     * Current phase label.
     */
    private String phase;

    /**
     * Completed progress units.
     */
    private int done;

    /**
     * Total progress units, or zero when unknown.
     */
    private int total;

    /**
     * Creates progress state for a new job.
     *
     * @param kind operation kind
     */
    public OperationProgress(OperationKind kind) {
        this.kind = kind;
        this.startedAt = System.currentTimeMillis();
        this.phase = "starting";
    }

    /**
     * Updates the current phase and resets counters.
     *
     * @param phase the current operation phase
     */
    @Override
    public synchronized void phase(String phase) {
        this.phase = phase;
        this.done = 0;
        this.total = 0;
    }

    /**
     * Updates the current numeric counters.
     *
     * @param done  completed units
     * @param total total units, or zero when unknown
     */
    @Override
    public synchronized void advance(int done, int total) {
        this.done = done;
        this.total = total;
    }

    /**
     * Creates an immutable snapshot of the current progress.
     *
     * @return progress snapshot
     */
    public synchronized ProgressSnapshot snapshot() {
        return new ProgressSnapshot(kind, phase, done, total, startedAt);
    }
}

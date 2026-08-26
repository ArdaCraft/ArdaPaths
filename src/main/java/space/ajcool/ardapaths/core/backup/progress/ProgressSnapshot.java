package space.ajcool.ardapaths.core.backup.progress;

/**
 * Immutable view of an active backup job's progress.
 *
 * @param kind      operation kind
 * @param phase     current phase label
 * @param done      completed progress units
 * @param total     total progress units, or zero when unknown
 * @param startedAt wall-clock start time in milliseconds
 */
public record ProgressSnapshot(
        OperationKind kind,
        String phase,
        int done,
        int total,
        long startedAt
) {
    /**
     * Formats this snapshot for operator-facing command feedback.
     *
     * @return compact progress summary
     */
    public String format() {
        String count = total > 0 ? done + "/" + total : Integer.toString(done);
        return kind.name().toLowerCase() + " " + phase + " " + count;
    }
}

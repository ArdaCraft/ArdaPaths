package space.ajcool.ardapaths.core.backup.progress;

/**
 * Receives progress updates from backup and restore operations.
 */
public interface ProgressReporter {

    /**
     * Updates the current phase label.
     *
     * @param phase the current operation phase
     */
    void phase(String phase);

    /**
     * Updates the numeric progress counters.
     *
     * @param done  completed units
     * @param total total units, or zero when unknown
     */
    void advance(int done, int total);
}

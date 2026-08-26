package space.ajcool.ardapaths.core.backup.progress;

/**
 * Kind of backup operation represented by an active job.
 */
public enum OperationKind {
    /**
     * Exporting current ArdaPaths data to disk.
     */
    BACKUP,

    /**
     * Importing ArdaPaths data from disk into the server.
     */
    RESTORE
}

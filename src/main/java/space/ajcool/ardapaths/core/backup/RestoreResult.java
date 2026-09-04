package space.ajcool.ardapaths.core.backup;

/**
 * Result of running an ArdaPaths restore command.
 *
 * @param source          backup source name
 * @param counts          restored object counts from the manifest
 * @param markersPlaced   number of marker block entities placed or updated
 * @param hard            whether stale markers were deleted
 * @param markersDeleted  number of stale marker blocks deleted
 * @param markersSkipped  number of marker payloads skipped because they could not be safely applied
 * @param missingChunks   number of missing chunks that caused marker payloads to be skipped
 * @param markerConflicts number of marker payloads skipped because another block occupied the target
 */
public record RestoreResult(String source, BackupStats counts, int markersPlaced, boolean hard, int markersDeleted,
                            int markersSkipped, int missingChunks, int markerConflicts) {

}

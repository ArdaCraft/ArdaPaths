package space.ajcool.ardapaths.core.backup;

import java.util.List;

/**
 * Result of running an ArdaPaths backup command.
 *
 * @param changed           whether data files were written
 * @param rotated           whether the previous data directory was zipped
 * @param counts            exported object counts
 * @param backupZipName     zip file name created for the previous snapshot, or null
 * @param skippedDimensions dimensions whose world data could not be scanned
 */
public record BackupResult(boolean changed, boolean rotated, BackupStats counts, String backupZipName, List<String> skippedDimensions) {
}

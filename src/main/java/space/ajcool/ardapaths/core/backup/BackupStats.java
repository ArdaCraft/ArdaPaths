package space.ajcool.ardapaths.core.backup;

/**
 * User-facing counts for backup and restore operations.
 *
 * @param dimensions dimension count
 * @param markers    marker count
 * @param paths      path count
 * @param chapters   chapter count
 * @param nodes      marker node count
 */
public record BackupStats(int dimensions, int markers, int paths, int chapters, int nodes) {

}

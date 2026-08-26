package space.ajcool.ardapaths.core.backup;

/**
 * Identifies one chunk in one Minecraft dimension.
 *
 * @param dimensionId dimension identifier containing the chunk
 * @param chunkPos    packed chunk X/Z position
 */
public record ChunkKey(String dimensionId, long chunkPos) {
}

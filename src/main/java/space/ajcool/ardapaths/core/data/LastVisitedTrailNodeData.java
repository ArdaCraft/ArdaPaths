package space.ajcool.ardapaths.core.data;

import net.minecraft.util.Identifier;

/**
 * Data class to store the last visited trail node information.
 *
 * @param selectedChapterId The ID of the selected chapter.
 * @param posX              The X coordinate of the last visited trail node.
 * @param posY              The Y coordinate of the last visited trail node.
 * @param posZ              The Z coordinate of the last visited trail node.
 * @param worldId           The identifier of the world where the trail node is located.
 */
public record LastVisitedTrailNodeData(String selectedChapterId, int posX, int posY, int posZ, Identifier worldId) {

}
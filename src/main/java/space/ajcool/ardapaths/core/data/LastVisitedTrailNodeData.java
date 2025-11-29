package space.ajcool.ardapaths.core.data;

import net.minecraft.util.Identifier;

public class LastVisitedTrailNodeData {

    private String selectedChapterId;
    private int posX;
    private int posY;
    private int posZ;
    private Identifier worldId;

    public LastVisitedTrailNodeData(String selectedChapterId, int posX, int posY, int posZ, Identifier worldId) {
        this.selectedChapterId = selectedChapterId;
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
        this.worldId = worldId;
    }

    public String getSelectedChapterId() {
        return selectedChapterId;
    }

    public void setSelectedChapterId(String selectedChapterId) {
        this.selectedChapterId = selectedChapterId;
    }

    public int getPosX() {
        return posX;
    }

    public void setPosX(int posX) {
        this.posX = posX;
    }

    public int getPosY() {
        return posY;
    }

    public void setPosY(int posY) {
        this.posY = posY;
    }

    public int getPosZ() {
        return posZ;
    }

    public void setPosZ(int posZ) {
        this.posZ = posZ;
    }

    public Identifier getWorldId() {
        return worldId;
    }

    public void setWorldId(Identifier worldId) {
        this.worldId = worldId;
    }
}

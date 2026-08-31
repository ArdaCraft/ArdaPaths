package space.ajcool.ardapaths.screens;

import net.minecraft.client.Minecraft;
import space.ajcool.ardapaths.core.Fabric;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;

/**
 * Central entry point for opening UI screens in ArdaPaths.
 * All screen opening logic goes through this class to ensure proper client-side checks.
 */
public class Screens {
    /**
     * Opens the Path Marker editor screen for the given marker block entity.
     * Only executes on the client side.
     *
     * @param pathMarkerBlockEntity the marker block entity to edit
     */
    public static void openEditorScreen(PathMarkerBlockEntity pathMarkerBlockEntity) {
        if (Fabric.isClient()) {
            Minecraft.getInstance().setScreen(new MarkerEditScreen(pathMarkerBlockEntity));
        }
    }

    /**
     * Opens the Path Selection screen where players can choose which path to follow.
     * Only executes on the client side.
     */
    public static void openSelectionScreen() {
        if (Fabric.isClient()) {
            Minecraft.getInstance().setScreen(new PathSelectionScreen());
        }
    }
}

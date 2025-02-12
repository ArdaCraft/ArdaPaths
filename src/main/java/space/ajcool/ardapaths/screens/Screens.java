package space.ajcool.ardapaths.screens;

import net.minecraft.client.MinecraftClient;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;
import space.ajcool.ardapaths.core.Fabric;

public class Screens {
    public static void openEditorScreen(PathMarkerBlockEntity pathMarkerBlockEntity) {
        if (Fabric.isClient()) {
            MinecraftClient.getInstance().setScreen(new MarkerEditScreen(pathMarkerBlockEntity));
        }
    }

    public static void openSelectionScreen() {
        if (Fabric.isClient()) {
            MinecraftClient.getInstance().setScreen(new PathSelectionScreen());
        }
    }
}

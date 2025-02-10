package space.ajcool.ardapaths.screens;

import net.minecraft.client.MinecraftClient;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;
import space.ajcool.ardapaths.utils.McUtils;

public class Screens {
    public static void openEditorScreen(PathMarkerBlockEntity pathMarkerBlockEntity) {
        if (McUtils.isClient()) {
            MinecraftClient.getInstance().setScreen(new PathMarkerEditScreen(pathMarkerBlockEntity));
        }
    }

    public static void openSelectionScreen() {
        if (McUtils.isClient()) {
            MinecraftClient.getInstance().setScreen(new PathSelectionScreen());
        }
    }
}

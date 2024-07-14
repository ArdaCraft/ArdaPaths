package space.ajcool.ardapaths.screen;


import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import space.ajcool.ardapaths.ArdaPathsClient;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.ArdaPathsConfig;
import space.ajcool.ardapaths.block.PathMarkerBlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Environment(value = EnvType.CLIENT)
public class PathSelectionScreen extends Screen
{
    public static int selectedPathId = 0;
    public static boolean callingForTeleport = false;

    public PathSelectionScreen()
    {
        super(Component.literal("Path Selection"));
    }

    @Override
    protected void init() {
        ArdaPaths.CONFIG.paths.forEach(pathSettings -> {
            addRenderableWidget(
                    new Button(width / 2 - 75, 40 + pathSettings.Id * 30, 150, 20, Component.literal(pathSettings.Name).withStyle(Style.EMPTY.withColor(pathSettings.PrimaryColor.encodedColor())), button -> {
                        selectedPathId = pathSettings.Id;
                        ArdaPathsClient.animationTrails.clear();
                        ArdaPathsClient.trailSoundInstance = null;
                        Minecraft.getInstance().setScreen(null);
                    })
            );
        });


        addRenderableWidget(
                new Button(width / 2 - 75, 80 + ArdaPaths.CONFIG.paths.size() * 30, 150, 20, Component.literal("Return To Path"), button -> {
                    callingForTeleport = true;
                    ArdaPathsClient.animationTrails.clear();
                    ArdaPathsClient.trailSoundInstance = null;
                    Minecraft.getInstance().setScreen(null);
                })
        );

//        addRenderableWidget(
//                new Button(width / 2 - 75, height - 40, 150, 20, Component.literal("Cancel"), button -> {
//                    Minecraft.getInstance().setScreen(null);
//                })
//        );
    }

    @Override
    public void render(PoseStack poseStack, int i, int j, float f)
    {
        fillGradient(poseStack, 0, 0, this.width, this.height, -1072689136, -804253680);
        drawCenteredString(poseStack, font, Component.literal("Select the path you wish to follow"), width / 2, 20, 0xffffff);

        for (ArdaPathsConfig.PathSettings path : ArdaPaths.CONFIG.paths)
        {
            if (selectedPathId != path.Id) continue;

            var text = Component.literal("You are currently on ").append(Component.literal(path.Name).withStyle(Style.EMPTY.withColor(path.PrimaryColor.encodedColor())));

            drawCenteredString(poseStack, font, text, width / 2, 60 + ArdaPaths.CONFIG.paths.size() * 30, 0xffffff);
        }



        super.render(poseStack, i, j, f);
    }
}
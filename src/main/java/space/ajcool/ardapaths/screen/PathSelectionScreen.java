package space.ajcool.ardapaths.screen;


import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import space.ajcool.ardapaths.ArdaPathsClient;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.ArdaPathsConfig;
import space.ajcool.ardapaths.block.PathMarkerBlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Logger;

@Environment(value = EnvType.CLIENT)
public class PathSelectionScreen extends Screen
{
    public static int selectedPathId = 0;
    public static boolean callingForTeleport = false;

    public PathSelectionScreen()
    {
        super(Text.literal("Path Selection"));
    }

    @Override
    protected void init() {
        ArdaPaths.CONFIG.paths.forEach(pathSettings -> {
            addDrawableChild(
                    new ButtonWidget(width / 2 - 75, 40 + pathSettings.Id * 30, 150, 20, Text.literal(pathSettings.Name).fillStyle(Style.EMPTY.withColor(pathSettings.PrimaryColor.encodedColor())), button -> {
                        selectedPathId = pathSettings.Id;
                        ArdaPathsClient.animationTrails.clear();
                        ArdaPathsClient.trailSoundInstance = null;
                        MinecraftClient.getInstance().setScreen(null);
                    }, Supplier::get)
            );
        });


        addDrawableChild(
                new ButtonWidget(width / 2 - 75, 80 + ArdaPaths.CONFIG.paths.size() * 30, 150, 20, Text.literal("Return To Path"), button -> {
                    callingForTeleport = true;
                    ArdaPathsClient.animationTrails.clear();
                    ArdaPathsClient.trailSoundInstance = null;
                    MinecraftClient.getInstance().setScreen(null);
                }, Supplier::get)
        );

//        addRenderableWidget(
//                new Button(width / 2 - 75, height - 40, 150, 20, Component.literal("Cancel"), button -> {
//                    Minecraft.getInstance().setScreen(null);
//                })
//        );
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta)
    {
        context.fillGradient(0, 0, this.width, this.height, -1072689136, -804253680);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Select the path you wish to follow"), width / 2, 20, 0xffffff);

        for (ArdaPathsConfig.PathSettings path : ArdaPaths.CONFIG.paths)
        {
            if (selectedPathId != path.Id) continue;

            var text = Text.literal("You are currently on ").append(Text.literal(path.Name).fillStyle(Style.EMPTY.withColor(path.PrimaryColor.encodedColor())));

            context.drawCenteredTextWithShadow(textRenderer, text, width / 2, 60 + ArdaPaths.CONFIG.paths.size() * 30, 0xffffff);
        }

        super.render(context, mouseX, mouseY, delta);
    }
}
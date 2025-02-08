package space.ajcool.ardapaths.screen;


import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import space.ajcool.ardapaths.ArdaPathsClient;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.ArdaPathsConfig;

import java.util.function.Supplier;

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
                    new ButtonWidget(width / 2 - 75, 40 + pathSettings.Id * 25, 150, 20, Text.literal(pathSettings.Name).fillStyle(Style.EMPTY.withColor(pathSettings.PrimaryColor.encodedColor())), button -> {
                        selectedPathId = pathSettings.Id;
                        ArdaPathsClient.animationTrails.clear();
                        ArdaPathsClient.trailSoundInstance = null;
                        MinecraftClient.getInstance().setScreen(null);
                    }, Supplier::get)
            );
        });

        addDrawableChild(
                new ButtonWidget(width / 2 - 75, 70 + ArdaPaths.CONFIG.paths.size() * 25, 150, 20, Text.literal("Return To Path"), button -> {
                    callingForTeleport = true;
                    ArdaPathsClient.animationTrails.clear();
                    ArdaPathsClient.trailSoundInstance = null;
                    MinecraftClient.getInstance().setScreen(null);
                }, Supplier::get)
        );

        addDrawableChild(
                new ButtonWidget(width / 2 - 75, 95 + ArdaPaths.CONFIG.paths.size() * 25, 150, 20, Text.literal("Marker Text: " + (ArdaPaths.CONFIG.markerText ? "On" : "Off")), button -> {
                    ArdaPaths.CONFIG.markerText = !ArdaPaths.CONFIG.markerText;
                    ArdaPathsConfig.INSTANCE.save();

                    MinecraftClient.getInstance().setScreen(null);
                    MinecraftClient.getInstance().setScreen(new PathSelectionScreen());
                }, Supplier::get)
        );
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

            context.drawCenteredTextWithShadow(textRenderer, text, width / 2, 50 + ArdaPaths.CONFIG.paths.size() * 25, 0xffffff);
        }

        super.render(context, mouseX, mouseY, delta);
    }
}
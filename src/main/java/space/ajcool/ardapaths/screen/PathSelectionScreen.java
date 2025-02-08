package space.ajcool.ardapaths.screen;


import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.ArdaPathsClient;
import space.ajcool.ardapaths.config.shared.PathSettings;
import space.ajcool.ardapaths.paths.TrailRenderer;

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
        ArdaPathsClient.CONFIG.paths.forEach(pathSettings -> {
            addDrawableChild(
                    new ButtonWidget(width / 2 - 75, 40 + pathSettings.id * 25, 150, 20, Text.literal(pathSettings.name).fillStyle(Style.EMPTY.withColor(pathSettings.primaryColor.asHex())), button -> {
                        selectedPathId = pathSettings.id;
                        TrailRenderer.clearTrails();
                        MinecraftClient.getInstance().setScreen(null);
                    }, Supplier::get)
            );
        });

        addDrawableChild(
                new ButtonWidget(width / 2 - 75, 70 + ArdaPaths.CONFIG.paths.size() * 25, 150, 20, Text.literal("Return To Path"), button -> {
                    callingForTeleport = true;
                    TrailRenderer.clearTrails();
                    MinecraftClient.getInstance().setScreen(null);
                }, Supplier::get)
        );

        addDrawableChild(
                new ButtonWidget(width / 2 - 75, 95 + ArdaPaths.CONFIG.paths.size() * 25, 150, 20, Text.literal("Marker Text: " + (ArdaPathsClient.CONFIG.proximityMessages ? "On" : "Off")), button -> {
                    ArdaPathsClient.CONFIG.proximityMessages = !ArdaPathsClient.CONFIG.proximityMessages;
                    ArdaPathsClient.CONFIG_MANAGER.save();

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

        for (PathSettings path : ArdaPathsClient.CONFIG.paths)
        {
            if (selectedPathId != path.id) continue;

            var text = Text.literal("You are currently on ").append(Text.literal(path.name).fillStyle(Style.EMPTY.withColor(path.primaryColor.asHex())));

            context.drawCenteredTextWithShadow(textRenderer, text, width / 2, 50 + ArdaPaths.CONFIG.paths.size() * 25, 0xffffff);
        }

        super.render(context, mouseX, mouseY, delta);
    }
}
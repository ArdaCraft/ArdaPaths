package space.ajcool.ardapaths.screens;


import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import space.ajcool.ardapaths.ArdaPathsClient;
import space.ajcool.ardapaths.config.shared.ChapterData;
import space.ajcool.ardapaths.config.shared.PathData;
import space.ajcool.ardapaths.mc.networking.PacketRegistry;
import space.ajcool.ardapaths.paths.ProximityMessageRenderer;
import space.ajcool.ardapaths.paths.TrailRenderer;
import space.ajcool.ardapaths.screens.widgets.dropdowns.ChapterDropdownWidget;
import space.ajcool.ardapaths.screens.widgets.dropdowns.PathDropdownWidget;

import java.util.List;
import java.util.function.Supplier;

@Environment(value = EnvType.CLIENT)
public class PathSelectionScreen extends Screen {
    private String selectedPathId;
    private String selectedChapterId;
    private boolean showProximityMessages;
    private boolean onlyRenderChapter;

    public PathSelectionScreen() {
        super(Text.literal("Path Selection"));
        this.selectedPathId = ArdaPathsClient.CONFIG.getSelectedPathId();
        this.selectedChapterId = ArdaPathsClient.CONFIG.getCurrentChapterId();
        this.showProximityMessages = ArdaPathsClient.CONFIG.showProximityMessages();
        this.onlyRenderChapter = ArdaPathsClient.CONFIG.onlyRenderChapter();
    }

    @Override
    protected void init() {
        int center = width / 2;
        int y = 35;

        PathDropdownWidget pathDropdown = this.addDrawableChild(new PathDropdownWidget(
                center - 75,
                y,
                150,
                20,
                selectedPathId
        ));

        ChapterDropdownWidget chapterDropdown = this.addDrawableChild(new ChapterDropdownWidget(
                center - 75,
                y += 40,
                150,
                20,
                selectedPathId,
                chapter -> selectedChapterId = chapter.getId()
        ));

        this.addDrawableChild(new ButtonWidget(
                center - 153,
                y += 50,
                150,
                20,
                Text.literal("Return to Path"),
                button -> {
                    ArdaPathsClient.callingForTeleport = true;
                    TrailRenderer.clearTrails();
                    this.close();
                },
                Supplier::get
        ));

        this.addDrawableChild(new ButtonWidget(
                center + 3,
                y,
                150,
                20,
                Text.literal("Return to Chapter Start"),
                button -> {
                    if (!selectedPathId.isEmpty() && !selectedChapterId.isEmpty()) {
                        PacketRegistry.CHAPTER_PLAYER_TELEPORT.sendToServer(selectedPathId, selectedChapterId);
                        TrailRenderer.clearTrails();
                    }
                    this.close();
                },
                Supplier::get
        ));

        this.addDrawableChild(new ButtonWidget(
                center - 153,
                y += 30,
                130,
                20,
                Text.literal("Marker Text: " + (showProximityMessages ? "On" : "Off")),
                button -> {
                    showProximityMessages = !showProximityMessages;
                    button.setMessage(Text.literal("Marker Text: " + (showProximityMessages ? "On" : "Off")));
                },
                Supplier::get
        ));

        this.addDrawableChild(new ButtonWidget(
                center - 17,
                y,
                170,
                20,
                Text.literal("Only Show Current Chapter: " + (onlyRenderChapter ? "On" : "Off")),
                button -> {
                    onlyRenderChapter = !onlyRenderChapter;
                    button.setMessage(Text.literal("Only Show Current Chapter: " + (onlyRenderChapter ? "On" : "Off")));
                },
                Supplier::get
        ));

        pathDropdown.setItemSelectedListener(path -> {
            if (!path.getId().equalsIgnoreCase(selectedPathId)) {
                TrailRenderer.clearTrails();
            }

            selectedPathId = path.getId();
            List<ChapterData> chapters = path.getChapters();
            chapterDropdown.setOptions(chapters);
        });
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fillGradient(0, 0, this.width, this.height, -1072689136, -804253680);

        context.drawCenteredTextWithShadow(
                textRenderer,
                Text.literal("Select the path you wish to follow"),
                width / 2,
                20,
                0xffffff
        );

        context.drawCenteredTextWithShadow(
                textRenderer,
                Text.literal("Teleport to chapter start"),
                width / 2,
                60,
                0xffffff
        );

        for (PathData path : ArdaPathsClient.CONFIG.getPaths()) {
            if (!selectedPathId.equalsIgnoreCase(path.getId())) continue;
            Text text = Text.literal("You are currently on ")
                            .append(Text.literal(path.getName())
                            .fillStyle(Style.EMPTY.withColor(path.getColor().asHex())));
            context.drawCenteredTextWithShadow(
                    textRenderer,
                    text,
                    width / 2,
                    105,
                    0xffffff
            );
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        super.close();
        ArdaPathsClient.CONFIG.setSelectedPath(selectedPathId);
        ArdaPathsClient.CONFIG.setCurrentChapter(selectedChapterId);
        ArdaPathsClient.CONFIG.showProximityMessages(showProximityMessages);
        ArdaPathsClient.CONFIG.onlyRenderChapter(onlyRenderChapter);
        ArdaPathsClient.CONFIG_MANAGER.save();
        ProximityMessageRenderer.clearMessage();
    }
}
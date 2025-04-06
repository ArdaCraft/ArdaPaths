package space.ajcool.ardapaths.screens;


import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import space.ajcool.ardapaths.ArdaPathsClient;
import space.ajcool.ardapaths.core.data.config.shared.ChapterData;
import space.ajcool.ardapaths.core.data.config.shared.Color;
import space.ajcool.ardapaths.core.data.config.shared.PathData;
import space.ajcool.ardapaths.paths.Paths;
import space.ajcool.ardapaths.paths.rendering.ProximityMessageRenderer;
import space.ajcool.ardapaths.paths.rendering.TrailRenderer;
import space.ajcool.ardapaths.screens.builders.DropdownBuilder;
import space.ajcool.ardapaths.screens.builders.TextBuilder;

import java.util.function.Supplier;

@Environment(value = EnvType.CLIENT)
public class PathSelectionScreen extends Screen {
    private String selectedPathId;
    private String selectedChapterId;
    private boolean showProximityMessages;

    public PathSelectionScreen() {
        super(Text.literal("Path Selection"));
        this.selectedPathId = ArdaPathsClient.CONFIG.getSelectedPathId();
        this.selectedChapterId = ArdaPathsClient.CONFIG.getCurrentChapterId();
        this.showProximityMessages = ArdaPathsClient.CONFIG.showProximityMessages();
    }

    @Override
    protected void init() {
        int center = width / 2;
        int y = 35;

        PathData currentPath = ArdaPathsClient.CONFIG.getPath(selectedPathId);
        ChapterData currentChapter = ArdaPathsClient.CONFIG.getCurrentChapter();

        this.addDrawableChild(TextBuilder.create()
                .setPosition(center - 75, y)
                .setSize(150, 20)
                .setText(Text.literal("You are currently on Chapter ")
                        .append(Text.literal(currentChapter != null ? currentChapter.getName() : "0"))
                        .append(Text.literal(" of "))
                        .append(Text.literal(currentPath != null ? currentPath.getName() : "Generic Path")
                                .fillStyle(Style.EMPTY.withColor(currentPath != null ? currentPath.getPrimaryColor().asHex() : Color.fromRgb(100, 100, 100).asHex()))))
                .build()
        );

        this.addDrawableChild(DropdownBuilder.<PathData>create()
                .setPosition(center - 75, y += 40)
                .setSize(150, 20)
                .setTitle(Text.literal("Select a Path to Follow:"))
                .setOptions(ArdaPathsClient.CONFIG.getPaths())
                .setOptionDisplay(item -> {
                    if (item == null) return Text.literal("No Path");
                    return Text.literal(item.getName()).fillStyle(Style.EMPTY.withColor(item.getPrimaryColor().asHex()));
                })
                .setSelected(ArdaPathsClient.CONFIG.getSelectedPath())
                .setOnSelect(path ->
                {
                    if (!path.getId().equalsIgnoreCase(selectedPathId))
                    {
                        TrailRenderer.clearTrails();
                    }

                    selectedPathId = path.getId();
                    selectedChapterId = path.getChapterIds().get(0);

                    Paths.setSelectedPath(selectedPathId);
                    Paths.gotoChapter(selectedChapterId, false);

                    this.clearAndInit();
                })

                .build()
        );

        this.addDrawableChild(DropdownBuilder.<ChapterData>create()
                .setPosition(center - 75, y += 40)
                .setSize(150, 20)
                .setTitle(Text.literal("Select a Chapter:"))
                .setOptions(currentPath.getChapters())
                .setOptionDisplay(item ->
                {
                    if (item == null) return Text.literal("No Chapter");
                    return Text.literal(item.getName());
                })
                .setSelected(currentChapter)
                .setOnSelect(chapter ->
                {
                    selectedChapterId = chapter == null ? "default" : chapter.getId();

                    Paths.gotoChapter(selectedChapterId, false);

                    this.clearAndInit();
                })
                .build()
        );

        this.addDrawableChild(new ButtonWidget(
                center - 65, y += 40,
                130,
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
                center - 65, y += 30,
                130,
                20,
                Text.literal("Return to Chapter Start"),
                button -> {
                    this.close();
                    if (!selectedPathId.isEmpty() && !selectedChapterId.isEmpty()) {
                        Paths.gotoChapter(selectedChapterId);
                    }
                },
                Supplier::get
        ));

        this.addDrawableChild(new ButtonWidget(
                center - 65, y + 30,
                130,
                20,
                Text.literal("Proximity Text: " + (showProximityMessages ? "On" : "Off")),
                button -> {
                    showProximityMessages = !showProximityMessages;
                    Paths.showProximityMessages(showProximityMessages);
                    ProximityMessageRenderer.clearMessage();
                    button.setMessage(Text.literal("Marker Text: " + (showProximityMessages ? "On" : "Off")));
                },
                Supplier::get
        ));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
    }
}
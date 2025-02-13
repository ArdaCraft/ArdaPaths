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
import space.ajcool.ardapaths.core.data.config.shared.PathData;
import space.ajcool.ardapaths.core.networking.PacketRegistry;
import space.ajcool.ardapaths.core.networking.packets.server.ChapterPlayerTeleportPacket;
import space.ajcool.ardapaths.paths.Paths;
import space.ajcool.ardapaths.paths.rendering.ProximityMessageRenderer;
import space.ajcool.ardapaths.paths.rendering.TrailRenderer;
import space.ajcool.ardapaths.screens.builders.DropdownBuilder;
import space.ajcool.ardapaths.screens.builders.TextBuilder;
import space.ajcool.ardapaths.screens.widgets.DropdownWidget;
import space.ajcool.ardapaths.screens.widgets.TextWidget;

import java.util.List;
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

        DropdownWidget<PathData> pathDropdown = this.addDrawableChild(DropdownBuilder.<PathData>create()
                .setPosition(center - 75, y)
                .setSize(150, 20)
                .setTitle(Text.literal("Select a Path to Follow:"))
                .setOptions(ArdaPathsClient.CONFIG.getPaths())
                .setOptionDisplay(item -> {
                    if (item == null) return Text.literal("No Path");
                    return Text.literal(item.getName()).fillStyle(Style.EMPTY.withColor(item.getColor().asHex()));
                })
                .setSelected(ArdaPathsClient.CONFIG.getSelectedPath())
                .build()
        );

        TextWidget pathDisplay = this.addDrawableChild(TextBuilder.create()
                .setPosition(center - 75, y += 20)
                .setSize(150, 20)
                .setText(Text.literal("You are currently on ").append(Text.literal(currentPath.getName()).fillStyle(Style.EMPTY.withColor(currentPath.getColor().asHex()))))
                .build()
        );

        this.addDrawableChild(new ButtonWidget(
                center - 40,
                y += 20,
                80,
                20,
                Text.literal("Return to Path"),
                button -> {
                    ArdaPathsClient.callingForTeleport = true;
                    TrailRenderer.clearTrails();
                    this.close();
                },
                Supplier::get
        ));

        DropdownWidget<ChapterData> chapterDropdown = this.addDrawableChild(DropdownBuilder.<ChapterData>create()
                        .setPosition(center - 75, y += 60)
                        .setSize(150, 20)
                        .setTitle(Text.literal("Select a Chapter to Return to:"))
                        .setOptions(currentPath.getChapters())
                        .setOptionDisplay(item -> {
                            if (item == null) return Text.literal("No Chapter");
                            return Text.literal(item.getName());
                        })
                        .setOnSelect(chapter -> selectedChapterId = chapter.getId())
                        .setAllowNull(true)
                        .build()
        );

        this.addDrawableChild(new ButtonWidget(
                center - 60,
                y += 30,
                120,
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
                center - 65,
                y + 30,
                130,
                20,
                Text.literal("Marker Text: " + (showProximityMessages ? "On" : "Off")),
                button -> {
                    showProximityMessages = !showProximityMessages;
                    Paths.showProximityMessages(showProximityMessages);
                    ProximityMessageRenderer.clearMessage();
                    button.setMessage(Text.literal("Marker Text: " + (showProximityMessages ? "On" : "Off")));
                },
                Supplier::get
        ));

        pathDropdown.setOnSelect(path -> {
            if (!path.getId().equalsIgnoreCase(selectedPathId)) {
                TrailRenderer.clearTrails();
            }

            Paths.setSelectedPath(path.getId());
            selectedPathId = path.getId();
            pathDisplay.setText(Text.literal("You are currently on ").append(Text.literal(path.getName()).fillStyle(Style.EMPTY.withColor(path.getColor().asHex()))));
            List<ChapterData> chapters = path.getChapters();
            chapterDropdown.setOptions(chapters);
            chapterDropdown.setSelected(null);
        });
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
    }
}
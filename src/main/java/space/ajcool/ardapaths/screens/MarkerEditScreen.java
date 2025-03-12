package space.ajcool.ardapaths.screens;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.EditBoxWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import space.ajcool.ardapaths.ArdaPathsClient;
import space.ajcool.ardapaths.core.data.config.shared.ChapterData;
import space.ajcool.ardapaths.core.data.config.shared.PathData;
import space.ajcool.ardapaths.core.networking.packets.server.ChapterStartRemovePacket;
import space.ajcool.ardapaths.core.networking.packets.server.ChapterStartUpdatePacket;
import space.ajcool.ardapaths.core.networking.packets.server.PathMarkerUpdatePacket;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;
import space.ajcool.ardapaths.core.networking.PacketRegistry;
import space.ajcool.ardapaths.screens.builders.CheckboxBuilder;
import space.ajcool.ardapaths.screens.builders.DropdownBuilder;
import space.ajcool.ardapaths.screens.builders.TextBuilder;
import space.ajcool.ardapaths.screens.widgets.CheckboxWidget;
import space.ajcool.ardapaths.screens.widgets.DropdownWidget;
import space.ajcool.ardapaths.core.Client;

import java.util.function.Supplier;

@Environment(value = EnvType.CLIENT)
public class MarkerEditScreen extends Screen {
    private final PathMarkerBlockEntity MARKER;

    private String selectedPathId;
    private String selectedChapterId;
    private boolean isChapterStart;
    private String proximityMessage;
    private int activationRange;
    private boolean displayAboveBlocks;
    private EditBoxWidget multiLineEditBox;

    public MarkerEditScreen(PathMarkerBlockEntity marker) {
        super(Text.literal("Path Marker Edit Screen"));
        MARKER = marker;

        this.selectedPathId = ArdaPathsClient.CONFIG.getSelectedPathId();
        PathMarkerBlockEntity.NbtData data = marker.getNbt(selectedPathId);
        this.selectedChapterId = data.getChapterId();
        this.isChapterStart = data.isChapterStart();
        this.proximityMessage = data.getProximityMessage();
        this.activationRange = data.getActivationRange();
        this.displayAboveBlocks = data.displayAboveBlocks();
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int currentY = 20;

        this.addDrawableChild(TextBuilder.create()
                .setPosition(centerX - 140, currentY)
                .setSize(280, 20)
                .setText(Text.literal("Edit Path Marker"))
                .build()
        );

        DropdownWidget<PathData> pathDropdown = this.addDrawableChild(DropdownBuilder.<PathData>create()
                .setPosition(centerX - 140, currentY += 40)
                .setSize(280, 20)
                .setTitle(Text.literal("Edit Data for Path:"))
                .setOptions(ArdaPathsClient.CONFIG.getPaths())
                .setOnSelect(path -> selectedPathId = path.getId())
                .setOptionDisplay(item -> {
                    if (item == null) return Text.literal("No Path");
                    return Text.literal(item.getName()).fillStyle(Style.EMPTY.withColor(item.getColor().asHex()));
                })
                .setSelected(ArdaPathsClient.CONFIG.getSelectedPath())
                .build()
        );

        PathMarkerBlockEntity.NbtData markerData = MARKER.getNbt(selectedPathId);
        ChapterData currentChapter = ArdaPathsClient.CONFIG.getPath(selectedPathId).getChapter(markerData.getChapterId());

        DropdownWidget<ChapterData> chapterDropdown = this.addDrawableChild(DropdownBuilder.<ChapterData>create()
                .setPosition(centerX - 140, currentY += 40)
                .setSize(280, 20)
                .setTitle(Text.literal("Chapter:"))
                .setOnSelect(chapter -> selectedChapterId = chapter.getId())
                .setOptionDisplay(item -> {
                    if (item == null) return Text.literal("No Chapter");
                    return Text.literal(item.getName());
                })
                .setOptions(ArdaPathsClient.CONFIG.getPath(selectedPathId).getChapters())
                .setSelected(currentChapter)
                .build()
        );

        CheckboxWidget isChapterStartButton = this.addDrawableChild(CheckboxBuilder.create()
                .setPosition(centerX - 140, currentY += 25)
                .setSize(15, 15)
                .setText(Text.literal("Is chapter start"))
                .setChecked(isChapterStart)
                .setOnChange(checked -> isChapterStart = checked)
                .build()
        );

        this.addDrawableChild(new ButtonWidget(
                centerX + 70,
                currentY,
                70,
                20,
                Text.literal("Edit Chapters"),
                button -> this.client.setScreen(new ChapterEditScreen(this)),
                Supplier::get
        ));

        this.multiLineEditBox = this.addDrawableChild(new EditBoxWidget(
                Client.mc().textRenderer,
                centerX - 140,
                currentY += 40,
                280,
                100,
                Text.literal("Add your message here..."),
                Text.empty()
        ));
        this.multiLineEditBox.setMaxLength(1000);
        this.multiLineEditBox.setChangeListener(string -> proximityMessage = string);
        this.multiLineEditBox.setText(proximityMessage);

        SliderWidget rangeWidget = this.addDrawableChild(new SliderWidget(
                centerX - 140,
                currentY += 115,
                280,
                20,
                ScreenTexts.EMPTY,
                activationRange / 100.0
        ) {
            {
                this.updateMessage();
            }

            @Override
            protected void updateMessage() {
                this.setMessage(Text.literal("Activation Range: " + activationRange));
            }

            @Override
            protected void applyValue() {
                activationRange = MathHelper.floor(MathHelper.clampedLerp(0.0, 100.0, this.value));
            }
        });

        CheckboxWidget displayAboveBlocksButton = this.addDrawableChild(CheckboxBuilder.create()
                .setPosition(centerX - 140, currentY += 30)
                .setSize(15, 15)
                .setText(Text.literal("Display trail above blocks"))
                .setChecked(displayAboveBlocks)
                .setOnChange(checked -> displayAboveBlocks = checked)
                .build()
        );

        this.addDrawableChild(new ButtonWidget(
                centerX - 75,
                currentY + 35,
                150,
                20,
                Text.literal("Done"),
                button -> close(),
                Supplier::get
        ));

        pathDropdown.setOnSelect(path -> {
            selectedPathId = path.getId();
            chapterDropdown.setOptions(path.getChapters());

            PathMarkerBlockEntity.NbtData data = MARKER.getNbt(selectedPathId);
            chapterDropdown.setSelected(path.getChapter(data.getChapterId()));

            if (chapterDropdown.getSelected() != null) {
                selectedChapterId = chapterDropdown.getSelected().getId();
                isChapterStart = data.isChapterStart();
                isChapterStartButton.setChecked(isChapterStart);
                displayAboveBlocks = data.displayAboveBlocks();
                displayAboveBlocksButton.setChecked(displayAboveBlocks);
            } else {
                selectedChapterId = "";
                isChapterStart = false;
                isChapterStartButton.setChecked(false);
                displayAboveBlocks = true;
                displayAboveBlocksButton.setChecked(true);
            }

            this.multiLineEditBox.setText(data.getProximityMessage());
            rangeWidget.setValue(data.getActivationRange() / 100.0);
        });

        chapterDropdown.setOnSelect(chapter -> {
            if (chapter == null) {
                selectedChapterId = "";
                isChapterStart = false;
                isChapterStartButton.setChecked(false);
                return;
            }
            selectedChapterId = chapter.getId();
        });
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);

        int centerX = this.width / 2;
        int currentY = 60;
        context.drawTextWithShadow(this.textRenderer, Text.literal("Proximity Message:"), centerX - 140, currentY + 93, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(mouseX, mouseY, button) || this.multiLineEditBox.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void tick() {
        this.multiLineEditBox.tick();
        super.tick();
    }

    @Override
    public void close() {
        super.close();

        if (!selectedPathId.isEmpty()) {
            PathMarkerBlockEntity.NbtData data = MARKER.getNbt(selectedPathId);
            String previousChapterId = data.getChapterId();

            data.setChapterId(selectedChapterId);
            data.setProximityMessage(proximityMessage);
            data.setActivationRange(activationRange);
            data.setChapterStart(isChapterStart);
            data.setDisplayAboveBlocks(displayAboveBlocks);

            if (isChapterStart) {
                ChapterStartUpdatePacket packet = new ChapterStartUpdatePacket(selectedPathId, selectedChapterId, MARKER.getPos());
                PacketRegistry.CHAPTER_START_UPDATE.send(packet);
            } else if (!previousChapterId.isEmpty()) {
                ChapterStartRemovePacket packet = new ChapterStartRemovePacket(selectedPathId, previousChapterId);
                PacketRegistry.CHAPTER_START_REMOVE.send(packet);
            }

            PathMarkerUpdatePacket packet = new PathMarkerUpdatePacket(MARKER.getPos(), MARKER.toNbt());
            PacketRegistry.PATH_MARKER_UPDATE.send(packet);
            MARKER.markUpdated();
        }
    }
}

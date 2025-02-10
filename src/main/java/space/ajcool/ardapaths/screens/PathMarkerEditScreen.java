package space.ajcool.ardapaths.screens;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.EditBoxWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import space.ajcool.ardapaths.ArdaPathsClient;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;
import space.ajcool.ardapaths.mc.networking.PacketRegistry;
import space.ajcool.ardapaths.screens.widgets.dropdowns.ChapterDropdownWidget;
import space.ajcool.ardapaths.screens.widgets.dropdowns.PathDropdownWidget;
import space.ajcool.ardapaths.utils.Client;

import java.util.function.Supplier;

@Environment(value = EnvType.CLIENT)
public class PathMarkerEditScreen extends Screen {
    private final PathMarkerBlockEntity MARKER;

    private String selectedPathId;
    private String selectedChapterId;
    private boolean isChapterStart;
    private String proximityMessage;
    private int activationRange;
    private EditBoxWidget multiLineEditBox;

    public PathMarkerEditScreen(PathMarkerBlockEntity marker) {
        super(Text.literal("Path Marker Edit Screen"));
        MARKER = marker;

        this.selectedPathId = ArdaPathsClient.CONFIG.getSelectedPathId();
        PathMarkerBlockEntity.NbtData data = marker.getNbt(selectedPathId);
        this.selectedChapterId = data.getChapterId();
        this.isChapterStart = data.isChapterStart();
        this.proximityMessage = data.getProximityMessage();
        this.activationRange = data.getActivationRange();
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int currentY = 60;

        PathDropdownWidget pathDropdown = this.addDrawableChild(new PathDropdownWidget(
                centerX - 140,
                currentY,
                280,
                20,
                selectedPathId
        ));

        ChapterDropdownWidget chapterDropdown = this.addDrawableChild(new ChapterDropdownWidget(
                centerX - 140,
                currentY += 40,
                280,
                20,
                selectedChapterId
        ));
        chapterDropdown.setHasNullItem(true);
        System.out.println("selectedChapterId: " + selectedChapterId);

        int plusButtonX = (centerX + 140) + 3;
        int pencilButtonX = plusButtonX + 22;
        this.addDrawableChild(new ButtonWidget(
                plusButtonX,
                currentY,
                20,
                20,
                Text.literal("+"),
                button -> this.client.setScreen(new ChapterEditScreen(this)),
                Supplier::get
        ));
        this.addDrawableChild(new ButtonWidget(
                pencilButtonX,
                currentY,
                20,
                20,
                Text.literal("✎"),
                button -> this.client.setScreen(new ChapterEditScreen(this, chapterDropdown.getSelected())),
                Supplier::get
        ));

        ButtonWidget isChapterStartButton = this.addDrawableChild(new ButtonWidget(
                centerX - 140,
                currentY += 25,
                120,
                20,
                Text.literal("Is Chapter Start: " + (isChapterStart ? "Yes" : "No")),
                button -> {
                    isChapterStart = !isChapterStart;
                    button.setMessage(Text.literal("Is Chapter Start: " + (isChapterStart ? "Yes" : "No")));
                },
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

        this.addDrawableChild(new ButtonWidget(
                centerX - 75,
                currentY + 35,
                150,
                20,
                Text.literal("Done"),
                button -> close(),
                Supplier::get
        ));

        pathDropdown.setItemSelectedListener(path -> {
            selectedPathId = path.getId();
            chapterDropdown.setOptions(path.getChapters());
            chapterDropdown.setSelected(ArdaPathsClient.CONFIG.getCurrentChapter());

            PathMarkerBlockEntity.NbtData data = MARKER.getNbt(selectedPathId);
            if (chapterDropdown.getSelected() != null) {
                selectedChapterId = chapterDropdown.getSelected().getId();
                isChapterStart = data.isChapterStart();
                isChapterStartButton.setMessage(Text.literal("Is Chapter Start: " + (isChapterStart ? "Yes" : "No")));
            }
            this.multiLineEditBox.setText(data.getProximityMessage());
            rangeWidget.setValue(data.getActivationRange() / 100.0);
        });

        chapterDropdown.setItemSelectedListener(chapter -> {
            if (chapter == null) {
                selectedChapterId = "";
                isChapterStart = false;
                return;
            }
            selectedChapterId = chapter.getId();

        });
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.literal("Edit Path Marker"),
                this.width / 2,
                20,
                0xFFFFFF
        );

        int centerX = this.width / 2;
        int currentY = 60;
        context.drawTextWithShadow(this.textRenderer, Text.literal("Edit Data for Path:"), centerX - 140, currentY - 12, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, Text.literal("Chapter:"), centerX - 140, currentY + 28, 0xFFFFFF);
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

            if (!selectedChapterId.isEmpty() && isChapterStart) {
                data.setChapterStart(true);
                PacketRegistry.CHAPTER_START_UPDATE.sendToServer(selectedPathId, selectedChapterId, MARKER.getPos());
            } else if (selectedChapterId.isEmpty() && !previousChapterId.isEmpty()) {
                data.setChapterStart(false);
                PacketRegistry.CHAPTER_START_UPDATE.sendToServer(selectedPathId, previousChapterId, null);
            }

            PacketRegistry.PATH_MARKER_UPDATE.sendToServer(MARKER.getPos(), MARKER.toNbt());
            MARKER.markUpdated();
        }
    }
}

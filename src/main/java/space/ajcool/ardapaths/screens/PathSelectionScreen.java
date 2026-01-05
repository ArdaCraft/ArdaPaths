package space.ajcool.ardapaths.screens;


import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;
import space.ajcool.ardapaths.ArdaPathsClient;
import space.ajcool.ardapaths.core.data.Journal;
import space.ajcool.ardapaths.core.data.config.shared.ChapterData;
import space.ajcool.ardapaths.core.data.config.shared.Color;
import space.ajcool.ardapaths.core.data.config.shared.PathData;
import space.ajcool.ardapaths.paths.Paths;
import space.ajcool.ardapaths.paths.rendering.ProximityMessageRenderer;
import space.ajcool.ardapaths.paths.rendering.ProximityTitleRenderer;
import space.ajcool.ardapaths.paths.rendering.TrailRenderer;
import space.ajcool.ardapaths.paths.rendering.objects.AnimatedMessage;
import space.ajcool.ardapaths.screens.builders.CheckboxBuilder;
import space.ajcool.ardapaths.screens.builders.DropdownBuilder;
import space.ajcool.ardapaths.screens.builders.TextBuilder;
import space.ajcool.ardapaths.screens.widgets.CheckboxWidget;
import space.ajcool.ardapaths.screens.widgets.DropdownWidget;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

@Environment(value = EnvType.CLIENT)
public class PathSelectionScreen extends Screen
{
    private static final int COLUMNS_SPACING = 10;
    private static final int UI_ELEMENT_SPACING = 10;
    private static final int UI_ELEMENT_WIDTH = 155;
    private static final int UI_ELEMENT_HEIGHT = 20;
    private static final int UI_SEPARATOR_SPACING = 10;;
    private static final int TITLE_SPACING = 20;

    private String selectedPathId;
    private String selectedChapterId;
    private boolean showProximityMessages;
    private boolean showChapterTitles;
    private final double proximityTextSpeedMultiplier;
    private final float titleDisplaySpeed;

    private SliderWidget proximityTextSpeedSlider;
    private SliderWidget titleDisplaySpeedSlider;

    public PathSelectionScreen()
    {
        super(Text.literal(Text.translatable("ardapaths.client.configuration.screens.path_selection").getString()));
        this.selectedPathId = ArdaPathsClient.CONFIG.getSelectedPathId();
        this.selectedChapterId = ArdaPathsClient.CONFIG.getCurrentChapterId();
        this.showProximityMessages = ArdaPathsClient.CONFIG.showProximityMessages();
        this.showChapterTitles = ArdaPathsClient.CONFIG.showChapterTitles();
        this.proximityTextSpeedMultiplier = ArdaPathsClient.CONFIG.getProximityTextSpeedMultiplier();
        this.titleDisplaySpeed = ArdaPathsClient.CONFIG.getChapterTitleDisplaySpeed();
    }

    @Override
    protected void init()
    {
        var totalUiHeight = (UI_ELEMENT_HEIGHT * 6) + (UI_ELEMENT_SPACING * 5) + UI_SEPARATOR_SPACING + TITLE_SPACING;

        int center = width / 2;
        int y = (height / 2) - (totalUiHeight / 2);

        PathData currentPath = ArdaPathsClient.CONFIG.getPath(selectedPathId);
        ChapterData currentChapter = ArdaPathsClient.CONFIG.getCurrentChapter();

        String currentChapterName = currentChapter != null ? currentChapter.getName() : "0";
        String currentPathName = currentPath != null ? currentPath.getName() : Text.translatable("ardapaths.client.configuration.screens.generic_path").toString();

        this.addDrawableChild(TextBuilder.create()
                .setPosition(center - 75, y)
                .setSize(150, 20)
                .setText(Text.literal(Text.translatable("ardapaths.client.configuration.screens.path_selection.current_path_chapter",currentChapterName).getString())
                        .append(Text.literal(Text.translatable(currentPathName).getString())
                                .fillStyle(Style.EMPTY.withColor(currentPath != null ? currentPath.getPrimaryColor().asHex() : Color.fromRgb(100, 100, 100).asHex()))))
                .build()
        );

        var horizontalHalfCenterGap = COLUMNS_SPACING /2;
        var uiElementVerticalGap = UI_ELEMENT_HEIGHT + UI_ELEMENT_SPACING;

        this.addDrawableChild(initializePathSelectionDropDown(center - UI_ELEMENT_WIDTH - horizontalHalfCenterGap, y += uiElementVerticalGap + TITLE_SPACING));
        this.addDrawableChild(this.initializeChapterSelectionDropDown(center + horizontalHalfCenterGap, y, currentPath, currentChapter));
        this.addDrawableChild(initializeReturnToChapterStartButton(center - UI_ELEMENT_WIDTH - horizontalHalfCenterGap,y+= uiElementVerticalGap));
        this.addDrawableChild(initializeReturnToPathButton(center + horizontalHalfCenterGap,y));

        // Horizontal Gap

        this.addDrawableChild(initializeProximityTextToggle(center - UI_ELEMENT_HEIGHT - horizontalHalfCenterGap,y += uiElementVerticalGap + UI_SEPARATOR_SPACING));
        proximityTextSpeedSlider = this.addDrawableChild(initializeProximityTextSpeedMultiplierSlider(center + horizontalHalfCenterGap, y));
        this.addDrawableChild(initializeChapterTitleDisplayToggle(center - UI_ELEMENT_HEIGHT - horizontalHalfCenterGap,y += uiElementVerticalGap));
        titleDisplaySpeedSlider = this.addDrawableChild(initializeTitleDisplaySpeedSlider(center + horizontalHalfCenterGap, y));

        this.addDrawableChild(initializeJournalButton(center - (UI_ELEMENT_WIDTH / 2),y + uiElementVerticalGap));
    }

    private @NotNull DropdownWidget<PathData> initializePathSelectionDropDown(int center, int y) {
        DropdownWidget<PathData> pathSelectionDropdown = DropdownBuilder.<PathData>create()
                .setPosition(center,y)
                .setSize(UI_ELEMENT_WIDTH, UI_ELEMENT_HEIGHT)
                .setTitle(Text.translatable( "ardapaths.client.configuration.screens.select_path_follow"))
                .setOptions(ArdaPathsClient.CONFIG.getPaths())
                .setOptionDisplay(item ->
                {
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

                    // Reset last visited node data
                    ArdaPathsClient.lastVisitedTrailNodeData = null;

                    selectedPathId = path.getId();
                    selectedChapterId = path.getChapterIds().get(0);

                    Paths.setSelectedPath(selectedPathId);
                    Paths.gotoChapter(selectedChapterId, false);

                    this.clearAndInit();
                })

                .build();
        pathSelectionDropdown.setTooltip(Tooltip.of(Text.translatable("ardapaths.client.configuration.screens.select_path_follow_tooltip")));
        return pathSelectionDropdown;
    }

    private @NotNull DropdownWidget<ChapterData> initializeChapterSelectionDropDown(int center, int y, PathData currentPath, ChapterData currentChapter) {

        List<ChapterData> chapterData = currentPath != null ? new ArrayList<>(currentPath.getChapters()) : new ArrayList<>();
        chapterData.sort(Comparator.comparingInt(ChapterData::getIndex));

        return DropdownBuilder.<ChapterData>create()
                .setPosition(center, y)
                .setSize(UI_ELEMENT_WIDTH, UI_ELEMENT_HEIGHT)
                .setTitle(Text.translatable("ardapaths.client.configuration.screens.select_chapter"))
                .setOptions(chapterData)
                .setOptionDisplay(item ->
                {
                    if (item == null) return Text.translatable("ardapaths.client.configuration.screens.no_chapter");
                    return Text.literal(item.getName());
                })
                .setSelected(currentChapter)
                .setOnSelect(chapter ->
                {
                    selectedChapterId = chapter == null ? "default" : chapter.getId();

                    // Reset last visited node data
                    ArdaPathsClient.lastVisitedTrailNodeData = null;

                    Paths.gotoChapter(selectedChapterId, false);

                    this.clearAndInit();
                })
                .build();
    }

    private @NotNull ButtonWidget initializeReturnToPathButton(int center, int y) {
        ButtonWidget returnToPathButton = new ButtonWidget(
                center, y,
                UI_ELEMENT_WIDTH,
                UI_ELEMENT_HEIGHT,
                Text.literal(Text.translatable("ardapaths.client.configuration.screens.return_path").getString()),
                button ->
                {
                    ArdaPathsClient.callingForTeleport = true;
                    TrailRenderer.clearTrails();
                    this.close();
                },
                Supplier::get
        );
        returnToPathButton.setTooltip(Tooltip.of(Text.translatable("ardapaths.client.configuration.screens.return_path_tooltip")));

        return  returnToPathButton;
    }

    private @NotNull ButtonWidget initializeReturnToChapterStartButton(int center, int y) {

        ButtonWidget returnChapterStartButton = new ButtonWidget(
                center, y,
                UI_ELEMENT_WIDTH,
                UI_ELEMENT_HEIGHT,
                Text.literal(Text.translatable("ardapaths.client.configuration.screens.return_chapter_start").getString()),
                button ->
                {
                    this.close();
                    if (!selectedPathId.isEmpty() && !selectedChapterId.isEmpty())
                    {
                        ProximityTitleRenderer.clearMessage();
                        Paths.gotoChapter(selectedChapterId);
                    }
                },
                Supplier::get
        );
        returnChapterStartButton.setTooltip(Tooltip.of(Text.translatable("ardapaths.client.configuration.screens.return_chapter_start_tooltip")));

        return returnChapterStartButton;
    }

    private @NotNull ButtonWidget initializeJournalButton(int x, int y) {

        ButtonWidget journalButton = new ButtonWidget(
                x, y,
                UI_ELEMENT_WIDTH,
                UI_ELEMENT_HEIGHT,
                Text.literal(Text.translatable("ardapaths.client.journal.screen.title").getString()),
                button ->
                {
                    this.close();
                    this.client.setScreen(new JournalScreen());
                },
                Supplier::get
        );
        journalButton.setTooltip(Tooltip.of(Text.translatable("ardapaths.client.journal.screen.title.tooltip")));
        journalButton.active = !Journal.getEntries().isEmpty();

        return journalButton;
    }

    private @NotNull CheckboxWidget initializeChapterTitleDisplayToggle(int center, int y) {

        CheckboxWidget chapterTitleDisplayToggle = CheckboxBuilder.create()
                .setPosition(center,y)
                .setSize(UI_ELEMENT_HEIGHT, UI_ELEMENT_HEIGHT)
                .setText(Text.translatable("ardapaths.client.configuration.screens.chapter_titles", (showChapterTitles ? Text.translatable("ardapaths.generic.on"):Text.translatable("ardapaths.generic.off"))))
                .setChecked(showChapterTitles)
                .setOnChange(checked -> {
                    showChapterTitles = checked;
                    titleDisplaySpeedSlider.active = checked;
                    Paths.showChapterTitles(showChapterTitles);
                    ProximityMessageRenderer.clearMessage();
                    ProximityTitleRenderer.clearMessage();
                })
                .build();

        chapterTitleDisplayToggle.setTooltip(Tooltip.of(Text.translatable("ardapaths.client.configuration.screens.chapter_titles_tooltip")));

        return chapterTitleDisplayToggle;
    }

    private @NotNull CheckboxWidget initializeProximityTextToggle(int center, int y) {

        CheckboxWidget proximityTextToggle = CheckboxBuilder.create()
                .setPosition(center,y)
                .setSize(UI_ELEMENT_HEIGHT, UI_ELEMENT_HEIGHT)
                .setText(Text.translatable("ardapaths.client.configuration.screens.proximity_text", (showProximityMessages ? Text.translatable("ardapaths.generic.on"):Text.translatable("ardapaths.generic.off"))))
                .setChecked(showProximityMessages)
                .setOnChange(checked -> {
                    showProximityMessages = checked;
                    proximityTextSpeedSlider.active = checked;
                    Paths.showProximityMessages(showProximityMessages);
                    ProximityMessageRenderer.clearMessage();
                    ProximityTitleRenderer.clearMessage();
                })
                .build();

        proximityTextToggle.setTooltip(Tooltip.of(Text.translatable("ardapaths.client.configuration.screens.proximity_text_tooltip")));

        return proximityTextToggle;
    }

    private @NotNull SliderWidget initializeProximityTextSpeedMultiplierSlider(int center, int y) {

        double proximityTextSpeedMultiplierPercent = (proximityTextSpeedMultiplier / AnimatedMessage.DEFAULT_PROXIMITY_TEXT_SPEED_MULTIPLIER) * 100.0;
        double proximityTextSpeedMultiplierClamped = MathHelper.clamp((proximityTextSpeedMultiplierPercent - 50.0) / 100.0, 0.0, 1.0);

        SliderWidget sliderWidget = new SliderWidget(
                center, y,
                UI_ELEMENT_WIDTH,
                UI_ELEMENT_HEIGHT,
                Text.literal(Text.translatable("ardapaths.client.configuration.screens.proximity_text_speed_multiplier").getString()),
                proximityTextSpeedMultiplierClamped
        ) {

            @Override
            protected void updateMessage() {
                int percent = (int) (50 + this.value * 100);
                this.setMessage(Text.literal(percent + "%"));
            }

            @Override
            protected void applyValue() {
                double percent = 50 + this.value * 100;
                Paths.setProximityMessagesSpeedMultiplier(AnimatedMessage.DEFAULT_PROXIMITY_TEXT_SPEED_MULTIPLIER * (percent / 100.0));
            }
        };
        sliderWidget.active = showProximityMessages;
        sliderWidget.setTooltip(Tooltip.of(Text.translatable("ardapaths.client.configuration.screens.proximity_text_speed_multiplier_tooltip")));
        return sliderWidget;
    }

    private @NotNull SliderWidget initializeTitleDisplaySpeedSlider(int center, int y) {

        double titleFadeDelaySeconds = titleDisplaySpeed / 1000;
        double titleFadeDelayClamped = MathHelper.clamp((titleFadeDelaySeconds - 1.0) / 4.0, 0.0, 1.0);

        SliderWidget sliderWidget = new SliderWidget(
                center, y,
                UI_ELEMENT_WIDTH,
                UI_ELEMENT_HEIGHT,
                Text.literal(Text.translatable("ardapaths.client.configuration.screens.chapter_title_speed_delay").getString()),
                titleFadeDelayClamped
        ) {

            @Override
            protected void updateMessage() {
                var seconds = (int)(1.0 + this.value * 4.0);
                this.setMessage(Text.literal(seconds + "s"));
            }

            @Override
            protected void applyValue() {

                float seconds = (float)(1.0 + this.value * 4.0);
                Paths.setChapterTitleDisplaySpeed(seconds * 1000);
            }
        };

        sliderWidget.active = showChapterTitles;
        sliderWidget.setTooltip(Tooltip.of(Text.translatable("ardapaths.client.configuration.screens.chapter_title_speed_delay_tooltip")));
        return sliderWidget;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta)
    {

        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
    }
}
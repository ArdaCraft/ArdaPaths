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
import space.ajcool.ardapaths.core.integration.DaylightCycles;
import space.ajcool.ardapaths.core.integration.Weathers;
import space.ajcool.ardapaths.paths.Paths;
import space.ajcool.ardapaths.paths.movement.AutoWalker;
import space.ajcool.ardapaths.paths.rendering.ProximityRenderer;
import space.ajcool.ardapaths.paths.rendering.TrailRenderer;
import space.ajcool.ardapaths.paths.rendering.objects.AnimatedMessage;
import space.ajcool.ardapaths.screens.layout.ScreenLayout;
import space.ajcool.ardapaths.screens.widgets.CheckboxWidget;
import space.ajcool.ardapaths.screens.widgets.DropdownWidget;
import space.ajcool.ardapaths.screens.widgets.TextWidget;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

/**
 * Screen for selecting and managing which path and chapter the player is currently following.
 * Provides controls for toggling trail display, proximity messages, chapter titles, waypoints,
 * and adjusting animation speeds and appearance settings.
 */
@Environment(value = EnvType.CLIENT)
public class PathSelectionScreen extends Screen
{
    /** Horizontal gap between left and right UI columns. */
    private static final int COLUMNS_SPACING = 10;

    /** Vertical spacing between consecutive UI elements. */
    private static final int UI_ELEMENT_SPACING = 10;

    /** Standard width for most UI elements. */
    private static final int UI_ELEMENT_WIDTH = 155;

    /** Standard height for most UI elements. */
    private static final int UI_ELEMENT_HEIGHT = 20;

    /** Vertical gap between UI sections. */
    private static final int UI_SEPARATOR_SPACING = 10;

    /** Vertical spacing below the title. */
    private static final int TITLE_SPACING = 20;

    /** ID of the currently selected path. */
    private String selectedPathId;

    /** ID of the currently selected chapter within the selected path. */
    private String selectedChapterId;

    /** Whether proximity messages are enabled for display. */
    private boolean showProximityMessages;

    /** Whether chapter titles are shown when entering a chapter. */
    private boolean showChapterTitles;

    /** Whether waypoint markers are displayed along the trail. */
    private boolean showTrailWaypoints;

    /**  Whether the client allows followed trail markers to change time and weather.*/
    private boolean useDynamicEnvironment;
    /** Multiplier applied to proximity message display speed. */

    private final double proximityTextSpeedMultiplier;
    /** Client preference controlling auto-walk speed relative to vanilla walking. */
    private final double autoWalkSpeedFactor;

    /** Duration in milliseconds that chapter titles remain visible on screen. */
    private final float titleDisplaySpeed;

    /** Slider widget for adjusting proximity message display speed. */
    private SliderWidget proximityTextSpeedSlider;

    /** Slider widget for adjusting chapter title display duration. */
    private SliderWidget titleDisplaySpeedSlider;

    /**
     * Initializes a new path selection screen with the current player's configuration state.
     */
    public PathSelectionScreen()
    {
        super(Text.literal(Text.translatable("ardapaths.client.configuration.screens.path_selection").getString()));
        this.selectedPathId = ArdaPathsClient.CONFIG.getSelectedPathId();
        this.selectedChapterId = ArdaPathsClient.CONFIG.getCurrentChapterId();
        this.showProximityMessages = ArdaPathsClient.CONFIG.showProximityMessages();
        this.showTrailWaypoints = ArdaPathsClient.CONFIG.showTrailWaypoints();
        this.showChapterTitles = ArdaPathsClient.CONFIG.showChapterTitles();
        this.useDynamicEnvironment = ArdaPathsClient.CONFIG.useDynamicEnvironment();
        this.proximityTextSpeedMultiplier = ArdaPathsClient.CONFIG.getProximityTextSpeedMultiplier();
        this.autoWalkSpeedFactor = ArdaPathsClient.CONFIG.getAutoWalkSpeedFactor();
        this.titleDisplaySpeed = ArdaPathsClient.CONFIG.getChapterTitleDisplaySpeed();
    }

    /**
     * Initializes and lays out all UI elements for the path selection screen.
     */
    @Override
    protected void init()
    {
        int center = width / 2;
        int y = 0;

        PathData currentPath = ArdaPathsClient.CONFIG.getPath(selectedPathId);
        ChapterData currentChapter = ArdaPathsClient.CONFIG.getCurrentChapter();

        String currentChapterName = currentChapter != null ? currentChapter.getName() : "0";
        String currentPathName = currentPath != null ? currentPath.getName() : Text.translatable("ardapaths.client.configuration.screens.generic_path").toString();

        this.addDrawableChild(TextWidget.create()
                        .setX(center - 75)
                        .setY(y)
                        .setWidth(150)
                        .setHeight(20)
                        .setMessage(Text.literal(Text.translatable("ardapaths.client.configuration.screens.path_selection.current_path_chapter",currentChapterName).getString())
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

        this.addDrawableChild(initializeProximityTextToggle(center - UI_ELEMENT_WIDTH - horizontalHalfCenterGap,y += uiElementVerticalGap + UI_SEPARATOR_SPACING));
        proximityTextSpeedSlider = this.addDrawableChild(initializeProximityTextSpeedMultiplierSlider(center + horizontalHalfCenterGap, y));
        this.addDrawableChild(initializeChapterTitleDisplayToggle(center - UI_ELEMENT_WIDTH - horizontalHalfCenterGap,y += uiElementVerticalGap));
        titleDisplaySpeedSlider = this.addDrawableChild(initializeTitleDisplaySpeedSlider(center + horizontalHalfCenterGap, y));

        this.addDrawableChild(initializeAutoWalkSpeedSlider(center - UI_ELEMENT_WIDTH - horizontalHalfCenterGap, y += uiElementVerticalGap));
        this.addDrawableChild(initializeShowTrailWaypointsToggle(center - UI_ELEMENT_WIDTH - horizontalHalfCenterGap,y += uiElementVerticalGap));

        if (Weathers.isAvailable() || DaylightCycles.isAvailable()) {
            this.addDrawableChild(initializeDynamicEnvironmentToggle(center - UI_ELEMENT_WIDTH - horizontalHalfCenterGap, y += uiElementVerticalGap));
        }

        this.addDrawableChild(initializeJournalButton(center - (UI_ELEMENT_WIDTH / 2),y + uiElementVerticalGap));

        ScreenLayout.centerVertically(this);
    }

    /**
     * Creates the dropdown for selecting which path to follow.
     *
     * @param center the x coordinate of the dropdown center
     * @param y the y coordinate of the dropdown
     * @return the configured path selection dropdown
     */
    private @NotNull DropdownWidget<PathData> initializePathSelectionDropDown(int center, int y) {
        DropdownWidget<PathData> pathSelectionDropdown = DropdownWidget.<PathData>create()
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

    /**
     * Creates the dropdown for selecting which chapter to follow within the current path.
     *
     * @param center the x coordinate of the dropdown center
     * @param y the y coordinate of the dropdown
     * @param currentPath the path data for populating available chapters
     * @param currentChapter the currently selected chapter
     * @return the configured chapter selection dropdown
     */
    private @NotNull DropdownWidget<ChapterData> initializeChapterSelectionDropDown(int center, int y, PathData currentPath, ChapterData currentChapter) {

        List<ChapterData> chapterData = currentPath != null ? new ArrayList<>(currentPath.getChapters()) : new ArrayList<>();
        chapterData.sort(Comparator.comparingInt(ChapterData::getIndex));

        return DropdownWidget.<ChapterData>create()
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

    /**
     * Creates the button to teleport the player to the start of the current path.
     *
     * @param center the x coordinate of the button center
     * @param y the y coordinate of the button
     * @return the configured return to path button
     */
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

    /**
     * Creates the button to teleport the player to the start of the current chapter.
     *
     * @param center the x coordinate of the button center
     * @param y the y coordinate of the button
     * @return the configured return to chapter start button
     */
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
                        ProximityRenderer.clear();
                        Paths.gotoChapter(selectedChapterId);
                    }
                },
                Supplier::get
        );
        returnChapterStartButton.setTooltip(Tooltip.of(Text.translatable("ardapaths.client.configuration.screens.return_chapter_start_tooltip")));

        return returnChapterStartButton;
    }

    /**
     * Creates the button to open the proximity message journal.
     *
     * @param x the x coordinate of the button
     * @param y the y coordinate of the button
     * @return the configured journal button
     */
    private @NotNull ButtonWidget initializeJournalButton(int x, int y) {

        ButtonWidget journalButton = new ButtonWidget(
                x, y,
                UI_ELEMENT_WIDTH,
                UI_ELEMENT_HEIGHT,
                Text.literal(Text.translatable("ardapaths.client.journal.screen.title").getString()),
                button ->
                {
                    this.close();
                    if (this.client != null)
                        this.client.setScreen(new JournalScreen());
                },
                Supplier::get
        );
        journalButton.setTooltip(Tooltip.of(Text.translatable("ardapaths.client.journal.screen.title.tooltip")));
        journalButton.active = !Journal.getEntries().isEmpty();

        return journalButton;
    }

    /**
     * Creates the checkbox to toggle chapter title display.
     *
     * @param x the x coordinate of the checkbox
     * @param y the y coordinate of the checkbox
     * @return the configured chapter title display toggle
     */
    private @NotNull CheckboxWidget initializeChapterTitleDisplayToggle(int x, int y) {

        CheckboxWidget chapterTitleDisplayToggle = CheckboxWidget.create()
                .setX(x)
                .setY(y)
                .setWidth(UI_ELEMENT_WIDTH)
                .setHeight(UI_ELEMENT_HEIGHT)
                .setText(Text.translatable("ardapaths.client.configuration.screens.chapter_titles", (showChapterTitles ? Text.translatable("ardapaths.generic.on"):Text.translatable("ardapaths.generic.off"))))
                .setChecked(showChapterTitles)
                .setEnabled(true)
                .setOnChange(checked -> {
                    showChapterTitles = checked;
                    titleDisplaySpeedSlider.active = checked;
                    Paths.showChapterTitles(showChapterTitles);
                    ProximityRenderer.clear();
                })
                .build();

        chapterTitleDisplayToggle.setTooltip(Tooltip.of(Text.translatable("ardapaths.client.configuration.screens.chapter_titles_tooltip")));

        return chapterTitleDisplayToggle;
    }

    /**
     * Creates the checkbox to toggle proximity message display.
     *
     * @param x the x coordinate of the checkbox
     * @param y the y coordinate of the checkbox
     * @return the configured proximity text toggle
     */
    private @NotNull CheckboxWidget initializeProximityTextToggle(int x, int y) {

        CheckboxWidget proximityTextToggle = CheckboxWidget.create()
                .setX(x)
                .setY(y)
                .setWidth(UI_ELEMENT_WIDTH)
                .setHeight(UI_ELEMENT_HEIGHT)
                .setText(Text.translatable("ardapaths.client.configuration.screens.proximity_text", (showProximityMessages ? Text.translatable("ardapaths.generic.on"):Text.translatable("ardapaths.generic.off"))))
                .setChecked(showProximityMessages)
                .setEnabled(true)
                .setOnChange(checked -> {
                    showProximityMessages = checked;
                    proximityTextSpeedSlider.active = checked;
                    Paths.showProximityMessages(showProximityMessages);
                    ProximityRenderer.clear();
                })
                .build();

        proximityTextToggle.setTooltip(Tooltip.of(Text.translatable("ardapaths.client.configuration.screens.proximity_text_tooltip")));

        return proximityTextToggle;
    }

    /**
     * Creates the checkbox to toggle trail waypoint display.
     *
     * @param x the x coordinate of the checkbox
     * @param y the y coordinate of the checkbox
     * @return the configured trail waypoint toggle
     */
    private @NotNull CheckboxWidget initializeShowTrailWaypointsToggle(int x, int y) {

        CheckboxWidget trailWaypointToggle = CheckboxWidget.create()
                .setX(x)
                .setY(y)
                .setWidth(UI_ELEMENT_WIDTH * 2 +COLUMNS_SPACING)
                .setHeight(UI_ELEMENT_HEIGHT)
                .setText(Text.translatable("ardapaths.client.configuration.screens.trail_waypoints", (showTrailWaypoints ? Text.translatable("ardapaths.generic.on"):Text.translatable("ardapaths.generic.off"))))
                .setChecked(showTrailWaypoints)
                .setEnabled(true)
                .setOnChange(checked -> {
                    showTrailWaypoints = checked;
                    Paths.showTrailWaypoints(showTrailWaypoints);
                    ProximityRenderer.clear();
                })
                .build();

        trailWaypointToggle.setTooltip(Tooltip.of(Text.translatable("ardapaths.client.configuration.screens.trail_waypoints.tooltip")));

        return trailWaypointToggle;
    }

    /**
     * Creates the toggle used to opt into dynamic time and weather changes from trail markers.
     *
     * @param x      the left edge of the checkbox row
     * @param y      the checkbox y coordinate
     * @return the configured dynamic environment toggle
     */
    private @NotNull CheckboxWidget initializeDynamicEnvironmentToggle(int x, int y) {

        CheckboxWidget dynamicEnvironmentToggle = CheckboxWidget.create()
                .setX(x)
                .setY(y)
                .setWidth(UI_ELEMENT_WIDTH * 2 +COLUMNS_SPACING)
                .setHeight(UI_ELEMENT_HEIGHT)
                .setText(Text.translatable("ardapaths.client.configuration.screens.dynamic_environment"))
                .setChecked(useDynamicEnvironment)
                .setEnabled(true)
                .setOnChange(checked -> {
                    useDynamicEnvironment = checked;
                    Paths.useDynamicEnvironment(useDynamicEnvironment);
                })
                .build();

        dynamicEnvironmentToggle.setTooltip(Tooltip.of(Text.translatable("ardapaths.client.configuration.screens.dynamic_environment_tooltip")));

        return dynamicEnvironmentToggle;
    }

    /**
     * Creates the slider for adjusting proximity message display speed.
     *
     * @param center the x coordinate of the slider center
     * @param y the y coordinate of the slider
     * @return the configured proximity text speed slider
     */
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

    /**
     * Creates the slider for adjusting chapter title display duration.
     *
     * @param center the x coordinate of the slider center
     * @param y the y coordinate of the slider
     * @return the configured title display speed slider
     */
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

    /**
     * Creates the slider used to adjust auto-walk speed.
     *
     * @param center the slider x coordinate
     * @param y      the slider y coordinate
     * @return the configured auto-walk speed slider
     */
    private @NotNull SliderWidget initializeAutoWalkSpeedSlider(int center, int y) {

        double autoWalkPercent = (autoWalkSpeedFactor / AutoWalker.DEFAULT_AUTO_WALK_SPEED_FACTOR) * 100.0D;
        double autoWalkSpeedClamped = MathHelper.clamp((autoWalkPercent - 25.0D) / 150.0D, 0.0D, 1.0D);

        SliderWidget sliderWidget = new SliderWidget(
                center, y,
                UI_ELEMENT_WIDTH * 2 +COLUMNS_SPACING,
                UI_ELEMENT_HEIGHT,
                Text.literal(Text.translatable("ardapaths.client.configuration.screens.auto_walk_speed").getString()),
                autoWalkSpeedClamped
        ) {

            @Override
            protected void updateMessage() {
                int percent = (int)Math.round(25.0D + this.value * 150.0D);
                this.setMessage(Text.literal(percent + "%"));
            }

            @Override
            protected void applyValue() {
                double percent = 25.0D + this.value * 150.0D;
                Paths.setAutoWalkSpeedFactor(AutoWalker.DEFAULT_AUTO_WALK_SPEED_FACTOR * (percent / 100.0D));
            }
        };

        sliderWidget.setTooltip(Tooltip.of(Text.translatable("ardapaths.client.configuration.screens.auto_walk_speed_tooltip")));
        return sliderWidget;
    }

    /**
     * Renders the screen background and all UI elements.
     *
     * @param context the draw context for rendering
     * @param mouseX the current mouse x coordinate
     * @param mouseY the current mouse y coordinate
     * @param delta the partial tick delta for animation
     */
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta)
    {

        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
    }
}

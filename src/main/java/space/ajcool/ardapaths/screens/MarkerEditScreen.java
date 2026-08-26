package space.ajcool.ardapaths.screens;

import lombok.extern.slf4j.Slf4j;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.EditBoxWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;
import space.ajcool.ardapaths.ArdaPathsClient;
import space.ajcool.ardapaths.core.Client;
import space.ajcool.ardapaths.core.data.*;
import space.ajcool.ardapaths.core.data.config.shared.ChapterData;
import space.ajcool.ardapaths.core.data.config.shared.PathData;
import space.ajcool.ardapaths.core.networking.PacketRegistry;
import space.ajcool.ardapaths.core.networking.packets.client.ChapterPathMarkersResponsePacket;
import space.ajcool.ardapaths.core.networking.packets.client.MarkerBulkClearResponsePacket;
import space.ajcool.ardapaths.core.networking.packets.client.MarkerTimeSpreadResponsePacket;
import space.ajcool.ardapaths.core.networking.packets.client.PathMarkerRemoteDataResponsePacket;
import space.ajcool.ardapaths.core.networking.packets.server.*;
import space.ajcool.ardapaths.mc.blocks.ModBlocks;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;
import space.ajcool.ardapaths.paths.Paths;
import space.ajcool.ardapaths.screens.widgets.*;

import java.util.*;
import java.util.function.Supplier;

/**
 * Screen for editing a path marker block's configuration.
 * Allows editing proximity messages, activation ranges, chapter assignments, visual effects (fade, opacity),
 * chapter start positions, and animation parameters.
 */
@Environment(value = EnvType.CLIENT)
@Slf4j(topic = "ardapaths")
public class MarkerEditScreen extends Screen
{
    /** Total fixed layout height used to vertically place the marker editor. */
    private static final int TOTAL_HEIGHT = 356;

    /** Minimum top margin for short windows. */
    private static final int MIN_TOP_MARGIN = 5;

    /** Shared width for the main marker editor controls. */
    private static final int MAIN_CONTROL_WIDTH = 280;

    /** Standard control height for buttons, dropdowns, sliders, and the tab bar. */
    private static final int CONTROL_HEIGHT = 20;

    /** X offset from center for left-aligned main controls. */
    private static final int MAIN_LEFT_OFFSET = 140;

    /** Y offset for the path selector. */
    private static final int PATH_Y_OFFSET = 42;

    /** Y offset for the chapter selector and edit button. */
    private static final int CHAPTER_Y_OFFSET = 70;

    /** Y offset for chapter-start controls. */
    private static final int CHAPTER_START_Y_OFFSET = 100;

    /** Y offset for the tab bar. */
    private static final int TAB_BAR_Y_OFFSET = 122;

    /** Y offset for the active tab content area. */
    private static final int CONTENT_TOP_OFFSET = 155;

    /** Reserved height for the active tab content area. */
    private static final int CONTENT_HEIGHT = 164;

    /** Y offset for the fixed footer row. */
    private static final int FOOTER_Y_OFFSET = 329;

    /** Index of the tab containing proximity message and animation settings. */
    private static final int GENERAL_TAB_INDEX = 0;

    /** Index of the tab containing time and weather settings. */
    private static final int TIME_AND_WEATHER_TAB_INDEX = 1;

    /** Index of the tab containing server-executed marker action settings. */
    private static final int MISC_TAB_INDEX = 2;

    /** Horizontal spacing between the marker list divider and the centered editor. */
    private static final int MARKER_LIST_GUTTER = 40;

    /** The marker block entity being edited. */
    private final PathMarkerBlockEntity MARKER;

    /** ID of the currently selected path in the form. */
    private String selectedPathId;

    /** ID of the currently selected chapter in the form. */
    private String selectedChapterId;

    /** Whether this marker marks the start of a chapter. */
    private boolean isChapterStart;

    /** Whether to display the chapter title on the trail when this is a chapter start. */
    private boolean showChapterStartTitle;

    /** Text displayed to the player when they trigger this marker's proximity zone. */
    private String proximityMessage;

    /** Distance in blocks from the marker that triggers the proximity message. */
    private int activationRange;

    /** Whether the proximity message and marker should render above block surfaces. */
    private boolean displayAboveBlocks;

    /** Multi-line text editor for the proximity message. */
    private EditBoxWidget multiLineEditBox;

    /** Input field for character reveal speed parameter. */
    private InputBoxWidget charRevealInput;

    /** Input field for fade delay offset parameter. */
    private InputBoxWidget fadeDelayOffsetInput;

    /** Input field for fade delay factor parameter. */
    private InputBoxWidget fadeDelayFactorInput;

    /** Input field for fade speed parameter. */
    private InputBoxWidget fadeSpeedInput;

    /** Input field for minimum opacity parameter. */
    private InputBoxWidget minOpacityInput;

    /**Input box used to configure the marker's optional time-of-day setting.*/
    private InputBoxWidget timeOfDayInput;

    /** Input box used to configure the marker's time transition range. */
    private InputBoxWidget timeTransitionRangeInput;

    /** Input box used to configure the marker's optional auto-teleport target. */
    private InputBoxWidget autoTeleportTargetInput;

    /** Input box used to configure the marker's optional focus look-at target. */
    private InputBoxWidget lookAtInput;

    /** Input box used to configure the marker's optional item grant. */
    private InputBoxWidget giveItemInput;

    /** Checkbox controlling whether to show chapter title on the trail. */
    private CheckboxWidget displayChapterTitleOnTrail;

    /** Currently visible tab in the fixed content area. */
    private int activeTab;

    /** Whether the next initialization should reload form values from marker NBT. */
    private boolean reloadFromMarker = true;

    /** Left-hand marker navigation panel. */
    private MarkerListPanelWidget markerListPanel;

    /** Last visible marker signature used to avoid rebuilding the list every tick. */
    private long lastMarkerListSignature;

    /** Marker rows returned by the server for the selected chapter. */
    private List<ChapterMarkerEntry> serverMarkers = List.of();

    /** Whether the marker navigation column is using server-provided chapter rows. */
    private boolean serverListActive;

    /** Whether the marker navigation column has completed its first build for this screen. */
    private boolean markerListInitialised;

    /** Scroll amount to apply to the next rebuilt marker navigation column. */
    private Double pendingMarkerListScrollAmount;

    /** Marker positions selected in the visible list order for bulk operations. */
    private List<BlockPos> selectedMarkers = new ArrayList<>();

    /** Anchor marker used when Shift-click extends a list range. */
    private BlockPos selectionAnchor;

    /** Open context menu for marker-list operations. */
    private ContextMenuWidget contextMenu;

    /** Speed at which characters are revealed in the proximity message. */
    private int charRevealSpeed;

    /** Offset applied to the fade delay calculation. */
    private int fadeDelayOffset;

    /** Factor applied to the fade delay calculation. */
    private int fadeDelayFactor;

    /** Speed at which text fades out. */
    private int fadeSpeed;

    /** Minimum opacity of the text when fully faded. */
    private int minOpacity;
    /**
     * Weather ordinal selected for this marker, or unset when the player should keep current weather.
     */
    private int weather;
    /**
     * Time-of-day ticks selected for this marker, or unset when the player should keep current time.
     */
    private int timeOfDay;
    /**
     * Distance in blocks selected for transitioning to the marker's time of day.
     */
    private int timeTransitionRange;
    /**
     * Target coordinates or warp name triggered when a player reaches this marker.
     */
    private String autoTeleportTarget;
    /**
     * Target coordinates focused by the client while the Focus key is held.
     */
    private String lookAt;
    /**
     * Item identifier granted when a player reaches this marker.
     */
    private String giveItem;
    /**
     * Server feedback from the most recent time-spread request.
     */
    private Text timeSpreadFeedback;
    /**
     * Whether the current time-spread feedback represents an error state.
     */
    private boolean timeSpreadFeedbackError;

    /**
     * Feedback from the most recent marker-load request.
     */
    private Text markerLoadFeedback;

    /**
     * Whether the current marker-load feedback represents an error state.
     */
    private boolean markerLoadFeedbackError;

    /** Hash of the form state to detect if the user has made changes. */
    private int formHash;

    /** The original set of path/chapter associations before any changes. */
    private final Set<AbstractMap.SimpleEntry<String, String>> originalPathAndChapterData;

    /**
     * Initializes a marker edit screen with the given marker.
     *
     * @param marker the marker block entity to edit
     */
    public MarkerEditScreen(PathMarkerBlockEntity marker)
    {
        this(marker, null, List.of(marker.getPos().toImmutable()));
    }

    /**
     * Initializes a marker edit screen with the given marker and original path/chapter data.
     *
     * @param marker the marker block entity to edit
     * @param originalPathAndChapterData the original path/chapter associations, or null to track from current state
     */
    public MarkerEditScreen(PathMarkerBlockEntity marker, Set<AbstractMap.SimpleEntry<String, String>> originalPathAndChapterData)
    {
        this(marker, originalPathAndChapterData, List.of(marker.getPos().toImmutable()));
    }

    /**
     * Initializes a marker edit screen with the given marker, tracked associations, and selected rows.
     *
     * @param marker the marker block entity to edit
     * @param originalPathAndChapterData the original path/chapter associations, or null to track from current state
     * @param initialSelection visible marker rows that should remain selected
     */
    public MarkerEditScreen(PathMarkerBlockEntity marker, Set<AbstractMap.SimpleEntry<String, String>> originalPathAndChapterData, Collection<BlockPos> initialSelection)
    {
        super(Text.literal("Path Marker Edit Screen"));
        MARKER = marker;
        this.originalPathAndChapterData = originalPathAndChapterData != null ? originalPathAndChapterData : trackInitialPathAndChapterData();
        this.selectedMarkers = initialSelection == null || initialSelection.isEmpty()
                ? new ArrayList<>(List.of(marker.getPos().toImmutable()))
                : new ArrayList<>(initialSelection.stream().map(BlockPos::toImmutable).toList());
        this.selectionAnchor = marker.getPos().toImmutable();

        selectedPathId = ArdaPathsClient.CONFIG.getSelectedPathId();
        selectedChapterId = ArdaPathsClient.CONFIG.getCurrentChapterId();

        PathMarkerBlockEntity.ChapterNbtData data = marker.getChapterData(selectedPathId, selectedChapterId, true);
        assert data != null;

        initFormFields(data);
    }

    /**
     * Initializes and lays out all UI elements for the marker editor.
     */
    @Override
    protected void init()
    {
        super.init();
        resetTabWidgetReferences();

        PathMarkerBlockEntity.ChapterNbtData data = MARKER.getChapterData(selectedPathId, selectedChapterId, true);
        assert data != null;

        if (reloadFromMarker) {
            initFormFields(data);
        }

        int centerX = this.width / 2;
        int top = layoutTop();
        int contentTop = top + CONTENT_TOP_OFFSET;
        int footerY = top + FOOTER_Y_OFFSET;

        this.buildMarkerIdButton(centerX, top - 10);
        this.buildTitle(centerX, top + 10);
        this.buildPathSelectionDropdown(centerX - MAIN_LEFT_OFFSET, top + PATH_Y_OFFSET);
        this.buildChapterSelectionDropdown(centerX - MAIN_LEFT_OFFSET, top + CHAPTER_Y_OFFSET);
        this.buildEditChaptersButton(centerX + 40, top + CHAPTER_Y_OFFSET);
        this.buildChapterStartCheckbox(centerX - MAIN_LEFT_OFFSET, top + CHAPTER_START_Y_OFFSET);
        displayChapterTitleOnTrail = this.buildChapterStartHideTitleCheckbox(centerX + 5, top + CHAPTER_START_Y_OFFSET);
        this.buildTabBar(centerX - MAIN_LEFT_OFFSET, top + TAB_BAR_Y_OFFSET);

        if (activeTab == GENERAL_TAB_INDEX) {
            buildGeneralTab(centerX, contentTop);
        } else if (activeTab == TIME_AND_WEATHER_TAB_INDEX) {
            buildTimeAndWeatherTab(centerX, contentTop);
        } else if (activeTab == MISC_TAB_INDEX) {
            buildMiscTab(centerX, contentTop);
        }

        this.buildDisplayAboveBlocksCheckbox(centerX - MAIN_LEFT_OFFSET, footerY);
        this.buildCloseButton(centerX + 15, footerY - 2);
        this.buildSaveButton(centerX + 80, footerY - 2);
        this.buildMarkerList(top);
        this.buildMarkerLoadFeedback(centerX, footerY);

        if (reloadFromMarker) {
            formHash = calculateFormHash();
        }
        reloadFromMarker = true;
    }

    /**
     * Computes the top edge shared by the fixed marker editor layout.
     *
     * @return the top y coordinate for the marker editor content
     */
    private int layoutTop() {
        return Math.max(MIN_TOP_MARGIN, (this.height - TOTAL_HEIGHT) / 2);
    }

    /**
     * Populates form fields from the marker's NBT data for the selected path and chapter.
     *
     * @param data the chapter NBT data to extract values from
     */
    private void initFormFields(PathMarkerBlockEntity.ChapterNbtData data) {

        this.isChapterStart = data.isChapterStart();
        this.showChapterStartTitle = data.isDisplayChapterTitleOnTrail();
        this.proximityMessage = data.getProximityMessage();
        this.activationRange = data.getActivationRange();
        this.displayAboveBlocks = data.isDisplayAboveBlocks();

        var unpackedMessageData = BitPacker.unpackFive(data.getPackedMessageData());

        charRevealSpeed = unpackedMessageData[0];
        fadeDelayOffset = unpackedMessageData[1];
        fadeDelayFactor = unpackedMessageData[2];
        fadeSpeed = unpackedMessageData[3];
        minOpacity = unpackedMessageData[4];
        weather = data.getWeather();
        timeOfDay = data.getTimeOfDay();
        timeTransitionRange = data.getTimeTransitionRange();
        autoTeleportTarget = data.getAutoTeleportTarget();
        lookAt = WarpTarget.formatCoordinates(data.getLookAt());
        giveItem = data.getGiveItem();
    }

    /**
     * Clears widget references whose widgets may not be rebuilt for the next active tab.
     */
    private void resetTabWidgetReferences() {
        multiLineEditBox = null;
        charRevealInput = null;
        fadeDelayOffsetInput = null;
        fadeDelayFactorInput = null;
        fadeSpeedInput = null;
        minOpacityInput = null;
        timeOfDayInput = null;
        timeTransitionRangeInput = null;
        autoTeleportTargetInput = null;
        lookAtInput = null;
        giveItemInput = null;
    }

    /**
     * Copies values from currently mounted widgets into durable fields before the widgets are discarded.
     */
    private void commitInputsToFields() {
        charRevealSpeed = parseIntegerOrFallback(charRevealInput, charRevealSpeed);
        fadeDelayOffset = parseIntegerOrFallback(fadeDelayOffsetInput, fadeDelayOffset);
        fadeDelayFactor = parseIntegerOrFallback(fadeDelayFactorInput, fadeDelayFactor);
        fadeSpeed = parseIntegerOrFallback(fadeSpeedInput, fadeSpeed);
        minOpacity = parseIntegerOrFallback(minOpacityInput, minOpacity);
        timeOfDay = parseTimeOfDayOrFallback(timeOfDayInput, timeOfDay);
        timeTransitionRange = parseTransitionRangeOrFallback(timeTransitionRangeInput, timeTransitionRange);
        autoTeleportTarget = parseTextOrFallback(autoTeleportTargetInput, autoTeleportTarget);
        lookAt = parseTextOrFallback(lookAtInput, lookAt);
        giveItem = parseTextOrFallback(giveItemInput, giveItem);
    }

    /**
     * Builds the fixed tab selector between the marker header and content area.
     *
     * @param x the x coordinate of the tab bar
     * @param y the y coordinate of the tab bar
     */
    private void buildTabBar(int x, int y) {
        this.addDrawableChild(TabBarWidget.create()
                .setX(x)
                .setY(y)
                .setWidth(MAIN_CONTROL_WIDTH)
                .setHeight(CONTROL_HEIGHT)
                .setTabs(List.of(
                        Text.translatable("ardapaths.client.marker.configuration.screens.tab.general"),
                        Text.translatable("ardapaths.client.marker.configuration.screens.tab.time_and_weather"),
                        Text.translatable("ardapaths.client.marker.configuration.screens.tab.misc")
                ))
                .setSelectedIndex(activeTab)
                .setContentHeight(CONTENT_TOP_OFFSET + CONTENT_HEIGHT - TAB_BAR_Y_OFFSET - CONTROL_HEIGHT)
                .setOnSelect(index ->
                {
                    commitInputsToFields();
                    activeTab = index;
                    reloadFromMarker = false;
                    this.clearAndInit();
                })
                .build()
        );
    }

    /**
     * Builds the tab containing proximity message and animation settings.
     *
     * @param centerX    the x coordinate of the screen centre
     * @param contentTop the y coordinate of the reserved content area
     */
    private void buildGeneralTab(int centerX, int contentTop) {
        this.buildMultilineEditBox(centerX - MAIN_LEFT_OFFSET, contentTop);
        this.buildFormLabels(centerX, contentTop);

        charRevealInput = this.buildIntegerInput(centerX + 100, contentTop, 0, BitPacker.MAX_8_BIT_VALUE);
        fadeDelayOffsetInput = this.buildIntegerInput(centerX + 100, contentTop + 20, 0, BitPacker.MAX_14_BIT_VALUE);
        fadeDelayFactorInput = this.buildIntegerInput(centerX + 100, contentTop + 40, 0, BitPacker.MAX_14_BIT_VALUE);
        fadeSpeedInput = this.buildIntegerInput(centerX + 100, contentTop + 60, 1, BitPacker.MAX_14_BIT_VALUE);
        minOpacityInput = this.buildIntegerInput(centerX + 100, contentTop + 80, 0, 255);

        charRevealInput.setTooltip(Tooltip.of(Text.translatable("ardapaths.client.marker.configuration.screens.rspeed_tooltip")));
        fadeDelayOffsetInput.setTooltip(Tooltip.of(Text.translatable("ardapaths.client.marker.configuration.screens.ffactor_tooltip")));
        fadeDelayFactorInput.setTooltip(Tooltip.of(Text.translatable("ardapaths.client.marker.configuration.screens.fdelay_tooltip")));
        fadeSpeedInput.setTooltip(Tooltip.of(Text.translatable("ardapaths.client.marker.configuration.screens.fspeed_tooltip")));
        minOpacityInput.setTooltip(Tooltip.of(Text.translatable("ardapaths.client.marker.configuration.screens.opacity_tooltip")));

        charRevealInput.setText(String.valueOf(charRevealSpeed));
        fadeDelayOffsetInput.setText(String.valueOf(fadeDelayOffset));
        fadeDelayFactorInput.setText(String.valueOf(fadeDelayFactor));
        fadeSpeedInput.setText(String.valueOf(fadeSpeed));
        minOpacityInput.setText(String.valueOf(minOpacity));

        this.multiLineEditBox.setMaxLength(1000);
        this.multiLineEditBox.setChangeListener(string -> proximityMessage = string);
        this.multiLineEditBox.setText(proximityMessage);

        this.buildActivationRangeSlider(centerX - MAIN_LEFT_OFFSET, contentTop + CONTENT_HEIGHT - 25);
    }

    /**
     * Builds the tab containing weather and time-of-day settings.
     *
     * @param centerX    the x coordinate of the screen center
     * @param contentTop the y coordinate of the reserved content area
     */
    private void buildTimeAndWeatherTab(int centerX, int contentTop) {
        this.buildWeatherSelectionDropdown(centerX - MAIN_LEFT_OFFSET, contentTop + 12);
        this.buildTimeOfDaySelector(centerX - MAIN_LEFT_OFFSET, contentTop + 52);
    }

    /**
     * Builds the tab containing marker action settings.
     *
     * @param centerX    the x coordinate of the screen center
     * @param contentTop the y coordinate of the reserved content area
     */
    private void buildMiscTab(int centerX, int contentTop) {
        int labelX = centerX - MAIN_LEFT_OFFSET;
        int inputX = centerX - 15;
        int inputWidth = 155;

        Text teleportLabel = Text.translatable("ardapaths.client.marker.configuration.screens.auto_teleport_target");
        this.addDrawableChild(new TextWidget(labelX, contentTop + 12, textRenderer.getWidth(teleportLabel), CONTROL_HEIGHT, teleportLabel));
        autoTeleportTargetInput = this.addDrawableChild(InputBoxWidget.create()
                .setX(inputX)
                .setY(contentTop + 12)
                .setWidth(inputWidth)
                .setHeight(17)
                .setEnabled(true)
                .setPlaceholder(Text.translatable("ardapaths.client.marker.configuration.screens.auto_teleport_target_placeholder"))
                .setValidator(this::validateAutoTeleportTarget)
                .build()
        );
        autoTeleportTargetInput.setTooltip(Tooltip.of(Text.translatable("ardapaths.client.marker.configuration.screens.auto_teleport_target_tooltip")));
        autoTeleportTargetInput.setChangeListener(ignored -> autoTeleportTargetInput.validateText());
        autoTeleportTargetInput.setText(autoTeleportTarget);

        Text lookAtLabel = Text.translatable("ardapaths.client.marker.configuration.screens.look_at");
        this.addDrawableChild(new TextWidget(labelX, contentTop + 92, textRenderer.getWidth(lookAtLabel), CONTROL_HEIGHT, lookAtLabel));
        lookAtInput = this.addDrawableChild(InputBoxWidget.create()
                .setX(inputX)
                .setY(contentTop + 92)
                .setWidth(inputWidth)
                .setHeight(17)
                .setEnabled(true)
                .setPlaceholder(Text.translatable("ardapaths.client.marker.configuration.screens.look_at_placeholder"))
                .setValidator(this::validateLookAt)
                .build()
        );
        lookAtInput.setTooltip(Tooltip.of(Text.translatable("ardapaths.client.marker.configuration.screens.look_at_tooltip")));
        lookAtInput.setChangeListener(ignored -> lookAtInput.validateText());
        lookAtInput.setText(lookAt);

        Text itemLabel = Text.translatable("ardapaths.client.marker.configuration.screens.give_item");
        this.addDrawableChild(new TextWidget(labelX, contentTop + 52, textRenderer.getWidth(itemLabel), CONTROL_HEIGHT, itemLabel));
        giveItemInput = this.addDrawableChild(InputBoxWidget.create()
                .setX(inputX)
                .setY(contentTop + 52)
                .setWidth(inputWidth)
                .setHeight(17)
                .setEnabled(true)
                .setPlaceholder(Text.translatable("ardapaths.client.marker.configuration.screens.give_item_placeholder"))
                .setValidator(this::validateGiveItem)
                .build()
        );
        giveItemInput.setTooltip(Tooltip.of(Text.translatable("ardapaths.client.marker.configuration.screens.give_item_tooltip")));
        giveItemInput.setChangeListener(ignored -> giveItemInput.validateText());
        giveItemInput.setText(giveItem);
    }

    /**
     * Creates and adds the title showing linked paths and chapters to the screen.
     *
     * @param x the x coordinate of the subtitle
     * @param y the y coordinate of the subtitle
     */
    private void buildTitle(int x, int y){

        var linkedChapters = 0;
        var linkedPaths = 0;

        if (MARKER.getPathData() != null){

            for (var pathEntry : MARKER.getPathData().keySet()){

                var linkedChaptersForPath = 0;
                var chapters = MARKER.getPathData().get(pathEntry);

                for (String chapter : chapters.keySet()) {

                    var chapterNbtData = MARKER.getChapterData(pathEntry, chapter, false);
                    if (chapterNbtData != null && !chapterNbtData.isEmpty())
                        linkedChaptersForPath++;
                }

                if (linkedChaptersForPath > 0) {
                    linkedPaths++;
                    linkedChapters += linkedChaptersForPath;
                }
            }
        }

        if (linkedPaths >= 1 || linkedChapters >= 1) {

            var message = Text.translatable("ardapaths.client.marker.configuration.screens.linked_chapters_and_paths", linkedPaths, linkedChapters);
            var hasLinks = (linkedPaths == 1 && linkedChapters > 1) || (linkedPaths > 1 && linkedChapters >= 1);

            var fullWidth = textRenderer.getWidth(message);

            if (hasLinks)
                fullWidth += 70;

            this.addDrawableChild(TextWidget.create()
                            .setX(x - (fullWidth / 2))
                            .setY(y)
                            .setWidth(textRenderer.getWidth(message))
                            .setHeight(20)
                            .setMessage(message)
                            .build()
            );

            if (hasLinks)
                this.buildMarkerEditLinksButton(x + (fullWidth / 2) - 60 , y);
        } else {
            var message = Text.translatable("ardapaths.client.marker.configuration.screens.no_linked_chapters_and_paths");
            var fullWidth = textRenderer.getWidth(message);
            this.addDrawableChild(TextWidget.create()
                            .setX(x - fullWidth / 2)
                            .setY(y)
                            .setWidth(280)
                            .setHeight(20)
                            .setMessage(message)
                            .build()
            );
        }
    }

    /**
     * Builds the local loaded-marker navigation column when the window is wide enough.
     *
     * @param top the top y coordinate of the main editor layout
     */
    private void buildMarkerList(int top) {
        double restoredScrollAmount = pendingMarkerListScrollAmount != null
                ? pendingMarkerListScrollAmount
                : markerListPanel != null ? markerListPanel.getScrollAmount() : 0.0;
        boolean restoreScroll = markerListInitialised || pendingMarkerListScrollAmount != null;
        boolean scrollToSelected = !markerListInitialised;
        markerListPanel = null;
        lastMarkerListSignature = 0L;

        int left = this.width / 2 - MAIN_LEFT_OFFSET - MARKER_LIST_GUTTER - MarkerListPanelWidget.MARKER_LIST_WIDTH;

        if (left < 0) return;

        int dividerX = this.width / 2 - MAIN_LEFT_OFFSET - MARKER_LIST_GUTTER / 2;
        markerListPanel = this.addDrawableChild(MarkerListPanelWidget.create()
                .setX(left)
                .setY(top)
                .setScreenHeight(this.height)
                .setListBottom(top + FOOTER_Y_OFFSET + CONTROL_HEIGHT)
                .setDividerX(dividerX)
                .setDividerHeight(TOTAL_HEIGHT)
                .setOnSelect(this::switchToMarker)
                .setOnTeleport(this::teleportToMarker)
                .setOnRangeSelect(this::selectMarkerRange)
                .setOnContextMenu(this::openMarkerContextMenu)
                .setOnFiltersChanged(() -> refreshMarkerList(true))
                .build());
        markerListPanel.setServerListActive(serverListActive);
        refreshMarkerList(scrollToSelected);
        if (restoreScroll) {
            markerListPanel.setScrollAmount(restoredScrollAmount);
        }
        pendingMarkerListScrollAmount = null;
        markerListInitialised = true;
        requestChapterMarkers();
    }

    /**
     * Validates all input fields in the form.
     *
     * @return true if all inputs are valid, false otherwise
     */
    private boolean validateForm()
    {
        boolean valid = true;
        valid &= charRevealInput == null || charRevealInput.validateText();
        valid &= fadeDelayOffsetInput == null || fadeDelayOffsetInput.validateText();
        valid &= fadeDelayFactorInput == null || fadeDelayFactorInput.validateText();
        valid &= fadeSpeedInput == null || fadeSpeedInput.validateText();
        valid &= minOpacityInput == null || minOpacityInput.validateText();
        valid &= timeOfDayInput == null || timeOfDayInput.validateText();
        valid &= timeTransitionRangeInput == null || timeTransitionRangeInput.validateText();
        valid &= autoTeleportTargetInput == null || autoTeleportTargetInput.validateText();
        valid &= lookAtInput == null || lookAtInput.validateText();
        valid &= giveItemInput == null || giveItemInput.validateText();
        return valid;
    }

    /**
     * Rebuilds the local marker list from currently loaded marker block entities.
     *
     * @param scrollToSelected whether to center the row for the marker being edited
     */
    private void refreshMarkerList(boolean scrollToSelected) {
        if (markerListPanel == null) return;

        List<MarkerListPanelWidget.MarkerRow> rows = serverListActive
                ? currentServerMarkerRows()
                : currentLocalMarkerRows();
        markerListPanel.setRows(rows, scrollToSelected);
        trimSelectionToDisplayedMarkers();
        lastMarkerListSignature = serverListActive ? 0L : markerListSignature();
    }

    /**
     * Removes selected positions that are no longer visible in the current marker list.
     */
    private void trimSelectionToDisplayedMarkers() {
        Set<BlockPos> visible = new HashSet<>(visibleMarkerPositions());
        selectedMarkers = new ArrayList<>(selectedMarkers.stream()
                .filter(visible::contains)
                .toList());
    }

    /**
     * Builds visible entries from currently loaded local markers.
     *
     * @return visible local marker rows
     */
    private List<MarkerListPanelWidget.MarkerRow> currentLocalMarkerRows() {
        return currentLocalMarkers().stream()
                .map(this::localMarkerRow)
                .toList();
    }

    /**
     * Builds visible entries from the server-provided chapter marker list.
     *
     * @return visible server marker rows
     */
    private List<MarkerListPanelWidget.MarkerRow> currentServerMarkerRows() {
        return serverMarkers.stream()
                .map(this::serverMarkerRow)
                .toList();
    }

    /**
     * Converts one local marker into panel row data.
     *
     * @param marker marker block entity to represent
     * @return marker row data
     */
    private MarkerListPanelWidget.MarkerRow localMarkerRow(PathMarkerBlockEntity marker) {
        PathMarkerBlockEntity.ChapterNbtData data = selectedChapterData(marker);
        return new MarkerListPanelWidget.MarkerRow(
                marker.getPos().toImmutable(),
                data.getTimeOfDay(),
                data.getWeather(),
                data.getProximityMessage(),
                data.hasMiscData(),
                marker.getPos().equals(MARKER.getPos()),
                selectedMarkers.contains(marker.getPos()),
                false
        );
    }

    /**
     * Converts one server marker row into panel row data.
     *
     * @param entry server marker row
     * @return marker row data
     */
    private MarkerListPanelWidget.MarkerRow serverMarkerRow(ChapterMarkerEntry entry) {
        if (entry.chainBreak()) {
            return MarkerListPanelWidget.MarkerRow.chainBreak();
        }

        BlockPos pos = BlockPos.fromLong(entry.packedPos());
        return new MarkerListPanelWidget.MarkerRow(
                pos,
                entry.timeOfDay(),
                entry.weather(),
                entry.proximityMessage(),
                entry.hasMiscData(),
                pos.equals(MARKER.getPos()),
                selectedMarkers.contains(pos),
                false
        );
    }

    /**
     * Requests the selected chapter's full marker chain from the server.
     */
    private void requestChapterMarkers() {
        if (this.client == null || selectedPathId == null || selectedChapterId == null ||
                selectedPathId.isEmpty() || selectedChapterId.isEmpty()) {
            useLocalMarkerList();
            return;
        }

        String requestedPathId = selectedPathId;
        String requestedChapterId = selectedChapterId;
        PacketRegistry.CHAPTER_PATH_MARKERS.send(
                new ChapterPathMarkersPacket(requestedPathId, requestedChapterId, MARKER.getPos().asLong()),
                response -> this.client.execute(() -> onChapterMarkersResponse(requestedPathId, requestedChapterId, response))
        );
    }

    /**
     * Applies the server response for a chapter marker list request.
     *
     * @param requestedPathId    path ID captured when the request was sent
     * @param requestedChapterId chapter ID captured when the request was sent
     * @param response           server response packet
     */
    private void onChapterMarkersResponse(String requestedPathId, String requestedChapterId, ChapterPathMarkersResponsePacket response) {
        if (!Objects.equals(requestedPathId, selectedPathId) || !Objects.equals(requestedChapterId, selectedChapterId)) {
            return;
        }

        List<ChapterMarkerEntry> incomingMarkers = List.copyOf(response.markers());
        boolean incomingServerListActive = response.status() != ChapterMarkersStatus.NO_CHAPTER_START && !incomingMarkers.isEmpty();
        boolean alreadyShowingServerRows = markerListInitialised && serverListActive;
        if (incomingMarkers.equals(serverMarkers) && incomingServerListActive == serverListActive) {
            return;
        }

        serverMarkers = incomingMarkers;
        serverListActive = incomingServerListActive;
        if (markerListPanel != null) markerListPanel.setServerListActive(serverListActive);
        refreshMarkerList(!alreadyShowingServerRows);
    }

    /**
     * Falls back to the local loaded-marker list.
     */
    private void useLocalMarkerList() {
        serverMarkers = List.of();
        serverListActive = false;
        if (markerListPanel != null) markerListPanel.setServerListActive(false);
        refreshMarkerList(true);
    }

    /**
     * Orders the selected chapter's loaded marker chain that contains the marker being edited.
     *
     * @return markers in stable selected-chapter trail order
     */
    private List<PathMarkerBlockEntity> currentLocalMarkers() {
        Map<BlockPos, PathMarkerBlockEntity> byPos = new LinkedHashMap<>();
        for (PathMarkerBlockEntity marker : Paths.getTickingMarkers()) {
            if (isLinkedToSelectedChapter(marker)) {
                byPos.put(marker.getPos().toImmutable(), marker);
            }
        }

        BlockPos markerPos = MARKER.getPos().toImmutable();
        byPos.put(markerPos, MARKER);

        Map<BlockPos, BlockPos> nextByPos = buildChapterLinks(byPos);
        Map<BlockPos, List<BlockPos>> prevByPos = new HashMap<>();
        for (Map.Entry<BlockPos, BlockPos> entry : nextByPos.entrySet()) {
            if (byPos.containsKey(entry.getValue())) {
                prevByPos.computeIfAbsent(entry.getValue(), ignored -> new ArrayList<>()).add(entry.getKey());
            }
        }

        List<BlockPos> backward = longestChainBackward(markerPos, prevByPos, new HashMap<>(), new HashSet<>());
        Collections.reverse(backward);
        List<BlockPos> ordered = new ArrayList<>(backward);
        ordered.add(markerPos);
        Set<BlockPos> visited = new HashSet<>(ordered);
        ordered.addAll(chainForward(markerPos, byPos, nextByPos, visited));

        return ordered.stream()
                .map(byPos::get)
                .toList();
    }

    /**
     * Builds outgoing links for selected-chapter markers that have target data.
     *
     * @param byPos selected-chapter marker candidates keyed by block position
     * @return links from each marker position to its absolute target position
     */
    private Map<BlockPos, BlockPos> buildChapterLinks(Map<BlockPos, PathMarkerBlockEntity> byPos) {
        Map<BlockPos, BlockPos> nextByPos = new HashMap<>();

        for (Map.Entry<BlockPos, PathMarkerBlockEntity> entry : byPos.entrySet()) {
            PathMarkerBlockEntity.ChapterNbtData data = selectedChapterData(entry.getValue());
            if (data.getTarget() != null) {
                nextByPos.put(entry.getKey(), entry.getKey().add(data.getTarget()));
            }
        }

        return nextByPos;
    }

    /**
     * Walks forward from the edited marker until the selected-chapter target leaves known markers or cycles.
     *
     * @param start     marker position where the forward walk begins
     * @param byPos     selected-chapter marker candidates keyed by block position
     * @param nextByPos outgoing links from marker positions to absolute target positions
     * @param visited   marker positions already claimed by the selected chain
     * @return marker positions after the start in target order
     */
    private List<BlockPos> chainForward(BlockPos start, Map<BlockPos, PathMarkerBlockEntity> byPos,
                                        Map<BlockPos, BlockPos> nextByPos, Set<BlockPos> visited) {
        List<BlockPos> positions = new ArrayList<>();

        BlockPos current = nextByPos.get(start);
        while (current != null && byPos.containsKey(current) && visited.add(current)) {
            positions.add(current);
            current = nextByPos.get(current);
        }

        return positions;
    }

    /**
     * Finds the longest incoming chain ending at a position, preferring stable position order on ties.
     *
     * @param position  marker position where the backward chain ends
     * @param prevByPos incoming links keyed by target marker position
     * @param memo      previously resolved longest incoming chains
     * @param visiting  positions on the current recursion stack for cycle safety
     * @return predecessor positions from nearest to farthest before the given position
     */
    private List<BlockPos> longestChainBackward(BlockPos position, Map<BlockPos, List<BlockPos>> prevByPos,
                                                Map<BlockPos, List<BlockPos>> memo, Set<BlockPos> visiting) {
        if (memo.containsKey(position)) {
            return memo.get(position);
        }

        if (!visiting.add(position)) {
            return List.of();
        }

        List<BlockPos> best = List.of();
        List<BlockPos> predecessors = new ArrayList<>(prevByPos.getOrDefault(position, List.of()));
        predecessors.sort(Comparator.comparingLong(BlockPos::asLong));

        for (BlockPos predecessor : predecessors) {
            if (visiting.contains(predecessor)) {
                continue;
            }

            List<BlockPos> candidate = new ArrayList<>();
            candidate.add(predecessor);
            candidate.addAll(longestChainBackward(predecessor, prevByPos, memo, visiting));
            if (candidate.size() > best.size() || candidate.size() == best.size() && candidate.get(0).asLong() < best.get(0).asLong()) {
                best = candidate;
            }
        }

        visiting.remove(position);
        memo.put(position, best);
        return best;
    }

    /**
     * Gets selected-chapter marker data without mutating the marker.
     *
     * @param marker the marker whose selected-chapter data should be read
     * @return existing selected-chapter data, or empty data when none exists
     */
    private PathMarkerBlockEntity.ChapterNbtData selectedChapterData(PathMarkerBlockEntity marker) {
        PathMarkerBlockEntity.ChapterNbtData data = marker.getChapterData(selectedPathId, selectedChapterId, false);
        return data == null ? PathMarkerBlockEntity.ChapterNbtData.empty(selectedChapterId) : data;
    }

    /**
     * Builds a cheap signature for linked marker membership, links, filter state, and selected-chapter content flags.
     *
     * @return hash-like signature of the current linked local marker state
     */
    private long markerListSignature() {
        long signature = 1125899906842597L;
        Map<BlockPos, PathMarkerBlockEntity> byPos = new HashMap<>();

        for (PathMarkerBlockEntity marker : Paths.getTickingMarkers()) {
            if (isLinkedToSelectedChapter(marker)) {
                byPos.put(marker.getPos().toImmutable(), marker);
            }
        }

        byPos.put(MARKER.getPos().toImmutable(), MARKER);
        if (markerListPanel != null) {
            signature = signature * 31 + markerListPanel.filterSignature();
        }

        List<BlockPos> positions = new ArrayList<>(byPos.keySet());
        positions.sort(Comparator.comparingLong(BlockPos::asLong));
        for (BlockPos pos : positions) {
            PathMarkerBlockEntity marker = byPos.get(pos);
            PathMarkerBlockEntity.ChapterNbtData data = selectedChapterData(marker);

            signature = signature * 31 + pos.asLong();
            signature = signature * 31 + packedTargetSignature(data.getTarget());
            signature = signature * 31 + (data.isChapterStart() ? 1 : 0);
            signature = signature * 31 + data.getTimeOfDay();
            signature = signature * 31 + data.getWeather();
            signature = signature * 31 + data.getProximityMessage().hashCode();
            signature = signature * 31 + (data.hasMiscData() ? 1 : 0);
        }

        return signature;
    }

    /**
     * Packs a target offset into a stable signature value.
     *
     * @param target relative target offset, or null when unset
     * @return stable signature contribution for the target
     */
    private long packedTargetSignature(BlockPos target) {
        if (target == null) {
            return 0L;
        }

        long signature = 17L;
        signature = signature * 31 + target.getX();
        signature = signature * 31 + target.getY();
        signature = signature * 31 + target.getZ();
        return signature;
    }

    /**
     * Checks whether a marker holds data for the selected path and chapter.
     *
     * @param marker the marker to inspect
     * @return true if the marker belongs to the chapter currently being edited
     */
    private boolean isLinkedToSelectedChapter(PathMarkerBlockEntity marker) {
        if (marker.getPos().equals(MARKER.getPos())) return true;

        PathMarkerBlockEntity.ChapterNbtData data = marker.getChapterData(selectedPathId, selectedChapterId, false);
        return data != null && !data.isEmpty();
    }

    /**
     * Returns visible marker positions in list order.
     *
     * @return ordered visible marker positions
     */
    private List<BlockPos> visibleMarkerPositions() {
        return markerListPanel == null ? List.of() : markerListPanel.getVisiblePositions();
    }

    /**
     * Selects a contiguous visible range without changing the edited marker.
     *
     * @param pos marker position where the range ends
     */
    private void selectMarkerRange(BlockPos pos) {
        List<BlockPos> positions = visibleMarkerPositions();
        int clickedIndex = positions.indexOf(pos);
        int anchorIndex = selectionAnchor == null ? -1 : positions.indexOf(selectionAnchor);
        if (clickedIndex < 0) return;
        if (anchorIndex < 0) {
            anchorIndex = positions.indexOf(MARKER.getPos());
        }
        if (anchorIndex < 0) {
            anchorIndex = clickedIndex;
        }

        int from = Math.min(anchorIndex, clickedIndex);
        int to = Math.max(anchorIndex, clickedIndex);
        selectedMarkers = new ArrayList<>(positions.subList(from, to + 1));
        refreshMarkerList(false);
    }

    /**
     * Opens the marker-list context menu at the requested cursor position.
     *
     * @param pos    clicked marker position
     * @param anchor cursor coordinates for menu placement
     */
    private void openMarkerContextMenu(BlockPos pos, MarkerListEntry.ContextMenuAnchor anchor) {
        if (!selectedMarkers.contains(pos)) {
            selectedMarkers = new ArrayList<>(List.of(pos.toImmutable()));
            selectionAnchor = pos.toImmutable();
            refreshMarkerList(false);
        }

        closeContextMenu();
        boolean hasSelection = !selectedMarkers.isEmpty();
        boolean canInterpolate = selectedMarkers.size() > 1;
        List<ContextMenuWidget.Item> items = List.of(
                new ContextMenuWidget.Item(
                        Text.translatable("ardapaths.client.marker.configuration.screens.marker_list.clear_time"),
                        Text.translatable("ardapaths.client.marker.configuration.screens.marker_list.clear_time.tooltip"),
                        hasSelection,
                        () -> confirmBulkClear(true, false)
                ),
                new ContextMenuWidget.Item(
                        Text.translatable("ardapaths.client.marker.configuration.screens.marker_list.clear_weather"),
                        Text.translatable("ardapaths.client.marker.configuration.screens.marker_list.clear_weather.tooltip"),
                        hasSelection,
                        () -> confirmBulkClear(false, true)
                ),
                new ContextMenuWidget.Item(
                        Text.translatable("ardapaths.client.marker.configuration.screens.marker_list.interpolate_time"),
                        Text.translatable("ardapaths.client.marker.configuration.screens.marker_list.interpolate_time.tooltip"),
                        canInterpolate,
                        this::openTimeInterpolationPopup
                )
        );

        ContextMenuWidget menu = ContextMenuWidget.create()
                .setX((int) anchor.mouseX())
                .setY((int) anchor.mouseY())
                .setItems(items)
                .build();
        int menuX = Math.max(0, Math.min((int) anchor.mouseX(), this.width - menu.getWidth()));
        int menuY = Math.max(0, Math.min((int) anchor.mouseY(), this.height - menu.getHeight()));
        contextMenu = this.addDrawableChild(ContextMenuWidget.create()
                .setX(menuX)
                .setY(menuY)
                .setItems(items)
                .build());
    }

    /**
     * Closes the open marker-list context menu if one exists.
     */
    private void closeContextMenu() {
        if (contextMenu != null) {
            remove(contextMenu);
            contextMenu = null;
        }
    }

    /**
     * Confirms a bulk clear operation with the player.
     *
     * @param clearTime    whether time data should be cleared
     * @param clearWeather whether weather data should be cleared
     */
    private void confirmBulkClear(boolean clearTime, boolean clearWeather) {
        closeContextMenu();
        if (this.client == null) return;
        Text message = clearTime
                ? Text.translatable("ardapaths.client.marker.configuration.screens.marker_list.clear_time.confirm", selectedMarkers.size())
                : Text.translatable("ardapaths.client.marker.configuration.screens.marker_list.clear_weather.confirm", selectedMarkers.size());
        this.client.setScreen(new ConfirmationPopup(message, () -> startBulkClear(clearTime, clearWeather), () -> {}, this));
    }

    /**
     * Sends a confirmed bulk clear request to the server.
     *
     * @param clearTime    whether time data should be cleared
     * @param clearWeather whether weather data should be cleared
     */
    private void startBulkClear(boolean clearTime, boolean clearWeather) {
        if (this.client == null) return;
        List<Long> packedPositions = selectedMarkers.stream().map(BlockPos::asLong).toList();
        timeSpreadFeedback = null;
        this.client.setScreen(new BusyPopup(
                Text.translatable("ardapaths.client.marker.configuration.screens.marker.time_spread.busy"),
                this,
                this::onTimeSpreadTimeout
        ));
        PacketRegistry.MARKER_BULK_CLEAR.send(
                new MarkerBulkClearPacket(packedPositions, selectedPathId, selectedChapterId, clearTime, clearWeather),
                this::onBulkClearResponse
        );
    }

    /**
     * Opens the interpolation endpoint popup for the current marker range.
     */
    private void openTimeInterpolationPopup() {
        closeContextMenu();
        if (this.client == null || selectedMarkers.size() < 2) return;
        this.client.setScreen(new TimeInterpolationPopup(
                this,
                selectedMarkers.get(0),
                selectedMarkers.get(selectedMarkers.size() - 1),
                this::startTimeInterpolation
        ));
    }

    /**
     * Sends validated interpolation endpoints to the existing time-spread handler.
     *
     * @param endpoints interpolation endpoints selected by the modal
     */
    private void startTimeInterpolation(TimeInterpolationPopup.Endpoints endpoints) {
        if (this.client == null) return;
        timeSpreadFeedback = null;
        this.client.setScreen(new BusyPopup(
                Text.translatable("ardapaths.client.marker.configuration.screens.marker.time_spread.busy"),
                this,
                this::onTimeSpreadTimeout
        ));
        PacketRegistry.MARKER_TIME_SPREAD.send(
                new MarkerTimeSpreadPacket(endpoints.startPacked(), endpoints.endPacked(), endpoints.startTime(), endpoints.endTime(), selectedPathId, selectedChapterId, false),
                this::onTimeSpreadResponse
        );
    }

    /**
     * Opens a fresh marker editor for a loaded marker after handling pending changes.
     *
     * @param pos the marker position to edit
     */
    private void switchToMarker(BlockPos pos) {
        selectedMarkers = new ArrayList<>(List.of(pos.toImmutable()));
        selectionAnchor = pos.toImmutable();
        switchToMarker(pos, selectedMarkers);
    }

    /**
     * Opens a fresh marker editor while preserving an explicit marker-list selection.
     *
     * @param pos       marker position to edit
     * @param selection marker positions selected in list order
     */
    private void switchToMarker(BlockPos pos, Collection<BlockPos> selection) {
        selectedMarkers = new ArrayList<>(selection.stream().map(BlockPos::toImmutable).toList());
        if (pos.equals(MARKER.getPos())) {
            refreshMarkerList(false);
            return;
        }

        PathMarkerBlockEntity target = findTickingMarker(pos);
        if (target == null) {
            promptPendingChangesThen(() -> requestRemoteMarker(pos, selectedMarkers), this, false);
            return;
        }

        MarkerEditScreen nextScreen = new MarkerEditScreen(target, originalPathAndChapterData, selectedMarkers);
        promptPendingChangesThen(() -> {
            nextScreen.seedChapterMarkers(serverMarkers, serverListActive, currentMarkerListScrollAmount());
            if (this.client != null) this.client.setScreen(nextScreen);
        }, nextScreen, false);
    }

    /**
     * Seeds chapter marker list state before a newly opened editor screen initializes.
     *
     * @param markers      chapter marker rows already known by the previous screen
     * @param active       whether those rows are server-provided chapter rows
     * @param scrollAmount marker list scroll amount to preserve
     */
    void seedChapterMarkers(List<ChapterMarkerEntry> markers, boolean active, double scrollAmount) {
        serverMarkers = List.copyOf(markers);
        serverListActive = active;
        markerListInitialised = true;
        pendingMarkerListScrollAmount = scrollAmount;
    }

    /**
     * Returns the current marker list scroll amount when the panel is mounted.
     *
     * @return current scroll amount, or zero when the marker list is unavailable
     */
    private double currentMarkerListScrollAmount() {
        return markerListPanel == null ? 0.0 : markerListPanel.getScrollAmount();
    }

    /**
     * Sends a teleport request to the selected marker after handling pending changes.
     *
     * @param pos the marker position to teleport to
     */
    private void teleportToMarker(BlockPos pos) {
        if (this.client == null || this.client.player == null) return;

        Identifier worldId = this.client.player.getWorld().getRegistryKey().getValue();
        promptPendingChangesThen(() -> {
            PacketRegistry.PLAYER_TELEPORT.send(new PlayerTeleportPacket(pos.getX(), pos.getY(), pos.getZ(), worldId));
            if (this.client != null) this.client.setScreen(null);
        }, null, false);
    }

    /**
     * Finds a currently loaded marker by exact block position.
     *
     * @param pos the marker position to find
     * @return the loaded marker, or null when it is no longer available
     */
    private PathMarkerBlockEntity findTickingMarker(BlockPos pos) {
        return Paths.getTickingMarkers().stream()
                .filter(marker -> marker.getPos().equals(pos))
                .findFirst()
                .orElse(null);
    }

    /**
     * Requests marker data for an unloaded marker and opens a detached editor when it arrives.
     *
     * @param pos       marker position to load
     * @param selection marker positions selected in list order
     */
    private void requestRemoteMarker(BlockPos pos, Collection<BlockPos> selection) {
        if (this.client == null) return;

        markerLoadFeedback = null;
        this.client.setScreen(new BusyPopup(
                Text.translatable("ardapaths.client.marker.configuration.screens.marker.load.busy"),
                this,
                this::onMarkerLoadTimeout
        ));
        PacketRegistry.PATH_MARKER_REMOTE_DATA.send(new PathMarkerRemoteDataPacket(pos.asLong()), response -> onMarkerLoadResponse(response, selection));
    }

    /**
     * Handles the timeout path for a marker-load request whose response callback expired.
     */
    private void onMarkerLoadTimeout() {
        markerLoadFeedback = Text.translatable("ardapaths.client.marker.configuration.screens.marker.load.timeout");
        markerLoadFeedbackError = true;
        reloadFromMarker = false;
    }

    /**
     * Handles the server response for a remote marker-load request.
     *
     * @param response  server response packet
     * @param selection marker positions selected in list order
     */
    @SuppressWarnings("resource")
    private void onMarkerLoadResponse(PathMarkerRemoteDataResponsePacket response, Collection<BlockPos> selection) {
        var minecraftClient = Client.mc();
        minecraftClient.execute(() -> {
            if (minecraftClient.world == null) {
                return;
            }

            if (response.status() == PathMarkerRemoteDataStatus.OK) {
                BlockPos pos = BlockPos.fromLong(response.packedPos()).toImmutable();
                PathMarkerBlockEntity remote = new PathMarkerBlockEntity(pos, ModBlocks.PATH_MARKER.getDefaultState());
                remote.readNbt(response.data());
                MarkerEditScreen nextScreen = new MarkerEditScreen(remote, originalPathAndChapterData, selection);
                nextScreen.seedChapterMarkers(serverMarkers, serverListActive, currentMarkerListScrollAmount());
                minecraftClient.setScreen(nextScreen);
                return;
            }

            markerLoadFeedback = remoteMarkerStatusText(response);
            markerLoadFeedbackError = true;
            if (minecraftClient.currentScreen instanceof BusyPopup) {
                minecraftClient.setScreen(this);
            }
            reloadFromMarker = false;
            clearAndInit();
        });
    }

    /**
     * Creates and adds the path selection dropdown widget.
     *
     * @param x the x coordinate of the dropdown
     * @param y the y coordinate of the dropdown
     */
    private void buildPathSelectionDropdown(int x, int y)
    {
        this.addDrawableChild(DropdownWidget.<PathData>create()
                .setPosition(x, y)
                .setSize(280, 20)
                .setTitle(Text.translatable("ardapaths.client.marker.configuration.screens.edit_path_data"))
                .setOptions(ArdaPathsClient.CONFIG.getPaths())
                .setOptionDisplay(item ->
                {
                    if (item == null) return Text.translatable("ardapaths.client.marker.configuration.screens.no_path");

                    MutableText label = Text.literal(item.getName()).fillStyle(Style.EMPTY.withColor(item.getPrimaryColor().asHex()));

                    if (isPathAndChapterLinked(item.getId(), selectedChapterId)) label.append(" •");

                    return label;
                })
                .setSelected(ArdaPathsClient.CONFIG.getPath(selectedPathId))
                .setOnSelect(path ->
                {
                    selectedPathId = path.getId();
                    selectedChapterId = path.getChapterIds().get(0);
                    serverMarkers = List.of();
                    serverListActive = false;
                    markerListInitialised = false;
                    pendingMarkerListScrollAmount = null;
                    this.clearAndInit();
                })
                .build()
        );
    }

    /**
     * Creates and adds the chapter selection dropdown widget.
     *
     * @param x the x coordinate of the dropdown
     * @param y the y coordinate of the dropdown
     */
    private void buildChapterSelectionDropdown(int x, int y)
    {
        PathData selectedPath = ArdaPathsClient.CONFIG.getPath(selectedPathId);

        List<ChapterData> chapters = selectedPath != null ? new ArrayList<>(selectedPath.getChapters()) : new ArrayList<>();
        chapters.sort(Comparator.comparingInt(ChapterData::getIndex));

        var selection = ArdaPathsClient.CONFIG.getPath(selectedPathId);
        var selectedChapter = selection != null ? selection.getChapter(selectedChapterId) : null;

        this.addDrawableChild(DropdownWidget.<ChapterData>create()
                .setPosition(x,y)
                .setSize(175, 20)
                .setTitle(Text.translatable("ardapaths.client.marker.configuration.screens.chapter"))
                .setOptionDisplay(item ->
                {
                    if (item == null) return Text.translatable("ardapaths.client.marker.configuration.screens.no_chapter");
                    MutableText label = Text.literal(item.getName());

                    if (isPathAndChapterLinked(selectedPathId, item.getId())) label.append(" •");

                    return label;
                })
                .setOptions(chapters)
                .setSelected(selectedChapter)
                .setOnSelect(chapter ->
                {
                    selectedChapterId = chapter.getId();
                    serverMarkers = List.of();
                    serverListActive = false;
                    markerListInitialised = false;
                    pendingMarkerListScrollAmount = null;
                    this.clearAndInit();
                })
                .build()
        );
    }

    /**
     * Builds the optional weather selection dropdown for marker traversal behavior.
     *
     * @param x the dropdown x coordinate
     * @param y the dropdown y coordinate
     */
    private void buildWeatherSelectionDropdown(int x, int y)
    {

        WeatherTypes selection = WeatherTypes.fromInt(weather);

        this.addDrawableChild(DropdownWidget.<WeatherTypes>create()
                .setPosition(x,y)
                .setSize(280, 20)
                .setTitle(Text.translatable("ardapaths.client.marker.configuration.screens.weather"))
                .setOptionDisplay(item ->
                {
                    if (item == null) return Text.translatable("ardapaths.client.marker.configuration.screens.no_weather");
                    return Text.literal(item.getDisplayName());
                })
                .setOptions(List.of(WeatherTypes.values()))
                .setSelected(selection)
                .setOnSelect(selected ->
                        weather = selected == WeatherTypes.DEFAULT ? PathMarkerBlockEntity.ChapterNbtData.UNSET : selected.ordinal())
                .build()
        );
    }

    /**
     * Creates and adds the time-of-day input and transition range selector.
     *
     * @param x the input x coordinate
     * @param y the input y coordinate
     */
    private void buildTimeOfDaySelector(int x, int y) {

        var label = Text.translatable("ardapaths.client.marker.configuration.screens.current_time_of_day");
        this.addDrawableChild(new TextWidget(x, y-17, textRenderer.getWidth(label), 17, label));
        timeOfDayInput = this.addDrawableChild(InputBoxWidget.create()
                .setX(x)
                .setY(y)
                .setWidth(135)
                .setHeight(17)
                .setEnabled(true)
                .setPlaceholder(Text.translatable("ardapaths.client.marker.configuration.screens.user_current"))
                .setValidator(this::validateCurrentTimeOfDay)
                .build()
        );
        timeOfDayInput.setChangeListener(ignored -> timeOfDayInput.validateText());

        timeOfDayInput.setText(TimeOfDay.format(timeOfDay));

        Text rangeLabel = Text.translatable("ardapaths.client.marker.configuration.screens.time_transition_range");
        this.addDrawableChild(new TextWidget(x + 145, y - 19, textRenderer.getWidth(rangeLabel), 17, rangeLabel));
        timeTransitionRangeInput = this.addDrawableChild(InputBoxWidget.create()
                .setX(x + 145)
                .setY(y)
                .setWidth(135)
                .setHeight(17)
                .setEnabled(true)
                .setPlaceholder(Text.translatable("ardapaths.client.marker.configuration.screens.time_transition_range_placeholder"))
                .setValidator(this::validateTimeTransitionRange)
                .build()
        );
        timeTransitionRangeInput.setText(TimeOfDay.formatTransitionRange(timeTransitionRange));
        timeTransitionRangeInput.setChangeListener(ignored -> timeTransitionRangeInput.validateText());
        timeTransitionRangeInput.setTooltip(Tooltip.of(Text.translatable("ardapaths.client.marker.configuration.screens.time_transition_range_tooltip")));

        if (timeSpreadFeedback != null) {
            Text formattedFeedback = timeSpreadFeedback.copy().formatted(timeSpreadFeedbackError ? Formatting.RED : Formatting.GRAY);
            this.addDrawableChild(new TextWidget(x, y + 30, 280, 17, formattedFeedback));
        }
    }

    /**
     * Adds marker-load feedback to the main panel when a remote marker request fails.
     *
     * @param centerX the x coordinate of the screen center
     * @param footerY the y coordinate of the footer row
     */
    private void buildMarkerLoadFeedback(int centerX, int footerY) {
        if (markerLoadFeedback == null) return;

        Text formattedFeedback = markerLoadFeedback.copy().formatted(markerLoadFeedbackError ? Formatting.RED : Formatting.GRAY);
        this.addDrawableChild(new TextWidget(centerX - MAIN_LEFT_OFFSET, footerY - 24, MAIN_CONTROL_WIDTH, 17, formattedFeedback));
    }

    /**
     * Creates and adds a button that copies this marker's selected path chapter ID.
     *
     * @param centerX the x coordinate of the screen center
     * @param top     the y coordinate of the button
     */
    @SuppressWarnings("resource")
    private void buildMarkerIdButton(int centerX, int top) {
        String markerId = MarkerId.format(MARKER.getPos());
        Text label = Text.translatable("ardapaths.client.marker.configuration.screens.marker_id", markerId);
        int buttonWidth = Math.min(MAIN_CONTROL_WIDTH, textRenderer.getWidth(label) + 10);
        ButtonWidget button = this.addDrawableChild(ButtonWidget.builder(label, ignored ->
                        Client.mc().keyboard.setClipboard(MarkerId.format(MARKER.getPos())))
                .dimensions(centerX - buttonWidth / 2, top, buttonWidth, CONTROL_HEIGHT)
                .build()
        );
        button.setTooltip(Tooltip.of(Text.translatable("ardapaths.client.marker.configuration.screens.marker_id_tooltip", markerId)));
    }

    /**
     * Creates and adds the button to open the marker links editor screen.
     *
     * @param x the x coordinate of the button
     * @param y the y coordinate of the button
     */
    private void buildMarkerEditLinksButton(int x, int y)
    {
        assert this.client != null;
        this.addDrawableChild(new ButtonWidget(
                x,
                y,
                60,
                20,
                Text.translatable("ardapaths.client.marker.configuration.screens.edit_links"),
                button -> this.client.setScreen(new MarkerLinksEditScreen(MARKER, originalPathAndChapterData)),
                Supplier::get
        ));
    }

    /**
     * Creates and adds the button to open the chapter editor screen.
     *
     * @param x the x coordinate of the button
     * @param y the y coordinate of the button
     */
    private void buildEditChaptersButton(int x, int y)
    {
        assert this.client != null;
        this.addDrawableChild(new ButtonWidget(
                x,
                y,
                100,
                20,
                Text.translatable("ardapaths.client.marker.configuration.screens.edit_chapters"),
                button -> this.client.setScreen(new ChapterEditScreen(this)),
                Supplier::get
        ));
    }

    /**
     * Creates and adds the checkbox for marking this marker as a chapter start.
     *
     * @param x the x coordinate of the checkbox
     * @param y the y coordinate of the checkbox
     */
    private void buildChapterStartCheckbox(int x, int y)
    {
        this.addDrawableChild(CheckboxWidget.create()
                        .setX(x)
                        .setY(y)
                        .setWidth(135)
                        .setHeight(15)
                        .setText(Text.translatable("ardapaths.client.marker.configuration.screens.is_chapter_start"))
                        .setChecked(isChapterStart)
                        .setEnabled(true)
                        .setOnChange(checked -> {
                            isChapterStart = checked;
                            if (displayChapterTitleOnTrail != null) {
                                displayChapterTitleOnTrail.setEnabled(isChapterStart);
                            }
                        })
                        .build()
        );
    }

    /**
     * Creates and adds the checkbox for showing chapter title on the trail.
     *
     * @param x the x coordinate of the checkbox
     * @param y the y coordinate of the checkbox
     * @return the configured chapter start title checkbox
     */
    private CheckboxWidget buildChapterStartHideTitleCheckbox(int x, int y)
    {
        return addDrawableChild(CheckboxWidget.create()
                .setX(x)
                .setY(y)
                .setWidth(135)
                .setHeight(15)
                .setText(Text.translatable("ardapaths.client.marker.configuration.screens.show_title_on_trail"))
                .setChecked(showChapterStartTitle)
                .setEnabled(isChapterStart)
                .setOnChange(checked -> showChapterStartTitle = checked)
                .build()
        );
    }

    /**
     * Creates and adds the multi-line text editor for the proximity message.
     *
     * @param x the x coordinate of the edit box
     * @param y the y coordinate of the edit box
     */
    @SuppressWarnings("resource")
    private void buildMultilineEditBox(int x, int y)
    {
        this.multiLineEditBox = this.addDrawableChild(new EditBoxWidget(
                Client.mc().textRenderer,
                x,
                y,
                180,
                100,
                Text.translatable("ardapaths.client.marker.configuration.screens.proximity_message_placeholder"),
                Text.empty()
        ));
    }

    /**
     * Creates and adds static labels for the proximity message and animation fields.
     *
     * @param centerX the x coordinate of the screen center
     * @param y       the y coordinate of the proximity message editor
     */
    private void buildFormLabels(int centerX, int y)
    {
        var fontHeight = textRenderer.fontHeight;
        var halfFontHeight = fontHeight / 2;

        var proximityMessage = Text.translatable("ardapaths.client.marker.configuration.screens.proximity_message");
        this.addDrawableChild(new TextWidget(
                centerX - 140,
                y - fontHeight - 2,
                textRenderer.getWidth(proximityMessage),
                fontHeight,
                Text.translatable("ardapaths.client.marker.configuration.screens.proximity_message")
        ));

        this.addDrawableChild(new TextWidget(
                centerX + 49,
                y += halfFontHeight,
                50,
                fontHeight,
                Text.translatable("ardapaths.client.marker.configuration.screens.rspeed")
        ));
        this.addDrawableChild(new TextWidget(
                centerX + 52,
                y += 20 - halfFontHeight,
                47,
                17,
                Text.translatable("ardapaths.client.marker.configuration.screens.fdelay")
        ));
        this.addDrawableChild(new TextWidget(
                centerX + 45,
                y += 20,
                54,
                17,
                Text.translatable("ardapaths.client.marker.configuration.screens.ffactor")
        ));
        this.addDrawableChild(new TextWidget(
                centerX + 49,
                y += 20,
                50,
                17,
                Text.translatable("ardapaths.client.marker.configuration.screens.fspeed")
        ));
        this.addDrawableChild(new TextWidget(
                centerX + 53,
                y + 20,
                46,
                17,
                Text.translatable("ardapaths.client.marker.configuration.screens.opacity")
        ));
    }

    /**
     * Builds an integer input constrained to values that can be persisted in packed marker data.
     *
     * @param x   the input x coordinate
     * @param y   the input y coordinate
     * @param min the minimum accepted value
     * @param max the maximum accepted value
     * @return the configured integer input widget
     */
    private InputBoxWidget buildIntegerInput(int x, int y, int min, int max)
    {
        return buildIntegerInput(x, y, min, max, 40);
    }

    /**
     * Builds an integer input constrained to the supplied bounds and width.
     *
     * @param x     the input x coordinate
     * @param y     the input y coordinate
     * @param min   the minimum accepted value
     * @param max   the maximum accepted value
     * @param width the input width
     * @return the configured integer input widget
     */
    @SuppressWarnings("SameParameterValue")
    private InputBoxWidget buildIntegerInput(int x, int y, int min, int max, int width)
    {
        return this.addDrawableChild(InputBoxWidget.create()
                .setX(x)
                .setY(y)
                .setWidth(width)
                .setHeight(17)
                .setEnabled(true)
                .setPlaceholder(Text.empty())
                .setValidator(text ->
                {
                    try
                    {
                        int value = Integer.parseInt(text);
                        if (value < min || value > max) {
                            throw new TextValidationError(String.format("Must be between %d and %d.", min, max));
                        }
                    }
                    catch (NumberFormatException e)
                    {
                        throw new TextValidationError(Text.translatable("ardapaths.generic.validation.error.integer").getString());
                    }
                })
                .build()
        );
    }

    /**
     * Creates and adds the slider for adjusting the proximity activation range.
     *
     * @param x the x coordinate of the slider
     * @param y the y coordinate of the slider
     */
    private void buildActivationRangeSlider(int x, int y)
    {
        this.addDrawableChild(new SliderWidget(
                x,
                y,
                280,
                20,
                ScreenTexts.EMPTY,
                activationRange / 100.0
        )
        {
            {
                this.updateMessage();
            }

            @Override
            protected void updateMessage()
            {
                this.setMessage(Text.translatable("ardapaths.client.marker.configuration.screens.activation_range", activationRange));
            }

            @Override
            protected void applyValue()
            {
                activationRange = MathHelper.floor(MathHelper.clampedLerp(0.0, 100.0, this.value));
            }
        });
    }

    /**
     * Creates and adds the checkbox for displaying the marker above block surfaces.
     *
     * @param x the x coordinate of the checkbox
     * @param y the y coordinate of the checkbox
     */
    private void buildDisplayAboveBlocksCheckbox(int x, int y)
    {
        this.addDrawableChild(CheckboxWidget.create()
                .setX(x)
                .setY(y)
                .setWidth(145)
                .setHeight(15)
                .setText(Text.translatable("ardapaths.client.marker.configuration.screens.display_trail_above_blocks"))
                .setChecked(displayAboveBlocks)
                .setEnabled(true)
                .setOnChange(checked -> displayAboveBlocks = checked)
                .build()
        );
    }

    /**
     * Creates and adds the close button to discard changes and exit the screen.
     *
     * @param x the x coordinate of the button
     * @param y the y coordinate of the button
     */
    private void buildCloseButton(int x, int y){

        var doneButton = new ButtonWidget(
                x,
                y,
                60,
                20,
                Text.translatable("ardapaths.generic.close"),
                button -> this.close(),
                Supplier::get
        );



        this.addDrawableChild(doneButton);
    }

    /**
     * Creates and adds the save button to persist form changes.
     *
     * @param x the x coordinate of the button
     * @param y the y coordinate of the button
     */
    private void buildSaveButton(int x, int y)
    {
        this.addDrawableChild(new ButtonWidget(
                x,
                y,
                60,
                20,
                Text.translatable("ardapaths.generic.save"),
                button ->
                {
                    if (validateForm()) {

                        commitInputsToFields();
                        saveAndRefreshMarkerList();

                    } else {

                        log.error(Text.translatable("ardapaths.generic.validation.form.errors").getString());
                    }
                },
                Supplier::get
        ));
    }

    /**
     * Renders the screen background, UI elements, and animation parameter labels.
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

    /**
     * Lets an open context menu consume clicks before the rest of the editor handles them.
     *
     * @param mouseX mouse x coordinate
     * @param mouseY mouse y coordinate
     * @param button clicked mouse button
     * @return true when the click is consumed
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (contextMenu != null) {
            if (contextMenu.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            closeContextMenu();
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * Closes an open context menu when Esc is pressed.
     *
     * @param keyCode  GLFW key code
     * @param scanCode hardware scan code
     * @param modifiers active key modifiers
     * @return true when the key is consumed
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (contextMenu != null && keyCode == GLFW.GLFW_KEY_ESCAPE) {
            closeContextMenu();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /**
     * Handles mouse release events, delegating to the multi-line edit box.
     *
     * @param mouseX the mouse x coordinate
     * @param mouseY the mouse y coordinate
     * @param button the mouse button code
     * @return true if the event was handled
     */
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button)
    {
        return super.mouseReleased(mouseX, mouseY, button) ||
                (this.multiLineEditBox != null && this.multiLineEditBox.mouseReleased(mouseX, mouseY, button));
    }

    /**
     * Updates the multi-line edit box state each tick.
     */
    @Override
    public void tick()
    {
        if (this.multiLineEditBox != null) {
            this.multiLineEditBox.tick();
        }

        if (markerListPanel != null && !serverListActive) {
            long signature = markerListSignature();
            if (signature != lastMarkerListSignature) {
                refreshMarkerList(false);
            }
        }

        super.tick();
    }

    /**
     * Closes the screen, prompting to save if changes have been made.
     */
    @Override
    public void close()
    {
        promptPendingChangesThen(super::close, this, true);
    }

    /**
     * Runs an action after prompting to save or discard pending marker changes.
     *
     * @param proceed         the action to run after pending changes are resolved
     * @param parentScreen    the screen the popup should return to after a callback
     * @param closeOnValidate whether the popup should close fully after a callback
     */
    private void promptPendingChangesThen(Runnable proceed, Screen parentScreen, boolean closeOnValidate) {
        Text validationWarning = pendingChangesWarning();
        var modifiedPathAndChapterData = listModifiedPathAndChapterData();
        Runnable discardOutcome = proceed;

        if (!modifiedPathAndChapterData.equals(Text.empty())) {
            discardOutcome = () -> {
                discardChapterAndPathDataChanges();
                proceed.run();
            };
        }

        if (!validationWarning.equals(Text.empty()))
        {
            assert this.client != null;
            ConfirmationPopup popup = new ConfirmationPopup(
                    validationWarning,
                    () -> {
                        commitInputsToFields();
                        saveAndRefreshMarkerList();
                        proceed.run();
                    },
                    discardOutcome,
                    parentScreen,
                    closeOnValidate
            );
            this.client.setScreen(popup);
            return;

        }

        proceed.run();
    }

    /**
     * Builds the unsaved-changes warning message for the current marker state.
     *
     * @return warning text, or empty text when there are no pending changes
     */
    private Text pendingChangesWarning() {
        Text validationWarning = Text.empty();
        var modifiedPathAndChapterData = listModifiedPathAndChapterData();

        if (wasFormModified() && !modifiedPathAndChapterData.equals(Text.empty())){

            validationWarning = Text.translatable("ardapaths.client.marker.configuration.screens.form.has_changes_added_path_and_chapter")
                    .append(modifiedPathAndChapterData)
                    .append(Text.translatable("ardapaths.generic.save_changes"));

        } else if (wasFormModified()){

            validationWarning = Text.translatable("ardapaths.client.marker.configuration.screens.form.has_changes");

        } else if (!modifiedPathAndChapterData.equals(Text.empty())){

            validationWarning = Text.translatable("ardapaths.client.marker.configuration.screens.form.added_path_and_chapter")
                    .append(modifiedPathAndChapterData)
                    .append(Text.translatable("ardapaths.generic.save_changes"));
        }

        return validationWarning;
    }

    /**
     * Persists all form data to the marker's NBT and sends update packets to the server.
     *
     */
    private void save()
    {
        if (selectedPathId.isEmpty()) return;

        PathMarkerBlockEntity.ChapterNbtData data = MARKER.getChapterData(selectedPathId, selectedChapterId, true);

        assert data != null;
        data.setProximityMessage(proximityMessage);
        data.setActivationRange(activationRange);
        data.setChapterStart(isChapterStart);
        data.setDisplayChapterTitleOnTrail(isChapterStart && showChapterStartTitle);
        data.setDisplayAboveBlocks(displayAboveBlocks);
        data.setWeather(weather);
        data.setTimeOfDay(timeOfDay);
        data.setTimeTransitionRange(timeTransitionRange);
        data.setAutoTeleportTarget(autoTeleportTarget);
        data.setLookAt(WarpTarget.parseCoordinates(lookAt));
        data.setGiveItem(giveItem);

        var packedData = BitPacker.packFive(charRevealSpeed, fadeDelayOffset, fadeDelayFactor, fadeSpeed, minOpacity);

        data.setPackedMessageData(packedData);

        if (isChapterStart)
        {
            ChapterStartUpdatePacket packet = new ChapterStartUpdatePacket(selectedPathId, selectedChapterId, MARKER.getPos());
            PacketRegistry.CHAPTER_START_UPDATE.send(packet);
        }
        else if (!selectedChapterId.isEmpty())
        {
            ChapterStartRemovePacket packet = new ChapterStartRemovePacket(selectedPathId, selectedChapterId);
            PacketRegistry.CHAPTER_START_REMOVE.send(packet);
        }

        PathMarkerUpdatePacket packet = new PathMarkerUpdatePacket(MARKER.getPos(), MARKER.toNbt());
        PacketRegistry.PATH_MARKER_UPDATE.send(packet);
        MARKER.markUpdated();
    }

    /**
     * Saves the current marker and reconciles the mounted marker navigation list.
     */
    private void saveAndRefreshMarkerList() {
        save();
        formHash = calculateFormHash();
        patchSavedServerRow();
        refreshMarkerList(false);
        requestChapterMarkers();
    }

    /**
     * Updates the active server-provided row with freshly saved marker data.
     */
    private void patchSavedServerRow() {
        if (!serverListActive) return;

        long packedMarkerPos = MARKER.getPos().asLong();
        PathMarkerBlockEntity.ChapterNbtData data = MARKER.getChapterData(selectedPathId, selectedChapterId, false);
        if (data == null) return;

        serverMarkers = serverMarkers.stream()
                .map(entry -> entry.chainBreak() || entry.packedPos() != packedMarkerPos
                        ? entry
                        : ChapterMarkerEntry.marker(packedMarkerPos, data))
                .toList();
    }

    /**
     * Calculates a hash of the current form state to detect changes.
     *
     * @return the hash of the form state
     */
    private int calculateFormHash(){

        if (selectedPathId.isEmpty())
            return 1;

        var setCharRevealSpeed = parseIntegerOrFallback(charRevealInput, charRevealSpeed);
        var setFadeDelayOffset = parseIntegerOrFallback(fadeDelayOffsetInput, fadeDelayOffset);
        var setFadeDelayFactor = parseIntegerOrFallback(fadeDelayFactorInput, fadeDelayFactor);
        var setFadeSpeed = parseIntegerOrFallback(fadeSpeedInput, fadeSpeed);
        var setMinOpacity = parseIntegerOrFallback(minOpacityInput, minOpacity);
        var setTimeOfDay = TimeOfDay.snap(parseTimeOfDayOrFallback(timeOfDayInput, timeOfDay));
        var setTimeTransitionRange = parseTransitionRangeOrFallback(timeTransitionRangeInput, timeTransitionRange);
        var setAutoTeleportTarget = parseTextOrFallback(autoTeleportTargetInput, autoTeleportTarget);
        var setLookAt = parseTextOrFallback(lookAtInput, lookAt);
        var setGiveItem = parseTextOrFallback(giveItemInput, giveItem);

        return Objects.hash(
                proximityMessage,
                activationRange,
                displayAboveBlocks,
                isChapterStart,
                showChapterStartTitle,
                setCharRevealSpeed,
                setFadeDelayOffset,
                setFadeDelayFactor,
                setFadeSpeed,
                setMinOpacity,
                weather,
                setTimeOfDay,
                setTimeTransitionRange,
                setAutoTeleportTarget,
                setLookAt,
                setGiveItem
        );
    }

    /**
     * Parses a text input, falling back to the last known valid value if the field is invalid.
     *
     * @param input         the input widget to parse
     * @param fallbackValue the value to use when validation fails
     * @return the input text or the fallback value
     */
    private String parseTextOrFallback(InputBoxWidget input, String fallbackValue) {
        if (input == null) return fallbackValue;
        if (!input.validateText()) return fallbackValue;
        return input.getText().trim();
    }

    /**
     * Parses an integer input, falling back to the last known valid value if the field is malformed.
     *
     * @param input        the input widget to parse
     * @param fallbackValue the value to use when parsing fails
     * @return the parsed integer or the fallback value
     */
    private int parseIntegerOrFallback(InputBoxWidget input, int fallbackValue) {
        if (input == null) return fallbackValue;

        try {
            return Integer.parseInt(input.getText());
        } catch (NumberFormatException e) {
            return fallbackValue;
        }
    }

    /**
     * Parses a time-of-day input, falling back to the last known valid value if the field is malformed.
     *
     * @param input         the input widget to parse
     * @param fallbackValue the value to use when parsing fails
     * @return the parsed time-of-day ticks or the fallback value
     */
    private int parseTimeOfDayOrFallback(InputBoxWidget input, int fallbackValue) {
        if (input == null) return fallbackValue;

        try {
            return TimeOfDay.parse(input.getText());
        } catch (TextValidationError e) {
            return fallbackValue;
        }
    }

    /**
     * Parses a transition range input, falling back to the last known valid value if the field is malformed.
     *
     * @param input         the input widget to parse
     * @param fallbackValue the value to use when parsing fails
     * @return parsed transition range or the fallback value
     */
    private int parseTransitionRangeOrFallback(InputBoxWidget input, int fallbackValue) {
        if (input == null) return fallbackValue;

        try {
            return TimeOfDay.parseTransitionRange(input.getText());
        } catch (TextValidationError e) {
            return fallbackValue;
        }
    }

    /**
     * Validates the optional current-marker time.
     *
     * @param text input text to validate
     * @throws TextValidationError when the time is malformed
     */
    private void validateCurrentTimeOfDay(String text) throws TextValidationError {
        TimeOfDay.parse(text);
    }

    /**
     * Validates the marker time transition mode or fixed transition range.
     *
     * @param text input text to validate
     * @throws TextValidationError when the range text is malformed
     */
    private void validateTimeTransitionRange(String text) throws TextValidationError {
        TimeOfDay.parseTransitionRange(text);
    }

    /**
     * Validates the optional auto-teleport target shape.
     *
     * @param text input text to validate
     * @throws TextValidationError when the target is neither coordinates nor a single-token warp name
     */
    private void validateAutoTeleportTarget(String text) throws TextValidationError {
        String value = text.trim();
        if (value.isEmpty() || WarpTarget.isCoordinates(value)) return;
        if (!value.isBlank() && !value.matches(".*\\s+.*")) return;
        throw new TextValidationError(Text.translatable("ardapaths.client.marker.configuration.screens.auto_teleport_target.invalid").getString());
    }

    /**
     * Validates the optional client focus target shape.
     *
     * @param text input text to validate
     * @throws TextValidationError when the target is not blank or coordinate text
     */
    private void validateLookAt(String text) throws TextValidationError {
        String value = text.trim();
        if (value.isEmpty() || WarpTarget.isCoordinates(value)) return;
        throw new TextValidationError(Text.translatable("ardapaths.client.marker.configuration.screens.look_at.invalid").getString());
    }

    /**
     * Validates that the optional give-item value names a registered item.
     *
     * @param text input text to validate
     * @throws TextValidationError when the item identifier is malformed or unknown
     */
    private void validateGiveItem(String text) throws TextValidationError {
        String value = text.trim();
        if (value.isEmpty()) return;
        if (GiveItemAction.isClear(value)) return;

        Identifier id = Identifier.tryParse(value);
        if (id != null && Registries.ITEM.containsId(id)) return;

        throw new TextValidationError(Text.translatable("ardapaths.client.marker.configuration.screens.give_item.invalid").getString());
    }

    /**
     * Handles the timeout path for a time-spread request whose response callback expired.
     */
    private void onTimeSpreadTimeout() {
        timeSpreadFeedback = Text.translatable("ardapaths.client.marker.configuration.screens.marker.time_spread.timeout");
        timeSpreadFeedbackError = true;
        reloadFromMarker = false;
    }

    /**
     * Handles the server response for a marker time-spread request.
     *
     * @param response server response packet
     */
    @SuppressWarnings("resource")
    private void onTimeSpreadResponse(MarkerTimeSpreadResponsePacket response) {
        var minecraftClient = Client.mc();
        minecraftClient.execute(() -> {
            if (minecraftClient.world == null) {
                return;
            }

            timeSpreadFeedback = timeSpreadStatusText(response);
            timeSpreadFeedbackError = response.status() != TimeSpreadStatus.OK && response.status() != TimeSpreadStatus.OK_STEP_EXCEEDED;
            if (minecraftClient.currentScreen instanceof BusyPopup) {
                minecraftClient.setScreen(this);
            }

            if (response.status() == TimeSpreadStatus.OK || response.status() == TimeSpreadStatus.OK_STEP_EXCEEDED) {
                reloadFromMarker = true;
                clearAndInit();
            } else {
                reloadFromMarker = false;
                clearAndInit();
            }
        });
    }

    /**
     * Handles the server response for a marker bulk-clear request.
     *
     * @param response server response packet
     */
    @SuppressWarnings("resource")
    private void onBulkClearResponse(MarkerBulkClearResponsePacket response) {
        var minecraftClient = Client.mc();
        minecraftClient.execute(() -> {
            if (minecraftClient.world == null) {
                return;
            }

            timeSpreadFeedback = Text.translatable("ardapaths.client.marker.configuration.screens.marker.bulk_clear.status." + response.status().name().toLowerCase(Locale.ROOT), response.updatedCount());
            timeSpreadFeedbackError = response.status() != TimeSpreadStatus.OK;
            if (minecraftClient.currentScreen instanceof BusyPopup) {
                minecraftClient.setScreen(this);
            }

            if (response.status() == TimeSpreadStatus.OK) {
                requestChapterMarkers();
                reloadFromMarker = selectedMarkers.contains(MARKER.getPos());
            } else {
                reloadFromMarker = false;
            }
            clearAndInit();
        });
    }

    /**
     * Converts a time-spread response into localized feedback text.
     *
     * @param response server response packet
     * @return localized status text
     */
    private Text timeSpreadStatusText(MarkerTimeSpreadResponsePacket response) {
        if (response.status() == TimeSpreadStatus.CHAIN_BROKEN || response.status() == TimeSpreadStatus.CHAIN_ENDED) {
            if (response.lastValidPos() == null) {
                return Text.translatable("ardapaths.client.marker.configuration.screens.marker.time_spread.status.invalid_data");
            }

            BlockPos pos = response.lastValidPos();
            String key = "ardapaths.client.marker.configuration.screens.marker.time_spread.status." + response.status().name().toLowerCase(Locale.ROOT);
            return Text.translatable(key, pos.getX(), pos.getY(), pos.getZ());
        }

        String key = "ardapaths.client.marker.configuration.screens.marker.time_spread.status." + response.status().name().toLowerCase(Locale.ROOT);
        return Text.translatable(key, response.updatedCount());
    }

    /**
     * Converts a remote path marker data response into localized feedback text.
     *
     * @param response server response packet
     * @return localized status text
     */
    private Text remoteMarkerStatusText(PathMarkerRemoteDataResponsePacket response) {
        String key = "ardapaths.client.marker.configuration.screens.marker.load.status." + response.status().name().toLowerCase(Locale.ROOT);
        return Text.translatable(key);
    }

    /**
     * Checks if the form has been modified since initialization.
     *
     * @return true if the form has been modified, false otherwise
     */
    private boolean wasFormModified(){

        return calculateFormHash() != formHash;
    }

    /**
     * Removes any path/chapter associations added since opening the screen.
     */
    private void discardChapterAndPathDataChanges(){

        var pathData = MARKER.getPathData();

        for (var pathEntryKey : pathData.keySet()) {
            var chapters = pathData.get(pathEntryKey);
            var iterator = chapters.keySet().iterator();

            while (iterator.hasNext()) {
                var chapterEntryKey = iterator.next();
                var comparedEntry = new AbstractMap.SimpleEntry<>(pathEntryKey, chapterEntryKey);

                if (!originalPathAndChapterData.contains(comparedEntry)) {
                    iterator.remove();
                }
            }
        }
        MARKER.markUpdated();
    }

    /**
     * Generates a formatted text summary of newly added path/chapter associations.
     *
     * @return text listing the modified path and chapter entries
     */
    private Text listModifiedPathAndChapterData(){

        MutableText modifiedEntries = Text.empty();

        if (!originalPathAndChapterData.isEmpty()) {

            var pathData = MARKER.getPathData();

            for (var pathEntryKey : pathData.keySet()) {

                for (var chapterEntryKey : pathData.get(pathEntryKey).keySet()) {

                    var comparedEntry = new AbstractMap.SimpleEntry<>(pathEntryKey, chapterEntryKey);
                    var chapterData = MARKER.getChapterData(pathEntryKey, chapterEntryKey);

                    var isSelectedPathAndChapter = pathEntryKey.equals(selectedPathId) && chapterEntryKey.equals(selectedChapterId) && wasFormModified();

                    if (!isSelectedPathAndChapter && chapterData.isEmpty()) continue;

                    if (!originalPathAndChapterData.contains(comparedEntry)) {

                        var configuredPath = ArdaPathsClient.CONFIG.getPath(pathEntryKey);

                        if (configuredPath == null) continue;

                        var chapter = configuredPath.getChapter(chapterEntryKey);

                        if (chapter == null) continue;

                        modifiedEntries.append(Text.literal(configuredPath.getName()).styled(style -> style.withColor(configuredPath.getPrimaryColor().asHex())))
                                .append(Text.literal(" - "))
                                .append(Text.literal(chapter.getName()).styled(style -> style.withColor(configuredPath.getSecondaryColor().asHex())))
                                .append(Text.literal(" "));
                    }
                }
            }
        }

        return modifiedEntries;
    }

    /**
     * Checks if a path/chapter combination was originally linked to this marker.
     *
     * @param pathId the path ID to check
     * @param chapterId the chapter ID to check
     * @return true if the path/chapter was in the original marker data, false otherwise
     */
    private boolean isPathAndChapterLinked(String pathId, String chapterId) {

        return originalPathAndChapterData.contains(new AbstractMap.SimpleEntry<>(pathId, chapterId));
    }

    /**
     * Captures the current set of path/chapter associations for change tracking.
     *
     * @return the initial set of path/chapter entries linked to this marker
     */
    private Set<AbstractMap.SimpleEntry<String, String>> trackInitialPathAndChapterData(){

        Set<AbstractMap.SimpleEntry<String, String>> pathAndChapterData = new HashSet<>();

        var pathData = MARKER.getPathData();

        for (var pathEntryKey : pathData.keySet()) {

            for (var chapterEntryKey : pathData.get(pathEntryKey).keySet()) {

                boolean isDefault = MARKER.getPathData().get(pathEntryKey).get(chapterEntryKey) == null || MARKER.getPathData().get(pathEntryKey).get(chapterEntryKey).isEmpty();
                if (!isDefault) pathAndChapterData.add(new AbstractMap.SimpleEntry<>(pathEntryKey, chapterEntryKey));
            }
        }

        return pathAndChapterData;
    }
}

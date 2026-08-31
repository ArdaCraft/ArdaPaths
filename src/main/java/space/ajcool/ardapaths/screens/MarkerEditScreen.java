package space.ajcool.ardapaths.screens;

import lombok.extern.slf4j.Slf4j;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;
import space.ajcool.ardapaths.ArdaPathsClient;
import space.ajcool.ardapaths.core.Client;
import space.ajcool.ardapaths.core.data.ChapterMarkerEntry;
import space.ajcool.ardapaths.core.data.MarkerId;
import space.ajcool.ardapaths.core.data.PathMarkerRemoteDataStatus;
import space.ajcool.ardapaths.core.data.TimeSpreadStatus;
import space.ajcool.ardapaths.core.data.config.shared.ChapterData;
import space.ajcool.ardapaths.core.data.config.shared.PathData;
import space.ajcool.ardapaths.core.networking.PacketRegistry;
import space.ajcool.ardapaths.core.networking.packets.client.MarkerBulkClearResponsePacket;
import space.ajcool.ardapaths.core.networking.packets.client.MarkerTimeSpreadResponsePacket;
import space.ajcool.ardapaths.core.networking.packets.client.PathMarkerRemoteDataResponsePacket;
import space.ajcool.ardapaths.core.networking.packets.server.*;
import space.ajcool.ardapaths.mc.blocks.ModBlocks;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;
import space.ajcool.ardapaths.paths.Paths;
import space.ajcool.ardapaths.paths.rendering.TrailRenderer;
import space.ajcool.ardapaths.screens.marker.*;
import space.ajcool.ardapaths.screens.widgets.CheckboxWidget;
import space.ajcool.ardapaths.screens.widgets.DropdownWidget;
import space.ajcool.ardapaths.screens.widgets.TabBarWidget;
import space.ajcool.ardapaths.screens.widgets.TextWidget;

import java.util.*;
import java.util.function.Supplier;

import static space.ajcool.ardapaths.screens.marker.MarkerEditLayout.*;

/**
 * Screen for editing a path marker block's configuration.
 * Allows editing proximity messages, activation ranges, chapter assignments, visual effects (fade, opacity),
 * chapter start positions, and animation parameters.
 */
@Environment(value = EnvType.CLIENT)
@Slf4j(topic = "ardapaths")
public class MarkerEditScreen extends ArdaPathsScreen {

    /** The marker block entity being edited. */
    private final PathMarkerBlockEntity MARKER;

    /** Mutable marker form values for the current selected path and chapter. */
    private final MarkerFormState state = new MarkerFormState();

    /** Tab sections mounted into the fixed marker editor content area. */
    private final List<MarkerEditorTab> tabs;

    /** Controller for marker-list rows, selection, context menu, and chapter row requests. */
    private final MarkerListController markerList;

    /** Tracks original path/chapter associations for this edit session. */
    private final MarkerLinkTracker linkTracker;

    /** ID of the currently selected path in the form. */
    private String selectedPathId;

    /** ID of the currently selected chapter in the form. */
    private String selectedChapterId;

    /** Checkbox controlling whether to show chapter title on the trail. */
    private CheckboxWidget displayChapterTitleOnTrail;

    /** Currently visible tab in the fixed content area. */
    private int activeTab;

    /** Whether the next initialization should reload form values from marker NBT. */
    private boolean reloadFromMarker = true;

    /** Server feedback from the most recent time-spread request. */
    private Component timeSpreadFeedback;

    /** Whether the current time-spread feedback represents an error state. */
    private boolean timeSpreadFeedbackError;

    /** Feedback from the most recent marker-load request. */
    private Component markerLoadFeedback;

    /** Whether the current marker-load feedback represents an error state. */
    private boolean markerLoadFeedbackError;

    /** Hash of the form state to detect if the user has made changes. */
    private int formHash;

    /**
     * Initializes a marker edit screen with the given marker.
     *
     * @param marker the marker block entity to edit
     */
    public MarkerEditScreen(PathMarkerBlockEntity marker) {
        this(marker, null, List.of(marker.getBlockPos().immutable()));
    }

    /**
     * Initializes a marker edit screen with the given marker and original path/chapter data.
     *
     * @param marker                     the marker block entity to edit
     * @param originalPathAndChapterData the original path/chapter associations, or null to track from current state
     */
    public MarkerEditScreen(PathMarkerBlockEntity marker, Set<AbstractMap.SimpleEntry<String, String>> originalPathAndChapterData) {
        this(marker, originalPathAndChapterData, List.of(marker.getBlockPos().immutable()));
    }

    /**
     * Initializes a marker edit screen with the given marker, tracked associations, and selected rows.
     *
     * @param marker                     the marker block entity to edit
     * @param originalPathAndChapterData the original path/chapter associations, or null to track from current state
     * @param initialSelection           visible marker rows that should remain selected
     */
    public MarkerEditScreen(PathMarkerBlockEntity marker, Set<AbstractMap.SimpleEntry<String, String>> originalPathAndChapterData, Collection<BlockPos> initialSelection) {
        super(Component.literal("Path Marker Edit Screen"));
        MARKER = marker;
        selectedPathId = ArdaPathsClient.CONFIG.getSelectedPathId();
        selectedChapterId = ArdaPathsClient.CONFIG.getCurrentChapterId();
        tabs = List.of(
                new GeneralTabSection(),
                new TimeWeatherTabSection(() -> timeSpreadFeedback, () -> timeSpreadFeedbackError),
                new MiscTabSection()
        );
        this.linkTracker = new MarkerLinkTracker(marker, originalPathAndChapterData);
        markerList = new MarkerListController(
                marker,
                selectedPathId,
                selectedChapterId,
                initialSelection,
                this::add,
                this::remove,
                this::switchToMarker,
                this::teleportToMarker,
                this::confirmBulkClear,
                this::openTimeInterpolationPopup
        );

        PathMarkerBlockEntity.ChapterNbtData data = ChapterMarkerChain.selectedChapterData(marker, selectedPathId, selectedChapterId);

        state.loadFrom(data);
    }

    /**
     * Adds a widget owned by a marker tab section to this screen.
     *
     * @param widget tab widget to add
     * @param <T>    widget type accepted by Minecraft's screen widget list
     * @return the added widget
     */
    public <T extends GuiEventListener & Renderable & NarratableEntry> T add(T widget) {
        return addRenderableWidget(widget);
    }

    /**
     * Removes a widget owned by a marker helper from this screen.
     *
     * @param widget widget to remove
     */
    public void remove(GuiEventListener widget) {
        removeWidget(widget);
    }

    /**
     * Initializes and lays out all UI elements for the marker editor.
     */
    @Override
    protected void init() {
        super.init();

        PathMarkerBlockEntity.ChapterNbtData data = ChapterMarkerChain.selectedChapterData(MARKER, selectedPathId, selectedChapterId);

        if (reloadFromMarker) {
            state.loadFrom(data);
        }

        MarkerEditLayout layout = MarkerEditLayout.of(this.width, this.height);
        int centerX = layout.centerX();
        int top = layout.top();
        int footerY = layout.footerY();

        this.buildMarkerIdButton(centerX, top - 10);
        this.buildTitle(centerX, top + 10);
        this.buildPathSelectionDropdown(layout.leftColumnX(), top + PATH_Y_OFFSET);
        this.buildChapterSelectionDropdown(layout.leftColumnX(), top + CHAPTER_Y_OFFSET);
        this.buildEditChaptersButton(centerX + 40, top + CHAPTER_Y_OFFSET);
        this.buildChapterStartCheckbox(layout.leftColumnX(), top + CHAPTER_START_Y_OFFSET);
        displayChapterTitleOnTrail = this.buildChapterStartHideTitleCheckbox(centerX + 5, top + CHAPTER_START_Y_OFFSET);
        this.buildTabBar(layout.leftColumnX(), top + TAB_BAR_Y_OFFSET, layout.tabContentHeight());

        tabs.get(activeTab).build(this, layout, state);

        this.buildDisplayAboveBlocksCheckbox(layout.leftColumnX(), footerY);
        this.buildCloseButton(centerX + 15, footerY - 2);
        this.buildSaveButton(centerX + 80, footerY - 2);
        markerList.setChapter(selectedPathId, selectedChapterId);
        markerList.build(layout);
        this.buildMarkerLoadFeedback(centerX, footerY);

        if (reloadFromMarker) {
            formHash = calculateFormHash();
        }
        reloadFromMarker = true;
    }

    /**
     * Copies values from currently mounted widgets into durable fields before the widgets are discarded.
     */
    private void commitInputsToFields() {
        tabs.get(activeTab).commitTo(state);
    }

    /**
     * Builds the fixed tab selector between the marker header and content area.
     *
     * @param x             the x coordinate of the tab bar
     * @param y             the y coordinate of the tab bar
     * @param contentHeight the active content height under the tab bar
     */
    private void buildTabBar(int x, int y, int contentHeight) {
        this.addRenderableWidget(TabBarWidget.create()
                .setX(x)
                .setY(y)
                .setWidth(MAIN_CONTROL_WIDTH)
                .setHeight(CONTROL_HEIGHT)
                .setTabs(List.of(
                        Component.translatable("ardapaths.client.marker.configuration.screens.tab.general"),
                        Component.translatable("ardapaths.client.marker.configuration.screens.tab.time_and_weather"),
                        Component.translatable("ardapaths.client.marker.configuration.screens.tab.misc")
                ))
                .setSelectedIndex(activeTab)
                .setContentHeight(contentHeight)
                .setOnSelect(index ->
                {
                    commitInputsToFields();
                    activeTab = index;
                    reloadFromMarker = false;
                    this.rebuildWidgets();
                })
                .build()
        );
    }

    /**
     * Creates and adds the title showing linked paths and chapters to the screen.
     *
     * @param x the x coordinate of the subtitle
     * @param y the y coordinate of the subtitle
     */
    private void buildTitle(int x, int y) {

        MarkerLinkTracker.LinkCounts linkCounts = linkTracker.linkCounts();
        int linkedPaths = linkCounts.paths();
        int linkedChapters = linkCounts.chapters();

        if (linkedPaths >= 1 || linkedChapters >= 1) {

            var message = Component.translatable("ardapaths.client.marker.configuration.screens.linked_chapters_and_paths", linkedPaths, linkedChapters);
            boolean hasLinks = linkedChapters >= 1;

            var fullWidth = font.width(message);

            if (hasLinks)
                fullWidth += 70;

            this.addRenderableWidget(TextWidget.create()
                    .setX(x - (fullWidth / 2))
                    .setY(y)
                    .setWidth(font.width(message))
                    .setHeight(20)
                    .setMessage(message)
                    .build()
            );

            if (hasLinks)
                this.buildMarkerEditLinksButton(x + (fullWidth / 2) - 60, y);
        } else {
            var message = Component.translatable("ardapaths.client.marker.configuration.screens.no_linked_chapters_and_paths");
            var fullWidth = font.width(message);
            this.addRenderableWidget(TextWidget.create()
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
     * Validates all input fields in the form.
     *
     * @return true if all inputs are valid, false otherwise
     */
    private boolean validateForm() {
        return tabs.get(activeTab).validate();
    }

    /**
     * Confirms a bulk clear operation with the player.
     *
     * @param clearTime    whether time data should be cleared
     * @param clearWeather whether weather data should be cleared
     */
    private void confirmBulkClear(boolean clearTime, boolean clearWeather) {
        markerList.closeContextMenu();
        if (this.minecraft == null) return;
        Component message = clearTime
                ? Component.translatable("ardapaths.client.marker.configuration.screens.marker_list.clear_time.confirm", markerList.selectedMarkers().size())
                : Component.translatable("ardapaths.client.marker.configuration.screens.marker_list.clear_weather.confirm", markerList.selectedMarkers().size());
        this.minecraft.setScreen(new ConfirmationPopup(message, () -> startBulkClear(clearTime, clearWeather), () -> {
        }, this));
    }

    /**
     * Sends a confirmed bulk clear request to the server.
     *
     * @param clearTime    whether time data should be cleared
     * @param clearWeather whether weather data should be cleared
     */
    private void startBulkClear(boolean clearTime, boolean clearWeather) {
        if (this.minecraft == null) return;
        List<Long> packedPositions = markerList.selectedMarkers().stream().map(BlockPos::asLong).toList();
        timeSpreadFeedback = null;
        this.minecraft.setScreen(new BusyPopup(
                Component.translatable("ardapaths.client.marker.configuration.screens.marker.time_spread.busy"),
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
        markerList.closeContextMenu();
        List<BlockPos> selectedMarkers = markerList.selectedMarkers();
        if (this.minecraft == null || selectedMarkers.size() < 2) return;
        this.minecraft.setScreen(new TimeInterpolationPopup(
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
        if (this.minecraft == null) return;
        timeSpreadFeedback = null;
        this.minecraft.setScreen(new BusyPopup(
                Component.translatable("ardapaths.client.marker.configuration.screens.marker.time_spread.busy"),
                this,
                this::onTimeSpreadTimeout
        ));
        PacketRegistry.MARKER_TIME_SPREAD.send(
                new MarkerTimeSpreadPacket(endpoints.startPacked(), endpoints.endPacked(), endpoints.startTime(), endpoints.endTime(), selectedPathId, selectedChapterId, false),
                this::onTimeSpreadResponse
        );
    }

    /**
     * Opens a fresh marker editor while preserving an explicit marker-list selection.
     *
     * @param pos       marker position to edit
     * @param selection marker positions selected in list order
     */
    private void switchToMarker(BlockPos pos, Collection<BlockPos> selection) {
        markerList.setSelectedMarkers(selection);
        if (pos.equals(MARKER.getBlockPos())) {
            markerList.refresh(false);
            return;
        }

        PathMarkerBlockEntity target = findTickingMarker(pos);
        if (target == null) {
            promptPendingChangesThen(() -> requestRemoteMarker(pos, markerList.selectedMarkers()), this, false);
            return;
        }

        MarkerEditScreen nextScreen = new MarkerEditScreen(target, null, markerList.selectedMarkers());
        promptPendingChangesThen(() -> {
            nextScreen.seedChapterMarkers(markerList.serverMarkers(), markerList.serverListActive(), markerList.currentMarkerListScrollAmount());
            if (this.minecraft != null) this.minecraft.setScreen(nextScreen);
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
        markerList.seedChapterMarkers(markers, active, scrollAmount);
    }

    /**
     * Sends a teleport request to the selected marker after handling pending changes.
     *
     * @param pos the marker position to teleport to
     */
    @SuppressWarnings("resource")
    private void teleportToMarker(BlockPos pos) {
        if (this.minecraft == null || this.minecraft.player == null) return;

        ResourceLocation worldId = this.minecraft.player.level().dimension().location();
        promptPendingChangesThen(() -> {
            PacketRegistry.PLAYER_TELEPORT.send(new PlayerTeleportPacket(pos.getX(), pos.getY(), pos.getZ(), worldId));
            if (this.minecraft != null) this.minecraft.setScreen(null);
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
                .filter(marker -> marker.getBlockPos().equals(pos))
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
        if (this.minecraft == null) return;

        markerLoadFeedback = null;
        this.minecraft.setScreen(new BusyPopup(
                Component.translatable("ardapaths.client.marker.configuration.screens.marker.load.busy"),
                this,
                this::onMarkerLoadTimeout
        ));
        PacketRegistry.PATH_MARKER_REMOTE_DATA.send(new PathMarkerRemoteDataPacket(pos.asLong()), response -> onMarkerLoadResponse(response, selection));
    }

    /**
     * Handles the timeout path for a marker-load request whose response callback expired.
     */
    private void onMarkerLoadTimeout() {
        markerLoadFeedback = Component.translatable("ardapaths.client.marker.configuration.screens.marker.load.timeout");
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
            if (minecraftClient.level == null) {
                return;
            }

            if (response.status() == PathMarkerRemoteDataStatus.OK) {
                BlockPos pos = BlockPos.of(response.packedPos()).immutable();
                PathMarkerBlockEntity remote = new PathMarkerBlockEntity(pos, ModBlocks.PATH_MARKER.defaultBlockState());
                remote.load(response.data());
                MarkerEditScreen nextScreen = new MarkerEditScreen(remote, null, selection);
                nextScreen.seedChapterMarkers(markerList.serverMarkers(), markerList.serverListActive(), markerList.currentMarkerListScrollAmount());
                minecraftClient.setScreen(nextScreen);
                return;
            }

            markerLoadFeedback = MarkerFeedbackText.remoteMarkerStatusText(response);
            markerLoadFeedbackError = true;
            if (minecraftClient.screen instanceof BusyPopup) {
                minecraftClient.setScreen(this);
            }
            reloadFromMarker = false;
            rebuildWidgets();
        });
    }

    /**
     * Creates and adds the path selection dropdown widget.
     *
     * @param x the x coordinate of the dropdown
     * @param y the y coordinate of the dropdown
     */
    private void buildPathSelectionDropdown(int x, int y) {
        this.addRenderableWidget(DropdownWidget.<PathData>create()
                .setPosition(x, y)
                .setSize(280, 20)
                .setTitle(Component.translatable("ardapaths.client.marker.configuration.screens.edit_path_data"))
                .setOptions(ArdaPathsClient.CONFIG.getPaths())
                .setOptionDisplay(item ->
                {
                    if (item == null)
                        return Component.translatable("ardapaths.client.marker.configuration.screens.no_path");

                    MutableComponent label = Component.literal(item.getName()).withStyle(Style.EMPTY.withColor(item.getPrimaryColor().asHex()));

                    if (linkTracker.isPathLinked(item.getId())) label.append(" •");

                    return label;
                })
                .setSelected(ArdaPathsClient.CONFIG.getPath(selectedPathId))
                .setOnSelect(path ->
                {
                    selectedPathId = path.getId();
                    selectedChapterId = chapterForSelectedPath(path);
                    markerList.resetChapter(selectedPathId, selectedChapterId);
                    syncPathfinderSelection();
                    this.rebuildWidgets();
                })
                .build()
        );
    }

    /**
     * Chooses the best chapter to edit when the path dropdown changes.
     *
     * @param path newly selected path
     * @return selected chapter ID for the path
     */
    private String chapterForSelectedPath(PathData path) {
        if (path.getId().equals(ArdaPathsClient.CONFIG.getSelectedPathId())) {
            return ArdaPathsClient.CONFIG.getCurrentChapterId();
        }

        String currentChapterName = currentPathfinderChapterName();
        List<ChapterData> linkedConfiguredChapters = linkTracker.linkedChapterIds(path.getId()).stream()
                .map(path::getChapter)
                .filter(Objects::nonNull)
                .filter(chapter -> !"default".equals(chapter.getId()))
                .toList();

        if (currentChapterName != null) {
            for (ChapterData chapter : linkedConfiguredChapters) {
                if (chapter.getName().equalsIgnoreCase(currentChapterName)) {
                    return chapter.getId();
                }
            }
        }

        if (!linkedConfiguredChapters.isEmpty()) {
            return linkedConfiguredChapters.stream()
                    .min(Comparator.comparing(ChapterData::getName, String.CASE_INSENSITIVE_ORDER))
                    .map(ChapterData::getId)
                    .orElse(linkedConfiguredChapters.get(0).getId());
        }

        if (linkTracker.isPathAndChapterLinked(path.getId(), "default")) {
            return "default";
        }

        List<String> chapterIds = path.getChapterIds();
        return chapterIds.isEmpty() ? "" : chapterIds.get(0);
    }

    /**
     * Resolves the display name of the chapter currently followed by the Pathfinder.
     *
     * @return current Pathfinder chapter name, or null when unavailable
     */
    private String currentPathfinderChapterName() {
        PathData selectedPath = ArdaPathsClient.CONFIG.getPath(ArdaPathsClient.CONFIG.getSelectedPathId());
        if (selectedPath == null) {
            return null;
        }

        ChapterData selectedChapter = selectedPath.getChapter(ArdaPathsClient.CONFIG.getCurrentChapterId());
        return selectedChapter == null ? null : selectedChapter.getName();
    }

    /**
     * Applies the editor's current path and chapter selection to the Pathfinder selection so the rendered trail follows
     * what is being edited, without teleporting the player.
     */
    private void syncPathfinderSelection() {
        if (selectedPathId.isEmpty()) return;

        boolean pathChanged = !selectedPathId.equalsIgnoreCase(ArdaPathsClient.CONFIG.getSelectedPathId());
        boolean chapterChanged = !selectedChapterId.equals(ArdaPathsClient.CONFIG.getCurrentChapterId());
        if (!pathChanged && !chapterChanged) return;

        if (pathChanged) TrailRenderer.clearTrails();
        ArdaPathsClient.lastVisitedTrailNodeData = null;

        Paths.setSelectedPath(selectedPathId);
        if (!selectedChapterId.isEmpty()) Paths.gotoChapter(selectedChapterId, false);
    }

    /**
     * Creates and adds the chapter selection dropdown widget.
     *
     * @param x the x coordinate of the dropdown
     * @param y the y coordinate of the dropdown
     */
    private void buildChapterSelectionDropdown(int x, int y) {
        PathData selectedPath = ArdaPathsClient.CONFIG.getPath(selectedPathId);

        List<ChapterData> chapters = selectedPath != null ? new ArrayList<>(selectedPath.getChapters()) : new ArrayList<>();
        chapters.sort(Comparator.comparingInt(ChapterData::getIndex));

        var selection = ArdaPathsClient.CONFIG.getPath(selectedPathId);
        var selectedChapter = selection != null ? selection.getChapter(selectedChapterId) : null;

        this.addRenderableWidget(DropdownWidget.<ChapterData>create()
                .setPosition(x, y)
                .setSize(175, 20)
                .setTitle(Component.translatable("ardapaths.client.marker.configuration.screens.chapter"))
                .setOptionDisplay(item ->
                {
                    if (item == null)
                        return Component.translatable("ardapaths.client.marker.configuration.screens.no_chapter");
                    MutableComponent label = Component.literal(item.getName());

                    if (linkTracker.isPathAndChapterLinked(selectedPathId, item.getId())) label.append(" •");

                    return label;
                })
                .setOptions(chapters)
                .setSelected(selectedChapter)
                .setOnSelect(chapter ->
                {
                    selectedChapterId = chapter.getId();
                    markerList.resetChapter(selectedPathId, selectedChapterId);
                    syncPathfinderSelection();
                    this.rebuildWidgets();
                })
                .build()
        );
    }

    /**
     * Adds marker-load feedback to the main panel when a remote marker request fails.
     *
     * @param centerX the x coordinate of the screen center
     * @param footerY the y coordinate of the footer row
     */
    private void buildMarkerLoadFeedback(int centerX, int footerY) {
        if (markerLoadFeedback == null) return;

        Component formattedFeedback = markerLoadFeedback.copy().withStyle(markerLoadFeedbackError ? ChatFormatting.RED : ChatFormatting.GRAY);
        this.addRenderableWidget(new TextWidget(centerX - MAIN_LEFT_OFFSET, footerY - 24, MAIN_CONTROL_WIDTH, 17, formattedFeedback));
    }

    /**
     * Creates and adds a button that copies this marker's selected path chapter ID.
     *
     * @param centerX the x coordinate of the screen center
     * @param top     the y coordinate of the button
     */
    @SuppressWarnings("resource")
    private void buildMarkerIdButton(int centerX, int top) {
        String markerId = MarkerId.format(MARKER.getBlockPos());
        Component label = Component.translatable("ardapaths.client.marker.configuration.screens.marker_id", markerId);
        int buttonWidth = Math.min(MAIN_CONTROL_WIDTH, font.width(label) + 10);
        Button button = this.addRenderableWidget(Button.builder(label, ignored ->
                        Client.mc().keyboardHandler.setClipboard(MarkerId.format(MARKER.getBlockPos())))
                .bounds(centerX - buttonWidth / 2, top, buttonWidth, CONTROL_HEIGHT)
                .build()
        );
        button.setTooltip(Tooltip.create(Component.translatable("ardapaths.client.marker.configuration.screens.marker_id_tooltip", markerId)));
    }

    /**
     * Creates and adds the button to open the marker links editor screen.
     *
     * @param x the x coordinate of the button
     * @param y the y coordinate of the button
     */
    private void buildMarkerEditLinksButton(int x, int y) {
        assert this.minecraft != null;
        this.addRenderableWidget(new Button(
                x,
                y,
                60,
                20,
                Component.translatable("ardapaths.client.marker.configuration.screens.edit_links"),
                button -> this.minecraft.setScreen(new MarkerLinksEditScreen(MARKER, linkTracker.originalEntries())),
                Supplier::get
        ));
    }

    /**
     * Creates and adds the button to open the chapter editor screen.
     *
     * @param x the x coordinate of the button
     * @param y the y coordinate of the button
     */
    private void buildEditChaptersButton(int x, int y) {
        assert this.minecraft != null;
        this.addRenderableWidget(new Button(
                x,
                y,
                100,
                20,
                Component.translatable("ardapaths.client.marker.configuration.screens.edit_chapters"),
                button -> this.minecraft.setScreen(new ChapterEditScreen(this)),
                Supplier::get
        ));
    }

    /**
     * Creates and adds the checkbox for marking this marker as a chapter start.
     *
     * @param x the x coordinate of the checkbox
     * @param y the y coordinate of the checkbox
     */
    private void buildChapterStartCheckbox(int x, int y) {
        this.addRenderableWidget(CheckboxWidget.create()
                .setX(x)
                .setY(y)
                .setWidth(135)
                .setHeight(15)
                .setText(Component.translatable("ardapaths.client.marker.configuration.screens.is_chapter_start"))
                .setChecked(state.isChapterStart())
                .setEnabled(true)
                .setOnChange(checked -> {
                    state.setChapterStart(checked);
                    if (displayChapterTitleOnTrail != null) {
                        displayChapterTitleOnTrail.setEnabled(state.isChapterStart());
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
    private CheckboxWidget buildChapterStartHideTitleCheckbox(int x, int y) {
        return addRenderableWidget(CheckboxWidget.create()
                .setX(x)
                .setY(y)
                .setWidth(135)
                .setHeight(15)
                .setText(Component.translatable("ardapaths.client.marker.configuration.screens.show_title_on_trail"))
                .setChecked(state.isShowChapterStartTitle())
                .setEnabled(state.isChapterStart())
                .setOnChange(state::setShowChapterStartTitle)
                .build()
        );
    }

    /**
     * Creates and adds the checkbox for displaying the marker above block surfaces.
     *
     * @param x the x coordinate of the checkbox
     * @param y the y coordinate of the checkbox
     */
    private void buildDisplayAboveBlocksCheckbox(int x, int y) {
        this.addRenderableWidget(CheckboxWidget.create()
                .setX(x)
                .setY(y)
                .setWidth(145)
                .setHeight(15)
                .setText(Component.translatable("ardapaths.client.marker.configuration.screens.display_trail_above_blocks"))
                .setChecked(state.isDisplayAboveBlocks())
                .setEnabled(true)
                .setOnChange(state::setDisplayAboveBlocks)
                .build()
        );
    }

    /**
     * Creates and adds the close button to discard changes and exit the screen.
     *
     * @param x the x coordinate of the button
     * @param y the y coordinate of the button
     */
    private void buildCloseButton(int x, int y) {
        this.addRenderableWidget(new Button(
                x,
                y,
                60,
                20,
                Component.translatable("ardapaths.generic.close"),
                button -> this.onClose(),
                Supplier::get
        ));
    }

    /**
     * Creates and adds the save button to persist form changes.
     *
     * @param x the x coordinate of the button
     * @param y the y coordinate of the button
     */
    private void buildSaveButton(int x, int y) {
        this.addRenderableWidget(new Button(
                x,
                y,
                60,
                20,
                Component.translatable("ardapaths.generic.save"),
                button ->
                {
                    if (validateForm()) {

                        commitInputsToFields();
                        saveAndRefreshMarkerList();

                    } else {

                        log.error(Component.translatable("ardapaths.generic.validation.form.errors").getString());
                    }
                },
                Supplier::get
        ));
    }

    /**
     * Renders the screen background, UI elements, and animation parameter labels.
     *
     * @param context the draw context for rendering
     * @param mouseX  the current mouse x coordinate
     * @param mouseY  the current mouse y coordinate
     * @param delta   the partial tick delta for animation
     */
    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        this.renderModBackground(context);
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
        if (markerList.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * Closes an open context menu when Esc is pressed.
     *
     * @param keyCode   GLFW key code
     * @param scanCode  hardware scan code
     * @param modifiers active key modifiers
     * @return true when the key is consumed
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && markerList.closeContextMenu()) {
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
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(mouseX, mouseY, button) ||
                tabs.get(activeTab).mouseReleased(mouseX, mouseY, button);
    }

    /**
     * Updates the multi-line edit box state each tick.
     */
    @Override
    public void tick() {
        tabs.get(activeTab).tick();
        markerList.tickSignature();

        super.tick();
    }

    /**
     * Closes the screen, prompting to save if changes have been made.
     */
    @Override
    public void onClose() {
        promptPendingChangesThen(super::onClose, this, true);
    }

    /**
     * Runs an action after prompting to save or discard pending marker changes.
     *
     * @param proceed         the action to run after pending changes are resolved
     * @param parentScreen    the screen the popup should return to after a callback
     * @param closeOnValidate whether the popup should close fully after a callback
     */
    private void promptPendingChangesThen(Runnable proceed, Screen parentScreen, boolean closeOnValidate) {
        Component validationWarning = pendingChangesWarning();
        var modifiedPathAndChapterData = linkTracker.listModifiedPathAndChapterData(selectedPathId, selectedChapterId, wasFormModified());
        Runnable discardOutcome = proceed;

        if (!modifiedPathAndChapterData.equals(Component.empty())) {
            discardOutcome = () -> {
                linkTracker.discardChapterAndPathDataChanges();
                proceed.run();
            };
        }

        if (!validationWarning.equals(Component.empty())) {
            assert this.minecraft != null;
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
            this.minecraft.setScreen(popup);
            return;

        }

        proceed.run();
    }

    /**
     * Builds the unsaved-changes warning message for the current marker state.
     *
     * @return warning text, or empty text when there are no pending changes
     */
    private Component pendingChangesWarning() {
        Component validationWarning = Component.empty();
        var modifiedPathAndChapterData = linkTracker.listModifiedPathAndChapterData(selectedPathId, selectedChapterId, wasFormModified());

        if (wasFormModified() && !modifiedPathAndChapterData.equals(Component.empty())) {

            validationWarning = Component.translatable("ardapaths.client.marker.configuration.screens.form.has_changes_added_path_and_chapter")
                    .append(modifiedPathAndChapterData)
                    .append(Component.translatable("ardapaths.generic.save_changes"));

        } else if (wasFormModified()) {

            validationWarning = Component.translatable("ardapaths.client.marker.configuration.screens.form.has_changes");

        } else if (!modifiedPathAndChapterData.equals(Component.empty())) {

            validationWarning = Component.translatable("ardapaths.client.marker.configuration.screens.form.added_path_and_chapter")
                    .append(modifiedPathAndChapterData)
                    .append(Component.translatable("ardapaths.generic.save_changes"));
        }

        return validationWarning;
    }

    /**
     * Persists all form data to the marker's NBT and sends update packets to the server.
     *
     */
    private void save() {
        if (selectedPathId.isEmpty()) return;

        PathMarkerBlockEntity.ChapterNbtData data = MARKER.getChapterData(selectedPathId, selectedChapterId, true);

        assert data != null;
        state.applyTo(data);
        if (data.isEmpty() && !linkTracker.isPathAndChapterLinked(selectedPathId, selectedChapterId)) {
            Map<String, PathMarkerBlockEntity.ChapterNbtData> chapters = MARKER.getPathData().get(selectedPathId);
            if (chapters != null) {
                chapters.remove(selectedChapterId);
                if (chapters.isEmpty()) {
                    MARKER.getPathData().remove(selectedPathId);
                }
            }
        }

        if (state.isChapterStart()) {
            ChapterStartUpdatePacket packet = new ChapterStartUpdatePacket(selectedPathId, selectedChapterId, MARKER.getBlockPos());
            PacketRegistry.CHAPTER_START_UPDATE.send(packet);
        } else if (!selectedChapterId.isEmpty()) {
            ChapterStartRemovePacket packet = new ChapterStartRemovePacket(selectedPathId, selectedChapterId, MARKER.getBlockPos());
            PacketRegistry.CHAPTER_START_REMOVE.send(packet);
        }

        PathMarkerUpdatePacket packet = new PathMarkerUpdatePacket(MARKER.getBlockPos(), MARKER.toNbt());
        PacketRegistry.PATH_MARKER_UPDATE.send(packet);
        MARKER.markUpdated();
        syncPathfinderSelection();
    }

    /**
     * Saves the current marker and reconciles the mounted marker navigation list.
     */
    private void saveAndRefreshMarkerList() {
        save();
        formHash = calculateFormHash();
        markerList.patchSavedServerRow();
        markerList.refresh(false);
        markerList.requestChapterMarkers();
    }

    /**
     * Calculates a hash of the current form state to detect changes.
     *
     * @return the hash of the form state
     */
    private int calculateFormHash() {

        if (selectedPathId.isEmpty())
            return 1;

        MarkerFormState snapshot = state.copy();
        tabs.get(activeTab).commitTo(snapshot);
        return snapshot.hash();
    }

    /**
     * Handles the timeout path for a time-spread request whose response callback expired.
     */
    private void onTimeSpreadTimeout() {
        timeSpreadFeedback = Component.translatable("ardapaths.client.marker.configuration.screens.marker.time_spread.timeout");
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
            if (minecraftClient.level == null) {
                return;
            }

            timeSpreadFeedback = MarkerFeedbackText.timeSpreadStatusText(response);
            timeSpreadFeedbackError = response.status() != TimeSpreadStatus.OK && response.status() != TimeSpreadStatus.OK_STEP_EXCEEDED;
            if (minecraftClient.screen instanceof BusyPopup) {
                minecraftClient.setScreen(this);
            }

            if (response.status() == TimeSpreadStatus.OK || response.status() == TimeSpreadStatus.OK_STEP_EXCEEDED) {
                reloadFromMarker = true;
                rebuildWidgets();
            } else {
                reloadFromMarker = false;
                rebuildWidgets();
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
            if (minecraftClient.level == null) {
                return;
            }

            timeSpreadFeedback = MarkerFeedbackText.bulkClearStatusText(response);
            timeSpreadFeedbackError = response.status() != TimeSpreadStatus.OK;
            if (minecraftClient.screen instanceof BusyPopup) {
                minecraftClient.setScreen(this);
            }

            if (response.status() == TimeSpreadStatus.OK) {
                markerList.requestChapterMarkers();
                reloadFromMarker = markerList.selectedMarkers().contains(MARKER.getBlockPos());
            } else {
                reloadFromMarker = false;
            }
            rebuildWidgets();
        });
    }

    /**
     * Checks if the form has been modified since initialization.
     *
     * @return true if the form has been modified, false otherwise
     */
    private boolean wasFormModified() {

        return calculateFormHash() != formHash;
    }
}

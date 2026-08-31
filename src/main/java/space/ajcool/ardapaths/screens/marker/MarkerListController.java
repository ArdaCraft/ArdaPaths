package space.ajcool.ardapaths.screens.marker;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import space.ajcool.ardapaths.core.Client;
import space.ajcool.ardapaths.core.data.ChapterMarkerEntry;
import space.ajcool.ardapaths.core.data.ChapterMarkersStatus;
import space.ajcool.ardapaths.core.networking.PacketRegistry;
import space.ajcool.ardapaths.core.networking.packets.client.ChapterPathMarkersResponsePacket;
import space.ajcool.ardapaths.core.networking.packets.server.ChapterPathMarkersPacket;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;
import space.ajcool.ardapaths.screens.widgets.ContextMenuWidget;
import space.ajcool.ardapaths.screens.widgets.MarkerListEntry;
import space.ajcool.ardapaths.screens.widgets.MarkerListPanelWidget;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Controller for the marker editor navigation list and its local/server row state.
 */
public class MarkerListController {
    /** Marker block entity being edited. */
    private final PathMarkerBlockEntity marker;

    /** Hook used to add controller-owned widgets to the screen. */
    private final WidgetAdder addWidget;

    /** Hook used to remove controller-owned widgets from the screen. */
    private final WidgetRemover removeWidget;

    /** Callback that opens another marker editor with a preserved selection. */
    private final BiConsumer<BlockPos, Collection<BlockPos>> openMarker;

    /** Callback that teleports the player to a marker. */
    private final Consumer<BlockPos> teleport;

    /** Callback that confirms a bulk clear action. */
    private final BiConsumer<Boolean, Boolean> confirmBulkClear;

    /** Callback that opens the time interpolation popup. */
    private final Runnable openInterpolation;

    /** Currently selected path ID for marker rows. */
    private String pathId;

    /** Currently selected chapter ID for marker rows. */
    private String chapterId;

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

    /** Whether the server reported that the selected chapter has no chapter-start marker. */
    private boolean missingChapterStart;

    /** Scroll amount to apply to the next rebuilt marker navigation column. */
    private Double pendingMarkerListScrollAmount;

    /** Marker positions selected in the visible list order for bulk operations. */
    private List<BlockPos> selectedMarkers;

    /** Anchor marker used when Shift-click extends a list range. */
    private BlockPos selectionAnchor;

    /** Open context menu for marker-list operations. */
    private ContextMenuWidget contextMenu;

    /**
     * Creates a marker-list controller for one marker editor screen.
     *
     * @param marker           marker block entity being edited
     * @param pathId           selected path ID
     * @param chapterId        selected chapter ID
     * @param initialSelection initially selected marker rows
     * @param addWidget        hook used to add widgets to the screen
     * @param removeWidget     hook used to remove widgets from the screen
     * @param openMarker       callback that opens a marker for editing
     * @param teleport         callback that teleports to a marker
     * @param confirmBulkClear callback that confirms a bulk clear action
     * @param openInterpolation callback that opens time interpolation
     */
    public MarkerListController(PathMarkerBlockEntity marker, String pathId, String chapterId, Collection<BlockPos> initialSelection,
                                WidgetAdder addWidget, WidgetRemover removeWidget,
                                BiConsumer<BlockPos, Collection<BlockPos>> openMarker,
                                Consumer<BlockPos> teleport,
                                BiConsumer<Boolean, Boolean> confirmBulkClear,
                                Runnable openInterpolation) {
        this.marker = marker;
        this.pathId = pathId;
        this.chapterId = chapterId;
        this.addWidget = addWidget;
        this.removeWidget = removeWidget;
        this.openMarker = openMarker;
        this.teleport = teleport;
        this.confirmBulkClear = confirmBulkClear;
        this.openInterpolation = openInterpolation;
        selectedMarkers = initialSelection == null || initialSelection.isEmpty()
                ? new ArrayList<>(List.of(marker.getBlockPos().immutable()))
                : new ArrayList<>(initialSelection.stream().map(BlockPos::immutable).toList());
        selectionAnchor = marker.getBlockPos().immutable();
    }

    /**
     * Updates the selected path/chapter and clears server row state.
     *
     * @param pathId    selected path ID
     * @param chapterId selected chapter ID
     */
    public void resetChapter(String pathId, String chapterId) {
        this.pathId = pathId;
        this.chapterId = chapterId;
        serverMarkers = List.of();
        serverListActive = false;
        missingChapterStart = false;
        markerListInitialised = false;
        pendingMarkerListScrollAmount = null;
    }

    /**
     * Updates the selected path/chapter without clearing existing seeded row state.
     *
     * @param pathId    selected path ID
     * @param chapterId selected chapter ID
     */
    public void setChapter(String pathId, String chapterId) {
        this.pathId = pathId;
        this.chapterId = chapterId;
    }

    /**
     * Builds the marker navigation column when the window is wide enough.
     *
     * @param layout coordinates for the current marker editor screen
     */
    public void build(MarkerEditLayout layout) {
        double restoredScrollAmount = pendingMarkerListScrollAmount != null
                ? pendingMarkerListScrollAmount
                : markerListPanel != null ? markerListPanel.getScrollAmount() : 0.0;
        boolean restoreScroll = markerListInitialised || pendingMarkerListScrollAmount != null;
        boolean scrollToSelected = !markerListInitialised;
        markerListPanel = null;
        lastMarkerListSignature = 0L;

        if (!layout.hasMarkerListRoom()) return;

        markerListPanel = addWidget.add(MarkerListPanelWidget.create()
                .setX(layout.markerListLeft())
                .setY(layout.top())
                .setScreenHeight(layout.screenHeight())
                .setListBottom(layout.footerY() + MarkerEditLayout.CONTROL_HEIGHT)
                .setDividerX(layout.markerListDividerX())
                .setDividerHeight(MarkerEditLayout.TOTAL_HEIGHT)
                .setOnSelect(this::selectMarker)
                .setOnTeleport(teleport)
                .setOnRangeSelect(this::selectMarkerRange)
                .setOnContextMenu(this::openMarkerContextMenu)
                .setOnFiltersChanged(() -> refresh(true))
                .build());
        markerListPanel.setServerListActive(serverListActive);
        refresh(scrollToSelected);
        if (restoreScroll) {
            markerListPanel.setScrollAmount(restoredScrollAmount);
        }
        pendingMarkerListScrollAmount = null;
        markerListInitialised = true;
        requestChapterMarkers();
    }

    /**
     * Rebuilds the marker list from current local or server rows.
     *
     * @param scrollToSelected whether to center the edited marker row
     */
    public void refresh(boolean scrollToSelected) {
        if (markerListPanel == null) return;

        List<MarkerListPanelWidget.MarkerRow> rows = serverListActive
                ? currentServerMarkerRows()
                : currentLocalMarkerRows();
        if (missingChapterStart) {
            rows = new ArrayList<>(rows);
            rows.add(0, MarkerListPanelWidget.MarkerRow.notice(Component.translatable(
                    "ardapaths.client.marker.configuration.screens.chapter_markers.missing_chapter_start")));
        }
        markerListPanel.setRows(rows, scrollToSelected);
        trimSelectionToDisplayedMarkers();
        lastMarkerListSignature = serverListActive ? 0L : currentMarkerListSignature();
    }

    /**
     * Checks for local marker-list signature changes and refreshes visible rows when needed.
     */
    public void tickSignature() {
        if (markerListPanel != null && !serverListActive) {
            long signature = currentMarkerListSignature();
            if (signature != lastMarkerListSignature) {
                refresh(false);
            }
        }
    }

    /**
     * Closes any open context menu.
     *
     * @return true when a menu was closed
     */
    public boolean closeContextMenu() {
        if (contextMenu != null) {
            removeWidget.remove(contextMenu);
            contextMenu = null;
            return true;
        }

        return false;
    }

    /**
     * Lets an open context menu consume a mouse click before the screen handles it.
     *
     * @param mouseX mouse x coordinate
     * @param mouseY mouse y coordinate
     * @param button clicked mouse button
     * @return true when the click is consumed by context-menu handling
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (contextMenu != null) {
            if (contextMenu.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            closeContextMenu();
            return true;
        }

        return false;
    }

    /**
     * Selects a single marker row and opens that marker.
     *
     * @param pos marker position to select
     */
    public void selectMarker(BlockPos pos) {
        selectedMarkers = new ArrayList<>(List.of(pos.immutable()));
        selectionAnchor = pos.immutable();
        openMarker.accept(pos, selectedMarkers);
    }

    /**
     * Selects marker rows without changing the edited marker.
     *
     * @param selection marker positions selected in list order
     */
    public void setSelectedMarkers(Collection<BlockPos> selection) {
        selectedMarkers = new ArrayList<>(selection.stream().map(BlockPos::immutable).toList());
    }

    /**
     * Returns a copy of marker positions selected in visible list order.
     *
     * @return selected marker positions
     */
    public List<BlockPos> selectedMarkers() {
        return List.copyOf(selectedMarkers);
    }

    /**
     * Seeds chapter marker list state before a newly opened editor screen initializes.
     *
     * @param markers      chapter marker rows already known by the previous screen
     * @param active       whether those rows are server-provided chapter rows
     * @param scrollAmount marker list scroll amount to preserve
     */
    public void seedChapterMarkers(List<ChapterMarkerEntry> markers, boolean active, double scrollAmount) {
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
    public double currentMarkerListScrollAmount() {
        return markerListPanel == null ? 0.0 : markerListPanel.getScrollAmount();
    }

    /**
     * Returns the current server-provided marker rows.
     *
     * @return server marker rows
     */
    public List<ChapterMarkerEntry> serverMarkers() {
        return List.copyOf(serverMarkers);
    }

    /**
     * Checks whether server-provided marker rows are active.
     *
     * @return true when the controller is showing server rows
     */
    public boolean serverListActive() {
        return serverListActive;
    }

    /**
     * Updates the active server-provided row with freshly saved marker data.
     */
    public void patchSavedServerRow() {
        if (!serverListActive) return;

        long packedMarkerPos = marker.getBlockPos().asLong();
        PathMarkerBlockEntity.ChapterNbtData data = marker.getChapterData(pathId, chapterId, false);
        if (data == null) return;

        serverMarkers = serverMarkers.stream()
                .map(entry -> entry.chainBreak() || entry.packedPos() != packedMarkerPos
                        ? entry
                        : ChapterMarkerEntry.marker(packedMarkerPos, data))
                .toList();
    }

    /**
     * Requests the selected chapter's full marker chain from the server.
     */
    @SuppressWarnings("resource")
    public void requestChapterMarkers() {
        if (pathId == null || chapterId == null || pathId.isEmpty() || chapterId.isEmpty()) {
            useLocalMarkerList();
            return;
        }

        Minecraft minecraft = Client.mc();
        String requestedPathId = pathId;
        String requestedChapterId = chapterId;
        PacketRegistry.CHAPTER_PATH_MARKERS.send(
                new ChapterPathMarkersPacket(requestedPathId, requestedChapterId, marker.getBlockPos().asLong()),
                response -> minecraft.execute(() -> onChapterMarkersResponse(requestedPathId, requestedChapterId, response))
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
        if (!Objects.equals(requestedPathId, pathId) || !Objects.equals(requestedChapterId, chapterId)) {
            return;
        }

        List<ChapterMarkerEntry> incomingMarkers = List.copyOf(response.markers());
        boolean incomingServerListActive = response.status() != ChapterMarkersStatus.NO_CHAPTER_START && !incomingMarkers.isEmpty();
        boolean incomingMissingChapterStart = response.status() == ChapterMarkersStatus.NO_CHAPTER_START;
        boolean missingChapterStartChanged = incomingMissingChapterStart != missingChapterStart;
        boolean alreadyShowingServerRows = markerListInitialised && serverListActive;
        missingChapterStart = incomingMissingChapterStart;
        if (incomingMarkers.equals(serverMarkers) && incomingServerListActive == serverListActive && !missingChapterStartChanged) {
            return;
        }

        serverMarkers = incomingMarkers;
        serverListActive = incomingServerListActive;
        if (markerListPanel != null) markerListPanel.setServerListActive(serverListActive);
        refresh(!alreadyShowingServerRows);
    }

    /**
     * Falls back to the local loaded-marker list.
     */
    private void useLocalMarkerList() {
        serverMarkers = List.of();
        serverListActive = false;
        missingChapterStart = false;
        if (markerListPanel != null) markerListPanel.setServerListActive(false);
        refresh(true);
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
        return ChapterMarkerChain.orderedLocalMarkers(marker, pathId, chapterId).stream()
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
     * @param rowMarker marker block entity to represent
     * @return marker row data
     */
    private MarkerListPanelWidget.MarkerRow localMarkerRow(PathMarkerBlockEntity rowMarker) {
        PathMarkerBlockEntity.ChapterNbtData data = ChapterMarkerChain.selectedChapterData(rowMarker, pathId, chapterId);
        return new MarkerListPanelWidget.MarkerRow(
                rowMarker.getBlockPos().immutable(),
                data.getTimeOfDay(),
                data.getWeather(),
                data.getProximityMessage(),
                data.hasMiscData(),
                rowMarker.getBlockPos().equals(marker.getBlockPos()),
                selectedMarkers.contains(rowMarker.getBlockPos())
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

        BlockPos pos = BlockPos.of(entry.packedPos());
        return new MarkerListPanelWidget.MarkerRow(
                pos,
                entry.timeOfDay(),
                entry.weather(),
                entry.proximityMessage(),
                entry.hasMiscData(),
                pos.equals(marker.getBlockPos()),
                selectedMarkers.contains(pos)
        );
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
            anchorIndex = positions.indexOf(marker.getBlockPos());
        }
        if (anchorIndex < 0) {
            anchorIndex = clickedIndex;
        }

        int from = Math.min(anchorIndex, clickedIndex);
        int to = Math.max(anchorIndex, clickedIndex);
        selectedMarkers = new ArrayList<>(positions.subList(from, to + 1));
        refresh(false);
    }

    /**
     * Opens the marker-list context menu at the requested cursor position.
     *
     * @param pos    clicked marker position
     * @param anchor cursor coordinates for menu placement
     */
    @SuppressWarnings("resource")
    private void openMarkerContextMenu(BlockPos pos, MarkerListEntry.ContextMenuAnchor anchor) {
        if (!selectedMarkers.contains(pos)) {
            selectedMarkers = new ArrayList<>(List.of(pos.immutable()));
            selectionAnchor = pos.immutable();
            refresh(false);
        }

        closeContextMenu();
        boolean hasSelection = !selectedMarkers.isEmpty();
        boolean canInterpolate = selectedMarkers.size() > 1;
        List<ContextMenuWidget.Item> items = List.of(
                new ContextMenuWidget.Item(
                        Component.translatable("ardapaths.client.marker.configuration.screens.marker_list.clear_time"),
                        Component.translatable("ardapaths.client.marker.configuration.screens.marker_list.clear_time.tooltip"),
                        hasSelection,
                        () -> confirmBulkClear.accept(true, false)
                ),
                new ContextMenuWidget.Item(
                        Component.translatable("ardapaths.client.marker.configuration.screens.marker_list.clear_weather"),
                        Component.translatable("ardapaths.client.marker.configuration.screens.marker_list.clear_weather.tooltip"),
                        hasSelection,
                        () -> confirmBulkClear.accept(false, true)
                ),
                new ContextMenuWidget.Item(
                        Component.translatable("ardapaths.client.marker.configuration.screens.marker_list.interpolate_time"),
                        Component.translatable("ardapaths.client.marker.configuration.screens.marker_list.interpolate_time.tooltip"),
                        canInterpolate,
                        openInterpolation
                )
        );

        ContextMenuWidget menu = ContextMenuWidget.create()
                .setX((int) anchor.mouseX())
                .setY((int) anchor.mouseY())
                .setItems(items)
                .build();
        int menuX = Math.max(0, Math.min((int) anchor.mouseX(), Client.mc().getWindow().getGuiScaledWidth() - menu.getWidth()));
        int menuY = Math.max(0, Math.min((int) anchor.mouseY(), Client.mc().getWindow().getGuiScaledHeight() - menu.getHeight()));
        menu.setX(menuX);
        menu.setY(menuY);
        contextMenu = addWidget.add(menu);
    }

    /**
     * Builds the extracted marker-list signature for the current selected chapter.
     *
     * @return current marker-list signature
     */
    private long currentMarkerListSignature() {
        long filterSignature = markerListPanel == null ? 0L : markerListPanel.filterSignature();
        return ChapterMarkerChain.signature(marker, pathId, chapterId, filterSignature);
    }

    /**
     * Hook for adding a widget to the owning screen.
     */
    @FunctionalInterface
    public interface WidgetAdder {
        /**
         * Adds a widget to the owning screen.
         *
         * @param widget widget to add
         * @param <T>    widget type accepted by Minecraft's screen widget list
         * @return the added widget
         */
        <T extends GuiEventListener & Renderable & NarratableEntry> T add(T widget);
    }

    /**
     * Hook for removing a widget from the owning screen.
     */
    @FunctionalInterface
    public interface WidgetRemover {
        /**
         * Removes a widget from the owning screen.
         *
         * @param widget widget to remove
         */
        void remove(GuiEventListener widget);
    }
}

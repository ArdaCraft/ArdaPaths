package space.ajcool.ardapaths.screens.widgets;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import space.ajcool.ardapaths.core.Client;
import space.ajcool.ardapaths.core.ModConstants;
import space.ajcool.ardapaths.core.data.TimeOfDay;
import space.ajcool.ardapaths.core.data.WeatherTypes;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity.ChapterNbtData;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Composite marker navigation column for the marker editor.
 */
// Instantiated via screen/builder factory; IntelliJ entry-point analysis can't follow it.
@SuppressWarnings("unused")
public class MarkerListPanelWidget implements Renderable, GuiEventListener, NarratableEntry {

    /**
     * Width of the marker navigation column.
     */
    public static final int MARKER_LIST_WIDTH = 118;

    /**
     * Height reserved for the marker list heading.
     */
    private static final int MARKER_LIST_HEADER_HEIGHT = 14;

    /**
     * Height reserved for each marker filter toggle.
     */
    private static final int MARKER_LIST_FILTER_HEIGHT = 15;

    /**
     * Horizontal space between marker filter toggles.
     */
    private static final int MARKER_LIST_FILTER_GAP = 2;

    /**
     * Header color used while showing marker data stored on the local marker.
     */
    private static final int HEADER_LOCAL_COLOR = 0xFFFFAA00;

    /**
     * Header color used while showing server-provided chapter marker data.
     */
    private static final int HEADER_CHAPTER_COLOR = 0xFF55FF55;

    /**
     * Icon texture for an active weather marker filter.
     */
    private static final Identifier CLOUD_ICON = ModConstants.modId("cloud-icon");

    /**
     * Icon texture for an inactive weather marker filter.
     */
    private static final Identifier CLOUD_ICON_OUTLINED = ModConstants.modId("cloud-icon-outlined");

    /**
     * Icon texture for an active time marker filter.
     */
    private static final Identifier MOON_ICON = ModConstants.modId("moon-icon");

    /**
     * Icon texture for an inactive time marker filter.
     */
    private static final Identifier MOON_ICON_OUTLINED = ModConstants.modId("moon-icon-outlined");

    /**
     * Icon texture for an active proximity text marker filter.
     */
    private static final Identifier TEXT_ICON = ModConstants.modId("text-icon");

    /**
     * Icon texture for an inactive proximity text marker filter.
     */
    private static final Identifier TEXT_ICON_OUTLINED = ModConstants.modId("text-icon-outlined");

    /**
     * Icon texture for an active miscellaneous marker action filter.
     */
    private static final Identifier GEAR_ICON = ModConstants.modId("gear-icon");

    /**
     * Icon texture for an inactive miscellaneous marker action filter.
     */
    private static final Identifier GEAR_ICON_OUTLINED = ModConstants.modId("gear-icon-outlined");

    /**
     * Whether the marker list hides markers without selected-chapter weather data.
     */
    private static boolean filterWeather;

    /**
     * Whether the marker list hides markers without selected-chapter time data.
     */
    private static boolean filterTime;

    /**
     * Whether the marker list hides markers without selected-chapter proximity text.
     */
    private static boolean filterProximityText;

    /**
     * Whether the marker list hides markers without selected-chapter action data.
     */
    private static boolean filterMisc;

    /**
     * Callback used when the player selects a marker for editing.
     */
    private final Consumer<BlockPos> onSelect;

    /**
     * Callback used when the player requests teleport to a marker.
     */
    private final Consumer<BlockPos> onTeleport;

    /**
     * Callback used when the player selects a marker range.
     */
    private final Consumer<BlockPos> onRangeSelect;

    /**
     * Callback used when the player opens a marker row context menu.
     */
    private final BiConsumer<BlockPos, MarkerListEntry.ContextMenuAnchor> onContextMenu;

    /**
     * Callback invoked after a marker filter changes.
     */
    private final Runnable onFiltersChanged;

    /**
     * Header above the marker rows.
     */
    private final TextWidget header;

    /**
     * Weather facet filter toggle.
     */
    private final IconToggleWidget weatherFilter;

    /**
     * Time facet filter toggle.
     */
    private final IconToggleWidget timeFilter;

    /**
     * Proximity text facet filter toggle.
     */
    private final IconToggleWidget proximityTextFilter;

    /**
     * Miscellaneous action facet filter toggle.
     */
    private final IconToggleWidget miscFilter;

    /**
     * Scrollable marker row list.
     */
    private final MarkerListWidget markerList;

    /**
     * X coordinate of the marker column.
     */
    @Getter
    @Setter
    // Accessed via Lombok-generated accessor; IntelliJ entry-point analysis can't follow it.
    @SuppressWarnings("unused")
    private int x;

    /**
     * Y coordinate of the marker column.
     */
    @Getter
    @Setter
    // Accessed via Lombok-generated accessor; IntelliJ entry-point analysis can't follow it.
    @SuppressWarnings("unused")
    private int y;

    /**
     * Full screen height used by the nested list widget.
     */
    @Getter
    @Setter
    // Accessed via Lombok-generated accessor; IntelliJ entry-point analysis can't follow it.
    @SuppressWarnings("unused")
    private int screenHeight;

    /**
     * Bottom boundary of the scrollable marker list.
     */
    @Getter
    @Setter
    // Accessed via Lombok-generated accessor; IntelliJ entry-point analysis can't follow it.
    @SuppressWarnings("unused")
    private int listBottom;

    /**
     * X coordinate for the divider separating the marker list from the form.
     */
    @Getter
    @Setter
    // Accessed via Lombok-generated accessor; IntelliJ entry-point analysis can't follow it.
    @SuppressWarnings("unused")
    private int dividerX;

    /**
     * Height of the divider separating the marker list from the form.
     */
    @Getter
    @Setter
    // Accessed via Lombok-generated accessor; IntelliJ entry-point analysis can't follow it.
    @SuppressWarnings("unused")
    private int dividerHeight;

    /**
     * Rows that survived filtering in current list order.
     */
    private List<MarkerRow> visibleRows = List.of();

    /**
     * Whether this composite currently has focus.
     */
    private boolean focused;

    /**
     * Builds a marker list panel with header, filters, scrollable rows, and divider.
     *
     * @param x                the marker column x coordinate
     * @param y                the marker column y coordinate
     * @param screenHeight     full screen height used by the list widget
     * @param listBottom       bottom boundary of the scrollable marker list
     * @param dividerX         x coordinate for the divider line
     * @param dividerHeight    height of the divider line
     * @param onSelect         callback for normal row clicks
     * @param onTeleport       callback for Ctrl-clicks
     * @param onRangeSelect    callback for Shift-clicks
     * @param onContextMenu    callback for right-clicks
     * @param onFiltersChanged callback fired after a filter toggle changes
     */
    @Builder(builderClassName = "MarkerListPanelBuilder", builderMethodName = "create", setterPrefix = "set")
    // Instantiated via screen/builder factory; IntelliJ entry-point analysis can't follow it.
    @SuppressWarnings("unused")
    public MarkerListPanelWidget(int x, int y, int screenHeight, int listBottom, int dividerX, int dividerHeight,
                                 Consumer<BlockPos> onSelect, Consumer<BlockPos> onTeleport,
                                 Consumer<BlockPos> onRangeSelect,
                                 BiConsumer<BlockPos, MarkerListEntry.ContextMenuAnchor> onContextMenu,
                                 Runnable onFiltersChanged) {
        this.x = x;
        this.y = y;
        this.screenHeight = screenHeight;
        this.listBottom = listBottom;
        this.dividerX = dividerX;
        this.dividerHeight = dividerHeight;
        this.onSelect = onSelect;
        this.onTeleport = onTeleport;
        this.onRangeSelect = onRangeSelect;
        this.onContextMenu = onContextMenu;
        this.onFiltersChanged = onFiltersChanged;
        this.header = buildHeader();
        this.weatherFilter = buildWeatherFilter();
        this.timeFilter = buildTimeFilter();
        this.proximityTextFilter = buildProximityTextFilter();
        this.miscFilter = buildMiscFilter();
        this.markerList = buildMarkerList();
        setServerListActive(false);
    }

    /**
     * Builds the marker list header widget.
     *
     * @return header widget
     */
    private TextWidget buildHeader() {
        return TextWidget.create()
                .setX(x)
                .setY(y)
                .setWidth(MARKER_LIST_WIDTH)
                .setHeight(MARKER_LIST_HEADER_HEIGHT)
                .setMessage(Component.empty())
                .build();
    }

    /**
     * Builds the weather filter toggle.
     *
     * @return weather filter toggle
     */
    @SuppressWarnings("SuspiciousNameCombination")
    private IconToggleWidget buildWeatherFilter() {
        IconToggleWidget filter = IconToggleWidget.create()
                .setX(filterX())
                .setY(filterY())
                .setWidth(MARKER_LIST_FILTER_HEIGHT)
                .setHeight(MARKER_LIST_FILTER_HEIGHT)
                .setActiveTexture(CLOUD_ICON)
                .setInactiveTexture(CLOUD_ICON_OUTLINED)
                .setActive(filterWeather)
                .setEnabled(true)
                .setOnChange(active -> {
                    filterWeather = active;
                    onFiltersChanged.run();
                })
                .build();
        filter.setTooltip(Tooltip.create(Component.translatable("ardapaths.client.marker.configuration.screens.local_markers.filter.weather.tooltip")));
        return filter;
    }

    /**
     * Builds the time filter toggle.
     *
     * @return time filter toggle
     */
    @SuppressWarnings("SuspiciousNameCombination")
    private IconToggleWidget buildTimeFilter() {
        IconToggleWidget filter = IconToggleWidget.create()
                .setX(filterX() + MARKER_LIST_FILTER_HEIGHT + MARKER_LIST_FILTER_GAP)
                .setY(filterY())
                .setWidth(MARKER_LIST_FILTER_HEIGHT)
                .setHeight(MARKER_LIST_FILTER_HEIGHT)
                .setActiveTexture(MOON_ICON)
                .setInactiveTexture(MOON_ICON_OUTLINED)
                .setActive(filterTime)
                .setEnabled(true)
                .setOnChange(active -> {
                    filterTime = active;
                    onFiltersChanged.run();
                })
                .build();
        filter.setTooltip(Tooltip.create(Component.translatable("ardapaths.client.marker.configuration.screens.local_markers.filter.time.tooltip")));
        return filter;
    }

    /**
     * Builds the proximity text filter toggle.
     *
     * @return proximity text filter toggle
     */
    @SuppressWarnings("SuspiciousNameCombination")
    private IconToggleWidget buildProximityTextFilter() {
        IconToggleWidget filter = IconToggleWidget.create()
                .setX(filterX() + (MARKER_LIST_FILTER_HEIGHT + MARKER_LIST_FILTER_GAP) * 2)
                .setY(filterY())
                .setWidth(MARKER_LIST_FILTER_HEIGHT)
                .setHeight(MARKER_LIST_FILTER_HEIGHT)
                .setActiveTexture(TEXT_ICON)
                .setInactiveTexture(TEXT_ICON_OUTLINED)
                .setActive(filterProximityText)
                .setEnabled(true)
                .setOnChange(active -> {
                    filterProximityText = active;
                    onFiltersChanged.run();
                })
                .build();
        filter.setTooltip(Tooltip.create(Component.translatable("ardapaths.client.marker.configuration.screens.local_markers.filter.text.tooltip")));
        return filter;
    }

    /**
     * Builds the miscellaneous action filter toggle.
     *
     * @return miscellaneous action filter toggle
     */
    @SuppressWarnings("SuspiciousNameCombination")
    private IconToggleWidget buildMiscFilter() {
        IconToggleWidget filter = IconToggleWidget.create()
                .setX(filterX() + (MARKER_LIST_FILTER_HEIGHT + MARKER_LIST_FILTER_GAP) * 3)
                .setY(filterY())
                .setWidth(MARKER_LIST_FILTER_HEIGHT)
                .setHeight(MARKER_LIST_FILTER_HEIGHT)
                .setActiveTexture(GEAR_ICON)
                .setInactiveTexture(GEAR_ICON_OUTLINED)
                .setActive(filterMisc)
                .setEnabled(true)
                .setOnChange(active -> {
                    filterMisc = active;
                    onFiltersChanged.run();
                })
                .build();
        filter.setTooltip(Tooltip.create(Component.translatable("ardapaths.client.marker.configuration.screens.local_markers.filter.misc.tooltip")));
        return filter;
    }

    /**
     * Builds the scrollable marker row list.
     *
     * @return marker row list
     */
    private MarkerListWidget buildMarkerList() {
        MarkerListWidget list = new MarkerListWidget(
                Client.mc(),
                MARKER_LIST_WIDTH,
                y + MARKER_LIST_HEADER_HEIGHT + MARKER_LIST_FILTER_HEIGHT + 8,
                listBottom,
                16
        );
        list.setX(x);
        return list;
    }

    /**
     * Updates the header for the active marker row data source.
     *
     * @param active whether server-provided chapter rows are being shown
     */
    public void setServerListActive(boolean active) {
        if (active) {
            header.setText(Component.translatable("ardapaths.client.marker.configuration.screens.chapter_markers"));
            header.setColor(HEADER_CHAPTER_COLOR);
            header.setTooltip(Tooltip.create(Component.translatable("ardapaths.client.marker.configuration.screens.chapter_markers.tooltip")));
        } else {
            header.setText(Component.translatable("ardapaths.client.marker.configuration.screens.local_markers"));
            header.setColor(HEADER_LOCAL_COLOR);
            header.setTooltip(Tooltip.create(Component.translatable("ardapaths.client.marker.configuration.screens.local_markers.tooltip")));
        }
    }

    /**
     * Computes the x coordinate for the first filter toggle.
     *
     * @return first filter x coordinate
     */
    private int filterX() {
        return x + MARKER_LIST_WIDTH - (MARKER_LIST_FILTER_HEIGHT * 4) - (MARKER_LIST_FILTER_GAP * 3);
    }

    /**
     * Computes the y coordinate shared by filter toggles.
     *
     * @return filter y coordinate
     */
    private int filterY() {
        return y + MARKER_LIST_HEADER_HEIGHT + 2;
    }

    /**
     * Replaces the panel rows after applying the active marker filters.
     *
     * @param rows             source rows supplied by the screen
     * @param scrollToSelected whether to center the focused row
     */
    public void setRows(List<MarkerRow> rows, boolean scrollToSelected) {
        this.visibleRows = rows.stream()
                .filter(this::isVisible)
                .toList();
        this.markerList.setMarkers(this.visibleRows.stream()
                .map(this::toEntry)
                .toList(), scrollToSelected);
    }

    /**
     * Checks whether a row passes the active marker filters.
     *
     * @param row row data to inspect
     * @return true when the row should be visible
     */
    private boolean isVisible(MarkerRow row) {
        return row.isNotice() ||
                noFiltersActive() ||
                row.focused() ||
                (filterWeather && hasWeatherData(row)) ||
                (filterTime && hasTimeData(row)) ||
                (filterProximityText && hasProximityMessage(row)) ||
                (filterMisc && row.hasMiscData());
    }

    /**
     * Converts panel row data into a rendered marker list entry.
     *
     * @param row row data to convert
     * @return marker list entry
     */
    private MarkerListEntry toEntry(MarkerRow row) {
        if (row.isNotice()) {
            return MarkerListEntry.notice(row.noticeText());
        }

        return new MarkerListEntry(
                row.pos(),
                row.timeOfDay(),
                hasWeatherData(row),
                hasProximityMessage(row),
                row.hasMiscData(),
                row.focused(),
                row.selected(),
                markerTooltipLines(row),
                onSelect,
                onTeleport,
                onRangeSelect,
                onContextMenu
        );
    }

    /**
     * Checks whether no marker facet filters are active.
     *
     * @return true when all marker rows should be shown
     */
    private boolean noFiltersActive() {
        return !filterWeather && !filterTime && !filterProximityText && !filterMisc;
    }

    /**
     * Checks whether row weather data should be shown and filtered.
     *
     * @param row row data to inspect
     * @return true when the row has non-default weather data
     */
    private boolean hasWeatherData(MarkerRow row) {
        return row.weather() != ChapterNbtData.UNSET && WeatherTypes.fromInt(row.weather()) != WeatherTypes.DEFAULT;
    }

    /**
     * Checks whether row time data should be shown and filtered.
     *
     * @param row row data to inspect
     * @return true when the row has configured time data
     */
    private boolean hasTimeData(MarkerRow row) {
        return row.timeOfDay() != ChapterNbtData.UNSET;
    }

    /**
     * Checks whether row proximity text should be shown and filtered.
     *
     * @param row row data to inspect
     * @return true when the row has proximity text
     */
    private boolean hasProximityMessage(MarkerRow row) {
        return !row.proximityMessage().isEmpty();
    }

    /**
     * Builds hover tooltip lines for marker row data.
     *
     * @param row row data to describe
     * @return styled tooltip lines for marker data and actions
     */
    private List<Component> markerTooltipLines(MarkerRow row) {
        List<Component> lines = new ArrayList<>();
        MutableComponent environment = Component.empty();
        boolean hasTime = hasTimeData(row);
        boolean hasWeather = hasWeatherData(row);

        if (hasTime) {
            environment.append(Component.literal(TimeOfDay.format(row.timeOfDay())).withStyle(ChatFormatting.BLUE));
        }

        if (hasTime && hasWeather) {
            environment.append(Component.literal(" - ").withStyle(ChatFormatting.GRAY));
        }

        if (hasWeather) {
            environment.append(Component.literal(WeatherTypes.fromInt(row.weather()).getDisplayName()).withStyle(ChatFormatting.GREEN));
        }

        if (hasTime || hasWeather) {
            lines.add(environment);
        }

        if (!row.proximityMessage().isEmpty()) {
            lines.add(Component.literal("\"" + ellipsizeTooltipText(row.proximityMessage()) + "\"").withStyle(ChatFormatting.WHITE));
        }

        if (row.hasMiscData()) {
            lines.add(Component.translatable("ardapaths.client.marker.configuration.screens.marker_list.misc_hint")
                    .withStyle(ChatFormatting.GRAY));
        }

        lines.add(Component.translatable("ardapaths.client.marker.configuration.screens.marker_list.teleport_hint")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        return lines;
    }

    /**
     * Shortens proximity text for compact marker-row tooltips.
     *
     * @param text proximity text to shorten
     * @return single-line text with an ellipsis when it exceeds the preview length
     */
    private String ellipsizeTooltipText(String text) {
        String flattened = text.replace('\n', ' ').trim();
        int maxLength = 80;
        if (flattened.length() <= maxLength) {
            return flattened;
        }

        return flattened.substring(0, maxLength - 3).stripTrailing() + "...";
    }

    /**
     * Returns the scroll offset of the nested marker row list.
     *
     * @return current list scroll amount in pixels
     */
    public double getScrollAmount() {
        return markerList.getScrollAmount();
    }

    /**
     * Restores the scroll offset of the nested marker row list.
     *
     * @param scrollAmount list scroll amount in pixels
     */
    public void setScrollAmount(double scrollAmount) {
        markerList.setScrollAmount(scrollAmount);
    }

    /**
     * Returns visible marker positions in list order.
     *
     * @return visible non-separator marker positions
     */
    public List<BlockPos> getVisiblePositions() {
        return visibleRows.stream()
                .filter(row -> !row.isNotice())
                .map(MarkerRow::pos)
                .toList();
    }

    /**
     * Returns a compact signature of the active filter state.
     *
     * @return filter state signature for tick-time list invalidation
     */
    public long filterSignature() {
        long signature = 17L;
        signature = signature * 31 + (filterWeather ? 1 : 0);
        signature = signature * 31 + (filterTime ? 1 : 0);
        signature = signature * 31 + (filterProximityText ? 1 : 0);
        signature = signature * 31 + (filterMisc ? 1 : 0);
        return signature;
    }

    /**
     * Draws child widgets followed by the divider line.
     *
     * @param context the draw context
     * @param mouseX  the current mouse x coordinate
     * @param mouseY  the current mouse y coordinate
     * @param delta   the partial tick delta
     */
    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        header.extractRenderState(context, mouseX, mouseY, delta);
        weatherFilter.extractRenderState(context, mouseX, mouseY, delta);
        timeFilter.extractRenderState(context, mouseX, mouseY, delta);
        proximityTextFilter.extractRenderState(context, mouseX, mouseY, delta);
        miscFilter.extractRenderState(context, mouseX, mouseY, delta);
        markerList.extractRenderState(context, mouseX, mouseY, delta);

        if (dividerHeight > 0) {
            context.fill(dividerX, y, dividerX + 1, y + dividerHeight, 0xFFFFFFFF);
        }
    }

    /**
     * Forwards mouse press handling to panel children.
     *
     * @param event   clicked mouse button event
     * @param doubled whether this click is a double-click
     * @return true when a child consumes the click
     */
    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubled) {
        return header.mouseClicked(event, doubled) ||
                weatherFilter.mouseClicked(event, doubled) ||
                timeFilter.mouseClicked(event, doubled) ||
                proximityTextFilter.mouseClicked(event, doubled) ||
                miscFilter.mouseClicked(event, doubled) ||
                markerList.mouseClicked(event, doubled);
    }

    /**
     * Forwards mouse release handling to panel children.
     *
     * @param event released mouse button event
     * @return true when a child consumes the release
     */
    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        return header.mouseReleased(event) ||
                weatherFilter.mouseReleased(event) ||
                timeFilter.mouseReleased(event) ||
                proximityTextFilter.mouseReleased(event) ||
                miscFilter.mouseReleased(event) ||
                markerList.mouseReleased(event);
    }

    /**
     * Forwards mouse drag handling to panel children.
     *
     * @param event  dragged mouse button event
     * @param deltaX cursor x delta
     * @param deltaY cursor y delta
     * @return true when a child consumes the drag
     */
    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent event, double deltaX, double deltaY) {
        return markerList.mouseDragged(event, deltaX, deltaY);
    }

    /**
     * Forwards mouse wheel handling to the marker list.
     *
     * @param mouseX           the mouse x coordinate
     * @param mouseY           the mouse y coordinate
     * @param horizontalAmount horizontal scroll wheel amount
     * @param verticalAmount   vertical scroll wheel amount
     * @return true when the list consumes the scroll
     */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return markerList.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    /**
     * Checks whether the cursor is over any panel child.
     *
     * @param mouseX the mouse x coordinate
     * @param mouseY the mouse y coordinate
     * @return true when a child is under the cursor
     */
    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return header.isMouseOver(mouseX, mouseY) ||
                weatherFilter.isMouseOver(mouseX, mouseY) ||
                timeFilter.isMouseOver(mouseX, mouseY) ||
                proximityTextFilter.isMouseOver(mouseX, mouseY) ||
                miscFilter.isMouseOver(mouseX, mouseY) ||
                markerList.isMouseOver(mouseX, mouseY);
    }

    /**
     * Checks whether this composite is focused.
     *
     * @return true when focused
     */
    @Override
    public boolean isFocused() {
        return focused;
    }

    /**
     * Updates composite focus state.
     *
     * @param focused whether the panel should be focused
     */
    @Override
    public void setFocused(boolean focused) {
        this.focused = focused;
        markerList.setFocused(focused);
    }

    /**
     * Reports the nested list selection type for narration traversal.
     *
     * @return nested list selection type
     */
    @Override
    public @NotNull NarrationPriority narrationPriority() {
        return markerList.narrationPriority();
    }

    /**
     * Forwards active row narration to the marker list.
     *
     * @param builder narration builder receiving child narration
     */
    @Override
    public void updateNarration(@NonNull NarrationElementOutput builder) {
        markerList.updateNarration(builder);
    }

    /**
     * Data the screen supplies for one marker-list row.
     *
     * @param pos              marker position represented by the row
     * @param timeOfDay        configured marker time, or unset
     * @param weather          configured marker weather, or unset
     * @param proximityMessage configured marker proximity message
     * @param hasMiscData      whether marker action data is configured
     * @param focused          whether this marker is currently being edited
     * @param selected         whether this marker is in the current multi-selection
     * @param noticeText       inert notice label, or null for marker rows
     */
    public record MarkerRow(BlockPos pos, int timeOfDay, int weather, String proximityMessage, boolean hasMiscData,
                            boolean focused,
                            boolean selected, Component noticeText) {

        /**
         * Creates a marker row with no notice label.
         *
         * @param pos              marker position represented by the row
         * @param timeOfDay        configured marker time, or unset
         * @param weather          configured marker weather, or unset
         * @param proximityMessage configured marker proximity message
         * @param hasMiscData      whether marker action data is configured
         * @param focused          whether this marker is currently being edited
         * @param selected         whether this marker is in the current multi-selection
         */
        public MarkerRow(BlockPos pos, int timeOfDay, int weather, String proximityMessage, boolean hasMiscData, boolean focused, boolean selected) {
            this(pos, timeOfDay, weather, proximityMessage, hasMiscData, focused, selected, null);
        }

        /**
         * Creates an inert separator between disconnected marker chains.
         *
         * @return chain-break row data
         */
        public static MarkerRow chainBreak() {
            return notice(Component.translatable("ardapaths.client.marker.configuration.screens.chapter_markers.break"));
        }

        /**
         * Creates an inert notice row.
         *
         * @param text notice label to render
         * @return notice row data
         */
        public static MarkerRow notice(Component text) {
            return new MarkerRow(BlockPos.ZERO, ChapterNbtData.UNSET, ChapterNbtData.UNSET, "", false, false, false, text);
        }

        /**
         * Checks whether this row is an inert notice instead of a marker.
         *
         * @return true when the row renders only a notice label
         */
        public boolean isNotice() {
            return noticeText != null;
        }
    }
}

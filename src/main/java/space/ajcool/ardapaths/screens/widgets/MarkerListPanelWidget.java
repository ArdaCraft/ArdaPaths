package space.ajcool.ardapaths.screens.widgets;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.core.Client;
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
public class MarkerListPanelWidget implements Drawable, Element, Selectable {
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
    private static final int HEADER_LOCAL_COLOR = 0xFFAA00;

    /**
     * Header color used while showing server-provided chapter marker data.
     */
    private static final int HEADER_CHAPTER_COLOR = 0x55FF55;

    /**
     * Icon texture for an active weather marker filter.
     */
    private static final Identifier CLOUD_ICON = new Identifier(ArdaPaths.MOD_ID, "textures/gui/cloud-icon.png");

    /**
     * Icon texture for an inactive weather marker filter.
     */
    private static final Identifier CLOUD_ICON_OUTLINED = new Identifier(ArdaPaths.MOD_ID, "textures/gui/cloud-icon-outlined.png");

    /**
     * Icon texture for an active time marker filter.
     */
    private static final Identifier MOON_ICON = new Identifier(ArdaPaths.MOD_ID, "textures/gui/moon-icon.png");

    /**
     * Icon texture for an inactive time marker filter.
     */
    private static final Identifier MOON_ICON_OUTLINED = new Identifier(ArdaPaths.MOD_ID, "textures/gui/moon-icon-outlined.png");

    /**
     * Icon texture for an active proximity text marker filter.
     */
    private static final Identifier TEXT_ICON = new Identifier(ArdaPaths.MOD_ID, "textures/gui/text-icon.png");

    /**
     * Icon texture for an inactive proximity text marker filter.
     */
    private static final Identifier TEXT_ICON_OUTLINED = new Identifier(ArdaPaths.MOD_ID, "textures/gui/text-icon-outlined.png");

    /**
     * Icon texture for an active miscellaneous marker action filter.
     */
    private static final Identifier GEAR_ICON = new Identifier(ArdaPaths.MOD_ID, "textures/gui/gear-icon.png");

    /**
     * Icon texture for an inactive miscellaneous marker action filter.
     */
    private static final Identifier GEAR_ICON_OUTLINED = new Identifier(ArdaPaths.MOD_ID, "textures/gui/gear-icon-outlined.png");

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
     * X coordinate of the marker column.
     */
    @Getter
    @Setter
    private int x;

    /**
     * Y coordinate of the marker column.
     */
    @Getter
    @Setter
    private int y;

    /**
     * Full screen height used by the nested list widget.
     */
    @Getter
    @Setter
    private int screenHeight;

    /**
     * Bottom boundary of the scrollable marker list.
     */
    @Getter
    @Setter
    private int listBottom;

    /**
     * X coordinate for the divider separating the marker list from the form.
     */
    @Getter
    @Setter
    private int dividerX;

    /**
     * Height of the divider separating the marker list from the form.
     */
    @Getter
    @Setter
    private int dividerHeight;

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
                .filter(row -> !row.isChainBreak())
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
     * Updates the header for the active marker row data source.
     *
     * @param active whether server-provided chapter rows are being shown
     */
    public void setServerListActive(boolean active) {
        if (active) {
            header.setText(Text.translatable("ardapaths.client.marker.configuration.screens.chapter_markers"));
            header.setTextColor(HEADER_CHAPTER_COLOR);
            header.setTooltip(Tooltip.of(Text.translatable("ardapaths.client.marker.configuration.screens.chapter_markers.tooltip")));
        } else {
            header.setText(Text.translatable("ardapaths.client.marker.configuration.screens.local_markers"));
            header.setTextColor(HEADER_LOCAL_COLOR);
            header.setTooltip(Tooltip.of(Text.translatable("ardapaths.client.marker.configuration.screens.local_markers.tooltip")));
        }
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
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        header.render(context, mouseX, mouseY, delta);
        weatherFilter.render(context, mouseX, mouseY, delta);
        timeFilter.render(context, mouseX, mouseY, delta);
        proximityTextFilter.render(context, mouseX, mouseY, delta);
        miscFilter.render(context, mouseX, mouseY, delta);
        markerList.render(context, mouseX, mouseY, delta);

        if (dividerHeight > 0) {
            context.fill(dividerX, y, dividerX + 1, y + dividerHeight, 0xFFFFFFFF);
        }
    }

    /**
     * Forwards mouse press handling to panel children.
     *
     * @param mouseX the mouse x coordinate
     * @param mouseY the mouse y coordinate
     * @param button clicked mouse button
     * @return true when a child consumes the click
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return header.mouseClicked(mouseX, mouseY, button) ||
                weatherFilter.mouseClicked(mouseX, mouseY, button) ||
                timeFilter.mouseClicked(mouseX, mouseY, button) ||
                proximityTextFilter.mouseClicked(mouseX, mouseY, button) ||
                miscFilter.mouseClicked(mouseX, mouseY, button) ||
                markerList.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * Forwards mouse release handling to panel children.
     *
     * @param mouseX the mouse x coordinate
     * @param mouseY the mouse y coordinate
     * @param button released mouse button
     * @return true when a child consumes the release
     */
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return header.mouseReleased(mouseX, mouseY, button) ||
                weatherFilter.mouseReleased(mouseX, mouseY, button) ||
                timeFilter.mouseReleased(mouseX, mouseY, button) ||
                proximityTextFilter.mouseReleased(mouseX, mouseY, button) ||
                miscFilter.mouseReleased(mouseX, mouseY, button) ||
                markerList.mouseReleased(mouseX, mouseY, button);
    }

    /**
     * Forwards mouse drag handling to panel children.
     *
     * @param mouseX the mouse x coordinate
     * @param mouseY the mouse y coordinate
     * @param button dragged mouse button
     * @param deltaX cursor x delta
     * @param deltaY cursor y delta
     * @return true when a child consumes the drag
     */
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return markerList.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    /**
     * Forwards mouse wheel handling to the marker list.
     *
     * @param mouseX the mouse x coordinate
     * @param mouseY the mouse y coordinate
     * @param amount scroll wheel amount
     * @return true when the list consumes the scroll
     */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return markerList.mouseScrolled(mouseX, mouseY, amount);
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
     * Checks whether this composite is focused.
     *
     * @return true when focused
     */
    @Override
    public boolean isFocused() {
        return focused;
    }

    /**
     * Reports the nested list selection type for narration traversal.
     *
     * @return nested list selection type
     */
    @Override
    public SelectionType getType() {
        return markerList.getType();
    }

    /**
     * Forwards active row narration to the marker list.
     *
     * @param builder narration builder receiving child narration
     */
    @Override
    public void appendNarrations(NarrationMessageBuilder builder) {
        markerList.appendNarrations(builder);
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
                .setMessage(Text.empty())
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
        filter.setTooltip(Tooltip.of(Text.translatable("ardapaths.client.marker.configuration.screens.local_markers.filter.weather.tooltip")));
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
        filter.setTooltip(Tooltip.of(Text.translatable("ardapaths.client.marker.configuration.screens.local_markers.filter.time.tooltip")));
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
        filter.setTooltip(Tooltip.of(Text.translatable("ardapaths.client.marker.configuration.screens.local_markers.filter.text.tooltip")));
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
        filter.setTooltip(Tooltip.of(Text.translatable("ardapaths.client.marker.configuration.screens.local_markers.filter.misc.tooltip")));
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
                screenHeight,
                y + MARKER_LIST_HEADER_HEIGHT + MARKER_LIST_FILTER_HEIGHT + 8,
                listBottom,
                16
        );
        list.setLeftPos(x);
        return list;
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
     * Computes the x coordinate for the first filter toggle.
     *
     * @return first filter x coordinate
     */
    private int filterX() {
        return x + MARKER_LIST_WIDTH - (MARKER_LIST_FILTER_HEIGHT * 4) - (MARKER_LIST_FILTER_GAP * 3);
    }

    /**
     * Checks whether a row passes the active marker filters.
     *
     * @param row row data to inspect
     * @return true when the row should be visible
     */
    private boolean isVisible(MarkerRow row) {
        return row.isChainBreak() ||
                noFiltersActive() ||
                row.focused() ||
                (filterWeather && hasWeatherData(row)) ||
                (filterTime && hasTimeData(row)) ||
                (filterProximityText && hasProximityMessage(row)) ||
                (filterMisc && row.hasMiscData());
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
     * Converts panel row data into a rendered marker list entry.
     *
     * @param row row data to convert
     * @return marker list entry
     */
    private MarkerListEntry toEntry(MarkerRow row) {
        if (row.isChainBreak()) {
            return MarkerListEntry.chainBreak();
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
     * Checks whether row time data should be shown and filtered.
     *
     * @param row row data to inspect
     * @return true when the row has configured time data
     */
    private boolean hasTimeData(MarkerRow row) {
        return row.timeOfDay() != ChapterNbtData.UNSET;
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
    private List<Text> markerTooltipLines(MarkerRow row) {
        List<Text> lines = new ArrayList<>();
        MutableText environment = Text.empty();
        boolean hasTime = hasTimeData(row);
        boolean hasWeather = hasWeatherData(row);

        if (hasTime) {
            environment.append(Text.literal(TimeOfDay.format(row.timeOfDay())).formatted(Formatting.BLUE));
        }

        if (hasTime && hasWeather) {
            environment.append(Text.literal(" - ").formatted(Formatting.GRAY));
        }

        if (hasWeather) {
            environment.append(Text.literal(WeatherTypes.fromInt(row.weather()).getDisplayName()).formatted(Formatting.GREEN));
        }

        if (hasTime || hasWeather) {
            lines.add(environment);
        }

        if (!row.proximityMessage().isEmpty()) {
            lines.add(Text.literal("\"" + ellipsizeTooltipText(row.proximityMessage()) + "\"").formatted(Formatting.WHITE));
        }

        if (row.hasMiscData()) {
            lines.add(Text.translatable("ardapaths.client.marker.configuration.screens.marker_list.misc_hint")
                    .formatted(Formatting.GRAY));
        }

        lines.add(Text.translatable("ardapaths.client.marker.configuration.screens.marker_list.teleport_hint")
                .formatted(Formatting.GRAY, Formatting.ITALIC));
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
     * Data the screen supplies for one marker-list row.
     *
     * @param pos              marker position represented by the row
     * @param timeOfDay        configured marker time, or unset
     * @param weather          configured marker weather, or unset
     * @param proximityMessage configured marker proximity message
     * @param hasMiscData      whether marker action data is configured
     * @param focused          whether this marker is currently being edited
     * @param selected         whether this marker is in the current multi-selection
     * @param isChainBreak     whether this row separates disconnected marker chains
     */
    public record MarkerRow(BlockPos pos, int timeOfDay, int weather, String proximityMessage, boolean hasMiscData, boolean focused,
                            boolean selected, boolean isChainBreak) {
        /**
         * Creates an inert separator between disconnected marker chains.
         *
         * @return chain-break row data
         */
        public static MarkerRow chainBreak() {
            return new MarkerRow(BlockPos.ORIGIN, ChapterNbtData.UNSET, ChapterNbtData.UNSET, "", false, false, false, true);
        }
    }
}

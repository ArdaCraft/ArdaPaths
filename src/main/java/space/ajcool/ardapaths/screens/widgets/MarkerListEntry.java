package space.ajcool.ardapaths.screens.widgets;

import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import space.ajcool.ardapaths.core.Client;
import space.ajcool.ardapaths.core.ModConstants;
import space.ajcool.ardapaths.core.data.TimeOfDay;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity.ChapterNbtData;
import space.ajcool.ardapaths.screens.GuiTextures;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Row in the local marker list showing marker coordinates and configured data hints.
 */
public class MarkerListEntry extends ObjectSelectionList.Entry<MarkerListEntry> {

    /**
     * Icon shown when a marker has time or weather data.
     */
    private static final ResourceLocation MOON_ICON = ModConstants.modId("textures/gui/moon-icon.png");

    /**
     * Icon shown when a marker has weather data.
     */
    private static final ResourceLocation CLOUD_ICON = ModConstants.modId("textures/gui/cloud-icon.png");

    /**
     * Icon shown when a marker has proximity text.
     */
    private static final ResourceLocation TEXT_ICON = ModConstants.modId("textures/gui/text-icon.png");

    /**
     * Icon shown when a marker has action data.
     */
    private static final ResourceLocation GEAR_ICON = ModConstants.modId("textures/gui/gear-icon.png");

    /**
     * Size in pixels used for marker data icons.
     */
    private static final int ICON_SIZE = 8;

    /**
     * Width of the time-of-day color chip at the start of timed marker rows.
     */
    private static final int TIME_BAR_WIDTH = 4;

    /**
     * Marker block position represented by this row.
     */
    @Getter
    private final BlockPos pos;

    /**
     * Configured marker time for the selected path, or unset when no time is configured.
     */
    private final int timeOfDay;

    /**
     * Whether the marker has weather data for the selected path.
     */
    private final boolean hasWeatherData;

    /**
     * Whether the marker has a proximity message for the selected path.
     */
    private final boolean hasProximityMessage;

    /**
     * Whether the marker has action data for the selected path.
     */
    private final boolean hasMiscData;

    /**
     * Whether this row represents the marker currently being edited.
     */
    @Getter
    private final boolean focused;

    /**
     * Whether this row belongs to the current marker-list multi-selection.
     */
    private final boolean selected;

    /**
     * Callback used when the player selects the marker for editing.
     */
    private final Consumer<BlockPos> onSelect;

    /**
     * Callback used when the player requests teleport to the marker.
     */
    private final Consumer<BlockPos> onTeleport;

    /**
     * Callback used when the player selects a range ending at this marker.
     */
    private final Consumer<BlockPos> onRangeSelect;

    /**
     * Callback used when the player opens the marker row context menu.
     */
    private final BiConsumer<BlockPos, ContextMenuAnchor> onContextMenu;

    /**
     * Coordinates displayed and narrated for this marker.
     */
    private final Component coordinateText;

    /**
     * Lines shown while hovering the marker row.
     */
    private final List<Component> tooltipLines;

    /**
     * Whether this row is a non-marker notice.
     */
    @Getter
    private final boolean notice;

    /**
     * Creates a row for a loaded local marker.
     *
     * @param pos                 the marker position
     * @param timeOfDay           configured marker time, or unset
     * @param hasWeatherData      whether the marker has weather data
     * @param hasProximityMessage whether the marker has proximity text
     * @param hasMiscData         whether the marker has action data
     * @param focused             whether this marker is currently being edited
     * @param selected            whether this marker belongs to the current multi-selection
     * @param tooltipLines        formatted tooltip lines for configured marker data
     * @param onSelect            callback for normal clicks
     * @param onTeleport          callback for Ctrl-clicks
     * @param onRangeSelect       callback for Shift-clicks
     * @param onContextMenu       callback for right-clicks
     */
    public MarkerListEntry(BlockPos pos, int timeOfDay, boolean hasWeatherData, boolean hasProximityMessage, boolean hasMiscData, boolean focused, boolean selected,
                           List<Component> tooltipLines, Consumer<BlockPos> onSelect, Consumer<BlockPos> onTeleport,
                           Consumer<BlockPos> onRangeSelect, BiConsumer<BlockPos, ContextMenuAnchor> onContextMenu) {
        this.pos = pos;
        this.timeOfDay = timeOfDay;
        this.hasWeatherData = hasWeatherData;
        this.hasProximityMessage = hasProximityMessage;
        this.hasMiscData = hasMiscData;
        this.focused = focused;
        this.selected = selected;
        this.tooltipLines = List.copyOf(tooltipLines);
        this.onSelect = onSelect;
        this.onTeleport = onTeleport;
        this.onRangeSelect = onRangeSelect;
        this.onContextMenu = onContextMenu;
        this.coordinateText = Component.literal(pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
        this.notice = false;
    }

    /**
     * Creates a row for an inert notice label.
     *
     * @param text notice text to render
     * @return notice marker list row
     */
    public static MarkerListEntry notice(Component text) {
        return new MarkerListEntry(text);
    }

    /**
     * Creates an inert notice row.
     *
     * @param text notice text to render
     */
    private MarkerListEntry(Component text) {
        this.pos = BlockPos.ZERO;
        this.timeOfDay = ChapterNbtData.UNSET;
        this.hasWeatherData = false;
        this.hasProximityMessage = false;
        this.hasMiscData = false;
        this.focused = false;
        this.selected = false;
        this.tooltipLines = List.of();
        this.onSelect = null;
        this.onTeleport = null;
        this.onRangeSelect = null;
        this.onContextMenu = null;
        this.coordinateText = text;
        this.notice = true;
    }

    /**
     * Renders marker coordinates with right-aligned icons for configured marker data.
     *
     * @param context     the draw context
     * @param index       the row index
     * @param y           the row y coordinate
     * @param x           the row x coordinate
     * @param entryWidth  the row width
     * @param entryHeight the row height
     * @param mouseX      the mouse x coordinate
     * @param mouseY      the mouse y coordinate
     * @param hovered     whether the row is hovered
     * @param tickDelta   the partial tick
     */
    @Override
    public void render(GuiGraphics context, int index, int y, int x, int entryWidth, int entryHeight,
                       int mouseX, int mouseY, boolean hovered, float tickDelta) {
        Minecraft client = Minecraft.getInstance();
        Font textRenderer = client.font;
        if (notice) {
            int textWidth = textRenderer.width(coordinateText);
            int textY = y + (entryHeight - textRenderer.lineHeight) / 2;
            context.drawString(textRenderer, coordinateText, x + (entryWidth - textWidth) / 2, textY, 0xFFFF5555);
            return;
        }

        int backgroundColor = focused ? 0x66336699 : selected ? 0x44336699 : hovered ? 0x33FFFFFF : 0x00000000;

        if (backgroundColor != 0x00000000) {
            context.fill(x, y, x + entryWidth, y + entryHeight, backgroundColor);
        }

        if (hasTimeData()) {
            context.fill(x, y, x + TIME_BAR_WIDTH, y + entryHeight, TimeOfDay.gradientColor(timeOfDay));
        }

        int iconX = x + entryWidth - 4;
        int iconY = y + (entryHeight - ICON_SIZE) / 2;
        if (hasProximityMessage) {
            iconX -= ICON_SIZE;
            GuiTextures.blit(context, TEXT_ICON, iconX, iconY, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
            iconX -= 2;
        }

        if (hasTimeData()) {
            iconX -= ICON_SIZE;
            GuiTextures.blit(context, MOON_ICON, iconX, iconY, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
            iconX -= 2;
        }

        if (hasWeatherData) {
            iconX -= ICON_SIZE;
            GuiTextures.blit(context, CLOUD_ICON, iconX, iconY, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
            iconX -= 2;
        }

        if (hasMiscData) {
            iconX -= ICON_SIZE;
            GuiTextures.blit(context, GEAR_ICON, iconX, iconY, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
            iconX -= 2;
        }

        int textWidth = Math.max(0, iconX - x - 8);
        int textY = y + (entryHeight - textRenderer.lineHeight) / 2;
        String trimmedText = textRenderer.plainSubstrByWidth(coordinateText.getString(), textWidth);
        context.drawString(textRenderer, Component.literal(trimmedText), x + 4, textY, 0xFFFFFF);

    }

    /**
     * Renders the row tooltip outside of the list's clipped row-rendering pass.
     *
     * @param context      the draw context
     * @param textRenderer renderer used for tooltip text
     * @param mouseX       the mouse x coordinate
     * @param mouseY       the mouse y coordinate
     */
    public void renderTooltip(GuiGraphics context, Font textRenderer, int mouseX, int mouseY) {
        if (!tooltipLines.isEmpty()) {
            context.renderComponentTooltip(textRenderer, tooltipLines, mouseX, mouseY);
        }
    }

    /**
     * Checks whether this marker row has a configured time.
     *
     * @return true when a marker time should be indicated
     */
    private boolean hasTimeData() {
        return timeOfDay != ChapterNbtData.UNSET;
    }

    /**
     * Handles selecting, range-selecting, teleporting, or opening a context menu for the marker.
     *
     * @param mouseX the mouse x coordinate
     * @param mouseY the mouse y coordinate
     * @param button the mouse button
     * @return true when the row handles the click
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (notice) return false;

        if (button == 1) {
            if (onContextMenu != null) {
                onContextMenu.accept(pos, new ContextMenuAnchor(mouseX, mouseY));
            }
            return true;
        }

        if (button != 0) return false;

        if (Client.isCtrlDown()) {
            if (onTeleport != null) onTeleport.accept(pos);
        } else if (net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
            if (onRangeSelect != null) onRangeSelect.accept(pos);
        } else {
            if (onSelect == null) return false;
            onSelect.accept(pos);
        }

        return true;
    }

    /**
     * Returns the coordinate narration for this row.
     *
     * @return the marker coordinate text
     */
    @Override
    public @NotNull Component getNarration() {
        return coordinateText;
    }

    /**
     * Coordinates where a context menu should be opened.
     *
     * @param mouseX absolute mouse x coordinate
     * @param mouseY absolute mouse y coordinate
     */
    public record ContextMenuAnchor(double mouseX, double mouseY) {
    }
}

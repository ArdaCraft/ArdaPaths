package space.ajcool.ardapaths.screens.marker;

import space.ajcool.ardapaths.screens.widgets.MarkerListPanelWidget;
import space.ajcool.ardapaths.screens.widgets.TabBarWidget;

/**
 * Fixed marker-editor layout constants and coordinates derived from the active screen size.
 *
 * @param screenWidth  width of the screen being laid out
 * @param screenHeight height of the screen being laid out
 */
public record MarkerEditLayout(int screenWidth, int screenHeight) {

    /** Total fixed layout height used to vertically place the marker editor. */
    public static final int TOTAL_HEIGHT = 364;

    /** Minimum top margin for short windows. */
    public static final int MIN_TOP_MARGIN = 5;

    /** Shared width for the main marker editor controls. */
    public static final int MAIN_CONTROL_WIDTH = 320;

    /** Standard control height for buttons, dropdowns, sliders, and the tab bar. */
    public static final int CONTROL_HEIGHT = 20;

    /** X offset from center for left-aligned main controls. */
    public static final int MAIN_LEFT_OFFSET = 160;

    /** Y offset for the path selector. */
    public static final int PATH_Y_OFFSET = 42;

    /** Y offset for the chapter selector and edit button. */
    public static final int CHAPTER_Y_OFFSET = 78;

    /** Y offset for chapter-start controls. */
    public static final int CHAPTER_START_Y_OFFSET = 108;

    /** Y offset for the tab bar. */
    public static final int TAB_BAR_Y_OFFSET = 130;

    /** Y offset for the active tab content area. */
    public static final int CONTENT_TOP_OFFSET = 163;

    /** Reserved height for the active tab content area. */
    public static final int CONTENT_HEIGHT = 164;

    /** Y offset for the fixed footer row. */
    public static final int FOOTER_Y_OFFSET = 337;

    /** Horizontal spacing between the marker list divider and the centered editor. */
    public static final int MARKER_LIST_GUTTER = 40;

    /** Horizontal space consumed by the marker-list column and its gutter. */
    private static final int MARKER_LIST_BLOCK = MarkerListPanelWidget.MARKER_LIST_WIDTH + MARKER_LIST_GUTTER;

    /**
     * Creates layout coordinates for a screen size.
     *
     * @param screenWidth  width of the screen being laid out
     * @param screenHeight height of the screen being laid out
     * @return marker editor layout for that screen size
     */
    public static MarkerEditLayout of(int screenWidth, int screenHeight) {
        return new MarkerEditLayout(screenWidth, screenHeight);
    }

    /**
     * Returns the top edge of the active tab content area.
     *
     * @return content top y coordinate
     */
    public int contentTop() {
        return top() + CONTENT_TOP_OFFSET;
    }

    /**
     * Returns the top edge shared by the fixed editor layout.
     *
     * @return top y coordinate
     */
    public int top() {
        return Math.max(MIN_TOP_MARGIN, (screenHeight - TOTAL_HEIGHT) / 2);
    }

    /**
     * Returns the padded right edge available for widgets inside the tab content panel.
     *
     * @return tab content right x coordinate
     */
    public int contentRight() {
        return contentLeft() + contentWidth();
    }

    /**
     * Returns the padded left edge available for widgets inside the tab content panel.
     *
     * @return tab content left x coordinate
     */
    public int contentLeft() {
        return leftColumnX() + TabBarWidget.CONTENT_PADDING;
    }

    /**
     * Returns the padded width available for widgets inside the tab content panel.
     *
     * @return tab content width
     */
    public int contentWidth() {
        return MAIN_CONTROL_WIDTH - 2 * TabBarWidget.CONTENT_PADDING;
    }

    /**
     * Returns the x coordinate used by left-aligned main controls.
     *
     * @return left column x coordinate
     */
    public int leftColumnX() {
        return centerX() - MAIN_LEFT_OFFSET;
    }

    /**
     * Returns the horizontal center of the marker-list-and-editor block. When the marker-list
     * column does not fit, this is simply the screen center; otherwise it is offset so that the
     * marker-list column and the editor panel are centered together as a pair.
     *
     * @return center x coordinate
     */
    public int centerX() {
        return screenWidth / 2 + (hasMarkerListRoom() ? MARKER_LIST_BLOCK / 2 : 0);
    }

    /**
     * Checks whether the current screen width can show the marker-list column.
     *
     * @return true when the marker-list column fits on screen
     */
    public boolean hasMarkerListRoom() {
        return screenWidth / 2 - MAIN_LEFT_OFFSET - MARKER_LIST_BLOCK / 2 >= MIN_TOP_MARGIN;
    }

    /**
     * Returns the x coordinate for the right edge of the main editor panel.
     *
     * @return panel right x coordinate
     */
    public int panelRight() {
        return leftColumnX() + MAIN_CONTROL_WIDTH;
    }

    /**
     * Returns the y coordinate of the fixed footer row.
     *
     * @return footer y coordinate
     */
    public int footerY() {
        return top() + FOOTER_Y_OFFSET;
    }

    /**
     * Returns the x coordinate for the marker-list divider.
     *
     * @return marker-list divider x coordinate
     */
    public int markerListDividerX() {
        return leftColumnX() - MARKER_LIST_GUTTER / 2;
    }

    /**
     * Returns the visual height between the tab bar and the tab content bottom.
     *
     * @return tab bar content height
     */
    public int tabContentHeight() {
        return CONTENT_TOP_OFFSET + CONTENT_HEIGHT - TAB_BAR_Y_OFFSET - CONTROL_HEIGHT;
    }

    /**
     * Returns the x coordinate for the left edge of the marker-list column.
     *
     * @return marker-list left x coordinate
     */
    public int markerListLeft() {
        return leftColumnX() - MARKER_LIST_GUTTER - MarkerListPanelWidget.MARKER_LIST_WIDTH;
    }
}

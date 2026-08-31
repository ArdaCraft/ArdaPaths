package space.ajcool.ardapaths.screens;

import space.ajcool.ardapaths.ArdaPathsClient;
import space.ajcool.ardapaths.core.data.config.shared.ChapterData;
import space.ajcool.ardapaths.core.data.config.shared.PathData;
import space.ajcool.ardapaths.core.networking.PacketRegistry;
import space.ajcool.ardapaths.core.networking.packets.server.PathMarkerLinksUpdatePacket;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;
import space.ajcool.ardapaths.screens.layout.ScreenLayout;
import space.ajcool.ardapaths.screens.widgets.TextWidget;

import java.util.*;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Screen for editing and managing which paths and chapters a marker is linked to.
 * Displays all available paths and chapters, allowing the player to add or remove marker associations.
 */
public class MarkerLinksEditScreen extends ArdaPathsScreen {

    /** The marker block entity being edited. */
    private final PathMarkerBlockEntity MARKER;
    /** The original set of path/chapter associations before any changes. */
    private final Set<AbstractMap.SimpleEntry<String, String>> originalPathAndChapterData;

    /**
     * Initializes a screen for editing marker path and chapter links.
     *
     * @param marker the marker block entity to edit
     * @param originalPathAndChapterData the original path/chapter associations to track for changes
     */
    protected MarkerLinksEditScreen(PathMarkerBlockEntity marker, Set<AbstractMap.SimpleEntry<String, String>> originalPathAndChapterData)
    {
        super(Component.translatable("ardapaths.client.chapter.configuration.screens.marker.links.edit"));
        this.MARKER = marker;
        this.originalPathAndChapterData = originalPathAndChapterData;
    }

    /**
     * Initializes and lays out all UI elements displaying the marker's linked paths and chapters.
     */
    @Override
    protected void init() {

        int centerX = this.width / 2;
        int y = 0;

        this.addRenderableWidget(TextWidget.create()
                        .setX(centerX - 70)
                        .setY(y)
                        .setWidth(140)
                        .setHeight(20)
                        .setMessage(Component.translatable("ardapaths.client.chapter.configuration.screens.marker.links.edit_marker_links"))
                        .build()
        );

        boolean renderedLinkedData = false;

        if (MARKER.getPathData() != null){

            for (var pathEntryKey : MARKER.getPathData().keySet()){

                var pathData = ArdaPathsClient.CONFIG.getPath(pathEntryKey);
                var markerChapters = MARKER.getPathData().get(pathEntryKey);

                if (markerChapters == null || markerChapters.isEmpty()) continue;

                var pathTitlePositionX = centerX - 70;
                var pathTitlePositionY = y += 30;
                boolean hasLinkedData = false;

                List<String> chapterIds = linkedChapterIds(pathEntryKey, pathData);

                for (String chapterId : chapterIds) {
                    PathMarkerBlockEntity.ChapterNbtData chapterNbtData = markerChapters.get(chapterId);

                    if (chapterNbtData == null || chapterNbtData.isEmpty()) continue;

                    hasLinkedData = true;
                    renderedLinkedData = true;
                    var chapterName = TextWidget.create()
                            .setX(centerX - 120)
                            .setY(y += 25)
                            .setWidth(120)
                            .setHeight(20)
                            .setMessage(chapterDisplayName(pathData, chapterId))
                            .build();
                    chapterName.alignRight();
                    this.addRenderableWidget(chapterName);

                    var unlinkButton = new Button(
                            centerX + 5,
                            y,
                            40,
                            20,
                            Component.translatable("ardapaths.client.chapter.configuration.screens.marker.links.unlink"),
                            button -> unlinkMarkerToPathAndChapter(pathEntryKey, chapterId),
                            Supplier::get
                    );
                    boolean samePath = Objects.equals(ArdaPathsClient.CONFIG.getSelectedPathId(), pathEntryKey);
                    boolean sameChapter = Objects.equals(ArdaPathsClient.CONFIG.getCurrentChapterId(), chapterId);

                    unlinkButton.active = !(samePath && sameChapter);
                    this.addRenderableWidget(unlinkButton);
                }

                if (hasLinkedData) {
                    Component pathName = pathDisplayName(pathData, pathEntryKey);

                    this.addRenderableWidget(TextWidget.create()
                                    .setX(pathTitlePositionX)
                                    .setY(pathTitlePositionY)
                                    .setWidth(140)
                                    .setHeight(20)
                                    .setMessage(pathName)
                                    .build());
                }
            }
        }

        if (!renderedLinkedData) {

            this.addRenderableWidget(TextWidget.create()
                            .setX(centerX - 70)
                            .setY(y+30)
                            .setWidth(140)
                            .setHeight(20)
                            .setMessage(Component.translatable("ardapaths.client.chapter.configuration.screens.marker.links.no_linked_data"))
                            .build()
            );
        }

        ScreenLayout.centerVertically(this);
    }

    /**
     * Removes the association between the marker and a specific path/chapter combination.
     *
     * @param pathEntryKey the ID of the path to unlink
     * @param chapterId the ID of the chapter to unlink
     */
    private void unlinkMarkerToPathAndChapter(String pathEntryKey, String chapterId) {

        MARKER.getPathData().get(pathEntryKey).remove(chapterId);

        if(MARKER.getPathData().get(pathEntryKey).isEmpty()) MARKER.getPathData().remove(pathEntryKey);

        PathMarkerLinksUpdatePacket packet = new PathMarkerLinksUpdatePacket(MARKER.getBlockPos(), MARKER.toNbt());
        PacketRegistry.PATH_MARKER_LINKS_UPDATE.send(packet);
        MARKER.markUpdated();

        originalPathAndChapterData.remove(new AbstractMap.SimpleEntry<>(pathEntryKey, chapterId));

        Minecraft.getInstance().setScreen(new MarkerLinksEditScreen(this.MARKER, originalPathAndChapterData));
    }

    /**
     * Gets the marker's linked chapter IDs with configured chapters first and unknown IDs last.
     *
     * @param pathEntryKey marker path ID whose chapter links should be listed
     * @param pathData configured path data, or null when the path ID is unknown locally
     * @return sorted linked chapter IDs
     */
    private List<String> linkedChapterIds(String pathEntryKey, PathData pathData) {
        List<String> chapterIds = new ArrayList<>(MARKER.getPathData().get(pathEntryKey).keySet());
        chapterIds.sort((left, right) -> compareChapterIds(pathData, left, right));
        return chapterIds;
    }

    /**
     * Compares chapter IDs by configured chapter order, placing unknown IDs after known chapters.
     *
     * @param pathData configured path data, or null when the path ID is unknown locally
     * @param left first chapter ID
     * @param right second chapter ID
     * @return comparison result for display ordering
     */
    private int compareChapterIds(PathData pathData, String left, String right) {
        ChapterData leftChapter = pathData != null ? pathData.getChapter(left) : null;
        ChapterData rightChapter = pathData != null ? pathData.getChapter(right) : null;

        if (leftChapter != null && rightChapter != null) {
            return Integer.compare(leftChapter.getIndex(), rightChapter.getIndex());
        }
        if (leftChapter != null) return -1;
        if (rightChapter != null) return 1;
        return left.compareTo(right);
    }

    /**
     * Creates the display label for a path link.
     *
     * @param pathData configured path data, or null when the path ID is unknown locally
     * @param pathId raw path ID from marker NBT
     * @return styled path label
     */
    private Component pathDisplayName(PathData pathData, String pathId) {
        if (pathData == null) {
            return Component.literal(pathId)
                    .withStyle(style -> style.withBold(true).withUnderlined(true).withItalic(true).withColor(0x888888));
        }

        return Component.literal(pathData.getName())
                .withStyle(style -> style.withBold(true))
                .withStyle(style -> style.withUnderlined(true))
                .withStyle(style -> style.withColor(pathData.getPrimaryColor().asHex()));
    }

    /**
     * Creates the display label for a chapter link.
     *
     * @param pathData configured path data, or null when the path ID is unknown locally
     * @param chapterId raw chapter ID from marker NBT
     * @return styled chapter label
     */
    private MutableComponent chapterDisplayName(PathData pathData, String chapterId) {
        ChapterData chapter = pathData != null ? pathData.getChapter(chapterId) : null;
        if (chapter == null) {
            return Component.literal(chapterId).withStyle(style -> style.withItalic(true).withColor(0x888888));
        }

        return Component.literal(chapter.getName());
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
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        this.renderModBackground(context);
        super.render(context, mouseX, mouseY, delta);
    }

    /**
     * Closes this screen and returns to the marker edit screen.
     */
    @Override
    public void onClose()
    {
        if (this.minecraft != null)
            this.minecraft.setScreen(new MarkerEditScreen(MARKER, originalPathAndChapterData));
    }
}

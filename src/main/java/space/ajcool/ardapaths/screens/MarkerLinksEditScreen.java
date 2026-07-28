package space.ajcool.ardapaths.screens;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import space.ajcool.ardapaths.ArdaPathsClient;
import space.ajcool.ardapaths.core.data.config.shared.ChapterData;
import space.ajcool.ardapaths.core.networking.PacketRegistry;
import space.ajcool.ardapaths.core.networking.packets.server.PathMarkerLinksUpdatePacket;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;
import space.ajcool.ardapaths.screens.widgets.TextWidget;

import java.util.*;
import java.util.function.Supplier;

/**
 * Screen for editing and managing which paths and chapters a marker is linked to.
 * Displays all available paths and chapters, allowing the player to add or remove marker associations.
 */
public class MarkerLinksEditScreen extends Screen {

    private final PathMarkerBlockEntity MARKER;
    private final Set<AbstractMap.SimpleEntry<String, String>> originalPathAndChapterData;

    protected MarkerLinksEditScreen(PathMarkerBlockEntity marker, Set<AbstractMap.SimpleEntry<String, String>> originalPathAndChapterData)
    {
        super(Text.translatable("ardapaths.client.chapter.configuration.screens.marker.links.edit"));
        this.MARKER = marker;
        this.originalPathAndChapterData = originalPathAndChapterData;
    }

    @Override
    protected void init() {

        int centerX = this.width / 2;
        int y = 20;

        this.addDrawableChild(TextWidget.create()
                        .setX(centerX - 70)
                        .setY(y)
                        .setWidth(140)
                        .setHeight(20)
                        .setMessage(Text.translatable("ardapaths.client.chapter.configuration.screens.marker.links.edit_marker_links"))
                        .build()
        );

        if (MARKER.getPathData() != null){

            for (var pathEntryKey : MARKER.getPathData().keySet()){

                var pathData = ArdaPathsClient.CONFIG.getPath(pathEntryKey);

                if (pathData != null){

                    if (MARKER.getPathData().get(pathEntryKey).isEmpty()) continue;

                    var pathTitlePositionX = centerX - 70;
                    var pathTitlePositionY = y += 30;
                    boolean hasLinkedData = false;

                    List<ChapterData> chapterData = new ArrayList<>(pathData.getChapters());
                    chapterData.sort(Comparator.comparingInt(ChapterData::getIndex));

                    for (ChapterData chapter : chapterData) {

                        boolean doesMarkerReferenceChapter = MARKER.getPathData().get(pathEntryKey).containsKey(chapter.getId());
                        boolean isDefault = MARKER.getPathData().get(pathEntryKey).get(chapter.getId()) == null || MARKER.getPathData().get(pathEntryKey).get(chapter.getId()).isEmpty();
                        boolean isInOriginalMarkerData = originalPathAndChapterData.contains(new AbstractMap.SimpleEntry<>(pathEntryKey, chapter.getId()));

                        if (doesMarkerReferenceChapter && !isDefault && isInOriginalMarkerData) {

                            hasLinkedData = true;
                            var chaperName = TextWidget.create()
                                    .setX(centerX - 120)
                                    .setY(y += 25)
                                    .setWidth(120)
                                    .setHeight(20)
                                    .setMessage(Text.literal(chapter.getName()))
                                    .build();
                            chaperName.alignRight();
                            this.addDrawableChild(chaperName);

                            var unlinkButton = new ButtonWidget(
                                    centerX + 5,
                                    y,
                                    40,
                                    20,
                                    Text.translatable("ardapaths.client.chapter.configuration.screens.marker.links.unlink"),
                                    button -> unlinkMarkerToPathAndChapter(pathEntryKey, chapter),
                                    Supplier::get
                            );
                            boolean samePath = Objects.equals(ArdaPathsClient.CONFIG.getSelectedPathId(), pathEntryKey);
                            boolean sameChapter = Objects.equals(ArdaPathsClient.CONFIG.getCurrentChapterId(), chapter.getId());

                            unlinkButton.active = !(samePath && sameChapter);
                            this.addDrawableChild(unlinkButton);
                        }
                    }

                    if (hasLinkedData) {
                        Text pathName = Text.literal(pathData.getName())
                                .styled(style -> style.withBold(true))
                                .styled(style -> style.withUnderline(true))
                                .styled(style -> style.withColor(pathData.getPrimaryColor().asHex()));

                        this.addDrawableChild(TextWidget.create()
                                        .setX(pathTitlePositionX)
                                        .setY(pathTitlePositionY)
                                        .setWidth(140)
                                        .setY(20)
                                        .setMessage(pathName)
                                        .build());
                    }
                }
            }

        } else {

            this.addDrawableChild(TextWidget.create()
                            .setX(centerX - 70)
                            .setY(y+30)
                            .setWidth(140)
                            .setY(20)
                            .setMessage(Text.translatable("ardapaths.client.chapter.configuration.screens.marker.links.no_linked_data"))
                            .build()
            );
        }
    }

    private void unlinkMarkerToPathAndChapter(String pathEntryKey, ChapterData chapter) {

        MARKER.getPathData().get(pathEntryKey).remove(chapter.getId());

        if(MARKER.getPathData().get(pathEntryKey).isEmpty()) MARKER.getPathData().remove(pathEntryKey);

        PathMarkerLinksUpdatePacket packet = new PathMarkerLinksUpdatePacket(MARKER.getPos(), MARKER.toNbt());
        PacketRegistry.PATH_MARKER_LINKS_UPDATE.send(packet);
        MARKER.markUpdated();

        originalPathAndChapterData.remove(new AbstractMap.SimpleEntry<>(pathEntryKey, chapter.getId()));

        MinecraftClient.getInstance().setScreen(new MarkerLinksEditScreen(this.MARKER, originalPathAndChapterData));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close()
    {
        if (this.client != null)
            this.client.setScreen(new MarkerEditScreen(MARKER, originalPathAndChapterData));
    }
}

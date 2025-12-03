package space.ajcool.ardapaths.screens;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import space.ajcool.ardapaths.ArdaPathsClient;
import space.ajcool.ardapaths.core.data.config.shared.ChapterData;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;
import space.ajcool.ardapaths.screens.builders.TextBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class MarkerLinksEditScreen extends Screen {

    private final Screen parent;
    private PathMarkerBlockEntity MARKER;

    protected MarkerLinksEditScreen(Screen parent, PathMarkerBlockEntity marker)
    {
        super(Text.translatable("ardapaths.client.chapter.configuration.screens.marker.links.edit"));
        this.parent = parent;
        this.MARKER = marker;
    }

    @Override
    protected void init() {

        int centerX = this.width / 2;
        int y = 20;

        this.addDrawableChild(TextBuilder.create()
                .setPosition(centerX - 70, y)
                .setSize(140, 20)
                .setText(Text.translatable("ardapaths.client.chapter.configuration.screens.marker.links.edit_marker_links"))
                .build()
        );

        if (MARKER.getPathData() != null){
            for (var pathEntryKey : MARKER.getPathData().keySet()){

                var pathData = ArdaPathsClient.CONFIG.getPath(pathEntryKey);

                if (pathData != null){
                    Text pathName = Text.literal(pathData.getName())
                            .styled(style -> style.withBold(true))
                            .styled(style -> style.withUnderline(true))
                            .styled(style -> style.withColor(pathData.getPrimaryColor().asHex()));

                    this.addDrawableChild(TextBuilder.create()
                            .setPosition(centerX - 70, y+=30)
                            .setSize(140, 20)
                            .setText(pathName)
                            .build());

                    List<ChapterData> chapterData = new ArrayList<>(pathData.getChapters());
                    chapterData.sort((o1, o2) -> Integer.compare(o1.getIndex(), o2.getIndex()));

                    for (ChapterData chapter : chapterData) {

                        if (MARKER.getPathData().get(pathEntryKey).containsKey(chapter.getId())) {

                            var chaperName = TextBuilder.create()
                                    .setPosition(centerX - 120, y += 25)
                                    .setSize(120, 20)
                                    .setText(Text.literal(chapter.getName()))
                                    .build();
                            chaperName.alignRight();
                            this.addDrawableChild(chaperName);

                            var unlinkButton = new ButtonWidget(
                                    centerX + 5,
                                    y,
                                    40,
                                    20,
                                    Text.translatable("ardapaths.client.chapter.configuration.screens.marker.links.unlink"),
                                    button -> {
                                        MARKER.getPathData().get(pathEntryKey).remove(chapter.getId());
                                        MinecraftClient.getInstance().setScreen(new MarkerLinksEditScreen(this.parent, this.MARKER));
                                    },
                                    Supplier::get
                            );
                            boolean samePath = Objects.equals(ArdaPathsClient.CONFIG.getSelectedPathId(), pathEntryKey);
                            boolean sameChapter = Objects.equals(ArdaPathsClient.CONFIG.getCurrentChapterId(), chapter.getId());

                            unlinkButton.active = !(samePath && sameChapter);
                            this.addDrawableChild(unlinkButton);
                        }
                    }
                }
            }
        } else {

            this.addDrawableChild(TextBuilder.create()
                    .setPosition(centerX - 70, y+30)
                    .setSize(140, 20)
                    .setText(Text.translatable("ardapaths.client.chapter.configuration.screens.marker.links.no_linked_data"))
                    .build()
            );
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close()
    {
        this.client.setScreen(this.parent);
    }
}

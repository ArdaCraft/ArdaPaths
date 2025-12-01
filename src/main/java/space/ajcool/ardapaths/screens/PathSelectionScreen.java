package space.ajcool.ardapaths.screens;


import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;
import space.ajcool.ardapaths.ArdaPathsClient;
import space.ajcool.ardapaths.core.data.config.shared.ChapterData;
import space.ajcool.ardapaths.core.data.config.shared.Color;
import space.ajcool.ardapaths.core.data.config.shared.PathData;
import space.ajcool.ardapaths.paths.Paths;
import space.ajcool.ardapaths.paths.rendering.ProximityMessageRenderer;
import space.ajcool.ardapaths.paths.rendering.ProximityTitleRenderer;
import space.ajcool.ardapaths.paths.rendering.TrailRenderer;
import space.ajcool.ardapaths.paths.rendering.objects.AnimatedMessage;
import space.ajcool.ardapaths.screens.builders.DropdownBuilder;
import space.ajcool.ardapaths.screens.builders.TextBuilder;
import space.ajcool.ardapaths.screens.widgets.DropdownWidget;

import java.util.List;
import java.util.function.Supplier;

@Environment(value = EnvType.CLIENT)
public class PathSelectionScreen extends Screen
{
    private String selectedPathId;
    private String selectedChapterId;
    private boolean showProximityMessages;
    private final double proximityTextSpeedMultiplier;

    public PathSelectionScreen()
    {
        super(Text.literal(Text.translatable("ardapaths.client.configuration.screens.path_selection").getString()));
        this.selectedPathId = ArdaPathsClient.CONFIG.getSelectedPathId();
        this.selectedChapterId = ArdaPathsClient.CONFIG.getCurrentChapterId();
        this.showProximityMessages = ArdaPathsClient.CONFIG.showProximityMessages();
        this.proximityTextSpeedMultiplier = ArdaPathsClient.CONFIG.getProximityTextSpeedMultiplier();
    }

    @Override
    protected void init()
    {
        int center = width / 2;
        int y = 35;

        PathData currentPath = ArdaPathsClient.CONFIG.getPath(selectedPathId);
        ChapterData currentChapter = ArdaPathsClient.CONFIG.getCurrentChapter();

        String currentChapterName = currentChapter != null ? currentChapter.getName() : "0";
        String currentPathName = currentPath != null ? currentPath.getName() : Text.translatable("ardapaths.client.configuration.screens.generic_path").toString();

        this.addDrawableChild(TextBuilder.create()
                .setPosition(center - 75, y)
                .setSize(150, 20)
                .setText(Text.literal(Text.translatable("ardapaths.client.configuration.screens.path_selection.current_path_chapter",currentChapterName).getString())
                        .append(Text.literal(Text.translatable(currentPathName).getString())
                                .fillStyle(Style.EMPTY.withColor(currentPath != null ? currentPath.getPrimaryColor().asHex() : Color.fromRgb(100, 100, 100).asHex()))))
                .build()
        );

        this.addDrawableChild(initializePathSelectionDropDown(center - 75, y += 40));
        this.addDrawableChild(this.initializeChapterSelectionDropDown(center - 75, y+= 40, currentPath, currentChapter));
        this.addDrawableChild(initializeReturnToPathButton(center - 65,y += 30));
        this.addDrawableChild(initializeReturnToChapterStartButton(center - 65,y += 30));
        this.addDrawableChild(initializeProximityTextToggle(center - 65,y += 30));
        this.addDrawableChild(initializeProximityTextSpeedMultiplierSlider(center - 65, y + 30));
    }

    private @NotNull DropdownWidget<PathData> initializePathSelectionDropDown(int center, int y) {
        DropdownWidget<PathData> pathSelectionDropdown = DropdownBuilder.<PathData>create()
                .setPosition(center,y)
                .setSize(150, 20)
                .setTitle(Text.translatable( "ardapaths.client.configuration.screens.select_path_follow"))
                .setOptions(ArdaPathsClient.CONFIG.getPaths())
                .setOptionDisplay(item ->
                {
                    if (item == null) return Text.literal("No Path");
                    return Text.literal(item.getName()).fillStyle(Style.EMPTY.withColor(item.getPrimaryColor().asHex()));
                })
                .setSelected(ArdaPathsClient.CONFIG.getSelectedPath())
                .setOnSelect(path ->
                {
                    if (!path.getId().equalsIgnoreCase(selectedPathId))
                    {
                        TrailRenderer.clearTrails();
                    }

                    // Reset last visited node data
                    ArdaPathsClient.lastVisitedTrailNodeData = null;

                    selectedPathId = path.getId();
                    selectedChapterId = path.getChapterIds().get(0);

                    Paths.setSelectedPath(selectedPathId);
                    Paths.gotoChapter(selectedChapterId, false);

                    this.clearAndInit();
                })

                .build();
        pathSelectionDropdown.setTooltip(Tooltip.of(Text.translatable("ardapaths.client.configuration.screens.select_path_follow_tooltip")));
        return pathSelectionDropdown;
    }

    private @NotNull DropdownWidget<ChapterData> initializeChapterSelectionDropDown(int center, int y, PathData currentPath, ChapterData currentChapter) {

        List<ChapterData> chapterData = currentPath != null ? currentPath.getChapters() : List.of();

        return DropdownBuilder.<ChapterData>create()
                .setPosition(center, y)
                .setSize(150, 20)
                .setTitle(Text.translatable("ardapaths.client.configuration.screens.select_chapter"))
                .setOptions(chapterData)
                .setOptionDisplay(item ->
                {
                    if (item == null) return Text.translatable("ardapaths.client.configuration.screens.no_chapter");
                    return Text.literal(item.getName());
                })
                .setSelected(currentChapter)
                .setOnSelect(chapter ->
                {
                    selectedChapterId = chapter == null ? "default" : chapter.getId();

                    // Reset last visited node data
                    ArdaPathsClient.lastVisitedTrailNodeData = null;

                    Paths.gotoChapter(selectedChapterId, false);

                    this.clearAndInit();
                })
                .build();
    }

    private @NotNull ButtonWidget initializeReturnToPathButton(int center, int y) {
        ButtonWidget returnToPathButton = new ButtonWidget(
                center, y,
                130,
                20,
                Text.literal(Text.translatable("ardapaths.client.configuration.screens.return_path").getString()),
                button ->
                {
                    ArdaPathsClient.callingForTeleport = true;
                    TrailRenderer.clearTrails();
                    this.close();
                },
                Supplier::get
        );
        returnToPathButton.setTooltip(Tooltip.of(Text.translatable("ardapaths.client.configuration.screens.return_path_tooltip")));

        return  returnToPathButton;
    }

    private @NotNull ButtonWidget initializeReturnToChapterStartButton(int center, int y) {

        ButtonWidget returnChapterStartButton = new ButtonWidget(
                center, y,
                130,
                20,
                Text.literal(Text.translatable("ardapaths.client.configuration.screens.return_chapter_start").getString()),
                button ->
                {
                    this.close();
                    if (!selectedPathId.isEmpty() && !selectedChapterId.isEmpty())
                    {
                        ProximityTitleRenderer.clearMessage();
                        Paths.gotoChapter(selectedChapterId);
                    }
                },
                Supplier::get
        );
        returnChapterStartButton.setTooltip(Tooltip.of(Text.translatable("ardapaths.client.configuration.screens.return_chapter_start_tooltip")));

        return returnChapterStartButton;
    }

    private @NotNull ButtonWidget initializeProximityTextToggle(int center, int y) {

        ButtonWidget proximityTextToggle = new ButtonWidget(
                center, y,
                130,
                20,
                Text.translatable("ardapaths.client.configuration.screens.proximity_text", (showProximityMessages ? Text.translatable("ardapaths.generic.on"):Text.translatable("ardapaths.generic.off"))),
                button ->
                {
                    showProximityMessages = !showProximityMessages;
                    Paths.showProximityMessages(showProximityMessages);
                    ProximityMessageRenderer.clearMessage();
                    ProximityTitleRenderer.clearMessage();
                    button.setMessage(Text.translatable("ardapaths.client.configuration.screens.marker_text", (showProximityMessages ? Text.translatable("ardapaths.generic.on"):Text.translatable("ardapaths.generic.off"))));
                },
                Supplier::get
        );
        proximityTextToggle.setTooltip(Tooltip.of(Text.translatable("ardapaths.client.configuration.screens.proximity_text_tooltip")));

        return proximityTextToggle;
    }

    private @NotNull SliderWidget initializeProximityTextSpeedMultiplierSlider(int center, int y) {

        double proximityTextSpeedMultiplierPercent = (proximityTextSpeedMultiplier / AnimatedMessage.DEFAULT_PROXIMITY_TEXT_SPEED_MULTIPLIER) * 100.0;
        double proximityTextSpeedMultiplierClamped = MathHelper.clamp((proximityTextSpeedMultiplierPercent - 50.0) / 100.0, 0.0, 1.0);

        SliderWidget sliderWidget = new SliderWidget(
                center, y,
                130,
                20,
                Text.literal(Text.translatable("ardapaths.client.configuration.screens.proximity_text_speed_multiplier").getString()),
                proximityTextSpeedMultiplierClamped
        ) {

            @Override
            protected void updateMessage() {
                int percent = (int) (50 + this.value * 100);
                this.setMessage(Text.literal(percent + "%"));
            }

            @Override
            protected void applyValue() {
                double percent = 50 + this.value * 100;
                Paths.setProximityMessagesSpeedMultiplier(AnimatedMessage.DEFAULT_PROXIMITY_TEXT_SPEED_MULTIPLIER * (percent / 100.0));
            }
        };

        sliderWidget.setTooltip(Tooltip.of(Text.translatable("ardapaths.client.configuration.screens.proximity_text_speed_multiplier_tooltip")));
        return sliderWidget;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta)
    {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
    }
}
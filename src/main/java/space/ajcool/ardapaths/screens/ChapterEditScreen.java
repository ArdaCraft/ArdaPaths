package space.ajcool.ardapaths.screens;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.ArdaPathsClient;
import space.ajcool.ardapaths.core.data.config.shared.ChapterData;
import space.ajcool.ardapaths.core.data.config.shared.Color;
import space.ajcool.ardapaths.core.data.config.shared.PathData;
import space.ajcool.ardapaths.core.networking.PacketRegistry;
import space.ajcool.ardapaths.core.networking.packets.server.PathDataUpdatePacket;
import space.ajcool.ardapaths.paths.Paths;
import space.ajcool.ardapaths.screens.builders.DropdownBuilder;
import space.ajcool.ardapaths.screens.builders.InputBoxBuilder;
import space.ajcool.ardapaths.screens.builders.TextBuilder;
import space.ajcool.ardapaths.screens.widgets.DropdownWidget;
import space.ajcool.ardapaths.screens.widgets.InputBoxWidget;
import space.ajcool.ardapaths.screens.widgets.TextValidationError;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ChapterEditScreen extends Screen
{
    private final Screen parent;
    private boolean creatingNew;
    private DropdownWidget<ChapterData> chapterDropdown;
    private InputBoxWidget idInput;
    private InputBoxWidget nameInput;
    private InputBoxWidget dateInput;
    private InputBoxWidget indexInput;
    private InputBoxWidget warpInput;
    private InputBoxWidget pathColorPrimary;
    private InputBoxWidget pathColorSecondary;
    private InputBoxWidget pathColorTertiary;
    private ButtonWidget applyColorChangesButton;
    private DropdownWidget<PathData> pathDropdown;

    protected ChapterEditScreen(Screen parent)
    {
        super(Text.translatable("ardapaths.client.chapter.configuration.screens.chapter_edit_title"));
        this.parent = parent;
        this.creatingNew = false;
    }

    @Override
    public void init()
    {
        int centerX = this.width / 2;
        int y = 20;

        this.addDrawableChild(TextBuilder.create()
                .setPosition(centerX - 70, y)
                .setSize(140, 20)
                .setText(Text.translatable("ardapaths.client.marker.configuration.screens.edit_chapters"))
                .build()
        );

        PathData selectedPath = ArdaPathsClient.CONFIG.getSelectedPath();
        pathDropdown = this.addDrawableChild(DropdownBuilder.<PathData>create()
                .setPosition(centerX - 140, y += 40)
                .setSize(280, 20)
                .setTitle(Text.translatable("ardapaths.client.chapter.configuration.screens.select_path"))
                .setOptions(ArdaPathsClient.CONFIG.getPaths())
                .setOptionDisplay(path ->
                {
                    if (path == null) return Text.translatable("ardapaths.generic.validation.chapter.screens.no_path_selected");
                    return Text.literal(path.getName()).fillStyle(Style.EMPTY.withColor(path.getPrimaryColor().asHex()));
                })
                .setSelected(selectedPath)
                .build()
        );

        var defaultTextColor = new Color(255, 255, 255);

        pathColorPrimary = buildColorInputBox(centerX - 140, y += 42, pathDropdown.getSelected() != null ? pathDropdown.getSelected().getPrimaryColor() : defaultTextColor, "ardapaths.client.marker.configuration.screens.path_primary_color");
        pathColorSecondary = buildColorInputBox(centerX - 70, y, pathDropdown.getSelected() != null ? pathDropdown.getSelected().getSecondaryColor() : defaultTextColor, "ardapaths.client.marker.configuration.screens.path_secondary_color");
        pathColorTertiary = buildColorInputBox(centerX, y, pathDropdown.getSelected() != null ? pathDropdown.getSelected().getTertiaryColor() : defaultTextColor, "ardapaths.client.marker.configuration.screens.path_tertiary_color");

        this.addDrawableChild(pathColorPrimary);
        this.addDrawableChild(pathColorSecondary);
        this.addDrawableChild(pathColorTertiary);

        applyColorChangesButton = ButtonWidget.builder(
                        Text.translatable("ardapaths.generic.apply"),
                                button -> { saveColorsToPath(); })
                        .position(centerX + 90, y)
                        .size(50, 20)
                        .tooltip(Tooltip.of(Text.translatable("ardapaths.client.marker.configuration.screens.path_colors_apppy_tooltip")))
                        .build();
        applyColorChangesButton.active = hasPathColorChanges();
        addDrawableChild(applyColorChangesButton);

        List<ChapterData> chapters = selectedPath != null ? new ArrayList<>(selectedPath.getChapters()) : new ArrayList<>();
        chapters.sort(Comparator.comparingInt(ChapterData::getIndex));

        chapterDropdown = this.addDrawableChild(DropdownBuilder.<ChapterData>create()
                .setPosition(centerX - 140, y += 35)
                .setSize(238, 20)
                .setTitle(Text.literal("Select Chapter to Edit:"))
                .setOptions(chapters)
                .setOptionDisplay(chapter ->
                {
                    if (chapter == null) return Text.translatable("ardapaths.generic.validation.chapter.screens.no_chapter_selected");
                    return Text.literal(chapter.getName());
                })
                .build()
        );
        int addButtonY = y;

        idInput = this.addDrawableChild(InputBoxBuilder.create()
                .setPosition(centerX - 75, y += 40)
                .setSize(150, 20)
                .setPlaceholder(Text.literal("Id..."))
                .setValidator(text ->
                {
                    if (text.length() < 3)
                    {
                        throw new TextValidationError(Text.translatable("ardapaths.generic.validation.error.string.three_char_long").getString());
                    }
                    else if (text.length() > 32)
                    {
                        throw new TextValidationError(Text.translatable("ardapaths.generic.validation.error.string.too_long_32").getString());
                    }
                    else if (creatingNew)
                    {
                        PathData path = pathDropdown.getSelected();
                        if (path == null)
                        {
                            throw new TextValidationError(Text.translatable("ardapaths.generic.validation.chapter.screens.no_path_selected").getString());
                        }
                        else if (path.getChapters() != null && !path.getChapters().isEmpty())
                        {
                            ChapterData chapter = path.getChapters().stream().filter(ch -> ch.getId().equalsIgnoreCase(text)).findFirst().orElse(null);
                            if (chapter != null)
                            {
                                throw new TextValidationError(Text.translatable("ardapaths.generic.validation.chapter.screens.id_in_use").getString());
                            }
                        }
                    }
                })
                .build()
        );

        nameInput = this.addDrawableChild(InputBoxBuilder.create()
                .setPosition(centerX - 75, y += 30)
                .setSize(150, 20)
                .setPlaceholder(Text.translatable("ardapaths.client.chapter.configuration.screens.name"))
                .build()
        );

        dateInput = this.addDrawableChild(InputBoxBuilder.create()
                .setPosition(centerX - 75, y += 30)
                .setSize(150, 20)
                .setPlaceholder(Text.translatable("ardapaths.client.chapter.configuration.screens.date"))
                .build()
        );

        indexInput = this.addDrawableChild(InputBoxBuilder.create()
                .setPosition(centerX - 75, y += 30)
                .setSize(150, 20)
                .setPlaceholder(Text.translatable("ardapaths.client.chapter.configuration.screens.index"))
                .setValidator(text ->
                {
                    try
                    {
                        Integer.parseInt(text);
                    }
                    catch (NumberFormatException e)
                    {
                        throw new TextValidationError(Text.translatable("ardapaths.generic.validation.error.integer").getString());
                    }
                })
                .build()
        );

        warpInput = this.addDrawableChild(InputBoxBuilder.create()
                .setPosition(centerX - 75, y += 30)
                .setSize(150, 20)
                .setPlaceholder(Text.translatable("ardapaths.client.chapter.configuration.screens.warp_location"))
                .build()
        );

        this.addDrawableChild(ButtonWidget.builder(
                        Text.literal("＋"),
                        button ->
                        {
                            resetFields();
                            indexInput.reset(String.valueOf(chapterDropdown.getOptions().size() + 1));
                        })
                .position(centerX + 100, addButtonY)
                .size(20, 20)
                .tooltip(Tooltip.of(Text.translatable("ardapaths.client.chapter.configuration.screens.create_chapter")))
                .build()
        );

        this.addDrawableChild(ButtonWidget.builder(
                        Text.literal("-"),
                        button -> { deleteChapter(); })
                .position(centerX + 120, addButtonY)
                .size(20, 20)
                .tooltip(Tooltip.of(Text.translatable("ardapaths.client.chapter.configuration.screens.delete_chapter")))
                .build()
        );

        this.addDrawableChild(ButtonWidget.builder(
                        Text.translatable("ardapaths.generic.clear"),
                        button ->
                        {
                            resetFields();
                            creatingNew = false;
                        })
                .position(centerX - 152, y += 40)
                .size(150, 20)
                .build()
        );

        this.addDrawableChild(ButtonWidget.builder(
                        Text.translatable("ardapaths.generic.save"),
                        button ->
                        {
                            if (!idInput.validateText() || !nameInput.validateText() || !dateInput.validateText() || !indexInput.validateText())
                                return;

                            PathData path = pathDropdown.getSelected();
                            if (path == null) return;

                            ChapterData chapter = new ChapterData(
                                    idInput.getText(),
                                    nameInput.getText(),
                                    dateInput.getText(),
                                    Integer.parseInt(indexInput.getText()),
                                    warpInput.getText()
                            );
                            Paths.updateChapter(path.getId(), chapter);

                            saveColorsToPath();

                            chapterDropdown.setOptions(path.getChapters());
                            resetFields();
                            creatingNew = false;
                        })
                .position(centerX + 2, y)
                .size(150, 20)
                .build()
        );

        pathDropdown.setOnSelect(path ->
        {
            if (path == null) return;
            chapterDropdown.setOptions(path.getChapters());
            boolean isCreatingNew = creatingNew;
            resetFields();
            creatingNew = isCreatingNew;
        });

        chapterDropdown.setOnSelect(chapter ->
        {
            if (chapter == null) return;
            idInput.disable();
            idInput.setText(chapter.getId());
            nameInput.setText(chapter.getName());
            dateInput.setText(chapter.getDate());
            indexInput.setText(String.valueOf(chapter.getIndex()));
            warpInput.setText(chapter.getWarp());
        });
    }

    private void saveColorsToPath() {
        // Update path colors if changed
        if (hasPathColorChanges()) {

            assert pathDropdown.getSelected() != null;

            Color inputPrimaryColor = Color.fromHexString(pathColorPrimary.getText());
            Color inputSecondaryColor = Color.fromHexString(pathColorSecondary.getText());
            Color inputTertiaryColor = Color.fromHexString(pathColorTertiary.getText());

            pathDropdown.getSelected().setPrimaryColor(inputPrimaryColor);
            pathDropdown.getSelected().setSecondaryColor(inputSecondaryColor);
            pathDropdown.getSelected().setTertiaryColor(inputTertiaryColor);

            PathDataUpdatePacket pathDataUpdatePacket = new PathDataUpdatePacket(pathDropdown.getSelected().getId(),
                    pathDropdown.getSelected().getName(),
                    inputPrimaryColor.asHex(),
                    inputSecondaryColor.asHex(),
                    inputTertiaryColor.asHex());

            PacketRegistry.PATH_DATA_UPDATE_REQUEST.send(pathDataUpdatePacket);
        }
    }

    private InputBoxWidget buildColorInputBox(int x, int y, Color textColor, String placeholder)
    {
        InputBoxWidget colorInputBox = InputBoxBuilder.create()
                .setPosition(x, y)
                .setSize(60, 17)
                .setValidator(text ->
                {
                    if (!text.matches("^#([a-fA-F0-9]{6})$"))
                        throw new TextValidationError(Text.translatable("ardapaths.client.marker.configuration.screens.path_colors.validation.error").getString());

                    applyColorChangesButton.active = hasPathColorChanges();
                })
                .setPlaceholder(Text.translatable(placeholder))
                .build();

        colorInputBox.setText(textColor.asHexString());
        colorInputBox.setTextColor(textColor.asHex());

        colorInputBox.setTooltip(Tooltip.of(Text.translatable("ardapaths.client.marker.configuration.screens.path_colors_tooltip")));

        colorInputBox.setChangeListener(input ->{
            colorInputBox.validateText();
            Color color = Color.fromHexString(input);
            colorInputBox.setTextColor(color.asHex());
        });

        return colorInputBox;
    }

    private void deleteChapter() {

        assert client != null;

        PathData path = pathDropdown.getSelected();
        if (path == null) return;

        ChapterData chapter = chapterDropdown.getSelected();
        if (chapter == null) return;

        if (chapter.getName().equalsIgnoreCase("default")) {
            ArdaPaths.LOGGER.warn("Attempted to delete default chapter, action blocked.");

            var message = Text.empty().append(Text.translatable("ardapaths.client.chapter.configuration.screens.error.delete_default_chapter").formatted(Formatting.RED));
            MinecraftClient.getInstance().player.sendMessage(message);

            return;
        }

        client.setScreen(new ConfirmationPopup(
                Text.translatable("ardapaths.client.marker.configuration.screens.chapter_delete_popup_text", chapter.getName()),
                // Popup closed / confirm
                () -> {

                    Paths.deleteChapter(path.getId(), chapter);
                    resetFields();
                },
                // Popup closed / decline
                () -> { ArdaPaths.LOGGER.info("Canceled chapter deletion."); },
                this
        ));
    }

    private boolean hasPathColorChanges(){

        if (pathDropdown.getSelected() != null) {
            Color initialPathPrimaryColor = pathDropdown.getSelected().getPrimaryColor();
            Color initialPathSecondaryColor = pathDropdown.getSelected().getSecondaryColor();
            Color initialPathTertiaryColor = pathDropdown.getSelected().getTertiaryColor();

            Color inputPrimaryColor = Color.fromHexString(pathColorPrimary.getText());
            Color inputSecondaryColor = Color.fromHexString(pathColorSecondary.getText());
            Color inputTertiaryColor = Color.fromHexString(pathColorTertiary.getText());

            return inputPrimaryColor.asHex() != initialPathPrimaryColor.asHex() ||
                    inputSecondaryColor.asHex() != initialPathSecondaryColor.asHex() ||
                    inputTertiaryColor.asHex() != initialPathTertiaryColor.asHex();
        }

        return false;
    }

    private void resetFields() {
        creatingNew = true;
        chapterDropdown.setSelected(null);
        idInput.enable();
        idInput.reset();
        nameInput.reset();
        dateInput.reset();
        indexInput.reset();
        warpInput.reset();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta)
    {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;
        int y = 85;

        context.drawTextWithShadow(this.textRenderer, Text.translatable("ardapaths.client.marker.configuration.screens.path_colors"), centerX - 139, y, 0xFFFFFF);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button)
    {
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void close()
    {
        this.client.setScreen(this.parent);
    }
}

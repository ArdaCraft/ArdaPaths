package space.ajcool.ardapaths.screens;

import lombok.extern.slf4j.Slf4j;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import space.ajcool.ardapaths.ArdaPathsClient;
import space.ajcool.ardapaths.core.data.config.shared.ChapterData;
import space.ajcool.ardapaths.core.data.config.shared.Color;
import space.ajcool.ardapaths.core.data.config.shared.PathData;
import space.ajcool.ardapaths.core.networking.PacketRegistry;
import space.ajcool.ardapaths.core.networking.packets.server.PathDataUpdatePacket;
import space.ajcool.ardapaths.paths.Paths;
import space.ajcool.ardapaths.screens.layout.ScreenLayout;
import space.ajcool.ardapaths.screens.widgets.DropdownWidget;
import space.ajcool.ardapaths.screens.widgets.InputBoxWidget;
import space.ajcool.ardapaths.screens.widgets.TextValidationError;
import space.ajcool.ardapaths.screens.widgets.TextWidget;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Screen for editing and creating chapter data within a path.
 * Allows configuration of chapter metadata (name, date, index), warp locations, and associated path colors.
 */
@Slf4j(topic = "ardapaths")
public class ChapterEditScreen extends ArdaPathsScreen
{
    /** Parent screen to return to */
    private final Screen parent;
    /** Whether this screen is creating a new chapter */
    private boolean creatingNew;
    /** Dropdown widget for selecting chapters */
    private DropdownWidget<ChapterData> chapterDropdown;
    /** Input widget for chapter ID */
    private InputBoxWidget idInput;
    /** Input widget for chapter name */
    private InputBoxWidget nameInput;
    /** Input widget for chapter date */
    private InputBoxWidget dateInput;
    /** Input widget for chapter index */
    private InputBoxWidget indexInput;
    /** Input widget for chapter warp location */
    private InputBoxWidget warpInput;
    /** Input widget for primary path color */
    private InputBoxWidget pathColorPrimary;
    /** Input widget for secondary path color */
    private InputBoxWidget pathColorSecondary;
    /** Input widget for tertiary path color */
    private InputBoxWidget pathColorTertiary;
    /** Button widget for applying color changes */
    private Button applyColorChangesButton;
    /** Dropdown widget for selecting paths */
    private DropdownWidget<PathData> pathDropdown;

    protected ChapterEditScreen(Screen parent)
    {
        super(Component.translatable("ardapaths.client.chapter.configuration.screens.chapter_edit_title"));
        this.parent = parent;
        this.creatingNew = false;
    }

    @Override
    public void init()
    {
        int centerX = this.width / 2;
        int y = 0;

        this.addRenderableWidget(TextWidget.create()
                        .setX(centerX - 70)
                        .setY(y)
                        .setWidth(140)
                        .setHeight(20)
                        .setMessage(Component.translatable("ardapaths.client.marker.configuration.screens.edit_chapters"))
                        .build()
        );

        PathData selectedPath = ArdaPathsClient.CONFIG.getSelectedPath();
        pathDropdown = this.addRenderableWidget(DropdownWidget.<PathData>create()
                .setPosition(centerX - 140, y += 40)
                .setSize(280, 20)
                .setTitle(Component.translatable("ardapaths.client.chapter.configuration.screens.select_path"))
                .setOptions(ArdaPathsClient.CONFIG.getPaths())
                .setOptionDisplay(path ->
                {
                    if (path == null) return Component.translatable("ardapaths.generic.validation.chapter.screens.no_path_selected");
                    return Component.literal(path.getName()).withStyle(Style.EMPTY.withColor(path.getPrimaryColor().asHex()));
                })
                .setSelected(selectedPath)
                .build()
        );

        var defaultTextColor = new Color(255, 255, 255);

        this.addRenderableWidget(TextWidget.create()
                .setX(centerX - 139)
                .setY(y + 25)
                .setWidth(140)
                .setHeight(17)
                .setMessage(Component.translatable("ardapaths.client.marker.configuration.screens.path_colors"))
                .build()
        );

        pathColorPrimary = buildColorInputBox(centerX - 140, y += 42, pathDropdown.getSelected() != null ? pathDropdown.getSelected().getPrimaryColor() : defaultTextColor, "ardapaths.client.marker.configuration.screens.path_primary_color");
        pathColorSecondary = buildColorInputBox(centerX - 70, y, pathDropdown.getSelected() != null ? pathDropdown.getSelected().getSecondaryColor() : defaultTextColor, "ardapaths.client.marker.configuration.screens.path_secondary_color");
        pathColorTertiary = buildColorInputBox(centerX, y, pathDropdown.getSelected() != null ? pathDropdown.getSelected().getTertiaryColor() : defaultTextColor, "ardapaths.client.marker.configuration.screens.path_tertiary_color");

        this.addRenderableWidget(pathColorPrimary);
        this.addRenderableWidget(pathColorSecondary);
        this.addRenderableWidget(pathColorTertiary);

        applyColorChangesButton = Button.builder(
                        Component.translatable("ardapaths.generic.apply"),
                                button -> saveColorsToPath())
                        .pos(centerX + 90, y)
                        .size(50, 20)
                        .tooltip(Tooltip.create(Component.translatable("ardapaths.client.marker.configuration.screens.path_colors_apppy_tooltip")))
                        .build();
        applyColorChangesButton.active = hasPathColorChanges();
        addRenderableWidget(applyColorChangesButton);

        List<ChapterData> chapters = selectedPath != null ? new ArrayList<>(selectedPath.getChapters()) : new ArrayList<>();
        chapters.sort(Comparator.comparingInt(ChapterData::getIndex));

        chapterDropdown = this.addRenderableWidget(DropdownWidget.<ChapterData>create()
                .setPosition(centerX - 140, y += 35)
                .setSize(238, 20)
                .setTitle(Component.literal("Select Chapter to Edit:"))
                .setOptions(chapters)
                .setOptionDisplay(chapter ->
                {
                    if (chapter == null) return Component.translatable("ardapaths.generic.validation.chapter.screens.no_chapter_selected");
                    return Component.literal(chapter.getName());
                })
                .build()
        );
        int addButtonY = y;

        idInput = this.addRenderableWidget(InputBoxWidget.create()
                        .setX(centerX - 75)
                        .setY(y += 40)
                        .setWidth(150)
                        .setHeight(20)
                        .setEnabled(true)
                        .setPlaceholder(Component.literal("Id..."))
                        .setValidator(text ->
                        {
                            if (text.length() < 3)
                            {
                                throw new TextValidationError(Component.translatable("ardapaths.generic.validation.error.string.three_char_long").getString());
                            }
                            else if (text.length() > 32)
                            {
                                throw new TextValidationError(Component.translatable("ardapaths.generic.validation.error.string.too_long_32").getString());
                            }
                            else if (creatingNew)
                            {
                                PathData path = pathDropdown.getSelected();
                                if (path == null)
                                {
                                    throw new TextValidationError(Component.translatable("ardapaths.generic.validation.chapter.screens.no_path_selected").getString());
                                }
                                else if (path.getChapters() != null && !path.getChapters().isEmpty())
                                {
                                    ChapterData chapter = path.getChapters().stream().filter(ch -> ch.getId().equalsIgnoreCase(text)).findFirst().orElse(null);
                                    if (chapter != null)
                                    {
                                        throw new TextValidationError(Component.translatable("ardapaths.generic.validation.chapter.screens.id_in_use").getString());
                                    }
                                }
                            }
                        })
                        .build()
        );

        nameInput = this.addRenderableWidget(InputBoxWidget.create()
                        .setX(centerX - 75)
                        .setY(y += 30)
                        .setWidth(150)
                        .setHeight(20)
                        .setEnabled(true)
                        .setPlaceholder(Component.translatable("ardapaths.client.chapter.configuration.screens.name"))
                        .build()
        );

        dateInput = this.addRenderableWidget(InputBoxWidget.create()
                .setX(centerX - 75)
                .setY(y += 30)
                .setWidth(150)
                .setHeight(20)
                .setEnabled(true)
                .setPlaceholder(Component.translatable("ardapaths.client.chapter.configuration.screens.date"))
                .build()
        );

        indexInput = this.addRenderableWidget(InputBoxWidget.create()
                .setX(centerX - 75)
                .setY(y += 30)
                .setWidth(150)
                .setHeight(20)
                .setEnabled(true)
                .setPlaceholder(Component.translatable("ardapaths.client.chapter.configuration.screens.index"))
                .setValidator(text ->
                {
                    try
                    {
                        Integer.parseInt(text);
                    }
                    catch (NumberFormatException e)
                    {
                        throw new TextValidationError(Component.translatable("ardapaths.generic.validation.error.integer").getString());
                    }
                })
                .build()
        );

        warpInput = this.addRenderableWidget(InputBoxWidget.create()
                .setX(centerX - 75)
                .setY(y += 30)
                .setWidth(150)
                .setHeight(20)
                .setEnabled(true)
                .setPlaceholder(Component.translatable("ardapaths.client.chapter.configuration.screens.warp_location"))
                .build()
        );

        this.addRenderableWidget(Button.builder(
                        Component.literal("＋"),
                        button ->
                        {
                            resetFields();
                            indexInput.reset(String.valueOf(chapterDropdown.getOptions().size() + 1));
                        })
                .pos(centerX + 100, addButtonY)
                .size(20, 20)
                .tooltip(Tooltip.create(Component.translatable("ardapaths.client.chapter.configuration.screens.create_chapter")))
                .build()
        );

        this.addRenderableWidget(Button.builder(
                        Component.literal("-"),
                        button -> deleteChapter())
                .pos(centerX + 120, addButtonY)
                .size(20, 20)
                .tooltip(Tooltip.create(Component.translatable("ardapaths.client.chapter.configuration.screens.delete_chapter")))
                .build()
        );

        this.addRenderableWidget(Button.builder(
                        Component.translatable("ardapaths.generic.clear"),
                        button ->
                        {
                            resetFields();
                            creatingNew = false;
                        })
                .pos(centerX - 152, y += 40)
                .size(150, 20)
                .build()
        );

        this.addRenderableWidget(Button.builder(
                        Component.translatable("ardapaths.generic.save"),
                        button ->
                        {
                            if (!idInput.validateText() || !nameInput.validateText() || !dateInput.validateText() || !indexInput.validateText())
                                return;

                            PathData path = pathDropdown.getSelected();
                            if (path == null) return;

                            ChapterData chapter = new ChapterData(
                                    idInput.getValue(),
                                    nameInput.getValue(),
                                    dateInput.getValue(),
                                    Integer.parseInt(indexInput.getValue()),
                                    warpInput.getValue()
                            );
                            Paths.updateChapter(path.getId(), chapter);

                            saveColorsToPath();

                            chapterDropdown.setOptions(path.getChapters());
                            resetFields();
                            creatingNew = false;
                        })
                .pos(centerX + 2, y)
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
            idInput.setValue(chapter.getId());
            nameInput.setValue(chapter.getName());
            dateInput.setValue(chapter.getDate());
            indexInput.setValue(String.valueOf(chapter.getIndex()));
            warpInput.setValue(chapter.getWarp());
        });

        ScreenLayout.centerVertically(this);
    }

    private void saveColorsToPath() {
        // Update path colors if changed
        if (hasPathColorChanges()) {

            assert pathDropdown.getSelected() != null;

            Color inputPrimaryColor = Color.fromHexString(pathColorPrimary.getValue());
            Color inputSecondaryColor = Color.fromHexString(pathColorSecondary.getValue());
            Color inputTertiaryColor = Color.fromHexString(pathColorTertiary.getValue());

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
        InputBoxWidget colorInputBox = InputBoxWidget.create()
                .setX(x)
                .setY(y)
                .setWidth(60)
                .setHeight(17)
                .setEnabled(true)
                .setValidator(text ->
                {
                    if (!text.matches("^#([a-fA-F0-9]{6})$"))
                        throw new TextValidationError(Component.translatable("ardapaths.client.marker.configuration.screens.path_colors.validation.error").getString());

                    applyColorChangesButton.active = hasPathColorChanges();
                })
                .setPlaceholder(Component.translatable(placeholder))
                .build();

        colorInputBox.setValue(textColor.asHexString());
        colorInputBox.setBackgroundColor(textColor.asHex());

        colorInputBox.setTooltip(Tooltip.create(Component.translatable("ardapaths.client.marker.configuration.screens.path_colors_tooltip")));

        colorInputBox.setValueListener(input ->{
            colorInputBox.validateText();
            Color color = Color.fromHexString(input);
            colorInputBox.setBackgroundColor(color.asHex());
        });

        return colorInputBox;
    }

    private void deleteChapter() {

        assert minecraft != null;

        PathData path = pathDropdown.getSelected();
        if (path == null) return;

        ChapterData chapter = chapterDropdown.getSelected();
        if (chapter == null) return;

        if (chapter.getName().equalsIgnoreCase("default")) {
            log.warn("Attempted to delete default chapter, action blocked.");

            var message = Component.empty().append(Component.translatable("ardapaths.client.chapter.configuration.screens.error.delete_default_chapter").withStyle(ChatFormatting.RED));
            var player = Minecraft.getInstance().player;

            if (player != null) player.sendSystemMessage(message);

            return;
        }

        minecraft.setScreen(new ConfirmationPopup(
                Component.translatable("ardapaths.client.marker.configuration.screens.chapter_delete_popup_text", chapter.getName()),
                // Popup closed / confirm
                () -> {

                    Paths.deleteChapter(path.getId(), chapter);
                    resetFields();
                },
                // Popup closed / decline
                () -> log.info("Canceled chapter deletion."),
                this
        ));
    }

    private boolean hasPathColorChanges(){

        if (pathDropdown.getSelected() != null) {
            Color initialPathPrimaryColor = pathDropdown.getSelected().getPrimaryColor();
            Color initialPathSecondaryColor = pathDropdown.getSelected().getSecondaryColor();
            Color initialPathTertiaryColor = pathDropdown.getSelected().getTertiaryColor();

            Color inputPrimaryColor = Color.fromHexString(pathColorPrimary.getValue());
            Color inputSecondaryColor = Color.fromHexString(pathColorSecondary.getValue());
            Color inputTertiaryColor = Color.fromHexString(pathColorTertiary.getValue());

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
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta)
    {
        this.renderModBackground(context);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button)
    {
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void onClose()
    {
        if (this.minecraft != null)
            this.minecraft.setScreen(this.parent);
    }
}

package space.ajcool.ardapaths.screens;

import lombok.extern.slf4j.Slf4j;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import space.ajcool.ardapaths.ArdaPathsClient;
import space.ajcool.ardapaths.core.Client;
import space.ajcool.ardapaths.core.data.WarpTarget;
import space.ajcool.ardapaths.core.data.config.shared.ChapterData;
import space.ajcool.ardapaths.core.data.config.shared.Color;
import space.ajcool.ardapaths.core.data.config.shared.PathData;
import space.ajcool.ardapaths.core.data.config.shared.PositionData;
import space.ajcool.ardapaths.core.networking.PacketRegistry;
import space.ajcool.ardapaths.core.networking.packets.server.PathDataUpdatePacket;
import space.ajcool.ardapaths.paths.Paths;
import space.ajcool.ardapaths.screens.layout.ScreenLayout;
import space.ajcool.ardapaths.screens.widgets.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Screen for editing and creating chapter data within a path.
 * Allows configuration of chapter metadata (name, date, index), warp locations, and associated path colors.
 */
@Slf4j(topic = "ardapaths")
public class ChapterEditScreen extends ArdaPathsScreen {

    /** Parent screen to return to */
    private final Screen parent;

    /** Whether this screen is creating a new chapter */
    private boolean creatingNew;

    /** Path selected inside this editor, independent from the player's followed path. */
    private String editingPathId;

    /** Chapter selected by the opener and applied once after the editor widgets are built. */
    private String initialChapterId;

    /** Dropdown widget for selecting chapters */
    private DropdownWidget<ChapterData> chapterDropdown;

    /** Input widget for chapter ID */
    private InputBoxWidget idInput;

    /** Input widget for chapter name */
    private InputBoxWidget nameInput;

    /** Input widget for chapter index */
    private InputBoxWidget indexInput;

    /** Input widget for chapter warp location */
    private InputBoxWidget warpInput;

    /** Input widget for chapter-start X coordinate. */
    private InputBoxWidget coordXInput;

    /** Input widget for chapter-start Y coordinate. */
    private InputBoxWidget coordYInput;

    /** Input widget for chapter-start Z coordinate. */
    private InputBoxWidget coordZInput;

    /** Suggestion-backed input for the chapter-start dimension identifier. */
    private SuggestionInputWidget dimensionInput;

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

    /**
     * Creates a chapter editor seeded with the marker editor's current selection.
     *
     * @param parent    parent screen to return to
     * @param pathId    path to select when the editor opens
     * @param chapterId chapter to select when the editor opens
     */
    protected ChapterEditScreen(Screen parent, String pathId, String chapterId) {
        super(Component.translatable("ardapaths.client.chapter.configuration.screens.chapter_edit_title"));
        this.parent = parent;
        this.creatingNew = false;
        this.editingPathId = pathId;
        this.initialChapterId = chapterId;
    }

    @Override
    public void init() {
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

        PathData selectedPath = editingPathId == null ? ArdaPathsClient.CONFIG.getSelectedPath() : ArdaPathsClient.CONFIG.getPath(editingPathId);
        pathDropdown = this.addRenderableWidget(DropdownWidget.<PathData>create()
                .setPosition(centerX - 140, y += 40)
                .setSize(280, 20)
                .setTitle(Component.translatable("ardapaths.client.chapter.configuration.screens.select_path"))
                .setOptions(ArdaPathsClient.CONFIG.getPaths())
                .setOptionDisplay(path ->
                {
                    if (path == null)
                        return Component.translatable("ardapaths.generic.validation.chapter.screens.no_path_selected");
                    return Component.literal(path.getName()).withStyle(Style.EMPTY.withColor(path.getPrimaryColor().asHex()));
                })
                .setSelected(selectedPath)
                .build()
        );

        var defaultTextColor = new Color(255, 255, 255);

        this.addRenderableWidget(TextWidget.create()
                .setX(centerX - 140)
                .setY(y + 25)
                .setWidth(140)
                .setHeight(17)
                .setMessage(Component.translatable("ardapaths.client.marker.configuration.screens.path_colors"))
                .build()
                .alignLeft()
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

        this.addRenderableWidget(CheckboxWidget.create()
                .setX(centerX - 140)
                .setY(y += 30)
                .setWidth(280)
                .setHeight(20)
                .setText(Component.translatable("ardapaths.client.chapter.configuration.screens.hide_default_chapter"))
                .setChecked(pathDropdown.getSelected() != null && pathDropdown.getSelected().isHideDefault())
                .setEnabled(pathDropdown.getSelected() != null)
                .setOnChange(hidden -> {
                    PathData path = pathDropdown.getSelected();
                    if (path == null) return;

                    path.setHideDefault(hidden);
                    sendPathDataUpdate(path);
                })
                .build());

        List<ChapterData> chapters = sortedChapters(selectedPath);

        chapterDropdown = this.addRenderableWidget(DropdownWidget.<ChapterData>create()
                .setPosition(centerX - 140, y += 35)
                .setSize(238, 20)
                .setTitle(Component.literal("Select Chapter to Edit:"))
                .setOptions(chapters)
                .setOptionDisplay(chapter ->
                {
                    if (chapter == null)
                        return Component.translatable("ardapaths.generic.validation.chapter.screens.no_chapter_selected");
                    return Component.literal(chapter.getName());
                })
                .build()
        );
        int addButtonY = y;

        addRowLabel(y += 40, "ardapaths.client.chapter.configuration.screens.label.id");
        idInput = this.addRenderableWidget(InputBoxWidget.create()
                .setX(centerX - 75)
                .setY(y)
                .setWidth(150)
                .setHeight(20)
                .setEnabled(true)
                .setPlaceholder(Component.literal("Id..."))
                .setValidator(text ->
                {
                    if (text.length() < 3) {
                        throw new TextValidationError(Component.translatable("ardapaths.generic.validation.error.string.three_char_long").getString());
                    } else if (text.length() > 32) {
                        throw new TextValidationError(Component.translatable("ardapaths.generic.validation.error.string.too_long_32").getString());
                    } else if (creatingNew) {
                        PathData path = pathDropdown.getSelected();
                        if (path == null) {
                            throw new TextValidationError(Component.translatable("ardapaths.generic.validation.chapter.screens.no_path_selected").getString());
                        } else if (path.getChapters() != null && !path.getChapters().isEmpty()) {
                            ChapterData chapter = path.getChapters().stream().filter(ch -> ch.getId().equalsIgnoreCase(text)).findFirst().orElse(null);
                            if (chapter != null) {
                                throw new TextValidationError(Component.translatable("ardapaths.generic.validation.chapter.screens.id_in_use").getString());
                            }
                        }
                    }
                })
                .build()
        );

        addRowLabel(y += 30, "ardapaths.client.chapter.configuration.screens.label.name");
        nameInput = this.addRenderableWidget(InputBoxWidget.create()
                .setX(centerX - 75)
                .setY(y)
                .setWidth(150)
                .setHeight(20)
                .setEnabled(true)
                .setPlaceholder(Component.translatable("ardapaths.client.chapter.configuration.screens.name"))
                .build()
        );

        addRowLabel(y += 30, "ardapaths.client.chapter.configuration.screens.label.index");
        indexInput = this.addRenderableWidget(InputBoxWidget.create()
                .setX(centerX - 75)
                .setY(y)
                .setWidth(150)
                .setHeight(20)
                .setEnabled(true)
                .setPlaceholder(Component.translatable("ardapaths.client.chapter.configuration.screens.index"))
                .setValidator(text ->
                {
                    try {
                        Integer.parseInt(text);
                    } catch (NumberFormatException e) {
                        throw new TextValidationError(Component.translatable("ardapaths.generic.validation.error.integer").getString());
                    }
                })
                .build()
        );

        addRowLabel(y += 30, "ardapaths.client.chapter.configuration.screens.label.warp");
        warpInput = this.addRenderableWidget(InputBoxWidget.create()
                .setX(centerX - 75)
                .setY(y)
                .setWidth(150)
                .setHeight(20)
                .setEnabled(true)
                .setPlaceholder(Component.translatable("ardapaths.client.chapter.configuration.screens.warp_location"))
                .setValidator(text ->
                {
                    if (WarpTarget.isCoordinates(text)) {
                        throw new TextValidationError(Component.translatable("ardapaths.client.chapter.configuration.screens.warp_is_coordinates").getString());
                    }
                })
                .build()
        );

        addRowLabel(y += 30, "ardapaths.client.chapter.configuration.screens.coordinates");
        coordXInput = this.addRenderableWidget(buildCoordinateInput(centerX - 75, y, "ardapaths.client.chapter.configuration.screens.coordinate_x"));
        coordYInput = this.addRenderableWidget(buildCoordinateInput(centerX - 23, y, "ardapaths.client.chapter.configuration.screens.coordinate_y"));
        coordZInput = this.addRenderableWidget(buildCoordinateInput(centerX + 29, y, "ardapaths.client.chapter.configuration.screens.coordinate_z"));

        addRowLabel(y += 30, "ardapaths.client.chapter.configuration.screens.dimension");
        dimensionInput = this.addRenderableWidget(new SuggestionInputWidget(
                centerX - 75,
                y,
                150,
                20,
                Component.literal("namespace:path"),
                DimensionSuggestions.localOptions(),
                2,
                true
        ));
        DimensionSuggestions.requestServerDimensions(dimensionInput, () -> Minecraft.getInstance().screen == this);

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
                            if (!idInput.validateText() || !nameInput.validateText() || !indexInput.validateText()
                                    || !warpInput.validateText() || !validateCoordinates() || !validateDimensionSelection())
                                return;

                            PathData path = pathDropdown.getSelected();
                            if (path == null) return;

                            ChapterData chapter = new ChapterData(
                                    idInput.getValue(),
                                    nameInput.getValue(),
                                    Integer.parseInt(indexInput.getValue()),
                                    warpInput.getValue()
                            );
                            BlockPos coordinates = coordinateValue();
                            if (coordinates != null) {
                                chapter.setCoordinates(PositionData.fromBlockPos(coordinates));
                                String dimension = dimensionInput.getValue().trim();
                                chapter.setDimension(dimension.isBlank() ? null : dimension);
                            }
                            Paths.updateChapter(path.getId(), chapter);

                            saveColorsToPath();

                            List<ChapterData> refreshedChapters = sortedChapters(path);
                            ChapterData savedChapter = findChapter(refreshedChapters, chapter.getId());
                            chapterDropdown.setOptions(refreshedChapters);
                            chapterDropdown.setSelected(savedChapter);
                            if (savedChapter != null) {
                                loadChapter(savedChapter);
                            }
                        })
                .pos(centerX + 2, y)
                .size(150, 20)
                .build()
        );

        pathDropdown.setOnSelect(path ->
        {
            if (path == null) return;
            editingPathId = path.getId();
            this.rebuildWidgets();
        });

        chapterDropdown.setOnSelect(chapter ->
        {
            if (chapter == null) return;
            loadChapter(chapter);
        });

        selectInitialChapter(chapters);
        ScreenLayout.centerVertically(this);
    }

    /**
     * Adds a left-hand label for a form row.
     *
     * @param y   vertical position for the label
     * @param key translation key to display
     */
    private void addRowLabel(int y, String key) {
        int centerX = this.width / 2;
        this.addRenderableWidget(TextWidget.create()
                .setX(centerX - 140)
                .setY(y)
                .setWidth(60)
                .setHeight(20)
                .setMessage(Component.translatable(key))
                .build()
        );
    }

    /**
     * Applies the opener-selected chapter after widgets have been constructed.
     *
     * @param chapters sorted chapter list currently displayed by the dropdown
     */
    private void selectInitialChapter(List<ChapterData> chapters) {
        if (initialChapterId == null || initialChapterId.isBlank()) return;

        ChapterData initialChapter = chapters.stream()
                .filter(chapter -> chapter.getId().equals(initialChapterId))
                .findFirst()
                .orElse(null);

        if (initialChapter != null) {
            chapterDropdown.setSelected(initialChapter);
            loadChapter(initialChapter);
        }
        initialChapterId = null;
    }

    /**
     * Loads an existing chapter into the form.
     *
     * @param chapter chapter to edit
     */
    private void loadChapter(ChapterData chapter) {
        creatingNew = false;
        idInput.disable();
        idInput.setValue(chapter.getId());
        nameInput.setValue(chapter.getName());
        indexInput.setValue(String.valueOf(chapter.getIndex()));
        warpInput.setValue(chapter.getWarp());
        setCoordinateFields(chapter.getCoordinates(), chapter.getCoordinates() != null ? chapter.getDimension() : "");
    }

    /**
     * Creates a mutable chapter list sorted for the editor dropdown.
     *
     * @param path path whose chapters should be listed, or null for an empty list
     * @return mutable chapter list sorted by index and then ID
     */
    private List<ChapterData> sortedChapters(PathData path) {
        List<ChapterData> chapters = path != null ? new ArrayList<>(path.getChapters()) : new ArrayList<>();
        chapters.sort(Comparator.comparingInt(ChapterData::getIndex).thenComparing(ChapterData::getId));
        return chapters;
    }

    /**
     * Finds a chapter in an option list by identifier so dropdown selection uses the listed instance.
     *
     * @param chapters chapter options to search
     * @param id       chapter identifier to match
     * @return matching chapter, or null when it is absent
     */
    private ChapterData findChapter(List<ChapterData> chapters, String id) {
        return chapters.stream()
                .filter(chapter -> chapter.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    /**
     * Creates one integer coordinate input.
     *
     * @param x           input x coordinate
     * @param y           input y coordinate
     * @param placeholder placeholder translation key
     * @return coordinate input widget
     */
    private InputBoxWidget buildCoordinateInput(int x, int y, String placeholder) {
        return InputBoxWidget.create()
                .setX(x)
                .setY(y)
                .setWidth(46)
                .setHeight(20)
                .setEnabled(true)
                .setPlaceholder(Component.translatable(placeholder))
                .setValidator(text ->
                {
                    if (text == null || text.isBlank()) return;
                    try {
                        Integer.parseInt(text);
                    } catch (NumberFormatException e) {
                        throw new TextValidationError(Component.translatable("ardapaths.generic.validation.error.integer").getString());
                    }
                })
                .build();
    }

    private InputBoxWidget buildColorInputBox(int x, int y, Color textColor, String placeholder) {
        InputBoxWidget colorInputBox = InputBoxWidget.create()
                .setX(x)
                .setY(y)
                .setWidth(60)
                .setHeight(20)
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

        colorInputBox.setValueListener(input -> {
            colorInputBox.validateText();
            Color color = Color.fromHexString(input);
            colorInputBox.setBackgroundColor(color.asHex());
        });

        return colorInputBox;
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

            sendPathDataUpdate(pathDropdown.getSelected());
        }
    }

    /**
     * Sends the current path metadata to the server.
     *
     * @param path path metadata to persist
     */
    private void sendPathDataUpdate(PathData path) {
        PathDataUpdatePacket pathDataUpdatePacket = new PathDataUpdatePacket(path.getId(),
                path.getName(),
                path.getPrimaryColor().asHex(),
                path.getSecondaryColor().asHex(),
                path.getTertiaryColor().asHex(),
                path.isHideDefault());

        PacketRegistry.PATH_DATA_UPDATE_REQUEST.send(pathDataUpdatePacket);
    }

    private boolean hasPathColorChanges() {

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
        indexInput.reset();
        warpInput.reset();
        coordXInput.reset();
        coordYInput.reset();
        coordZInput.reset();
        dimensionInput.reset();
    }

    /**
     * Validates that coordinate fields are either all empty or all filled with integers.
     *
     * @return true when coordinate inputs are valid
     */
    private boolean validateCoordinates() {
        boolean validIntegers = coordXInput.validateText() && coordYInput.validateText() && coordZInput.validateText();
        if (!validIntegers) return false;

        boolean x = !coordXInput.getValue().isBlank();
        boolean y = !coordYInput.getValue().isBlank();
        boolean z = !coordZInput.getValue().isBlank();
        if (x == y && y == z) return true;

        coordXInput.setFocused(false);
        coordYInput.setFocused(false);
        coordZInput.setFocused(false);
        return false;
    }

    /**
     * Validates the dimension row against the coordinate row.
     *
     * @return true when coordinates and dimension are valid together
     */
    private boolean validateDimensionSelection() {
        boolean hasCoordinates = coordinateValueTextFilled();
        boolean validDimension = dimensionInput.validateText();
        if (!validDimension) return false;

        boolean hasDimension = !dimensionInput.getValue().isBlank();
        if (hasCoordinates && !hasDimension) {
            sendErrorMessage(Component.translatable("ardapaths.client.chapter.configuration.screens.dimension_required"));
            return false;
        }

        if (!hasCoordinates && hasDimension) {
            sendErrorMessage(Component.translatable("ardapaths.client.chapter.configuration.screens.coordinates_required_for_dimension"));
            return false;
        }
        return true;
    }

    /**
     * Checks whether every coordinate field contains text.
     *
     * @return true when all coordinates are present
     */
    private boolean coordinateValueTextFilled() {
        return !coordXInput.getValue().isBlank() && !coordYInput.getValue().isBlank() && !coordZInput.getValue().isBlank();
    }

    /**
     * Reads explicit chapter coordinates from the fields.
     *
     * @return block position, or null when the fields are empty
     */
    private BlockPos coordinateValue() {
        if (coordXInput.getValue().isBlank() && coordYInput.getValue().isBlank() && coordZInput.getValue().isBlank()) {
            return null;
        }

        return new BlockPos(
                Integer.parseInt(coordXInput.getValue()),
                Integer.parseInt(coordYInput.getValue()),
                Integer.parseInt(coordZInput.getValue())
        );
    }

    /**
     * Fills the coordinate inputs from chapter data.
     *
     * @param coordinates coordinates to display, or null to clear the row
     * @param dimension   dimension to display when coordinates are present
     */
    private void setCoordinateFields(PositionData coordinates, String dimension) {
        if (coordinates == null) {
            coordXInput.reset();
            coordYInput.reset();
            coordZInput.reset();
            dimensionInput.reset();
            return;
        }

        BlockPos pos = coordinates.toBlockPos();
        coordXInput.setValue(String.valueOf(pos.getX()));
        coordYInput.setValue(String.valueOf(pos.getY()));
        coordZInput.setValue(String.valueOf(pos.getZ()));
        dimensionInput.reset(dimension == null ? "" : dimension);
    }

    /**
     * Sends a red validation message to the local player.
     *
     * @param message message to send
     */
    private void sendErrorMessage(Component message) {
        var player = Minecraft.getInstance().player;
        if (player != null) player.sendSystemMessage(Component.empty().append(message.copy().withStyle(ChatFormatting.RED)));
    }

    @SuppressWarnings("resource")
    private void deleteChapter() {

        PathData path = pathDropdown.getSelected();
        if (path == null) return;

        ChapterData chapter = chapterDropdown.getSelected();
        if (chapter == null) return;

        if (chapter.getName().equalsIgnoreCase("default")) {
            log.warn("Attempted to delete default chapter, action blocked.");

            sendErrorMessage(Component.translatable("ardapaths.client.chapter.configuration.screens.error.delete_default_chapter"));

            return;
        }

        Client.mc().setScreen(new ConfirmationPopup(
                Component.translatable("ardapaths.client.marker.configuration.screens.chapter_delete_popup_text", chapter.getName()),
                // Popup closed / confirm
                () -> {

                    Paths.deleteChapter(path.getId(), chapter);
                    chapterDropdown.setOptions(sortedChapters(path));
                    resetFields();
                },
                // Popup closed / decline
                () -> log.info("Canceled chapter deletion."),
                this
        ));
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @SuppressWarnings("resource")
    @Override
    public void onClose() {
        Client.mc().setScreen(this.parent);
    }
}

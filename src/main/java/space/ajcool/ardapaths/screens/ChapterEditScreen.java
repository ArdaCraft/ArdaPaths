package space.ajcool.ardapaths.screens;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import space.ajcool.ardapaths.ArdaPathsClient;
import space.ajcool.ardapaths.core.data.config.shared.ChapterData;
import space.ajcool.ardapaths.core.data.config.shared.PathData;
import space.ajcool.ardapaths.paths.Paths;
import space.ajcool.ardapaths.screens.builders.DropdownBuilder;
import space.ajcool.ardapaths.screens.builders.InputBoxBuilder;
import space.ajcool.ardapaths.screens.builders.TextBuilder;
import space.ajcool.ardapaths.screens.widgets.DropdownWidget;
import space.ajcool.ardapaths.screens.widgets.InputBoxWidget;
import space.ajcool.ardapaths.screens.widgets.TextValidationError;

public class ChapterEditScreen extends Screen {
    private final Screen parent;
    private boolean creatingNew;

    protected ChapterEditScreen(Screen parent) {
        super(Text.literal("Chapter Edit Screen"));
        this.parent = parent;
        this.creatingNew = false;
    }

    @Override
    public void init() {
        int centerX = this.width / 2;
        int y = 20;

        this.addDrawableChild(TextBuilder.create()
                .setPosition(centerX - 70, y)
                .setSize(140, 20)
                .setText(Text.literal("Edit Chapters"))
                .build()
        );

        DropdownWidget<PathData> pathDropdown = this.addDrawableChild(DropdownBuilder.<PathData>create()
                .setPosition(centerX - 140, y += 40)
                .setSize(280, 20)
                .setTitle(Text.literal("Select Path:"))
                .setOptions(ArdaPathsClient.CONFIG.getPaths())
                .setOptionDisplay(path -> {
                    if (path == null) return Text.literal("No Path Selected");
                    return Text.literal(path.getName()).fillStyle(Style.EMPTY.withColor(path.getPrimaryColor().asHex()));
                })
                .setSelected(ArdaPathsClient.CONFIG.getSelectedPath())
                .build()
        );

        DropdownWidget<ChapterData> chapterDropdown = this.addDrawableChild(DropdownBuilder.<ChapterData>create()
                .setPosition(centerX - 140, y += 35)
                .setSize(258, 20)
                .setTitle(Text.literal("Select Chapter to Edit:"))
                .setOptions(ArdaPathsClient.CONFIG.getSelectedPath().getChapters())
                .setOptionDisplay(chapter -> {
                    if (chapter == null) return Text.literal("No Chapter Selected");
                    return Text.literal(chapter.getName());
                })
                .build()
        );
        int addButtonY = y;

        InputBoxWidget idInput = this.addDrawableChild(InputBoxBuilder.create()
                .setPosition(centerX - 75, y += 40)
                .setSize(150, 20)
                .setPlaceholder(Text.literal("Id..."))
                .setValidator(text -> {
                    if (text.length() < 3) {
                        throw new TextValidationError("Must be at least 3 characters long.");
                    } else if (text.length() > 32) {
                        throw new TextValidationError("Cannot be more than 32 characters long.");
                    } else if (creatingNew) {
                        PathData path = pathDropdown.getSelected();
                        if (path == null) {
                            throw new TextValidationError("No path selected.");
                        } else if (path.getChapters() != null && !path.getChapters().isEmpty()) {
                            ChapterData chapter = path.getChapters().stream().filter(ch -> ch.getId().equalsIgnoreCase(text)).findFirst().orElse(null);
                            if (chapter != null) {
                                throw new TextValidationError("This ID is already in use.");
                            }
                        }
                    }
                })
                .build()
        );

        InputBoxWidget nameInput = this.addDrawableChild(InputBoxBuilder.create()
                .setPosition(centerX - 75, y += 30)
                .setSize(150, 20)
                .setPlaceholder(Text.literal("Name..."))
                .build()
        );

        InputBoxWidget dateInput = this.addDrawableChild(InputBoxBuilder.create()
                .setPosition(centerX - 75, y += 30)
                .setSize(150, 20)
                .setPlaceholder(Text.literal("Date..."))
                .build()
        );

        InputBoxWidget indexInput = this.addDrawableChild(InputBoxBuilder.create()
                .setPosition(centerX - 75, y += 30)
                .setSize(150, 20)
                .setPlaceholder(Text.literal("Index..."))
                .setValidator(text -> {
                    try {
                        Integer.parseInt(text);
                    } catch (NumberFormatException e) {
                        throw new TextValidationError("Must be an integer.");
                    }
                })
                .build()
        );

        this.addDrawableChild(ButtonWidget.builder(
                        Text.literal("＋"),
                        button -> {
                            creatingNew = true;
                            chapterDropdown.setSelected(null);
                            idInput.enable();
                            idInput.reset();
                            nameInput.reset();
                            dateInput.reset();
                            indexInput.reset(String.valueOf(chapterDropdown.getOptions().size() + 1));
                        })
                .position(centerX + 120, addButtonY)
                .size(20, 20)
                .tooltip(Tooltip.of(Text.literal("Create a new chapter")))
                .build()
        );

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Clear"),
                button -> {
                    creatingNew = false;
                    chapterDropdown.setSelected(null);
                    idInput.enable();
                    idInput.reset();
                    nameInput.reset();
                    dateInput.reset();
                    indexInput.reset();
                })
                .position(centerX - 152, y += 40)
                .size(150, 20)
                .build()
        );

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Save"),
                button -> {
                        if (!idInput.validateText() || !nameInput.validateText() || !dateInput.validateText() || !indexInput.validateText()) return;

                        PathData path = pathDropdown.getSelected();
                        if (path == null) return;

                        ChapterData chapter = new ChapterData(
                                idInput.getText(),
                                nameInput.getText(),
                                dateInput.getText(),
                                Integer.parseInt(indexInput.getText())
                        );
                        Paths.updateChapter(path.getId(), chapter);

                        creatingNew = false;
                        chapterDropdown.setOptions(path.getChapters());
                        chapterDropdown.setSelected(null);
                        idInput.enable();
                        idInput.reset();
                        nameInput.reset();
                        dateInput.reset();
                        indexInput.reset();
                })
                .position(centerX + 2, y)
                .size(150, 20)
                .build()
        );

        pathDropdown.setOnSelect(path -> {
            if (path == null) return;
            chapterDropdown.setOptions(path.getChapters());
            chapterDropdown.setSelected(null);
            idInput.enable();
            idInput.reset();
            nameInput.reset();
            dateInput.reset();
            indexInput.reset();
        });

        chapterDropdown.setOnSelect(chapter -> {
            if (chapter == null) return;
            idInput.disable();
            idInput.setText(chapter.getId());
            nameInput.setText(chapter.getName());
            dateInput.setText(chapter.getDate());
            indexInput.setText(String.valueOf(chapter.getIndex()));
        });
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void close() {
        this.client.setScreen(this.parent);
    }
}

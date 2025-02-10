package space.ajcool.ardapaths.screens;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import space.ajcool.ardapaths.ArdaPathsClient;
import space.ajcool.ardapaths.config.shared.ChapterData;
import space.ajcool.ardapaths.config.shared.PathData;
import space.ajcool.ardapaths.mc.networking.PacketRegistry;
import space.ajcool.ardapaths.screens.widgets.InputBoxWidget;
import space.ajcool.ardapaths.screens.widgets.dropdowns.PathDropdownWidget;
import space.ajcool.ardapaths.screens.widgets.TextValidationError;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public class ChapterEditScreen extends Screen {
    private final Screen parent;
    private ChapterData initialData;
    private PathDropdownWidget pathDropdown;
    private InputBoxWidget idInput;
    private InputBoxWidget nameInput;
    private InputBoxWidget dateInput;

    public ChapterEditScreen(Screen parent) {
        super(Text.literal("Edit a Chapter"));
        this.parent = parent;
    }

    public ChapterEditScreen(Screen parent, ChapterData data) {
        this(parent);
        this.initialData = data;
    }

    @Override
    public void init() {
        super.init();

        int y = 40;

        this.pathDropdown = this.addDrawableChild(new PathDropdownWidget(
                this.width / 2 - 75,
                y,
                150,
                20,
                ArdaPathsClient.CONFIG.getSelectedPathId()
        ));

        y += 24;

        this.idInput = this.addDrawableChild(new InputBoxWidget(
                this.client.textRenderer,
                this.width / 2 - 75,
                y += 24,
                150,
                20,
                Text.literal("three_is_company"),
                text -> {
                    if (text.length() < 3) {
                        throw new TextValidationError("Must be at least 3 characters long.");
                    } else if (text.length() > 32) {
                        throw new TextValidationError("Cannot be more than 32 characters long.");
                    } else {
                        PathData path = this.pathDropdown.getSelected();
                        if (path == null) {
                            throw new TextValidationError("No path selected.");
                        } else if (path.getChapters() != null && !path.getChapters().isEmpty()) {
                            ChapterData chapter = path.getChapters().stream().filter(ch -> ch.getId().equalsIgnoreCase(text)).findFirst().orElse(null);
                            if (chapter != null) {
                                throw new TextValidationError("This ID is already in use.");
                            }
                        }
                    }
                }
        ));

        y += 24;

        this.nameInput = this.addDrawableChild(new InputBoxWidget(
                this.client.textRenderer,
                this.width / 2 - 75,
                y += 24,
                150,
                20,
                Text.literal("Three is Company")
        ));

        y += 24;

        this.dateInput = this.addDrawableChild(new InputBoxWidget(
                this.client.textRenderer,
                this.width / 2 - 75,
                y += 24,
                150,
                20,
                Text.literal("03/01/1892"),
                text -> {
                    if (text.isEmpty()) return;

                    DateFormat format = new SimpleDateFormat("dd/MM/yyyy");
                    try {
                        format.parse(text);
                    } catch (Exception e) {
                        throw new TextValidationError("Date must be in the format DD/MM/YYYY.");
                    }
                }
        ));

        y += 24;

        this.addDrawableChild(new ButtonWidget(
                this.width / 2 - 155,
                y += 24,
                150,
                20,
                Text.literal("Back"),
                button -> this.close(),
                Supplier::get
        ));

        this.addDrawableChild(new ButtonWidget(
                this.width / 2 + 5,
                y,
                150,
                20,
                Text.literal("Save"),
                button -> {
                    if (this.pathDropdown.getSelected() == null || !this.idInput.validateText() || !this.nameInput.validateText() || !this.dateInput.validateText()) return;
                    this.saveChapterData();
                    this.close();
                },
                Supplier::get
        ));

        if (this.initialData != null) {
            this.idInput.setText(this.initialData.getId());
            this.nameInput.setText(this.initialData.getName());
            this.dateInput.setText(this.initialData.getDate());
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);

        int y = 88;
        context.drawTextWithShadow(this.client.textRenderer, Text.literal("Chapter ID:"), this.width / 2 - 75, y - 12, 0xFFFFFF);
        y += 48;
        context.drawTextWithShadow(this.client.textRenderer, Text.literal("Chapter Name:"), this.width / 2 - 75, y - 12, 0xFFFFFF);
        y += 48;
        context.drawTextWithShadow(this.client.textRenderer, Text.literal("Chapter Date:"), this.width / 2 - 75, y - 12, 0xFFFFFF);
    }

    @Override
    public void close() {
        this.client.setScreen(this.parent);
    }

    public void saveChapterData() {
        PathData path = this.pathDropdown.getSelected();
        if (path == null) return;

        String id = this.idInput.getText();
        String name = this.nameInput.getText();
        String date = this.dateInput.getText();

        PacketRegistry.CHAPTER_UPDATE.sendToServer(path.getId(), id, name, date);
        path.setChapter(id, new ChapterData(id, name, date));
        ArdaPathsClient.CONFIG.setPath(path);
        ArdaPathsClient.CONFIG_MANAGER.save();
    }
}

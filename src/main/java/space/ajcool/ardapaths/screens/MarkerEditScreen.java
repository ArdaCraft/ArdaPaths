package space.ajcool.ardapaths.screens;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.EditBoxWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.ArdaPathsClient;
import space.ajcool.ardapaths.core.Client;
import space.ajcool.ardapaths.core.data.BitPacker;
import space.ajcool.ardapaths.core.data.config.shared.ChapterData;
import space.ajcool.ardapaths.core.data.config.shared.Color;
import space.ajcool.ardapaths.core.data.config.shared.PathData;
import space.ajcool.ardapaths.core.networking.PacketRegistry;
import space.ajcool.ardapaths.core.networking.packets.server.ChapterStartRemovePacket;
import space.ajcool.ardapaths.core.networking.packets.server.ChapterStartUpdatePacket;
import space.ajcool.ardapaths.core.networking.packets.server.PathDataUpdatePacket;
import space.ajcool.ardapaths.core.networking.packets.server.PathMarkerUpdatePacket;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;
import space.ajcool.ardapaths.screens.builders.CheckboxBuilder;
import space.ajcool.ardapaths.screens.builders.DropdownBuilder;
import space.ajcool.ardapaths.screens.builders.InputBoxBuilder;
import space.ajcool.ardapaths.screens.builders.TextBuilder;
import space.ajcool.ardapaths.screens.widgets.CheckboxWidget;
import space.ajcool.ardapaths.screens.widgets.DropdownWidget;
import space.ajcool.ardapaths.screens.widgets.InputBoxWidget;
import space.ajcool.ardapaths.screens.widgets.TextValidationError;

import java.util.function.Supplier;

@Environment(value = EnvType.CLIENT)
public class MarkerEditScreen extends Screen
{
    private final PathMarkerBlockEntity MARKER;

    private String selectedPathId;
    private String selectedChapterId;
    private boolean isChapterStart;
    private boolean showChapterStartTitle;
    private String proximityMessage;
    private int activationRange;
    private boolean displayAboveBlocks;
    private DropdownWidget<PathData> pathSelectionDropdown;
    private EditBoxWidget multiLineEditBox;
    private InputBoxWidget charRevealInput;
    private InputBoxWidget fadeDelayOffsetInput;
    private InputBoxWidget fadeDelayFactorInput;
    private InputBoxWidget fadeSpeedInput;
    private InputBoxWidget minOpacityInput;
    private InputBoxWidget pathColorPrimary;
    private InputBoxWidget pathColorSecondary;
    private InputBoxWidget pathColorTertiary;
    private CheckboxWidget displayChapterTitleOnTrail;

    private int charRevealSpeed;
    private int fadeDelayOffset;
    private int fadeDelayFactor;
    private int fadeSpeed;
    private int minOpacity;

    public MarkerEditScreen(PathMarkerBlockEntity marker)
    {
        super(Text.literal("Path Marker Edit Screen"));
        MARKER = marker;

        selectedPathId = ArdaPathsClient.CONFIG.getSelectedPathId();
        selectedChapterId = ArdaPathsClient.CONFIG.getCurrentChapterId();

        ArdaPaths.LOGGER.info("Editing Marker NBT[{}]",marker.toNbt().toString());

        PathMarkerBlockEntity.ChapterNbtData data = marker.getChapterData(selectedPathId, selectedChapterId);

        this.isChapterStart = data.isChapterStart();
        this.showChapterStartTitle = data.isDisplayChapterTitleOnTrail();
        this.proximityMessage = data.getProximityMessage();
        this.activationRange = data.getActivationRange();
        this.displayAboveBlocks = data.displayAboveBlocks();

        ArdaPaths.LOGGER.info("" + data.getPackedMessageData());

        var unpackedMessageData = BitPacker.unpackFive(data.getPackedMessageData());

        charRevealSpeed = unpackedMessageData[0];
        fadeDelayOffset = unpackedMessageData[1];
        fadeDelayFactor = unpackedMessageData[2];
        fadeSpeed = unpackedMessageData[3];
        minOpacity = unpackedMessageData[4];
    }

    @Override
    protected void init()
    {
        super.init();

        PathMarkerBlockEntity.ChapterNbtData data = MARKER.getChapterData(selectedPathId, selectedChapterId);

        this.isChapterStart = data.isChapterStart();
        this.proximityMessage = data.getProximityMessage();
        this.activationRange = data.getActivationRange();
        this.displayAboveBlocks = data.displayAboveBlocks();

        var unpackedMessageData = BitPacker.unpackFive(data.getPackedMessageData());

        charRevealSpeed = unpackedMessageData[0];
        fadeDelayOffset = unpackedMessageData[1];
        fadeDelayFactor = unpackedMessageData[2];
        fadeSpeed = unpackedMessageData[3];
        minOpacity = unpackedMessageData[4];

        int centerX = this.width / 2;
        int currentY = 20;

        this.buildTitle(centerX - 140, currentY);

        pathSelectionDropdown = this.buildPathSelectionDropdown(centerX - 140, currentY += 40);

        var defaultTextColor = new Color(255, 255, 255);

        pathColorPrimary = buildColorInputBox(centerX - 140, currentY += 42, pathSelectionDropdown.getSelected() != null ? pathSelectionDropdown.getSelected().getPrimaryColor() : defaultTextColor, "ardapaths.client.marker.configuration.screens.path_primary_color");
        pathColorSecondary = buildColorInputBox(centerX - 46, currentY, pathSelectionDropdown.getSelected() != null ? pathSelectionDropdown.getSelected().getSecondaryColor() : defaultTextColor, "ardapaths.client.marker.configuration.screens.path_secondary_color");
        pathColorTertiary = buildColorInputBox(centerX + 49, currentY, pathSelectionDropdown.getSelected() != null ? pathSelectionDropdown.getSelected().getTertiaryColor() : defaultTextColor, "ardapaths.client.marker.configuration.screens.path_tertiary_color");

        this.addDrawableChild(pathColorPrimary);
        this.addDrawableChild(pathColorSecondary);
        this.addDrawableChild(pathColorTertiary);

        this.buildChapterSelectionDropdown(centerX - 140, currentY += 40);
        this.buildEditChaptersButton(centerX + 40, currentY);
        this.buildChapterStartCheckbox(centerX - 49, currentY += 30);
        displayChapterTitleOnTrail = this.buildChapterStartHideTitleCheckbox(centerX + 125, currentY);
        this.buildMultilineEditBox(centerX - 140,currentY += 40);

        var sideY = currentY;

        charRevealInput = this.buildIntegerInput(centerX + 100, sideY);
        fadeDelayOffsetInput = this.buildIntegerInput(centerX + 100, sideY+= 20);
        fadeDelayFactorInput = this.buildIntegerInput(centerX + 100, sideY+= 20);
        fadeSpeedInput = this.buildIntegerInput(centerX + 100, sideY+= 20);
        minOpacityInput = this.buildIntegerInput(centerX + 100, sideY+ 20);

        charRevealInput.setTooltip(Tooltip.of(Text.translatable("ardapaths.client.marker.configuration.screens.rspeed_tooltip")));
        fadeDelayOffsetInput.setTooltip(Tooltip.of(Text.translatable("ardapaths.client.marker.configuration.screens.ffactor_tooltip")));
        fadeDelayFactorInput.setTooltip(Tooltip.of(Text.translatable("ardapaths.client.marker.configuration.screens.fdelay_tooltip")));
        fadeSpeedInput.setTooltip(Tooltip.of(Text.translatable("ardapaths.client.marker.configuration.screens.fspeed_tooltip")));
        minOpacityInput.setTooltip(Tooltip.of(Text.translatable("ardapaths.client.marker.configuration.screens.opacity_tooltip")));

        charRevealInput.setText(String.valueOf(charRevealSpeed));
        fadeDelayOffsetInput.setText(String.valueOf(fadeDelayOffset));
        fadeDelayFactorInput.setText(String.valueOf(fadeDelayFactor));
        fadeSpeedInput.setText(String.valueOf(fadeSpeed));
        minOpacityInput.setText(String.valueOf(minOpacity));

        this.multiLineEditBox.setMaxLength(1000);
        this.multiLineEditBox.setChangeListener(string -> proximityMessage = string);
        this.multiLineEditBox.setText(proximityMessage);

        this.buildActivationRangeSlider(centerX - 140, currentY += 115);
        this.buildDisplayAboveBlocksCheckbox(centerX - 1, currentY += 30);
        this.buildDoneButton(centerX + 15, currentY -=2);
        this.buildSaveButton(centerX + 80, currentY);
    }

    private void buildTitle(int x, int y){
        this.addDrawableChild(TextBuilder.create()
                .setPosition(x, y)
                .setSize(280, 20)
                .setText(Text.translatable("ardapaths.client.marker.configuration.screens.edit_path_marker"))
                .build()
        );
    }

    private InputBoxWidget buildColorInputBox(int x, int y, Color textColor, String placeholder)
    {
        InputBoxWidget colorInputBox = InputBoxBuilder.create()
                .setPosition(x, y)
                .setSize(90, 17)
                .setValidator(text ->
                {
                    if (!text.matches("^#([a-fA-F0-9]{6})$"))
                        throw new TextValidationError(Text.translatable("ardapaths.client.marker.configuration.screens.path_colors.validation.error").getString());
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

    private boolean validateForm()
    {
        return pathColorPrimary.validateText() &&
                pathColorSecondary.validateText() &&
                pathColorTertiary.validateText() &&
                charRevealInput.validateText() &&
                fadeDelayOffsetInput.validateText() &&
                fadeDelayFactorInput.validateText() &&
                fadeSpeedInput.validateText() &&
                minOpacityInput.validateText();
    }

    private DropdownWidget<PathData> buildPathSelectionDropdown(int x, int y)
    {
        return this.addDrawableChild(DropdownBuilder.<PathData>create()
                .setPosition(x,y)
                .setSize(280, 20)
                .setTitle(Text.translatable("ardapaths.client.marker.configuration.screens.edit_path_data"))
                .setOptions(ArdaPathsClient.CONFIG.getPaths())
                .setOptionDisplay(item ->
                {
                    if (item == null) return Text.translatable("ardapaths.client.marker.configuration.screens.no_path");
                    return Text.literal(item.getName()).fillStyle(Style.EMPTY.withColor(item.getPrimaryColor().asHex()));
                })
                .setSelected(ArdaPathsClient.CONFIG.getPath(selectedPathId))
                .setOnSelect(path ->
                {
                    selectedPathId = path.getId();
                    selectedChapterId = path.getChapterIds().get(0);
                    this.clearAndInit();
                })
                .build()
        );
    }

    private void buildChapterSelectionDropdown(int x, int y)
    {
        this.addDrawableChild(DropdownBuilder.<ChapterData>create()
                .setPosition(x,y)
                .setSize(175, 20)
                .setTitle(Text.translatable("ardapaths.client.marker.configuration.screens.chapter"))
                .setOptionDisplay(item ->
                {
                    if (item == null) return Text.translatable("ardapaths.client.marker.configuration.screens.no_chapter");
                    return Text.literal(item.getName());
                })
                .setOptions(ArdaPathsClient.CONFIG.getPath(selectedPathId).getChapters())
                .setSelected(ArdaPathsClient.CONFIG.getPath(selectedPathId).getChapter(selectedChapterId))
                .setOnSelect(chapter ->
                {
                    selectedChapterId = chapter.getId();
                    this.clearAndInit();
                })
                .build()
        );
    }

    private void buildEditChaptersButton(int x, int y)
    {
        this.addDrawableChild(new ButtonWidget(
                x,
                y,
                100,
                20,
                Text.translatable("ardapaths.client.marker.configuration.screens.edit_chapters"),
                button -> this.client.setScreen(new ChapterEditScreen(this)),
                Supplier::get
        ));
    }

    private void buildChapterStartCheckbox(int x, int y)
    {
        this.addDrawableChild(CheckboxBuilder.create()
                .setPosition(x,y)
                .setSize(15, 15)
                .setText(Text.translatable("ardapaths.client.marker.configuration.screens.is_chapter_start"))
                .setChecked(isChapterStart)
                .setOnChange(checked -> {
                    isChapterStart = checked;
                    displayChapterTitleOnTrail.setEnabled(isChapterStart);
                })
                .build()
        );
    }

    private CheckboxWidget buildChapterStartHideTitleCheckbox(int x, int y)
    {
        return addDrawableChild(CheckboxBuilder.create()
                .setPosition(x,y)
                .setSize(15, 15)
                .setText(Text.translatable("ardapaths.client.marker.configuration.screens.show_title_on_trail"))
                .setChecked(showChapterStartTitle)
                .setEnabled(isChapterStart)
                .setOnChange(checked -> showChapterStartTitle = checked)
                .build()
        );
    }

    private void buildMultilineEditBox(int x, int y)
    {
        this.multiLineEditBox = this.addDrawableChild(new EditBoxWidget(
                Client.mc().textRenderer,
                x,
                y,
                180,
                100,
                Text.translatable("ardapaths.client.marker.configuration.screens.proximity_message_placeholder"),
                Text.empty()
        ));
    }

    private InputBoxWidget buildIntegerInput(int x, int y)
    {
        return this.addDrawableChild(InputBoxBuilder.create()
                .setPosition(x, y)
                .setSize(40, 17)
                .setPlaceholder(Text.empty())
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
    }

    private void buildActivationRangeSlider(int x, int y)
    {
        this.addDrawableChild(new SliderWidget(
                x,
                y,
                280,
                20,
                ScreenTexts.EMPTY,
                activationRange / 100.0
        )
        {
            {
                this.updateMessage();
            }

            @Override
            protected void updateMessage()
            {
                this.setMessage(Text.translatable("ardapaths.client.marker.configuration.screens.activation_range", activationRange));
            }

            @Override
            protected void applyValue()
            {
                activationRange = MathHelper.floor(MathHelper.clampedLerp(0.0, 100.0, this.value));
            }
        });
    }

    private void buildDisplayAboveBlocksCheckbox(int x, int y)
    {
        this.addDrawableChild(CheckboxBuilder.create()
                .setPosition(x, y)
                .setSize(15, 15)
                .setText(Text.translatable("ardapaths.client.marker.configuration.screens.display_trail_above_blocks"))
                .setChecked(displayAboveBlocks)
                .setOnChange(checked -> displayAboveBlocks = checked)
                .build()
        );
    }

    private void buildDoneButton(int x, int y){
        this.addDrawableChild(new ButtonWidget(
                x,
                y,
                60,
                20,
                Text.translatable("ardapaths.generic.done"),
                button -> {

                    if (validateForm()) {

                        charRevealSpeed = Integer.parseInt(charRevealInput.getText());
                        fadeDelayOffset = Integer.parseInt(fadeDelayOffsetInput.getText());
                        fadeDelayFactor = Integer.parseInt(fadeDelayFactorInput.getText());
                        fadeSpeed = Integer.parseInt(fadeSpeedInput.getText());
                        minOpacity = Integer.parseInt(minOpacityInput.getText());

                        close();
                    } else {

                        ArdaPaths.LOGGER.error(Text.translatable("ardapaths.generic.validation.form.errors").getString());
                    }
                },
                Supplier::get
        ));
    }

    private void buildSaveButton(int x, int y)
    {
        this.addDrawableChild(new ButtonWidget(
                x,
                y,
                60,
                20,
                Text.translatable("ardapaths.generic.save"),
                button ->
                {
                    if (validateForm()) {

                        charRevealSpeed = Integer.parseInt(charRevealInput.getText());
                        fadeDelayOffset = Integer.parseInt(fadeDelayOffsetInput.getText());
                        fadeDelayFactor = Integer.parseInt(fadeDelayFactorInput.getText());
                        fadeSpeed = Integer.parseInt(fadeSpeedInput.getText());
                        minOpacity = Integer.parseInt(minOpacityInput.getText());

                        save();
                        this.clearAndInit();

                    } else {

                        ArdaPaths.LOGGER.error(Text.translatable("ardapaths.generic.validation.form.errors").getString());
                    }
                },
                Supplier::get
        ));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta)
    {
        this.renderBackground(context);

        int centerX = this.width / 2;
        int currentY = 87;

        context.drawTextWithShadow(this.textRenderer, Text.translatable("ardapaths.client.marker.configuration.screens.path_colors"), centerX - 140, currentY, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, Text.translatable("ardapaths.client.marker.configuration.screens.proximity_message"), centerX - 140, currentY + 108, 0xFFFFFF);

        int sideY = 215;

        context.drawTextWithShadow(this.textRenderer, Text.translatable("ardapaths.client.marker.configuration.screens.rspeed"), centerX + 49, sideY, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, Text.translatable("ardapaths.client.marker.configuration.screens.fdelay"), centerX + 52, sideY += 20, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, Text.translatable("ardapaths.client.marker.configuration.screens.ffactor"), centerX + 45, sideY += 20, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, Text.translatable("ardapaths.client.marker.configuration.screens.fspeed"), centerX + 49, sideY += 20, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, Text.translatable("ardapaths.client.marker.configuration.screens.opacity"), centerX + 53, sideY += 20, 0xFFFFFF);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button)
    {
        return super.mouseReleased(mouseX, mouseY, button) || this.multiLineEditBox.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void tick()
    {
        this.multiLineEditBox.tick();
        super.tick();
    }

    @Override
    public void close()
    {
        super.close();

        save();
    }

    private void save()
    {
        if (selectedPathId.isEmpty()) return;

        PathMarkerBlockEntity.ChapterNbtData data = MARKER.getChapterData(selectedPathId, selectedChapterId);

        data.setProximityMessage(proximityMessage);
        data.setActivationRange(activationRange);
        data.setChapterStart(isChapterStart);
        data.setDisplayChapterTitleOnTrail(isChapterStart && showChapterStartTitle);
        data.setDisplayAboveBlocks(displayAboveBlocks);

        var packedData = BitPacker.packFive(charRevealSpeed, fadeDelayOffset, fadeDelayFactor, fadeSpeed, minOpacity);

        data.setPackedMessageData(packedData);

        if (isChapterStart)
        {
            ChapterStartUpdatePacket packet = new ChapterStartUpdatePacket(selectedPathId, selectedChapterId, MARKER.getPos());
            PacketRegistry.CHAPTER_START_UPDATE.send(packet);
        }
        else if (!selectedChapterId.isEmpty())
        {
            ChapterStartRemovePacket packet = new ChapterStartRemovePacket(selectedPathId, selectedChapterId);
            PacketRegistry.CHAPTER_START_REMOVE.send(packet);
        }

        // Update path colors if changed
        if (pathSelectionDropdown.getSelected() != null) {

            Color initialPathPrimaryColor = pathSelectionDropdown.getSelected().getPrimaryColor();
            Color initialPathSecondaryColor = pathSelectionDropdown.getSelected().getSecondaryColor();
            Color initialPathTertiaryColor = pathSelectionDropdown.getSelected().getTertiaryColor();

            Color inputPrimaryColor = Color.fromHexString(pathColorPrimary.getText());
            Color inputSecondaryColor = Color.fromHexString(pathColorSecondary.getText());
            Color inputTertiaryColor = Color.fromHexString(pathColorTertiary.getText());

            if (inputPrimaryColor.asHex() != initialPathPrimaryColor.asHex() ||
                inputSecondaryColor.asHex() != initialPathSecondaryColor.asHex() ||
                inputTertiaryColor.asHex() != initialPathTertiaryColor.asHex()) {

                pathSelectionDropdown.getSelected().setPrimaryColor(inputPrimaryColor);
                pathSelectionDropdown.getSelected().setSecondaryColor(inputSecondaryColor);
                pathSelectionDropdown.getSelected().setTertiaryColor(inputTertiaryColor);

                PathDataUpdatePacket pathDataUpdatePacket = new PathDataUpdatePacket(pathSelectionDropdown.getSelected().getId(),
                        pathSelectionDropdown.getSelected().getName(),
                        inputPrimaryColor.asHex(),
                        inputSecondaryColor.asHex(),
                        inputTertiaryColor.asHex());

                PacketRegistry.PATH_DATA_UPDATE_REQUEST.send(pathDataUpdatePacket);
            }
        }

        PathMarkerUpdatePacket packet = new PathMarkerUpdatePacket(MARKER.getPos(), MARKER.toNbt());
        PacketRegistry.PATH_MARKER_UPDATE.send(packet);
        MARKER.markUpdated();
    }
}

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
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.ArdaPathsClient;
import space.ajcool.ardapaths.core.Client;
import space.ajcool.ardapaths.core.data.BitPacker;
import space.ajcool.ardapaths.core.data.config.shared.ChapterData;
import space.ajcool.ardapaths.core.data.config.shared.PathData;
import space.ajcool.ardapaths.core.networking.PacketRegistry;
import space.ajcool.ardapaths.core.networking.packets.server.ChapterStartRemovePacket;
import space.ajcool.ardapaths.core.networking.packets.server.ChapterStartUpdatePacket;
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

import java.util.*;
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
    private EditBoxWidget multiLineEditBox;
    private InputBoxWidget charRevealInput;
    private InputBoxWidget fadeDelayOffsetInput;
    private InputBoxWidget fadeDelayFactorInput;
    private InputBoxWidget fadeSpeedInput;
    private InputBoxWidget minOpacityInput;
    private CheckboxWidget displayChapterTitleOnTrail;

    private int charRevealSpeed;
    private int fadeDelayOffset;
    private int fadeDelayFactor;
    private int fadeSpeed;
    private int minOpacity;

    private int formHash;

    private final Set<AbstractMap.SimpleEntry<String, String>> originalPathAndChapterData;

    public MarkerEditScreen(PathMarkerBlockEntity marker)
    {
        this(marker, null);
    }

    public MarkerEditScreen(PathMarkerBlockEntity marker, Set<AbstractMap.SimpleEntry<String, String>> originalPathAndChapterData)
    {
        super(Text.literal("Path Marker Edit Screen"));
        MARKER = marker;
        this.originalPathAndChapterData = originalPathAndChapterData != null ? originalPathAndChapterData : trackInitialPathAndChapterData();

        selectedPathId = ArdaPathsClient.CONFIG.getSelectedPathId();
        selectedChapterId = ArdaPathsClient.CONFIG.getCurrentChapterId();

        PathMarkerBlockEntity.ChapterNbtData data = marker.getChapterData(selectedPathId, selectedChapterId, true);
        assert data != null;

        initFormFields(data);
    }

    @Override
    protected void init()
    {
        super.init();

        PathMarkerBlockEntity.ChapterNbtData data = MARKER.getChapterData(selectedPathId, selectedChapterId, true);
        assert data != null;

        initFormFields(data);

        int centerX = this.width / 2;
        int currentY = 20;

        this.buildTitle(centerX - 140, currentY);
        this.buildSubtitle(centerX - 179, currentY+=25);
        this.buildPathSelectionDropdown(centerX - 140, currentY += 40);

        this.buildChapterSelectionDropdown(centerX - 140, currentY += 40);
        this.buildEditChaptersButton(centerX + 40, currentY);
        this.buildChapterStartCheckbox(centerX - 69, currentY += 30);
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
        this.buildDisplayAboveBlocksCheckbox(centerX - 26, currentY += 30);
        this.buildCloseButton(centerX + 15, currentY -=2);
        this.buildSaveButton(centerX + 80, currentY);

        formHash = calculateFormHash();
    }

    private void initFormFields(PathMarkerBlockEntity.ChapterNbtData data) {

        this.isChapterStart = data.isChapterStart();
        this.showChapterStartTitle = data.isDisplayChapterTitleOnTrail();
        this.proximityMessage = data.getProximityMessage();
        this.activationRange = data.getActivationRange();
        this.displayAboveBlocks = data.displayAboveBlocks();

        var unpackedMessageData = BitPacker.unpackFive(data.getPackedMessageData());

        charRevealSpeed = unpackedMessageData[0];
        fadeDelayOffset = unpackedMessageData[1];
        fadeDelayFactor = unpackedMessageData[2];
        fadeSpeed = unpackedMessageData[3];
        minOpacity = unpackedMessageData[4];
    }

    private void buildTitle(int x, int y){
        this.addDrawableChild(TextBuilder.create()
                .setPosition(x, y)
                .setSize(280, 20)
                .setText(Text.translatable("ardapaths.client.marker.configuration.screens.edit_path_marker"))
                .build()
        );
    }

    private void buildSubtitle(int x, int y){

        var linkedChapters = 0;
        var linkedPaths = 0;

        if (MARKER.getPathData() != null){

            for (var pathEntry : MARKER.getPathData().keySet()){

                var linkedChaptersForPath = 0;
                var chapters = MARKER.getPathData().get(pathEntry);

                for (String chapter : chapters.keySet()) {

                    var chapterNbtData = MARKER.getChapterData(pathEntry, chapter, false);
                    if (chapterNbtData != null && !chapterNbtData.isEmpty())
                        linkedChaptersForPath++;
                }

                if (linkedChaptersForPath > 0) {
                    linkedPaths++;
                    linkedChapters += linkedChaptersForPath;
                }
            }
        }

        if (linkedPaths >= 1 || linkedChapters >= 1) {

            this.addDrawableChild(TextBuilder.create()
                    .setPosition(x, y)
                    .setSize(280, 20)
                    .setText(Text.translatable("ardapaths.client.marker.configuration.screens.linked_chapters_and_paths", linkedPaths, linkedChapters))
                    .build()
            );

            if ((linkedPaths == 1 && linkedChapters > 1) || (linkedPaths > 1 && linkedChapters >= 1))
                this.buildMarkerEditLinksButton(x+260, y);
        } else {
            this.addDrawableChild(TextBuilder.create()
                    .setPosition(x + 35, y)
                    .setSize(280, 20)
                    .setText(Text.translatable("ardapaths.client.marker.configuration.screens.no_linked_chapters_and_paths"))
                    .build()
            );
        }
    }

    private boolean validateForm()
    {
        return charRevealInput.validateText() &&
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

                    MutableText label = Text.literal(item.getName()).fillStyle(Style.EMPTY.withColor(item.getPrimaryColor().asHex()));

                    if (isPathAndChapterLinked(item.getId(), selectedChapterId)) label.append(" •");

                    return label;
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
        PathData selectedPath = ArdaPathsClient.CONFIG.getPath(selectedPathId);

        List<ChapterData> chapters = selectedPath != null ? new ArrayList<>(selectedPath.getChapters()) : new ArrayList<>();
        chapters.sort(Comparator.comparingInt(ChapterData::getIndex));

        this.addDrawableChild(DropdownBuilder.<ChapterData>create()
                .setPosition(x,y)
                .setSize(175, 20)
                .setTitle(Text.translatable("ardapaths.client.marker.configuration.screens.chapter"))
                .setOptionDisplay(item ->
                {
                    if (item == null) return Text.translatable("ardapaths.client.marker.configuration.screens.no_chapter");
                    MutableText label = Text.literal(item.getName());

                    if (isPathAndChapterLinked(selectedPathId, item.getId())) label.append(" •");

                    return label;
                })
                .setOptions(chapters)
                .setSelected(ArdaPathsClient.CONFIG.getPath(selectedPathId).getChapter(selectedChapterId))
                .setOnSelect(chapter ->
                {
                    selectedChapterId = chapter.getId();
                    this.clearAndInit();
                })
                .build()
        );
    }

    private void buildMarkerEditLinksButton(int x, int y)
    {
        this.addDrawableChild(new ButtonWidget(
                x,
                y,
                60,
                20,
                Text.translatable("ardapaths.client.marker.configuration.screens.edit_links"),
                button -> this.client.setScreen(new MarkerLinksEditScreen(MARKER, originalPathAndChapterData)),
                Supplier::get
        ));
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

    private void buildCloseButton(int x, int y){

        var doneButton = new ButtonWidget(
                x,
                y,
                60,
                20,
                Text.translatable("ardapaths.generic.close"),
                button -> {this.close();},
                Supplier::get
        );



        this.addDrawableChild(doneButton);
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
        int currentY = 112;

        context.drawTextWithShadow(this.textRenderer, Text.translatable("ardapaths.client.marker.configuration.screens.proximity_message"), centerX - 140, currentY + 65, 0xFFFFFF);

        int sideY = currentY+86;

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

    public void saveAndClose()
    {
        charRevealSpeed = Integer.parseInt(charRevealInput.getText());
        fadeDelayOffset = Integer.parseInt(fadeDelayOffsetInput.getText());
        fadeDelayFactor = Integer.parseInt(fadeDelayFactorInput.getText());
        fadeSpeed = Integer.parseInt(fadeSpeedInput.getText());
        minOpacity = Integer.parseInt(minOpacityInput.getText());

        this.close();
        save();
    }

    @Override
    public void close()
    {
        Text validationWarning = Text.empty();
        var modifiedPathAndChapterData = listModifiedPathAndChapterData();
        Runnable popupOutcome = super::close;

        // Form was modified and chapter or path data was added
        if (wasFormModified() && !modifiedPathAndChapterData.equals(Text.empty())){

            validationWarning = Text.translatable("ardapaths.client.marker.configuration.screens.form.has_changes_added_path_and_chapter")
                    .append(modifiedPathAndChapterData)
                    .append(Text.translatable("ardapaths.generic.save_changes"));
            popupOutcome = () -> {
                discardChapterAndPathDataChanges();
                super.close();
            };

        // Form was modified
        } else if (wasFormModified()){

            validationWarning = Text.translatable("ardapaths.client.marker.configuration.screens.form.has_changes");

        // Path or chapter data  was added
        } else if (!modifiedPathAndChapterData.equals(Text.empty())){

            validationWarning = Text.translatable("ardapaths.client.marker.configuration.screens.form.added_path_and_chapter")
                    .append(modifiedPathAndChapterData)
                    .append(Text.translatable("ardapaths.generic.save_changes"));
            popupOutcome = () -> {
                discardChapterAndPathDataChanges();
                super.close();
            };
        }

        if (!validationWarning.equals(Text.empty()))
        {
            ConfirmationPopup popup = new ConfirmationPopup(
                    validationWarning,
                    this::saveAndClose,
                    popupOutcome,
                    this,
                    true
            );
            this.client.setScreen(popup);
            return;

        }

        super.close();
    }

    private void save()
    {
        if (selectedPathId.isEmpty()) return;

        PathMarkerBlockEntity.ChapterNbtData data = MARKER.getChapterData(selectedPathId, selectedChapterId, true);

        assert data != null;
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

        PathMarkerUpdatePacket packet = new PathMarkerUpdatePacket(MARKER.getPos(), MARKER.toNbt());
        PacketRegistry.PATH_MARKER_UPDATE.send(packet);
        MARKER.markUpdated();
    }

    private int calculateFormHash(){

        if (selectedPathId.isEmpty())
            return 1;

        var setCharRevealSpeed = charRevealInput.getText() != null ? Integer.parseInt(charRevealInput.getText()) : "";
        var setFadeDelayOffset = fadeDelayOffsetInput.getText() != null ? Integer.parseInt(fadeDelayOffsetInput.getText()) : "";
        var setFadeDelayFactor = fadeDelayFactorInput.getText() != null ? Integer.parseInt(fadeDelayFactorInput.getText()) : "";
        var setFadeSpeed = fadeSpeedInput.getText() != null ? Integer.parseInt(fadeSpeedInput.getText()) : "";
        var setMinOpacity = minOpacityInput.getText() != null ? Integer.parseInt(minOpacityInput.getText()) : "";

        return Objects.hash(
                proximityMessage,
                activationRange,
                displayAboveBlocks,
                isChapterStart,
                showChapterStartTitle,
                setCharRevealSpeed,
                setFadeDelayOffset,
                setFadeDelayFactor,
                setFadeSpeed,
                setMinOpacity
        );
    }

    private boolean wasFormModified(){

        return calculateFormHash() != formHash;
    }

    private void discardChapterAndPathDataChanges(){

        var pathData = MARKER.getPathData();

        for (var pathEntryKey : pathData.keySet()) {
            var chapters = pathData.get(pathEntryKey);
            var iterator = chapters.keySet().iterator();

            while (iterator.hasNext()) {
                var chapterEntryKey = iterator.next();
                var comparedEntry = new AbstractMap.SimpleEntry<>(pathEntryKey, chapterEntryKey);

                if (!originalPathAndChapterData.contains(comparedEntry)) {
                    iterator.remove();
                }
            }
        }
        MARKER.markUpdated();
    }

    private Text listModifiedPathAndChapterData(){

        MutableText modifiedEntries = Text.empty();

        if (!originalPathAndChapterData.isEmpty()) {

            var pathData = MARKER.getPathData();

            for (var pathEntryKey : pathData.keySet()) {

                for (var chapterEntryKey : pathData.get(pathEntryKey).keySet()) {

                    var comparedEntry = new AbstractMap.SimpleEntry<>(pathEntryKey, chapterEntryKey);
                    var chapterData = MARKER.getChapterData(pathEntryKey, chapterEntryKey);

                    var isSelectedPathAndChapter = pathEntryKey.equals(selectedPathId) && chapterEntryKey.equals(selectedChapterId) && wasFormModified();

                    if (!isSelectedPathAndChapter && chapterData.isEmpty()) continue;

                    if (!originalPathAndChapterData.contains(comparedEntry)) {

                        var configuredPath = ArdaPathsClient.CONFIG.getPath(pathEntryKey);
                        modifiedEntries.append(Text.literal(configuredPath.getName()).styled(style -> style.withColor(configuredPath.getPrimaryColor().asHex())))
                                .append(Text.literal(" - "))
                                .append(Text.literal(configuredPath.getChapter(chapterEntryKey).getName()).styled(style -> style.withColor(configuredPath.getSecondaryColor().asHex())))
                                .append(Text.literal(" "));
                    }
                }
            }
        }

        return modifiedEntries;
    }

    private boolean isPathAndChapterLinked(String pathId, String chapterId) {

        return originalPathAndChapterData.contains(new AbstractMap.SimpleEntry<>(pathId, chapterId));
    }

    private Set<AbstractMap.SimpleEntry<String, String>> trackInitialPathAndChapterData(){

        Set<AbstractMap.SimpleEntry<String, String>> pathAndChapterData = new HashSet<>();

        var pathData = MARKER.getPathData();

        for (var pathEntryKey : pathData.keySet()) {

            for (var chapterEntryKey : pathData.get(pathEntryKey).keySet()) {

                boolean isDefault = MARKER.getPathData().get(pathEntryKey).get(chapterEntryKey) == null || MARKER.getPathData().get(pathEntryKey).get(chapterEntryKey).isEmpty();
                if (!isDefault) pathAndChapterData.add(new AbstractMap.SimpleEntry<>(pathEntryKey, chapterEntryKey));
            }
        }

        return pathAndChapterData;
    }
}

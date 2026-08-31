package space.ajcool.ardapaths.screens.marker;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import space.ajcool.ardapaths.core.Client;
import space.ajcool.ardapaths.core.data.BitPacker;
import space.ajcool.ardapaths.screens.MarkerEditScreen;
import space.ajcool.ardapaths.screens.widgets.InputBoxWidget;
import space.ajcool.ardapaths.screens.widgets.TextWidget;

/**
 * General marker editor tab containing proximity message and animation settings.
 */
public class GeneralTabSection implements MarkerEditorTab {
    /** Width of compact numeric inputs used by animation settings. */
    private static final int INPUT_WIDTH = 40;

    /** Multi-line text editor for the proximity message. */
    private MultiLineEditBox multiLineEditBox;

    /** Input field for character reveal speed parameter. */
    private InputBoxWidget charRevealInput;

    /** Input field for fade delay offset parameter. */
    private InputBoxWidget fadeDelayOffsetInput;

    /** Input field for fade delay factor parameter. */
    private InputBoxWidget fadeDelayFactorInput;

    /** Input field for fade speed parameter. */
    private InputBoxWidget fadeSpeedInput;

    /** Input field for minimum opacity parameter. */
    private InputBoxWidget minOpacityInput;

    /**
     * Builds the general marker editor tab.
     *
     * @param screen marker edit screen that owns the widgets
     * @param layout current marker edit layout
     * @param state  mutable form state to display and edit
     */
    @Override
    public void build(MarkerEditScreen screen, MarkerEditLayout layout, MarkerFormState state) {
        int contentTop = layout.contentTop();
        int inputX = layout.contentRight() - INPUT_WIDTH;

        buildMultilineEditBox(screen, layout.contentLeft(), contentTop, state);
        buildFormLabels(screen, layout.contentLeft(), inputX, contentTop);

        charRevealInput = buildIntegerInput(screen, inputX, contentTop, 0, BitPacker.MAX_8_BIT_VALUE);
        fadeDelayOffsetInput = buildIntegerInput(screen, inputX, contentTop + 20, 0, BitPacker.MAX_14_BIT_VALUE);
        fadeDelayFactorInput = buildIntegerInput(screen, inputX, contentTop + 40, 0, BitPacker.MAX_14_BIT_VALUE);
        fadeSpeedInput = buildIntegerInput(screen, inputX, contentTop + 60, 1, BitPacker.MAX_14_BIT_VALUE);
        minOpacityInput = buildIntegerInput(screen, inputX, contentTop + 80, 0, 255);

        charRevealInput.setTooltip(Tooltip.create(Component.translatable("ardapaths.client.marker.configuration.screens.rspeed_tooltip")));
        fadeDelayOffsetInput.setTooltip(Tooltip.create(Component.translatable("ardapaths.client.marker.configuration.screens.ffactor_tooltip")));
        fadeDelayFactorInput.setTooltip(Tooltip.create(Component.translatable("ardapaths.client.marker.configuration.screens.fdelay_tooltip")));
        fadeSpeedInput.setTooltip(Tooltip.create(Component.translatable("ardapaths.client.marker.configuration.screens.fspeed_tooltip")));
        minOpacityInput.setTooltip(Tooltip.create(Component.translatable("ardapaths.client.marker.configuration.screens.opacity_tooltip")));

        charRevealInput.setValue(String.valueOf(state.getCharRevealSpeed()));
        fadeDelayOffsetInput.setValue(String.valueOf(state.getFadeDelayOffset()));
        fadeDelayFactorInput.setValue(String.valueOf(state.getFadeDelayFactor()));
        fadeSpeedInput.setValue(String.valueOf(state.getFadeSpeed()));
        minOpacityInput.setValue(String.valueOf(state.getMinOpacity()));

        buildActivationRangeSlider(screen, layout.contentLeft(), contentTop + MarkerEditLayout.CONTENT_HEIGHT - 25, layout.contentWidth(), state);
    }

    /**
     * Copies mounted general-tab widget values into a form state.
     *
     * @param state mutable form state to update
     */
    @Override
    public void commitTo(MarkerFormState state) {
        state.setCharRevealSpeed(MarkerFields.parseIntegerOrFallback(charRevealInput, state.getCharRevealSpeed()));
        state.setFadeDelayOffset(MarkerFields.parseIntegerOrFallback(fadeDelayOffsetInput, state.getFadeDelayOffset()));
        state.setFadeDelayFactor(MarkerFields.parseIntegerOrFallback(fadeDelayFactorInput, state.getFadeDelayFactor()));
        state.setFadeSpeed(MarkerFields.parseIntegerOrFallback(fadeSpeedInput, state.getFadeSpeed()));
        state.setMinOpacity(MarkerFields.parseIntegerOrFallback(minOpacityInput, state.getMinOpacity()));
    }

    /**
     * Validates all mounted general-tab input fields.
     *
     * @return true when all mounted inputs are valid
     */
    @Override
    public boolean validate() {
        boolean valid = true;
        valid &= charRevealInput == null || charRevealInput.validateText();
        valid &= fadeDelayOffsetInput == null || fadeDelayOffsetInput.validateText();
        valid &= fadeDelayFactorInput == null || fadeDelayFactorInput.validateText();
        valid &= fadeSpeedInput == null || fadeSpeedInput.validateText();
        valid &= minOpacityInput == null || minOpacityInput.validateText();
        return valid;
    }

    /**
     * Delegates mouse release to the multi-line edit box.
     *
     * @param mouseX the mouse x coordinate
     * @param mouseY the mouse y coordinate
     * @param button the mouse button code
     * @return true if the multi-line edit box handled the release
     */
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return multiLineEditBox != null && multiLineEditBox.mouseReleased(mouseX, mouseY, button);
    }

    /**
     * Ticks the multi-line edit box while the tab is mounted.
     */
    @Override
    public void tick() {
        if (multiLineEditBox != null) {
            multiLineEditBox.tick();
        }
    }

    /**
     * Creates and adds the multi-line text editor for the proximity message.
     *
     * @param screen marker edit screen that owns the widget
     * @param x      the x coordinate of the edit box
     * @param y      the y coordinate of the edit box
     * @param state  mutable form state to update from edits
     */
    @SuppressWarnings("resource")
    private void buildMultilineEditBox(MarkerEditScreen screen, int x, int y, MarkerFormState state) {
        multiLineEditBox = screen.add(new MultiLineEditBox(
                Client.mc().font,
                x,
                y,
                180,
                100,
                Component.translatable("ardapaths.client.marker.configuration.screens.proximity_message_placeholder"),
                Component.empty()
        ));
        multiLineEditBox.setCharacterLimit(1000);
        multiLineEditBox.setValueListener(state::setProximityMessage);
        multiLineEditBox.setValue(state.getProximityMessage());
    }

    /**
     * Creates and adds static labels for the proximity message and animation fields.
     *
     * @param screen      marker edit screen that owns the widgets
     * @param contentLeft the x coordinate of the tab content's padded left edge
     * @param inputX      the x coordinate of the right-hand input column
     * @param y           the y coordinate of the proximity message editor
     */
    @SuppressWarnings("resource")
    private void buildFormLabels(MarkerEditScreen screen, int contentLeft, int inputX, int y) {
        var font = Client.mc().font;
        int fontHeight = font.lineHeight;
        int halfFontHeight = fontHeight / 2;

        Component proximityMessage = Component.translatable("ardapaths.client.marker.configuration.screens.proximity_message");
        screen.add(new TextWidget(
                contentLeft,
                y - fontHeight - 2,
                font.width(proximityMessage),
                fontHeight,
                proximityMessage
        ));

        screen.add(new TextWidget(inputX - 1 - 50, y += halfFontHeight, 50, fontHeight, Component.translatable("ardapaths.client.marker.configuration.screens.rspeed")));
        screen.add(new TextWidget(inputX - 1 - 47, y += 20 - halfFontHeight, 47, 17, Component.translatable("ardapaths.client.marker.configuration.screens.fdelay")));
        screen.add(new TextWidget(inputX - 1 - 54, y += 20, 54, 17, Component.translatable("ardapaths.client.marker.configuration.screens.ffactor")));
        screen.add(new TextWidget(inputX - 1 - 50, y += 20, 50, 17, Component.translatable("ardapaths.client.marker.configuration.screens.fspeed")));
        screen.add(new TextWidget(inputX - 1 - 46, y + 20, 46, 17, Component.translatable("ardapaths.client.marker.configuration.screens.opacity")));
    }

    /**
     * Builds an integer input constrained to values that can be persisted in packed marker data.
     *
     * @param screen marker edit screen that owns the widget
     * @param x      the input x coordinate
     * @param y      the input y coordinate
     * @param min    the minimum accepted value
     * @param max    the maximum accepted value
     * @return the configured integer input widget
     */
    private InputBoxWidget buildIntegerInput(MarkerEditScreen screen, int x, int y, int min, int max) {
        return screen.add(InputBoxWidget.create()
                .setX(x)
                .setY(y)
                .setWidth(INPUT_WIDTH)
                .setHeight(17)
                .setEnabled(true)
                .setPlaceholder(Component.empty())
                .setValidator(MarkerFields.rangeValidator(min, max))
                .build()
        );
    }

    /**
     * Creates and adds the slider for adjusting the proximity activation range.
     *
     * @param screen marker edit screen that owns the widget
     * @param x      the x coordinate of the slider
     * @param y      the y coordinate of the slider
     * @param width  the slider width
     * @param state  mutable form state to update from slider movement
     */
    private void buildActivationRangeSlider(MarkerEditScreen screen, int x, int y, int width, MarkerFormState state) {
        screen.add(new AbstractSliderButton(
                x,
                y,
                width,
                20,
                CommonComponents.EMPTY,
                state.getActivationRange() / 100.0
        ) {
            {
                this.updateMessage();
            }

            /**
             * Refreshes the activation-range label from the form state.
             */
            @Override
            protected void updateMessage() {
                this.setMessage(Component.translatable("ardapaths.client.marker.configuration.screens.activation_range", state.getActivationRange()));
            }

            /**
             * Writes the slider value to the form state.
             */
            @Override
            protected void applyValue() {
                state.setActivationRange(Mth.floor(Mth.clampedLerp(0.0, 100.0, this.value)));
            }
        });
    }
}

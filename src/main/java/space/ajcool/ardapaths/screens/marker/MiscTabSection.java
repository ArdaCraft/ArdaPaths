package space.ajcool.ardapaths.screens.marker;

import net.minecraft.network.chat.Component;
import space.ajcool.ardapaths.core.Client;
import space.ajcool.ardapaths.screens.MarkerEditScreen;
import space.ajcool.ardapaths.screens.widgets.InputBoxWidget;
import space.ajcool.ardapaths.screens.widgets.TextValidator;
import space.ajcool.ardapaths.screens.widgets.TextWidget;

/**
 * Miscellaneous marker editor tab containing marker action settings.
 */
public class MiscTabSection implements MarkerEditorTab {
    /** Width of miscellaneous action text inputs. */
    private static final int INPUT_WIDTH = 155;

    /** Input box used to configure the marker's optional auto-teleport target. */
    private InputBoxWidget autoTeleportTargetInput;

    /** Input box used to configure the marker's optional focus look-at target. */
    private InputBoxWidget lookAtInput;

    /** Input box used to configure the marker's optional item grant. */
    private InputBoxWidget giveItemInput;

    /** Marker edit screen currently building this tab. */
    private MarkerEditScreen screen;

    /** Current tab input x coordinate. */
    private int inputX;

    /** Current tab label x coordinate. */
    private int labelX;

    /**
     * Builds the miscellaneous marker editor tab.
     *
     * @param screen marker edit screen that owns the widgets
     * @param layout current marker edit layout
     * @param state  mutable form state to display and edit
     */
    @Override
    public void build(MarkerEditScreen screen, MarkerEditLayout layout, MarkerFormState state) {
        this.screen = screen;
        labelX = layout.contentLeft();
        inputX = layout.contentRight() - INPUT_WIDTH;

        int contentTop = layout.contentTop();
        autoTeleportTargetInput = textField("auto_teleport_target", contentTop + 12, MarkerFields::validateAutoTeleportTarget, state.getAutoTeleportTarget());
        giveItemInput = textField("give_item", contentTop + 52, MarkerFields::validateGiveItem, state.getGiveItem());
        lookAtInput = textField("look_at", contentTop + 92, MarkerFields::validateLookAt, state.getLookAt());
    }

    /**
     * Copies mounted miscellaneous-tab widget values into a form state.
     *
     * @param state mutable form state to update
     */
    @Override
    public void commitTo(MarkerFormState state) {
        state.setAutoTeleportTarget(MarkerFields.parseTextOrFallback(autoTeleportTargetInput, state.getAutoTeleportTarget()));
        state.setLookAt(MarkerFields.parseTextOrFallback(lookAtInput, state.getLookAt()));
        state.setGiveItem(MarkerFields.parseTextOrFallback(giveItemInput, state.getGiveItem()));
    }

    /**
     * Validates all mounted miscellaneous-tab input fields.
     *
     * @return true when all mounted inputs are valid
     */
    @Override
    public boolean validate() {
        boolean valid = true;
        valid &= autoTeleportTargetInput == null || autoTeleportTargetInput.validateText();
        valid &= lookAtInput == null || lookAtInput.validateText();
        valid &= giveItemInput == null || giveItemInput.validateText();
        return valid;
    }

    /**
     * Creates a labelled text field for a miscellaneous marker action setting.
     *
     * @param translationKeyPrefix marker setting translation-key suffix
     * @param y                    field y coordinate
     * @param validator            field text validator
     * @param value                initial field value
     * @return configured input box
     */
    @SuppressWarnings("resource")
    private InputBoxWidget textField(String translationKeyPrefix, int y, TextValidator validator, String value) {
        Component label = Component.translatable("ardapaths.client.marker.configuration.screens." + translationKeyPrefix);
        screen.add(new TextWidget(labelX, y, Client.mc().font.width(label), MarkerEditLayout.CONTROL_HEIGHT, label));
        InputBoxWidget input = screen.add(InputBoxWidget.create()
                .setX(inputX)
                .setY(y)
                .setWidth(INPUT_WIDTH)
                .setHeight(17)
                .setEnabled(true)
                .setPlaceholder(Component.translatable("ardapaths.client.marker.configuration.screens." + translationKeyPrefix + "_placeholder"))
                .setValidator(validator)
                .build()
        );
        input.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("ardapaths.client.marker.configuration.screens." + translationKeyPrefix + "_tooltip")));
        input.setValueListener(ignored -> input.validateText());
        input.setValue(value);
        return input;
    }
}

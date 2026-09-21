package space.ajcool.ardapaths.screens.marker;

import net.minecraft.network.chat.Component;
import space.ajcool.ardapaths.core.Client;
import space.ajcool.ardapaths.screens.MarkerEditScreen;
import space.ajcool.ardapaths.screens.widgets.*;

/**
 * Miscellaneous marker editor tab containing marker action settings.
 */
public class MiscTabSection implements MarkerEditorTab {
    /** Width of miscellaneous action text inputs. */
    private static final int INPUT_WIDTH = 155;

    /**
     * Vertical distance between consecutive field rows.
     */
    private static final int ROW_SPACING = 30;

    /** Width of each target-marker coordinate input. */
    private static final int COORDINATE_WIDTH = 46;

    /** Horizontal gap between target-marker row inputs. */
    private static final int TARGET_MARKER_GAP = 4;

    /** Input box used to configure the marker's optional auto-teleport target. */
    private InputBoxWidget autoTeleportTargetInput;

    /** Input box used to configure the marker's optional focus look-at target. */
    private InputBoxWidget lookAtInput;

    /** Input box used to configure the marker's optional item grant. */
    private InputBoxWidget giveItemInput;

    /** Suggestion-backed dimension input for the optional target marker. */
    private SuggestionInputWidget targetMarkerDimensionInput;

    /** X coordinate input for the optional target marker. */
    private InputBoxWidget targetMarkerXInput;

    /** Y coordinate input for the optional target marker. */
    private InputBoxWidget targetMarkerYInput;

    /** Z coordinate input for the optional target marker. */
    private InputBoxWidget targetMarkerZInput;

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
        giveItemInput = textField("give_item", contentTop + 12 + ROW_SPACING, MarkerFields::validateGiveItem, state.getGiveItem());
        lookAtInput = textField("look_at", contentTop + 12 + 2 * ROW_SPACING, MarkerFields::validateLookAt, state.getLookAt());
        targetMarkerFields(layout, contentTop + 12 + 3 * ROW_SPACING, state);
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
        if (validateTargetMarkerInputs()) {
            state.setTargetMarkerDimension(targetMarkerDimensionInput == null ? state.getTargetMarkerDimension() : targetMarkerDimensionInput.getValue().trim());
            state.setTargetMarkerX(targetMarkerXInput == null ? state.getTargetMarkerX() : targetMarkerXInput.getValue().trim());
            state.setTargetMarkerY(targetMarkerYInput == null ? state.getTargetMarkerY() : targetMarkerYInput.getValue().trim());
            state.setTargetMarkerZ(targetMarkerZInput == null ? state.getTargetMarkerZ() : targetMarkerZInput.getValue().trim());
        }
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
        valid &= validateTargetMarkerInputs();
        return valid;
    }

    /**
     * Creates the target-marker continuation fields.
     *
     * @param layout current marker edit layout
     * @param y      label y coordinate
     * @param state  current form state
     */
    @SuppressWarnings("resource")
    private void targetMarkerFields(MarkerEditLayout layout, int y, MarkerFormState state) {
        Component label = Component.translatable("ardapaths.client.marker.configuration.screens.target_marker");
        screen.add(new TextWidget(labelX, y, Client.mc().font.width(label), MarkerEditLayout.CONTROL_HEIGHT, label));

        int rowY = y + 12;
        int dimensionWidth = layout.contentRight() - layout.contentLeft() - (COORDINATE_WIDTH * 3) - (TARGET_MARKER_GAP * 3);
        targetMarkerDimensionInput = screen.add(new SuggestionInputWidget(
                layout.contentLeft(),
                rowY,
                dimensionWidth,
                17,
                Component.translatable("ardapaths.client.marker.configuration.screens.target_marker_dimension_placeholder"),
                DimensionSuggestions.localOptions(),
                2,
                false
        ));
        targetMarkerDimensionInput.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("ardapaths.client.marker.configuration.screens.target_marker_dimension_tooltip")));
        targetMarkerDimensionInput.setValue(state.getTargetMarkerDimension());

        int x = layout.contentLeft() + dimensionWidth + TARGET_MARKER_GAP;
        targetMarkerXInput = targetMarkerCoordinate("target_marker_x", x, rowY, state.getTargetMarkerX());
        targetMarkerYInput = targetMarkerCoordinate("target_marker_y", x + COORDINATE_WIDTH + TARGET_MARKER_GAP, rowY, state.getTargetMarkerY());
        targetMarkerZInput = targetMarkerCoordinate("target_marker_z", x + (COORDINATE_WIDTH + TARGET_MARKER_GAP) * 2, rowY, state.getTargetMarkerZ());

        DimensionSuggestions.requestServerDimensions(targetMarkerDimensionInput, () -> Client.mc().screen == screen);
    }

    /**
     * Creates one target-marker coordinate field.
     *
     * @param translationKeyPrefix marker setting translation-key suffix
     * @param x                    field x coordinate
     * @param y                    field y coordinate
     * @param value                initial field value
     * @return configured coordinate input
     */
    private InputBoxWidget targetMarkerCoordinate(String translationKeyPrefix, int x, int y, String value) {
        InputBoxWidget input = screen.add(InputBoxWidget.create()
                .setX(x)
                .setY(y)
                .setWidth(COORDINATE_WIDTH)
                .setHeight(17)
                .setEnabled(true)
                .setPlaceholder(Component.translatable("ardapaths.client.marker.configuration.screens." + translationKeyPrefix + "_placeholder"))
                .setValidator(text -> MarkerFields.validateTargetMarker(
                        targetMarkerDimensionInput == null ? "" : targetMarkerDimensionInput.getValue(),
                        targetMarkerXInput == null ? "" : targetMarkerXInput.getValue(),
                        targetMarkerYInput == null ? "" : targetMarkerYInput.getValue(),
                        targetMarkerZInput == null ? "" : targetMarkerZInput.getValue()
                ))
                .build());
        input.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("ardapaths.client.marker.configuration.screens." + translationKeyPrefix + "_tooltip")));
        input.setValueListener(ignored -> validateTargetMarkerInputs());
        input.setValue(value);
        return input;
    }

    /**
     * Validates the complete target-marker field group.
     *
     * @return true when the group is blank or complete and valid
     */
    private boolean validateTargetMarkerInputs() {
        boolean valid = targetMarkerDimensionInput == null || targetMarkerDimensionInput.validateText();
        for (InputBoxWidget input : new InputBoxWidget[]{targetMarkerXInput, targetMarkerYInput, targetMarkerZInput}) {
            valid &= input == null || input.validateText();
        }
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

package space.ajcool.ardapaths.screens;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import space.ajcool.ardapaths.core.data.MarkerId;
import space.ajcool.ardapaths.core.data.TimeOfDay;
import space.ajcool.ardapaths.screens.widgets.InputBoxWidget;
import space.ajcool.ardapaths.screens.widgets.TextValidationError;
import space.ajcool.ardapaths.screens.widgets.TextWidget;

import java.util.function.Consumer;

/**
 * Modal screen for choosing the endpoint markers and times used by server-side interpolation.
 */
public class TimeInterpolationPopup extends ArdaPathsScreen {

    /**
     * Width of text input controls.
     */
    private static final int INPUT_WIDTH = 135;

    /**
     * Height of text input controls.
     */
    private static final int INPUT_HEIGHT = 17;

    /**
     * Screen returned to after accepting or cancelling interpolation.
     */
    private final Screen parentScreen;

    /**
     * Initially selected first marker.
     */
    private final BlockPos startMarker;

    /**
     * Initially selected last marker.
     */
    private final BlockPos endMarker;

    /**
     * Callback receiving validated interpolation endpoints.
     */
    private final Consumer<Endpoints> onConfirm;

    /**
     * Input for the start marker ID.
     */
    private InputBoxWidget startMarkerInput;

    /**
     * Input for the end marker ID.
     */
    private InputBoxWidget endMarkerInput;

    /**
     * Input for the start marker time.
     */
    private InputBoxWidget startTimeInput;

    /**
     * Input for the end marker time.
     */
    private InputBoxWidget endTimeInput;

    /**
     * Creates a time interpolation popup.
     *
     * @param parentScreen screen returned to after closing
     * @param startMarker  initially selected first marker
     * @param endMarker    initially selected last marker
     * @param onConfirm    callback for validated endpoint values
     */
    public TimeInterpolationPopup(Screen parentScreen, BlockPos startMarker, BlockPos endMarker, Consumer<Endpoints> onConfirm) {
        super(Component.translatable("ardapaths.client.marker.configuration.screens.time_interpolation.title"));
        this.parentScreen = parentScreen;
        this.startMarker = startMarker;
        this.endMarker = endMarker;
        this.onConfirm = onConfirm;
    }

    /**
     * Builds labels, inputs, and buttons.
     */
    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int left = centerX - 140;

        startMarkerInput = buildMarkerInput(left, centerY - 35, Component.translatable("ardapaths.client.marker.configuration.screens.time_interpolation.start_marker"), MarkerId.format(startMarker));
        endMarkerInput = buildMarkerInput(left + 145, centerY - 35, Component.translatable("ardapaths.client.marker.configuration.screens.time_interpolation.end_marker"), MarkerId.format(endMarker));
        startTimeInput = buildTimeInput(left, centerY + 10, Component.translatable("ardapaths.client.marker.configuration.screens.time_interpolation.start_time"));
        endTimeInput = buildTimeInput(left + 145, centerY + 10, Component.translatable("ardapaths.client.marker.configuration.screens.time_interpolation.end_time"));

        this.addRenderableWidget(Button.builder(Component.translatable("ardapaths.client.marker.configuration.screens.time_interpolation.ok"), ignored -> confirm())
                .bounds(centerX - 60, centerY + 45, 50, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.translatable("ardapaths.client.marker.configuration.screens.time_interpolation.cancel"), ignored -> onClose())
                .bounds(centerX + 10, centerY + 45, 60, 20)
                .build());
    }

    /**
     * Builds a marker ID input and its label.
     *
     * @param x     input x coordinate
     * @param y     input y coordinate
     * @param label label text
     * @param value initial input value
     * @return configured marker ID input
     */
    private InputBoxWidget buildMarkerInput(int x, int y, Component label, String value) {
        this.addRenderableWidget(new TextWidget(x, y - 17, font.width(label), 17, label));
        InputBoxWidget input = this.addRenderableWidget(InputBoxWidget.create()
                .setX(x)
                .setY(y)
                .setWidth(INPUT_WIDTH)
                .setHeight(INPUT_HEIGHT)
                .setEnabled(true)
                .setPlaceholder(Component.empty())
                .setValidator(MarkerId::parse)
                .build());
        input.setValue(value);
        return input;
    }

    /**
     * Builds a required time input and its label.
     *
     * @param x     input x coordinate
     * @param y     input y coordinate
     * @param label label text
     * @return configured time input
     */
    private InputBoxWidget buildTimeInput(int x, int y, Component label) {
        this.addRenderableWidget(new TextWidget(x, y - 17, font.width(label), 17, label));
        return this.addRenderableWidget(InputBoxWidget.create()
                .setX(x)
                .setY(y)
                .setWidth(INPUT_WIDTH)
                .setHeight(INPUT_HEIGHT)
                .setEnabled(true)
                .setPlaceholder(Component.literal("hh:mm"))
                .setValidator(this::validateRequiredTime)
                .build());
    }

    /**
     * Validates fields, sends endpoint data to the callback, and closes the popup.
     */
    private void confirm() {
        boolean valid = startMarkerInput.validateText();
        valid &= endMarkerInput.validateText();
        valid &= startTimeInput.validateText();
        valid &= endTimeInput.validateText();
        if (!valid) return;

        try {
            Endpoints endpoints = new Endpoints(
                    MarkerId.parse(startMarkerInput.getValue()),
                    MarkerId.parse(endMarkerInput.getValue()),
                    TimeOfDay.parse(startTimeInput.getValue()),
                    TimeOfDay.parse(endTimeInput.getValue())
            );
            onClose();
            onConfirm.accept(endpoints);
        } catch (TextValidationError ignored) {
        }
    }

    /**
     * Returns to the marker editor.
     */
    @Override
    public void onClose() {
        minecraft.setScreen(parentScreen);
    }

    /**
     * Validates a required time-of-day value.
     *
     * @param text input text
     * @throws TextValidationError when the value is blank or malformed
     */
    private void validateRequiredTime(String text) throws TextValidationError {
        int parsed = TimeOfDay.parse(text);
        if (parsed == TimeOfDay.UNSET) {
            throw new TextValidationError(Component.translatable("ardapaths.client.marker.configuration.screens.time_interpolation.time_required").getString());
        }
    }

    /**
     * Renders the modal title after the blur pass has completed.
     *
     * @param context draw context
     * @param mouseX  current mouse x coordinate
     * @param mouseY  current mouse y coordinate
     * @param partialTick   partial tick delta
     */
    @Override
    protected void extractModContent(GuiGraphicsExtractor context, int mouseX, int mouseY, float partialTick) {
        context.centeredText(this.font, this.title, this.width / 2, this.height / 2 - 65, 0xFFFFFFFF);
    }

    /**
     * Validated interpolation endpoint values.
     *
     * @param startPacked packed start marker position
     * @param endPacked   packed end marker position
     * @param startTime   start time in daytime ticks
     * @param endTime     end time in daytime ticks
     */
    public record Endpoints(long startPacked, long endPacked, int startTime, int endTime) {

    }
}

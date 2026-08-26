package space.ajcool.ardapaths.screens;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import space.ajcool.ardapaths.core.data.MarkerId;
import space.ajcool.ardapaths.core.data.TimeOfDay;
import space.ajcool.ardapaths.screens.widgets.InputBoxWidget;
import space.ajcool.ardapaths.screens.widgets.TextValidationError;
import space.ajcool.ardapaths.screens.widgets.TextWidget;

import java.util.function.Consumer;

/**
 * Modal screen for choosing the endpoint markers and times used by server-side interpolation.
 */
public class TimeInterpolationPopup extends Screen {
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
        super(Text.translatable("ardapaths.client.marker.configuration.screens.time_interpolation.title"));
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

        startMarkerInput = buildMarkerInput(left, centerY - 35, Text.translatable("ardapaths.client.marker.configuration.screens.time_interpolation.start_marker"), MarkerId.format(startMarker));
        endMarkerInput = buildMarkerInput(left + 145, centerY - 35, Text.translatable("ardapaths.client.marker.configuration.screens.time_interpolation.end_marker"), MarkerId.format(endMarker));
        startTimeInput = buildTimeInput(left, centerY + 10, Text.translatable("ardapaths.client.marker.configuration.screens.time_interpolation.start_time"));
        endTimeInput = buildTimeInput(left + 145, centerY + 10, Text.translatable("ardapaths.client.marker.configuration.screens.time_interpolation.end_time"));

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("ardapaths.client.marker.configuration.screens.time_interpolation.ok"), ignored -> confirm())
                .dimensions(centerX - 60, centerY + 45, 50, 20)
                .build());
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("ardapaths.client.marker.configuration.screens.time_interpolation.cancel"), ignored -> close())
                .dimensions(centerX + 10, centerY + 45, 60, 20)
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
    private InputBoxWidget buildMarkerInput(int x, int y, Text label, String value) {
        this.addDrawableChild(new TextWidget(x, y - 17, textRenderer.getWidth(label), 17, label));
        InputBoxWidget input = this.addDrawableChild(InputBoxWidget.create()
                .setX(x)
                .setY(y)
                .setWidth(INPUT_WIDTH)
                .setHeight(INPUT_HEIGHT)
                .setEnabled(true)
                .setPlaceholder(Text.empty())
                .setValidator(MarkerId::parse)
                .build());
        input.setText(value);
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
    private InputBoxWidget buildTimeInput(int x, int y, Text label) {
        this.addDrawableChild(new TextWidget(x, y - 17, textRenderer.getWidth(label), 17, label));
        return this.addDrawableChild(InputBoxWidget.create()
                .setX(x)
                .setY(y)
                .setWidth(INPUT_WIDTH)
                .setHeight(INPUT_HEIGHT)
                .setEnabled(true)
                .setPlaceholder(Text.literal("hh:mm"))
                .setValidator(this::validateRequiredTime)
                .build());
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
            throw new TextValidationError(Text.translatable("ardapaths.client.marker.configuration.screens.time_interpolation.time_required").getString());
        }
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
                    MarkerId.parse(startMarkerInput.getText()),
                    MarkerId.parse(endMarkerInput.getText()),
                    TimeOfDay.parse(startTimeInput.getText()),
                    TimeOfDay.parse(endTimeInput.getText())
            );
            close();
            onConfirm.accept(endpoints);
        } catch (TextValidationError ignored) {
        }
    }

    /**
     * Returns to the marker editor.
     */
    @Override
    public void close() {
        if (client == null) return;
        client.setScreen(parentScreen);
    }

    /**
     * Renders the modal background and contents.
     *
     * @param context draw context
     * @param mouseX  current mouse x coordinate
     * @param mouseY  current mouse y coordinate
     * @param delta   partial tick delta
     */
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 2 - 65, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
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

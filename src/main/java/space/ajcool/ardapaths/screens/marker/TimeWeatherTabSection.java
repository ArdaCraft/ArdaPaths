package space.ajcool.ardapaths.screens.marker;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import space.ajcool.ardapaths.core.Client;
import space.ajcool.ardapaths.core.data.TimeOfDay;
import space.ajcool.ardapaths.core.data.WeatherTypes;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;
import space.ajcool.ardapaths.screens.MarkerEditScreen;
import space.ajcool.ardapaths.screens.widgets.DropdownWidget;
import space.ajcool.ardapaths.screens.widgets.InputBoxWidget;
import space.ajcool.ardapaths.screens.widgets.TabBarWidget;
import space.ajcool.ardapaths.screens.widgets.TextWidget;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Time and weather marker editor tab.
 */
public class TimeWeatherTabSection implements MarkerEditorTab {

    /** Horizontal gutter between side-by-side time inputs. */
    private static final int INPUT_GUTTER = 2 * TabBarWidget.CONTENT_PADDING;

    /** Supplies feedback from the most recent time-spread or bulk-clear request. */
    private final Supplier<Component> feedbackSupplier;

    /** Supplies whether the current feedback represents an error state. */
    private final BooleanSupplier feedbackErrorSupplier;

    /** Input box used to configure the marker's optional time-of-day setting. */
    private InputBoxWidget timeOfDayInput;

    /** Input box used to configure the marker's time transition range. */
    private InputBoxWidget timeTransitionRangeInput;

    /**
     * Creates a time and weather tab section.
     *
     * @param feedbackSupplier      supplier for response feedback text
     * @param feedbackErrorSupplier supplier for response feedback severity
     */
    public TimeWeatherTabSection(Supplier<Component> feedbackSupplier, BooleanSupplier feedbackErrorSupplier) {
        this.feedbackSupplier = feedbackSupplier;
        this.feedbackErrorSupplier = feedbackErrorSupplier;
    }

    /**
     * Builds the time and weather tab widgets.
     *
     * @param screen marker edit screen that owns the widgets
     * @param layout current marker edit layout
     * @param state  mutable form state to display and edit
     */
    @Override
    public void build(MarkerEditScreen screen, MarkerEditLayout layout, MarkerFormState state) {
        int x = layout.contentLeft();
        int contentTop = layout.contentTop();
        buildWeatherSelectionDropdown(screen, x, contentTop + 12, layout.contentWidth(), state);
        buildTimeOfDaySelector(screen, x, contentTop + 52, layout.contentWidth(), state);
    }

    /**
     * Builds the optional weather selection dropdown for marker traversal behavior.
     *
     * @param screen marker edit screen that owns the widget
     * @param x      the dropdown x coordinate
     * @param y      the dropdown y coordinate
     * @param width  the dropdown width
     * @param state  mutable form state to update from selection
     */
    private void buildWeatherSelectionDropdown(MarkerEditScreen screen, int x, int y, int width, MarkerFormState state) {
        WeatherTypes selection = WeatherTypes.fromInt(state.getWeather());

        screen.add(DropdownWidget.<WeatherTypes>create()
                .setPosition(x, y)
                .setSize(width, 20)
                .setTitle(Component.translatable("ardapaths.client.marker.configuration.screens.weather"))
                .setOptionDisplay(item -> {
                    if (item == null)
                        return Component.translatable("ardapaths.client.marker.configuration.screens.no_weather");
                    return Component.literal(item.getDisplayName());
                })
                .setOptions(List.of(WeatherTypes.values()))
                .setSelected(selection)
                .setOnSelect(selected -> state.setWeather(selected == WeatherTypes.DEFAULT ? PathMarkerBlockEntity.ChapterNbtData.UNSET : selected.ordinal()))
                .build()
        );
    }

    /**
     * Creates and adds the time-of-day input and transition range selector.
     *
     * @param screen marker edit screen that owns the widgets
     * @param x      the input x coordinate
     * @param y      the input y coordinate
     * @param width  the available tab content width
     * @param state  mutable form state to display
     */
    @SuppressWarnings("resource")
    private void buildTimeOfDaySelector(MarkerEditScreen screen, int x, int y, int width, MarkerFormState state) {
        var font = Client.mc().font;
        int inputWidth = (width - INPUT_GUTTER) / 2;
        int rightColumnX = x + width - inputWidth;
        Component label = Component.translatable("ardapaths.client.marker.configuration.screens.current_time_of_day");
        screen.add(new TextWidget(x, y - 17, font.width(label), 17, label));
        timeOfDayInput = screen.add(InputBoxWidget.create()
                .setX(x)
                .setY(y)
                .setWidth(inputWidth)
                .setHeight(20)
                .setEnabled(true)
                .setPlaceholder(Component.translatable("ardapaths.client.marker.configuration.screens.user_current"))
                .setValidator(MarkerFields::validateCurrentTimeOfDay)
                .build()
        );
        timeOfDayInput.setValueListener(ignored -> timeOfDayInput.validateText());
        timeOfDayInput.setValue(TimeOfDay.format(state.getTimeOfDay()));

        Component rangeLabel = Component.translatable("ardapaths.client.marker.configuration.screens.time_transition_range");
        screen.add(new TextWidget(rightColumnX, y - 19, font.width(rangeLabel), 17, rangeLabel));
        timeTransitionRangeInput = screen.add(InputBoxWidget.create()
                .setX(rightColumnX)
                .setY(y)
                .setWidth(inputWidth)
                .setHeight(20)
                .setEnabled(true)
                .setPlaceholder(Component.translatable("ardapaths.client.marker.configuration.screens.time_transition_range_placeholder"))
                .setValidator(MarkerFields::validateTimeTransitionRange)
                .build()
        );
        timeTransitionRangeInput.setValue(TimeOfDay.formatTransitionRange(state.getTimeTransitionRange()));
        timeTransitionRangeInput.setValueListener(ignored -> timeTransitionRangeInput.validateText());
        timeTransitionRangeInput.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("ardapaths.client.marker.configuration.screens.time_transition_range_tooltip")));

        Component feedback = feedbackSupplier.get();
        if (feedback != null) {
            Component formattedFeedback = feedback.copy().withStyle(feedbackErrorSupplier.getAsBoolean() ? ChatFormatting.RED : ChatFormatting.GRAY);
            screen.add(new TextWidget(x, y + 30, width, 17, formattedFeedback));
        }
    }

    /**
     * Copies mounted time and weather widget values into a form state.
     *
     * @param state mutable form state to update
     */
    @Override
    public void commitTo(MarkerFormState state) {
        state.setTimeOfDay(MarkerFields.parseTimeOfDayOrFallback(timeOfDayInput, state.getTimeOfDay()));
        state.setTimeTransitionRange(MarkerFields.parseTransitionRangeOrFallback(timeTransitionRangeInput, state.getTimeTransitionRange()));
    }

    /**
     * Validates all mounted time and weather input fields.
     *
     * @return true when all mounted inputs are valid
     */
    @Override
    public boolean validate() {
        boolean valid = true;
        valid &= timeOfDayInput == null || timeOfDayInput.validateText();
        valid &= timeTransitionRangeInput == null || timeTransitionRangeInput.validateText();
        return valid;
    }
}

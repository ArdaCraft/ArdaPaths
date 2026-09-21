package space.ajcool.ardapaths.screens.marker;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import space.ajcool.ardapaths.core.Client;
import space.ajcool.ardapaths.core.data.TimeActivation;
import space.ajcool.ardapaths.core.data.TimeOfDay;
import space.ajcool.ardapaths.core.data.WeatherTypes;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;
import space.ajcool.ardapaths.screens.MarkerEditScreen;
import space.ajcool.ardapaths.screens.widgets.DropdownWidget;
import space.ajcool.ardapaths.screens.widgets.InputBoxWidget;
import space.ajcool.ardapaths.screens.widgets.PreviousTimeSuggestion;
import space.ajcool.ardapaths.screens.widgets.TextWidget;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Time and weather marker editor tab.
 */
public class TimeWeatherTabSection implements MarkerEditorTab {

    /** Supplies feedback from the most recent time-spread or bulk-clear request. */
    private final Supplier<Component> feedbackSupplier;

    /** Supplies whether the current feedback represents an error state. */
    private final BooleanSupplier feedbackErrorSupplier;

    /** Supplies the nearest previous configured marker time, or null when none exists. */
    private final Supplier<Long> previousTimeSupplier;

    /** Input box used to configure the marker's optional date-time setting. */
    private InputBoxWidget timeOfDayInput;

    /** Helper that controls the previous-time suggestion button. */
    private PreviousTimeSuggestion previousTimeSuggestion;

    /**
     * Creates a time and weather tab section.
     *
     * @param feedbackSupplier      supplier for response feedback text
     * @param feedbackErrorSupplier supplier for response feedback severity
     * @param previousTimeSupplier  supplier for the previous configured marker time
     */
    public TimeWeatherTabSection(Supplier<Component> feedbackSupplier, BooleanSupplier feedbackErrorSupplier, Supplier<Long> previousTimeSupplier) {
        this.feedbackSupplier = feedbackSupplier;
        this.feedbackErrorSupplier = feedbackErrorSupplier;
        this.previousTimeSupplier = previousTimeSupplier;
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
     * Creates and adds the date-time input and transition range selector.
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
        Component label = Component.translatable("ardapaths.client.marker.configuration.screens.current_time_of_day");
        screen.add(new TextWidget(x, y - 17, font.width(label), 17, label));
        Component placeholder = Component.translatable("ardapaths.client.marker.configuration.screens.date_time_placeholder");
        timeOfDayInput = screen.add(InputBoxWidget.create()
                .setX(x)
                .setY(y)
                .setWidth(width)
                .setHeight(20)
                .setEnabled(true)
                .setPlaceholder(placeholder)
                .setValidator(MarkerFields::validateCurrentTimeOfDay)
                .build()
        );
        timeOfDayInput.setTooltip(Tooltip.create(placeholder));
        timeOfDayInput.setValueListener(ignored -> timeOfDayInput.validateText());
        timeOfDayInput.setValue(TimeOfDay.format(state.getTimeOfDay()));
        previousTimeSuggestion = PreviousTimeSuggestion.create(timeOfDayInput, previousTimeSupplier, x, y + 22, width, 20, screen::add);

        Component rangeLabel = Component.translatable("ardapaths.client.marker.configuration.screens.time_transition_range");
        DropdownWidget<TimeActivation> timeActivationDropdown = screen.add(DropdownWidget.<TimeActivation>create()
                .setPosition(x, y + 46)
                .setSize(width, 20)
                .setTitle(rangeLabel)
                .setOptionDisplay(item -> timeActivationLabel(item, state.getActivationRange()))
                .setOptions(List.of(TimeActivation.COMPUTED, TimeActivation.MARKER_RANGE))
                .setSelected(state.getTimeActivation())
                .setOnSelect(state::setTimeActivation)
                .build()
        );
        timeActivationDropdown.setTooltip(Tooltip.create(Component.translatable("ardapaths.client.marker.configuration.screens.time_transition_range_tooltip")));

        Component feedback = feedbackSupplier.get();
        if (feedback != null) {
            Component formattedFeedback = feedback.copy().withStyle(feedbackErrorSupplier.getAsBoolean() ? ChatFormatting.RED : ChatFormatting.GRAY);
            screen.add(new TextWidget(x, y + 74, width, 17, formattedFeedback));
        }
    }

    /**
     * Updates the previous-time suggestion button visibility and label.
     */
    @Override
    public void tick() {
        if (previousTimeSuggestion == null) {
            return;
        }

        previousTimeSuggestion.tick();
    }

    /**
     * Copies mounted time and weather widget values into a form state.
     *
     * @param state mutable form state to update
     */
    @Override
    public void commitTo(MarkerFormState state) {
        state.setTimeOfDay(MarkerFields.parseTimeOfDayOrFallback(timeOfDayInput, state.getTimeOfDay()));
    }

    /**
     * Validates all mounted time and weather input fields.
     *
     * @return true when all mounted inputs are valid
     */
    @Override
    public boolean validate() {
        return timeOfDayInput == null || timeOfDayInput.validateText();
    }

    /**
     * Builds the display label for a time activation dropdown option.
     *
     * @param activation      option being rendered
     * @param activationRange current marker activation range
     * @return localized option label
     */
    private Component timeActivationLabel(TimeActivation activation, int activationRange) {
        if (activation == TimeActivation.COMPUTED) {
            return Component.translatable("ardapaths.client.marker.configuration.screens.time_activation.computed");
        }

        return Component.translatable("ardapaths.client.marker.configuration.screens.time_activation.marker_range", activationRange);
    }
}

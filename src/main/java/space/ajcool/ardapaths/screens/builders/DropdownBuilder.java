package space.ajcool.ardapaths.screens.builders;

import net.minecraft.text.Text;
import space.ajcool.ardapaths.screens.widgets.DropdownWidget;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class DropdownBuilder<T> {
    private int x;
    private int y;
    private int width;
    private int height;
    private Text title;
    private List<T> options = List.of();
    private Function<T, Text> optionDisplay = item -> Text.empty();
    private T selected = null;
    private Consumer<T> onSelect = null;
    private boolean allowNull = false;
    private boolean expanded = false;
    private int maxVisibleOptions = 8;

    public static <T> DropdownBuilder<T> create() {
        return new DropdownBuilder<>();
    }

    public DropdownBuilder<T> setPosition(int x, int y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public DropdownBuilder<T> setSize(int width, int height) {
        this.width = width;
        this.height = height;
        return this;
    }

    public DropdownBuilder<T> setTitle(Text title) {
        this.title = title;
        return this;
    }

    public DropdownBuilder<T> setOptions(List<T> options) {
        this.options = options;
        return this;
    }

    public DropdownBuilder<T> setOptionDisplay(Function<T, Text> optionDisplay) {
        this.optionDisplay = optionDisplay;
        return this;
    }

    public DropdownBuilder<T> setSelected(T selected) {
        this.selected = selected;
        return this;
    }

    public DropdownBuilder<T> setOnSelect(Consumer<T> onSelect) {
        this.onSelect = onSelect;
        return this;
    }

    public DropdownBuilder<T> setAllowNull(boolean allowNull) {
        this.allowNull = allowNull;
        return this;
    }

    public DropdownBuilder<T> setExpanded(boolean expanded) {
        this.expanded = expanded;
        return this;
    }

    public DropdownBuilder<T> setMaxVisibleOptions(int maxVisibleOptions) {
        this.maxVisibleOptions = maxVisibleOptions;
        return this;
    }

    public DropdownWidget<T> build() {
        return new DropdownWidget<>(
                x,
                y,
                width,
                height,
                title,
                options,
                optionDisplay,
                selected,
                onSelect,
                allowNull,
                expanded,
                maxVisibleOptions
        );
    }
}

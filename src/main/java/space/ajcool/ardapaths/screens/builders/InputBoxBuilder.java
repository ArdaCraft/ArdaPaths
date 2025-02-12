package space.ajcool.ardapaths.screens.builders;

import net.minecraft.text.Text;
import space.ajcool.ardapaths.screens.widgets.InputBoxWidget;
import space.ajcool.ardapaths.screens.widgets.TextValidator;

public class InputBoxBuilder {
    private int x;
    private int y;
    private int width;
    private int height;
    private Text title;
    private Text placeholder;
    private TextValidator validator = text -> {};
    private boolean enabled = true;

    public static InputBoxBuilder create() {
        return new InputBoxBuilder();
    }

    public InputBoxBuilder setPosition(int x, int y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public InputBoxBuilder setSize(int width, int height) {
        this.width = width;
        this.height = height;
        return this;
    }

    public InputBoxBuilder setTitle(Text title) {
        this.title = title;
        return this;
    }

    public InputBoxBuilder setPlaceholder(Text placeholder) {
        this.placeholder = placeholder;
        return this;
    }

    public InputBoxBuilder setValidator(TextValidator validator) {
        this.validator = validator;
        return this;
    }

    public InputBoxBuilder setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public InputBoxWidget build() {
        return new InputBoxWidget(x, y, width, height, title, placeholder, validator, enabled);
    }
}

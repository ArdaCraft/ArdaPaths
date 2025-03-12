package space.ajcool.ardapaths.screens.builders;

import net.minecraft.text.Text;
import space.ajcool.ardapaths.screens.widgets.TextWidget;

public class TextBuilder {
    private int x = 0;
    private int y = 0;
    private int width = 0;
    private int height = 0;
    private Text message = null;

    public static TextBuilder create() {
        return new TextBuilder();
    }

    public TextBuilder setPosition(int x, int y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public TextBuilder setSize(int width, int height) {
        this.width = width;
        this.height = height;
        return this;
    }

    public TextBuilder setText(Text message) {
        this.message = message;
        return this;
    }

    public TextWidget build() {
        return new TextWidget(x, y, width, height, message);
    }
}

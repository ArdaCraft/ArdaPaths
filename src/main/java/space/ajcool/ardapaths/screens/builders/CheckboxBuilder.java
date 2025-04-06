package space.ajcool.ardapaths.screens.builders;

import net.minecraft.text.Text;
import space.ajcool.ardapaths.screens.widgets.CheckboxWidget;

import java.util.function.Consumer;

public class CheckboxBuilder
{
    private int x;
    private int y;
    private int width;
    private int height;
    private Text text;
    private boolean checked = false;
    private Consumer<Boolean> onChange = null;

    public static CheckboxBuilder create()
    {
        return new CheckboxBuilder();
    }

    public CheckboxBuilder setPosition(int x, int y)
    {
        this.x = x;
        this.y = y;
        return this;
    }

    public CheckboxBuilder setSize(int width, int height)
    {
        this.width = width;
        this.height = height;
        return this;
    }

    public CheckboxBuilder setText(Text text)
    {
        this.text = text;
        return this;
    }

    public CheckboxBuilder setChecked(boolean checked)
    {
        this.checked = checked;
        return this;
    }

    public CheckboxBuilder setOnChange(Consumer<Boolean> onChange)
    {
        this.onChange = onChange;
        return this;
    }

    public CheckboxWidget build()
    {
        return new CheckboxWidget(x, y, width, height, text, checked, onChange);
    }
}

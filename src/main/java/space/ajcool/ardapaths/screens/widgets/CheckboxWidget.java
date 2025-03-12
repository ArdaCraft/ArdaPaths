package space.ajcool.ardapaths.screens.widgets;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import space.ajcool.ardapaths.core.Client;

import java.util.function.Consumer;

public class CheckboxWidget extends PressableWidget {
    private static final Identifier TEXTURE = new Identifier("textures/gui/checkbox.png");
    private final Text text;
    private boolean checked;
    private Consumer<Boolean> onChange;

    public CheckboxWidget(int x, int y, int width, int height, Text text, boolean checked, Consumer<Boolean> onChange) {
        super(x, y, width, height, null);
        this.text = text;
        this.checked = checked;
        this.onChange = onChange;
    }

    @Override
    protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        int x = this.getX();
        int y = this.getY();

        if (this.isHovered()) {
            if (checked) {
                context.drawTexture(TEXTURE, x, y, width, height, 20, 20, 20, 20, 64, 64);
            } else {
                context.drawTexture(TEXTURE, x, y, width, height, 20, 0, 20, 20, 64, 64);
            }
        } else {
            if (checked) {
                context.drawTexture(TEXTURE, x, y, width, height, 0, 20, 20, 20, 64, 64);
            } else {
                context.drawTexture(TEXTURE, x, y, width, height, 0, 0, 20, 20, 64, 64);
            }
        }

        TextRenderer textRenderer = Client.mc().textRenderer;
        int textX = x + width + 5;
        int textY = y + (height - textRenderer.fontHeight) / 2;
        context.drawTextWithShadow(textRenderer, text, textX, textY, 0xFFFFFF);
    }

    @Override
    public void onPress() {
        checked = !checked;
        if (onChange != null) {
            onChange.accept(checked);
        }
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        this.appendDefaultNarrations(builder);
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
        if (onChange != null) {
            onChange.accept(checked);
        }
    }

    public void setOnChange(Consumer<Boolean> onChange) {
        this.onChange = onChange;
    }
}

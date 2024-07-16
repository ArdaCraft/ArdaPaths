package space.ajcool.ardapaths.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.EditBoxWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import space.ajcool.ardapaths.block.PathMarkerBlockEntity;
import space.ajcool.ardapaths.ArdaPaths;

import java.nio.charset.Charset;
import java.util.function.Supplier;

import static com.ibm.icu.text.PluralRules.Operand.i;

@Environment(value = EnvType.CLIENT)
public class PathMarkerEditScreen extends Screen
{
    private PathMarkerBlockEntity pathMarker;

    private String proximityMessage = "";
    private int activationRange = 1;

    private EditBoxWidget multiLineEditBox;

    public PathMarkerEditScreen(PathMarkerBlockEntity pathMarkerBlockEntity)
    {
        super(Text.literal("Path Marker Edit Screen"));

        pathMarker = pathMarkerBlockEntity;

        proximityMessage = pathMarker.proximityMessage;
        activationRange = pathMarker.activationRange;
    }

    @Override
    protected void init()
    {
        if (client == null) return;

        //if (client != null) client.keyboard.setSendRepeatsToGui(true);

        this.multiLineEditBox = this.addDrawableChild(new EditBoxWidget(client.textRenderer, this.width / 2 - 140, 40, 280, 100, Text.literal("Proximity Message"), Text.literal("Edit Proximity Message")));
        this.multiLineEditBox.setText(proximityMessage);
        this.multiLineEditBox.setMaxLength(1000);
        this.multiLineEditBox.setChangeListener(string -> proximityMessage = string);

        addDrawableChild(
                new ButtonWidget(width / 2 - 75, height - 40, 150, 20, Text.literal("Done"), button -> MinecraftClient.getInstance().setScreen(null), Supplier::get)
        );

        addDrawableChild(new SliderWidget(this.width / 2 - 140, 155, 280, 20, ScreenTexts.EMPTY, activationRange / 100.0)
        {
            {
                this.updateMessage();
            }

            @Override
            protected void updateMessage() {
                this.setMessage(Text.literal("Activation Range: " + activationRange ));
            }

            @Override
            protected void applyValue() {
                activationRange = MathHelper.floor(MathHelper.clampedLerp(0.0, 100.0, this.value));
            }
        });
    }

    @Override
    public boolean mouseReleased(double d, double e, int i) {
        if (super.mouseReleased(d, e, i)) {
            return true;
        }
        return this.multiLineEditBox.mouseReleased(d, e, i);
    }

    @Override
    public void removed() {
        if (client == null) return;

        pathMarker.proximityMessage = proximityMessage;
        pathMarker.activationRange = activationRange;

        //client.keyboard.setSendRepeatsToGui(false);

        var packetBuffer = PacketByteBufs.create();

        packetBuffer.writeBlockPos(pathMarker.getPos());
        packetBuffer.writeNbt(pathMarker.createNbt());

        ClientPlayNetworking.send(ArdaPaths.PATH_MARKER_UPDATE_PACKET, packetBuffer);

        pathMarker.markUpdated();
    }

    @Override
    public void tick() {
        this.multiLineEditBox.tick();
        super.tick();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta)
    {
        context.fillGradient(0, 0, this.width, this.height, -1072689136, -804253680);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Edit Path Marker"), width / 2, 20, 0xffffff);

        super.render(context, mouseX, mouseY, delta);
    }
}

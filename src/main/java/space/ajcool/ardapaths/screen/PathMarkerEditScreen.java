package space.ajcool.ardapaths.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.util.Mth;
import space.ajcool.ardapaths.block.PathMarkerBlockEntity;
import space.ajcool.ardapaths.ArdaPaths;

import java.nio.charset.Charset;

@Environment(value = EnvType.CLIENT)
public class PathMarkerEditScreen extends Screen
{
    private PathMarkerBlockEntity pathMarker;

    private String proximityMessage = "";
    private int activationRange = 1;

    private MultiLineEditBox multiLineEditBox;

    public PathMarkerEditScreen(PathMarkerBlockEntity pathMarkerBlockEntity)
    {
        super(Component.literal("Path Marker Edit Screen"));

        pathMarker = pathMarkerBlockEntity;

        proximityMessage = pathMarker.proximityMessage;
        activationRange = pathMarker.activationRange;
    }

    @Override
    protected void init()
    {
        if (minecraft != null) minecraft.keyboardHandler.setSendRepeatsToGui(true);

        this.multiLineEditBox = this.addRenderableWidget(new MultiLineEditBox(minecraft.font, this.width / 2 - 140, 40, 280, 100, Component.literal("Proximity Message"), Component.literal("Edit Proximity Message")));
        this.multiLineEditBox.setValue(proximityMessage);
        this.multiLineEditBox.setCharacterLimit(1000);
        this.multiLineEditBox.setValueListener(string -> proximityMessage = string);

        addRenderableWidget(
                new Button(width / 2 - 75, height - 40, 150, 20, Component.literal("Done"), button -> Minecraft.getInstance().setScreen(null))
        );

        addRenderableWidget(new AbstractSliderButton(this.width / 2 - 140, 155, 280, 20, CommonComponents.EMPTY, activationRange / 100.0)
        {
            {
                this.updateMessage();
            }

            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal("Activation Range: " + activationRange ));
            }

            @Override
            protected void applyValue() {
                activationRange = Mth.floor(Mth.clampedLerp(0.0, 100.0, this.value));
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
        if (minecraft == null) return;

        pathMarker.proximityMessage = proximityMessage;
        pathMarker.activationRange = activationRange;

        minecraft.keyboardHandler.setSendRepeatsToGui(false);

        var packetBuffer = PacketByteBufs.create();

        packetBuffer.writeBlockPos(pathMarker.getBlockPos());
        packetBuffer.writeNbt(pathMarker.saveWithoutMetadata());

        ClientPlayNetworking.send(ArdaPaths.PATH_MARKER_UPDATE_PACKET, packetBuffer);

        pathMarker.markUpdated();
    }

    @Override
    public void tick() {
        this.multiLineEditBox.tick();
        super.tick();
    }

    @Override
    public void render(PoseStack poseStack, int i, int j, float f)
    {
        fillGradient(poseStack, 0, 0, this.width, this.height, -1072689136, -804253680);
        drawCenteredString(poseStack, font, Component.literal("Edit Path Marker"), width / 2, 20, 0xffffff);

        super.render(poseStack, i, j, f);
    }
}

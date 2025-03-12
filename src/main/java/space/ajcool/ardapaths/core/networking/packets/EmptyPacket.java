package space.ajcool.ardapaths.core.networking.packets;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import space.ajcool.ardapaths.core.consumers.networking.IPacket;

public record EmptyPacket() implements IPacket {
    private static final PacketByteBuf EMPTY = PacketByteBufs.create();

    @Override
    public PacketByteBuf build() {
        return EMPTY;
    }

    public static EmptyPacket read(PacketByteBuf buf) {
        return new EmptyPacket();
    }
}

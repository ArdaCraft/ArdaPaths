package space.ajcool.ardapaths.core.networking.packets.server;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import space.ajcool.ardapaths.core.consumers.networking.IPacket;

public record PlayerTeleportPacket(double x, double y, double z) implements IPacket {

    @Override
    public PacketByteBuf build() {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        return buf;
    }

    public static PlayerTeleportPacket read(PacketByteBuf buf) {
        final double x = buf.readDouble();
        final double y = buf.readDouble();
        final double z = buf.readDouble();
        return new PlayerTeleportPacket(x, y, z);
    }
}

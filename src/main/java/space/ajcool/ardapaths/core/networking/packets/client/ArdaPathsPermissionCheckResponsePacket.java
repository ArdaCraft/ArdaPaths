package space.ajcool.ardapaths.core.networking.packets.client;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import space.ajcool.ardapaths.core.consumers.networking.IPacket;

public record ArdaPathsPermissionCheckResponsePacket(boolean hasPermission) implements IPacket {

    @Override
    public PacketByteBuf build() {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(hasPermission);
        return buf;
    }

    public static ArdaPathsPermissionCheckResponsePacket read(PacketByteBuf buf)
    {
        final boolean hasPerm = buf.readBoolean();
        return new ArdaPathsPermissionCheckResponsePacket(hasPerm);
    }
}

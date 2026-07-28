package space.ajcool.ardapaths.core.networking.packets.client;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import space.ajcool.ardapaths.core.consumers.networking.IPacket;

/**
 * Packet sent from server to client in response to a permission check request.
 * Contains the result of whether the player has permission to edit paths.
 * @param hasPermission true if the player has the required edit permissions, false otherwise
 */
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

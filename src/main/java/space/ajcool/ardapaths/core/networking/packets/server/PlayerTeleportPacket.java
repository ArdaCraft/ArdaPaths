package space.ajcool.ardapaths.core.networking.packets.server;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import space.ajcool.ardapaths.core.consumers.networking.IPacket;

/**
 * Packet sent from client to server to request teleporting the player to specific coordinates.
 * @param x the X coordinate
 * @param y the Y coordinate
 * @param z the Z coordinate
 * @param worldId the world/dimension identifier, or null to teleport within the current world
 */
public record PlayerTeleportPacket(double x, double y, double z, Identifier worldId) implements IPacket
{

    @Override
    public PacketByteBuf build()
    {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeIdentifier(worldId);
        return buf;
    }

    public static PlayerTeleportPacket read(PacketByteBuf buf)
    {
        final double x = buf.readDouble();
        final double y = buf.readDouble();
        final double z = buf.readDouble();
        final Identifier worldId = buf.readIdentifier();
        return new PlayerTeleportPacket(x, y, z, worldId);
    }
}

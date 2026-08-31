package space.ajcool.ardapaths.core.networking.packets.server;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import space.ajcool.ardapaths.core.ModConstants;
import space.ajcool.ardapaths.core.consumers.networking.IPacket;

/**
 * Packet sent from client to server to request teleporting the player to specific coordinates.
 * @param x the X coordinate
 * @param y the Y coordinate
 * @param z the Z coordinate
 * @param worldId the world/dimension identifier, or null to teleport within the current world
 */
public record PlayerTeleportPacket(double x, double y, double z, ResourceLocation worldId) implements IPacket
{
    /**
     * Network channel used for direct player teleport requests.
     */
    public static final ResourceLocation CHANNEL = ModConstants.modId("player_teleport");

    @Override
    public FriendlyByteBuf build()
    {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeResourceLocation(worldId);
        return buf;
    }

    public static PlayerTeleportPacket read(FriendlyByteBuf buf)
    {
        final double x = buf.readDouble();
        final double y = buf.readDouble();
        final double z = buf.readDouble();
        final ResourceLocation worldId = buf.readResourceLocation();
        return new PlayerTeleportPacket(x, y, z, worldId);
    }
}

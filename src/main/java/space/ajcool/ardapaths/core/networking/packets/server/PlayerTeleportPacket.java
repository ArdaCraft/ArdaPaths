package space.ajcool.ardapaths.core.networking.packets.server;

import net.fabricmc.fabric.api.networking.v1.FriendlyByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import space.ajcool.ardapaths.core.ModConstants;
import space.ajcool.ardapaths.core.consumers.networking.IPacket;

/**
 * Packet sent from client to server to request teleporting the player to specific coordinates.
 *
 * @param x       the X coordinate
 * @param y       the Y coordinate
 * @param z       the Z coordinate
 * @param worldId the world/dimension identifier, or null to teleport within the current world
 */
public record PlayerTeleportPacket(double x, double y, double z, Identifier worldId) implements IPacket {

    /**
     * Network channel used for direct player teleport requests.
     */
    public static final Identifier CHANNEL = ModConstants.modId("player_teleport");

    /**
     * Custom payload type used for typed Fabric networking.
     */
    public static final CustomPacketPayload.Type<PlayerTeleportPacket> TYPE = new CustomPacketPayload.Type<>(CHANNEL);

    public static PlayerTeleportPacket read(FriendlyByteBuf buf) {
        final double x = buf.readDouble();
        final double y = buf.readDouble();
        final double z = buf.readDouble();
        final Identifier worldId = buf.readIdentifier();
        return new PlayerTeleportPacket(x, y, z, worldId);
    }

    /**
     * Gets the custom payload type for this packet.
     *
     * @return this packet's payload type
     */
    @Override
    public CustomPacketPayload.@NotNull Type<PlayerTeleportPacket> type() {
        return TYPE;
    }

    @Override
    public FriendlyByteBuf build() {
        FriendlyByteBuf buf = FriendlyByteBufs.create();
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeIdentifier(worldId);
        return buf;
    }
}

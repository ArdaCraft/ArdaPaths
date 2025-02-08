package space.ajcool.ardapaths.networking.packets;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import space.ajcool.ardapaths.networking.AbstractPacket;

public class PathPlayerTeleportPacket extends AbstractPacket {
    public PathPlayerTeleportPacket() {
        super("player_teleport");
    }

    /**
     * Create a new {@link PathPlayerTeleportPacket} packet.
     *
     * @param x The x coordinate
     * @param y The y coordinate
     * @param z The z coordinate
     */
    public PacketByteBuf create(double x, double y, double z) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        return buf;
    }

    /**
     * Send a {@link PathPlayerTeleportPacket} packet to the server.
     *
     * @param x The x coordinate
     * @param y The y coordinate
     * @param z The z coordinate
     */
    public void sendToServer(double x, double y, double z) {
        super.sendToServer(create(x, y, z));
    }

    @Override
    public void handle(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        double x = buf.readDouble();
        double y = buf.readDouble();
        double z = buf.readDouble();

        server.execute(() -> player.requestTeleport(x, y, z));
    }
}

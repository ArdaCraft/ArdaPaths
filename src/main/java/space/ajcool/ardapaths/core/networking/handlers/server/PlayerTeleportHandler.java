package space.ajcool.ardapaths.core.networking.handlers.server;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import space.ajcool.ardapaths.core.consumers.networking.ServerPacketHandler;
import space.ajcool.ardapaths.core.networking.packets.server.PlayerTeleportPacket;

public class PlayerTeleportHandler extends ServerPacketHandler<PlayerTeleportPacket>
{
    public PlayerTeleportHandler()
    {
        super("player_teleport", PlayerTeleportPacket::read);
    }

    @Override
    protected void handle(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PlayerTeleportPacket packet, PacketSender sender)
    {
        server.execute(() -> player.requestTeleport(packet.x(), packet.y(), packet.z()));
    }
}

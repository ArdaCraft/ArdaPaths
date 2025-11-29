package space.ajcool.ardapaths.core.networking.handlers.server;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
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

        server.execute(() -> {

            if (packet.worldId() != null) {

                RegistryKey<World> key = RegistryKey.of(RegistryKeys.WORLD, packet.worldId());
                ServerWorld serverWorld = server.getWorld(key);

                if (serverWorld != null){

                    player.teleport(serverWorld, packet.x(), packet.y(), packet.z(), player.getYaw(), player.getPitch());
                    return;
                }
            }

            player.teleport(packet.x(), packet.y(), packet.z());

        });
    }
}

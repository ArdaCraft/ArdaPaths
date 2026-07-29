package space.ajcool.ardapaths.core.networking.handlers.server;

import lombok.extern.slf4j.Slf4j;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.core.consumers.networking.ServerPacketHandler;
import space.ajcool.ardapaths.core.networking.packets.server.PlayerTeleportPacket;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;

/**
 * Handles player teleportation requests from the client.
 * Processes incoming {@link PlayerTeleportPacket} and teleports the player to the specified coordinates,
 * optionally in a different world dimension if specified.
 */
@Slf4j(topic = "ardapaths")
public class PlayerTeleportHandler extends ServerPacketHandler<PlayerTeleportPacket>
{
    public PlayerTeleportHandler()
    {
        super("player_teleport", PlayerTeleportPacket::read);
    }

    @Override
    protected void handle(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PlayerTeleportPacket packet, PacketSender sender)
    {
        if (!Double.isFinite(packet.x()) || !Double.isFinite(packet.y()) || !Double.isFinite(packet.z())) {
            log.warn("Rejected teleport request from {} with non-finite coordinates", player.getUuidAsString());
            return;
        }

        ServerWorld serverWorld = resolveWorld(server, player, packet);
        if (serverWorld == null) {
            log.warn("Rejected teleport request from {} to unknown world {}", player.getUuidAsString(), packet.worldId());
            return;
        }

        BlockPos destination = BlockPos.ofFloored(packet.x(), packet.y(), packet.z());
        if (!isAllowedDestination(serverWorld, destination)) {
            log.warn("Rejected teleport request from {} to unauthorized destination {} in {}", player.getUuidAsString(), destination, serverWorld.getRegistryKey().getValue());
            return;
        }

        player.teleport(serverWorld, packet.x(), packet.y(), packet.z(), player.getYaw(), player.getPitch());
    }

    /**
     * Resolves the destination world from the packet or the player's current world.
     *
     * @param server the active Minecraft server
     * @param player the player requesting teleportation
     * @param packet the teleport packet to validate
     * @return the resolved server world, or null if the world does not exist
     */
    private ServerWorld resolveWorld(MinecraftServer server, ServerPlayerEntity player, PlayerTeleportPacket packet) {
        if (packet.worldId() == null) {
            return player.getServerWorld();
        }

        RegistryKey<World> key = RegistryKey.of(RegistryKeys.WORLD, packet.worldId());
        return server.getWorld(key);
    }

    /**
     * Checks whether a requested teleport block is backed by trusted server state.
     *
     * @param world       the destination world
     * @param destination the floored requested destination position
     * @return true when the destination is a configured chapter start or loaded path marker
     */
    private boolean isAllowedDestination(ServerWorld world, BlockPos destination) {
        if (ArdaPaths.CONFIG.isChapterStartPosition(destination)) {
            return true;
        }

        return world.getBlockEntity(destination) instanceof PathMarkerBlockEntity;
    }
}

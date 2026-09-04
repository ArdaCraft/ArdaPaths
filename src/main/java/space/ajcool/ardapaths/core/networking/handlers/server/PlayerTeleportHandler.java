package space.ajcool.ardapaths.core.networking.handlers.server;

import lombok.extern.slf4j.Slf4j;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.Level;
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
public class PlayerTeleportHandler extends ServerPacketHandler<PlayerTeleportPacket> {

    public PlayerTeleportHandler() {
        super(PlayerTeleportPacket.TYPE, PlayerTeleportPacket::read);
    }

    @Override
    protected void handle(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler, PlayerTeleportPacket packet, PacketSender sender) {
        if (!Double.isFinite(packet.x()) || !Double.isFinite(packet.y()) || !Double.isFinite(packet.z())) {
            log.warn("Rejected teleport request from {} with non-finite coordinates", player.getStringUUID());
            return;
        }

        ServerLevel serverWorld = resolveWorld(server, player, packet);
        if (serverWorld == null) {
            log.warn("Rejected teleport request from {} to unknown world {}", player.getStringUUID(), packet.worldId());
            return;
        }

        BlockPos destination = BlockPos.containing(packet.x(), packet.y(), packet.z());
        if (!isAllowedDestination(serverWorld, destination)) {
            log.warn("Rejected teleport request from {} to unauthorized destination {} in {}", player.getStringUUID(), destination, serverWorld.dimension().location());
            return;
        }

        player.teleportTo(serverWorld, packet.x(), packet.y(), packet.z(), player.getYRot(), player.getXRot());
    }

    /**
     * Resolves the destination world from the packet or the player's current world.
     *
     * @param server the active Minecraft server
     * @param player the player requesting teleportation
     * @param packet the teleport packet to validate
     * @return the resolved server world, or null if the world does not exist
     */
    private ServerLevel resolveWorld(MinecraftServer server, ServerPlayer player, PlayerTeleportPacket packet) {
        if (packet.worldId() == null) {
            return player.serverLevel();
        }

        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, packet.worldId());
        return server.getLevel(key);
    }

    /**
     * Checks whether a requested teleport block is backed by trusted server state.
     *
     * @param world       the destination world
     * @param destination the floored requested destination position
     * @return true when the destination is a configured chapter start or loaded path marker
     */
    private boolean isAllowedDestination(ServerLevel world, BlockPos destination) {
        if (ArdaPaths.CONFIG.isChapterStartPosition(destination)) {
            return true;
        }

        return world.getBlockEntity(destination) instanceof PathMarkerBlockEntity;
    }
}

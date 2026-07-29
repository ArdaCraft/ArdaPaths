package space.ajcool.ardapaths.core.networking.handlers.server;

import lombok.extern.slf4j.Slf4j;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.core.consumers.networking.RespondablePacketHandler;
import space.ajcool.ardapaths.core.networking.packets.EmptyPacket;
import space.ajcool.ardapaths.mc.items.ModItems;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server packet handler for giving the player the pathfinder item.
 */
@Slf4j(topic = "ardapaths")
public class WieldPathfinderRequestHandler extends RespondablePacketHandler<EmptyPacket, EmptyPacket> {
    /**
     * Minimum time between accepted wield requests from the same player.
     */
    private static final long REQUEST_COOLDOWN_MS = 1_000L;

    /**
     * Last accepted request timestamp by player UUID.
     */
    private final Map<UUID, Long> lastAcceptedRequests = new ConcurrentHashMap<>();

    /**
     * Handler constructor
     */
    public WieldPathfinderRequestHandler()
    {
        super("wield_pathfinder_request_channel", EmptyPacket::read,
                "wield_pathfinder_request_channel", EmptyPacket::read);
    }

    /**
     * Handles the packet by giving the player the pathfinder item.
     * @param server the server instance
     * @param player the current player
     * @param handler the network handler
     * @param packet the received packet
     * @param sender the sender
     */
    @Override
    public EmptyPacket handle(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, EmptyPacket packet, PacketSender sender) {
        if (!acceptRequest(player)) {
            log.warn("Rate-limited Pathfinder wield request from {}", player.getUuidAsString());
            return new EmptyPacket();
        }

        Item pathfinder = Registries.ITEM.get(new Identifier(ArdaPaths.MOD_ID, ModItems.PATH_REVEALER_ID));
        PlayerInventory inventory = player.getInventory();

        int selectedSlot = inventory.selectedSlot;

        for (int i = 0; i < inventory.size(); i++) {

            ItemStack stack = inventory.getStack(i);

            if (stack.isOf(pathfinder)) {

                if (i != selectedSlot) {
                    ItemStack oldSelected = inventory.getStack(selectedSlot);

                    inventory.setStack(selectedSlot, stack);
                    inventory.setStack(i, oldSelected);
                }

                return new EmptyPacket();
            }
        }

        // Not found, create one directly in hand
        inventory.setStack(selectedSlot, new ItemStack(pathfinder));

        return new EmptyPacket();
    }

    /**
     * Checks and records whether a player may issue a wield request now.
     *
     * @param player the requesting player
     * @return true when the request is outside the cooldown window
     */
    private boolean acceptRequest(ServerPlayerEntity player) {
        long now = System.currentTimeMillis();
        UUID playerId = player.getUuid();
        Long previous = lastAcceptedRequests.get(playerId);

        if (previous != null && now - previous < REQUEST_COOLDOWN_MS) {
            sweepOldRequests(now);
            return false;
        }

        lastAcceptedRequests.put(playerId, now);
        sweepOldRequests(now);
        return true;
    }

    /**
     * Removes old rate-limit entries for players that are no longer making requests.
     *
     * @param now the current wall-clock time in milliseconds
     */
    private void sweepOldRequests(long now) {
        lastAcceptedRequests.entrySet().removeIf(entry -> now - entry.getValue() > REQUEST_COOLDOWN_MS * 60L);
    }
}

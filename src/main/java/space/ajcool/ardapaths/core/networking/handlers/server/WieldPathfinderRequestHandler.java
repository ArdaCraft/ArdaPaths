package space.ajcool.ardapaths.core.networking.handlers.server;

import lombok.extern.slf4j.Slf4j;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import space.ajcool.ardapaths.core.ModConstants;
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
     * Channel identifier for pathfinder wield requests and responses.
     */
    private static final ResourceLocation CHANNEL = ModConstants.modId("wield_pathfinder_request_channel");

    /**
     * Handler constructor
     */
    public WieldPathfinderRequestHandler()
    {
        super(CHANNEL, EmptyPacket::read, CHANNEL, EmptyPacket::read);
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
    public EmptyPacket handle(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler, EmptyPacket packet, PacketSender sender) {
        if (!acceptRequest(player)) {
            log.warn("Rate-limited Pathfinder wield request from {}", player.getStringUUID());
            return new EmptyPacket();
        }

        Item pathfinder = BuiltInRegistries.ITEM.get(ModConstants.modId(ModItems.PATH_REVEALER_ID));
        Inventory inventory = player.getInventory();

        int selectedSlot = inventory.selected;

        for (int i = 0; i < inventory.getContainerSize(); i++) {

            ItemStack stack = inventory.getItem(i);

            if (stack.is(pathfinder)) {

                if (i != selectedSlot) {
                    ItemStack oldSelected = inventory.getItem(selectedSlot);

                    inventory.setItem(selectedSlot, stack);
                    inventory.setItem(i, oldSelected);
                }

                return new EmptyPacket();
            }
        }

        // Not found, create one directly in hand
        inventory.setItem(selectedSlot, new ItemStack(pathfinder));

        return new EmptyPacket();
    }

    /**
     * Checks and records whether a player may issue a wield request now.
     *
     * @param player the requesting player
     * @return true when the request is outside the cooldown window
     */
    private boolean acceptRequest(ServerPlayer player) {
        long now = System.currentTimeMillis();
        UUID playerId = player.getUUID();
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

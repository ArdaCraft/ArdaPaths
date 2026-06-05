package space.ajcool.ardapaths.core.networking.handlers.server;

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

/**
 * Server packet handler for giving the player the pathfinder item.
 */
public class WieldPathfinderRequestHandler extends RespondablePacketHandler<EmptyPacket, EmptyPacket> {

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
}

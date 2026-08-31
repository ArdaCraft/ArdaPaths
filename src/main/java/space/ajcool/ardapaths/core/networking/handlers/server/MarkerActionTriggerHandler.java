package space.ajcool.ardapaths.core.networking.handlers.server;

import lombok.extern.slf4j.Slf4j;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import space.ajcool.ardapaths.core.ModConstants;
import space.ajcool.ardapaths.core.consumers.networking.ServerPacketHandler;
import space.ajcool.ardapaths.core.data.GiveItemAction;
import space.ajcool.ardapaths.core.data.WarpTarget;
import space.ajcool.ardapaths.core.integration.Warps;
import space.ajcool.ardapaths.core.networking.packets.server.MarkerActionTriggerPacket;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;
import space.ajcool.ardapaths.mc.items.ModItems;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles server-executed marker actions that are triggered by Pathfinder traversal.
 */
@Slf4j(topic = "ardapaths")
public class MarkerActionTriggerHandler extends ServerPacketHandler<MarkerActionTriggerPacket> {
    /**
     * Minimum marker action trigger range in blocks.
     */
    private static final int MIN_ACTION_TRIGGER_RANGE = 3;

    /**
     * Extra distance accepted beyond the trigger range to account for client re-arm buffering.
     */
    private static final double PROXIMITY_EXIT_BUFFER = 2.0D;

    /**
     * Minimum time between item grants from the same marker to the same player.
     */
    private static final long ITEM_GRANT_COOLDOWN_MS = 30_000L;

    /**
     * Item grant timestamps keyed by player UUID and marker position.
     */
    private final Map<UUID, Map<BlockPos, Long>> itemGrantCooldowns = new ConcurrentHashMap<>();

    /**
     * Creates the marker-action trigger packet handler.
     */
    public MarkerActionTriggerHandler() {
        super(MarkerActionTriggerPacket.CHANNEL, MarkerActionTriggerPacket::read);
    }

    /**
     * Handles a marker-action trigger by re-reading marker NBT and executing its configured actions.
     *
     * @param server  the Minecraft server
     * @param player  the player who reached the marker
     * @param handler the network handler
     * @param packet  the received trigger packet
     * @param sender  the packet sender
     */
    @SuppressWarnings("resource")
    @Override
    protected void handle(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler, MarkerActionTriggerPacket packet, PacketSender sender) {
        BlockEntity blockEntity = player.serverLevel().getBlockEntity(packet.markerPos());
        if (!(blockEntity instanceof PathMarkerBlockEntity marker)) return;

        PathMarkerBlockEntity.ChapterNbtData chapterData = marker.getChapterData(packet.pathId(), packet.chapterId(), false);
        if (chapterData == null) return;
        if (!playerIsHoldingPathfinder(player)) return;
        if (!playerIsInActionRange(player, packet.markerPos(), chapterData)) return;

        processAutoTeleport(server, player, chapterData);
        processGiveItem(player, packet.markerPos(), chapterData);
    }

    /**
     * Checks whether the player is currently holding the Pathfinder in either hand.
     *
     * @param player the player who sent the packet
     * @return true when either hand contains the Pathfinder item
     */
    private boolean playerIsHoldingPathfinder(ServerPlayer player) {
        Item pathfinder = pathfinderItem();
        return player.getMainHandItem().is(pathfinder) || player.getOffhandItem().is(pathfinder);
    }

    /**
     * Checks whether the player is close enough to trigger the marker action.
     *
     * @param player      the player who sent the packet
     * @param markerPos   the marker block position
     * @param chapterData server-read marker chapter data
     * @return true when the player's block position is within the accepted range
     */
    private boolean playerIsInActionRange(ServerPlayer player, BlockPos markerPos, PathMarkerBlockEntity.ChapterNbtData chapterData) {
        double acceptedRange = Math.max(chapterData.getActivationRange(), MIN_ACTION_TRIGGER_RANGE) + PROXIMITY_EXIT_BUFFER;
        return player.blockPosition().distSqr(markerPos) <= Mth.square(acceptedRange);
    }

    /**
     * Executes the marker's configured auto-teleport target when present.
     *
     * @param server      the server that owns the player
     * @param player      the player to teleport
     * @param chapterData server-read marker chapter data
     */
    private void processAutoTeleport(MinecraftServer server, ServerPlayer player, PathMarkerBlockEntity.ChapterNbtData chapterData) {
        String target = chapterData.getAutoTeleportTarget().trim();
        if (target.isEmpty()) return;

        BlockPos coordinates = WarpTarget.parseCoordinates(target);
        if (coordinates != null) {
            player.teleportTo(player.serverLevel(), coordinates.getX() + 0.5D, coordinates.getY(), coordinates.getZ() + 0.5D, player.getYRot(), player.getXRot());
            return;
        }

        Runnable fallback = () -> {
            log.warn("Failed to resolve marker auto-teleport warp {} for player {}", target, player.getStringUUID());
            player.displayClientMessage(Component.literal("Failed to teleport to marker target \"" + target + "\""), false);
        };

        log.info("Attempting marker auto-teleport warp for player {} at {}", player.getStringUUID(), target);
        Warps.warpTo(server, player, target, fallback);
    }

    /**
     * Grants the marker's configured item when present and not on cooldown.
     *
     * @param player      the player to receive the item
     * @param markerPos   the marker block position
     * @param chapterData server-read marker chapter data
     */
    private void processGiveItem(ServerPlayer player, BlockPos markerPos, PathMarkerBlockEntity.ChapterNbtData chapterData) {
        String value = chapterData.getGiveItem().trim();
        if (value.isEmpty() || !acceptItemGrant(player, markerPos)) return;

        if (GiveItemAction.isClear(value)) {
            processClearHand(player);
            return;
        }

        Item item = GiveItemAction.resolveItem(value);
        if (item == null) {
            failGiveItem(player, value);
            return;
        }

        if (playerIsHolding(player, item)) return;
        if (moveFromInventoryToFreeHand(player, item)) return;

        ItemStack stack = new ItemStack(item);
        if (placeInFreeNonPathfinderHand(player, stack)) return;
        if (!player.getInventory().add(stack)) failGiveItem(player, value);
    }

    /**
     * Checks and records whether this marker may grant an item to this player now.
     *
     * @param player    the player requesting an item grant
     * @param markerPos the marker block position
     * @return true when the marker is outside the player's item-grant cooldown
     */
    private boolean acceptItemGrant(ServerPlayer player, BlockPos markerPos) {
        long now = System.currentTimeMillis();
        Map<BlockPos, Long> playerCooldowns = itemGrantCooldowns.computeIfAbsent(player.getUUID(), ignored -> new ConcurrentHashMap<>());
        Long previous = playerCooldowns.get(markerPos);

        if (previous != null && now - previous < ITEM_GRANT_COOLDOWN_MS) {
            sweepOldItemGrants(now);
            return false;
        }

        playerCooldowns.put(markerPos.immutable(), now);
        sweepOldItemGrants(now);
        return true;
    }

    /**
     * Checks whether the player already has the configured item in either hand.
     *
     * @param player the player being checked
     * @param item   the configured marker action item
     * @return true when the item is in the player's main hand or off-hand
     */
    private boolean playerIsHolding(ServerPlayer player, Item item) {
        return player.getMainHandItem().is(item) || player.getOffhandItem().is(item);
    }

    /**
     * Moves an existing inventory stack into the free non-Pathfinder hand.
     *
     * @param player the player whose inventory is searched
     * @param item   the item to move into hand
     * @return true when the grant is fully handled without minting a new stack
     */
    private boolean moveFromInventoryToFreeHand(ServerPlayer player, Item item) {
        InteractionHand hand = freeNonPathfinderHand(player);
        if (hand == null) return true;

        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.is(item)) continue;

            inventory.setItem(slot, ItemStack.EMPTY);
            player.setItemInHand(hand, stack);
            return true;
        }

        return false;
    }

    /**
     * Places an item stack in an empty hand that is not holding the Pathfinder.
     *
     * @param player the player to receive the item
     * @param stack  the stack to place
     * @return true when the stack was placed directly into a hand
     */
    private boolean placeInFreeNonPathfinderHand(ServerPlayer player, ItemStack stack) {
        InteractionHand hand = freeNonPathfinderHand(player);
        if (hand == null) return false;

        player.setItemInHand(hand, stack);
        return true;
    }

    /**
     * Finds an empty hand that is not the hand holding the Pathfinder.
     *
     * @param player the player whose hands are inspected
     * @return the empty non-Pathfinder hand, or null when neither hand is suitable
     */
    private @Nullable InteractionHand freeNonPathfinderHand(ServerPlayer player) {
        Item pathfinder = pathfinderItem();

        if (player.getMainHandItem().is(pathfinder) && player.getOffhandItem().isEmpty()) return InteractionHand.OFF_HAND;
        if (player.getOffhandItem().is(pathfinder) && player.getMainHandItem().isEmpty()) return InteractionHand.MAIN_HAND;

        return null;
    }

    /**
     * Stores the item held in the non-Pathfinder hand back into inventory.
     *
     * @param player the player whose hand should be cleared
     */
    private void processClearHand(ServerPlayer player) {
        InteractionHand hand = nonPathfinderHand(player);
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty() || stack.is(pathfinderItem())) return;

        player.setItemInHand(hand, ItemStack.EMPTY);
        if (storeInInventory(player, stack)) return;

        player.setItemInHand(hand, stack);
        player.displayClientMessage(Component.translatable("ardapaths.client.marker.give_item.inventory_full", stack.getHoverName().getString()), false);
    }

    /**
     * Chooses the hand that is not currently holding the Pathfinder.
     *
     * @param player the player whose hands are inspected
     * @return the non-Pathfinder hand
     */
    private InteractionHand nonPathfinderHand(ServerPlayer player) {
        if (player.getOffhandItem().is(pathfinderItem())) return InteractionHand.MAIN_HAND;
        return InteractionHand.OFF_HAND;
    }

    /**
     * Stores a full hand stack into inventory, preferring storage rows over the hotbar.
     *
     * @param player the player whose inventory receives the stack
     * @param stack  the stack to store
     * @return true when the whole stack was stored
     */
    private boolean storeInInventory(ServerPlayer player, ItemStack stack) {
        Inventory inventory = player.getInventory();
        if (storeInSlotRange(inventory, stack, Inventory.getSelectionSize(), Inventory.INVENTORY_SIZE)) return true;

        return storeInSlotRange(inventory, stack, 0, Inventory.getSelectionSize());
    }

    /**
     * Stores a stack in an inventory slot range without partially consuming the source stack.
     *
     * @param inventory the inventory that receives the stack
     * @param stack     the stack to store
     * @param startSlot the first slot to inspect
     * @param endSlot   the slot after the last slot to inspect
     * @return true when the whole stack was stored in the range
     */
    private boolean storeInSlotRange(Inventory inventory, ItemStack stack, int startSlot, int endSlot) {
        for (int slot = startSlot; slot < endSlot; slot++) {
            ItemStack existing = inventory.getItem(slot);
            if (existing.isEmpty() || !ItemStack.isSameItemSameTags(existing, stack)) continue;
            if (existing.getCount() + stack.getCount() > existing.getMaxStackSize()) continue;

            existing.grow(stack.getCount());
            inventory.setChanged();
            return true;
        }

        for (int slot = startSlot; slot < endSlot; slot++) {
            if (!inventory.getItem(slot).isEmpty()) continue;

            inventory.setItem(slot, stack);
            return true;
        }

        return false;
    }

    /**
     * Resolves the registered Pathfinder item.
     *
     * @return the Pathfinder item
     */
    private Item pathfinderItem() {
        return BuiltInRegistries.ITEM.get(ModConstants.modId(ModItems.PATH_REVEALER_ID));
    }

    /**
     * Sends the item-grant failure message to a player.
     *
     * @param player the player to notify
     * @param itemId the configured item identifier that failed
     */
    private void failGiveItem(ServerPlayer player, String itemId) {
        player.displayClientMessage(Component.translatable("ardapaths.client.marker.give_item.failed", itemId), false);
    }

    /**
     * Removes expired item-grant cooldown entries.
     *
     * @param now the current wall-clock time in milliseconds
     */
    private void sweepOldItemGrants(long now) {
        itemGrantCooldowns.entrySet().removeIf(playerEntry -> {
            playerEntry.getValue().entrySet().removeIf(markerEntry -> now - markerEntry.getValue() > ITEM_GRANT_COOLDOWN_MS * 4L);
            return playerEntry.getValue().isEmpty();
        });
    }
}

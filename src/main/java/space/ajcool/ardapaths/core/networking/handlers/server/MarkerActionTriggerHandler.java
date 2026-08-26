package space.ajcool.ardapaths.core.networking.handlers.server;

import lombok.extern.slf4j.Slf4j;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import space.ajcool.ardapaths.ArdaPaths;
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
        super("marker_action_trigger", MarkerActionTriggerPacket::read);
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
    @Override
    protected void handle(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, MarkerActionTriggerPacket packet, PacketSender sender) {
        BlockEntity blockEntity = player.getServerWorld().getBlockEntity(packet.markerPos());
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
    private boolean playerIsHoldingPathfinder(ServerPlayerEntity player) {
        Item pathfinder = pathfinderItem();
        return player.getMainHandStack().isOf(pathfinder) || player.getOffHandStack().isOf(pathfinder);
    }

    /**
     * Checks whether the player is close enough to trigger the marker action.
     *
     * @param player      the player who sent the packet
     * @param markerPos   the marker block position
     * @param chapterData server-read marker chapter data
     * @return true when the player's block position is within the accepted range
     */
    private boolean playerIsInActionRange(ServerPlayerEntity player, BlockPos markerPos, PathMarkerBlockEntity.ChapterNbtData chapterData) {
        double acceptedRange = Math.max(chapterData.getActivationRange(), MIN_ACTION_TRIGGER_RANGE) + PROXIMITY_EXIT_BUFFER;
        return player.getBlockPos().getSquaredDistance(markerPos) <= MathHelper.square(acceptedRange);
    }

    /**
     * Executes the marker's configured auto-teleport target when present.
     *
     * @param server      the server that owns the player
     * @param player      the player to teleport
     * @param chapterData server-read marker chapter data
     */
    private void processAutoTeleport(MinecraftServer server, ServerPlayerEntity player, PathMarkerBlockEntity.ChapterNbtData chapterData) {
        String target = chapterData.getAutoTeleportTarget().trim();
        if (target.isEmpty()) return;

        BlockPos coordinates = WarpTarget.parseCoordinates(target);
        if (coordinates != null) {
            player.teleport(player.getServerWorld(), coordinates.getX() + 0.5D, coordinates.getY(), coordinates.getZ() + 0.5D, player.getYaw(), player.getPitch());
            return;
        }

        Runnable fallback = () -> {
            log.warn("Failed to resolve marker auto-teleport warp {} for player {}", target, player.getUuidAsString());
            player.sendMessage(Text.literal("Failed to teleport to marker target \"" + target + "\""), false);
        };

        log.info("Attempting marker auto-teleport warp for player {} at {}", player.getUuidAsString(), target);
        Warps.warpTo(server, player, target, fallback);
    }

    /**
     * Grants the marker's configured item when present and not on cooldown.
     *
     * @param player      the player to receive the item
     * @param markerPos   the marker block position
     * @param chapterData server-read marker chapter data
     */
    private void processGiveItem(ServerPlayerEntity player, BlockPos markerPos, PathMarkerBlockEntity.ChapterNbtData chapterData) {
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
        if (!player.getInventory().insertStack(stack)) failGiveItem(player, value);
    }

    /**
     * Checks and records whether this marker may grant an item to this player now.
     *
     * @param player    the player requesting an item grant
     * @param markerPos the marker block position
     * @return true when the marker is outside the player's item-grant cooldown
     */
    private boolean acceptItemGrant(ServerPlayerEntity player, BlockPos markerPos) {
        long now = System.currentTimeMillis();
        Map<BlockPos, Long> playerCooldowns = itemGrantCooldowns.computeIfAbsent(player.getUuid(), ignored -> new ConcurrentHashMap<>());
        Long previous = playerCooldowns.get(markerPos);

        if (previous != null && now - previous < ITEM_GRANT_COOLDOWN_MS) {
            sweepOldItemGrants(now);
            return false;
        }

        playerCooldowns.put(markerPos.toImmutable(), now);
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
    private boolean playerIsHolding(ServerPlayerEntity player, Item item) {
        return player.getMainHandStack().isOf(item) || player.getOffHandStack().isOf(item);
    }

    /**
     * Moves an existing inventory stack into the free non-Pathfinder hand.
     *
     * @param player the player whose inventory is searched
     * @param item   the item to move into hand
     * @return true when the grant is fully handled without minting a new stack
     */
    private boolean moveFromInventoryToFreeHand(ServerPlayerEntity player, Item item) {
        Hand hand = freeNonPathfinderHand(player);
        if (hand == null) return true;

        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < PlayerInventory.MAIN_SIZE; slot++) {
            ItemStack stack = inventory.getStack(slot);
            if (!stack.isOf(item)) continue;

            inventory.setStack(slot, ItemStack.EMPTY);
            player.setStackInHand(hand, stack);
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
    private boolean placeInFreeNonPathfinderHand(ServerPlayerEntity player, ItemStack stack) {
        Hand hand = freeNonPathfinderHand(player);
        if (hand == null) return false;

        player.setStackInHand(hand, stack);
        return true;
    }

    /**
     * Finds an empty hand that is not the hand holding the Pathfinder.
     *
     * @param player the player whose hands are inspected
     * @return the empty non-Pathfinder hand, or null when neither hand is suitable
     */
    private @Nullable Hand freeNonPathfinderHand(ServerPlayerEntity player) {
        Item pathfinder = pathfinderItem();

        if (player.getMainHandStack().isOf(pathfinder) && player.getOffHandStack().isEmpty()) return Hand.OFF_HAND;
        if (player.getOffHandStack().isOf(pathfinder) && player.getMainHandStack().isEmpty()) return Hand.MAIN_HAND;

        return null;
    }

    /**
     * Stores the item held in the non-Pathfinder hand back into inventory.
     *
     * @param player the player whose hand should be cleared
     */
    private void processClearHand(ServerPlayerEntity player) {
        Hand hand = nonPathfinderHand(player);
        ItemStack stack = player.getStackInHand(hand);
        if (stack.isEmpty() || stack.isOf(pathfinderItem())) return;

        player.setStackInHand(hand, ItemStack.EMPTY);
        if (storeInInventory(player, stack)) return;

        player.setStackInHand(hand, stack);
        player.sendMessage(Text.translatable("ardapaths.client.marker.give_item.inventory_full", stack.getName().getString()), false);
    }

    /**
     * Chooses the hand that is not currently holding the Pathfinder.
     *
     * @param player the player whose hands are inspected
     * @return the non-Pathfinder hand
     */
    private Hand nonPathfinderHand(ServerPlayerEntity player) {
        if (player.getOffHandStack().isOf(pathfinderItem())) return Hand.MAIN_HAND;
        return Hand.OFF_HAND;
    }

    /**
     * Stores a full hand stack into inventory, preferring storage rows over the hotbar.
     *
     * @param player the player whose inventory receives the stack
     * @param stack  the stack to store
     * @return true when the whole stack was stored
     */
    private boolean storeInInventory(ServerPlayerEntity player, ItemStack stack) {
        PlayerInventory inventory = player.getInventory();
        if (storeInSlotRange(inventory, stack, PlayerInventory.getHotbarSize(), PlayerInventory.MAIN_SIZE)) return true;

        return storeInSlotRange(inventory, stack, 0, PlayerInventory.getHotbarSize());
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
    private boolean storeInSlotRange(PlayerInventory inventory, ItemStack stack, int startSlot, int endSlot) {
        for (int slot = startSlot; slot < endSlot; slot++) {
            ItemStack existing = inventory.getStack(slot);
            if (existing.isEmpty() || !ItemStack.canCombine(existing, stack)) continue;
            if (existing.getCount() + stack.getCount() > existing.getMaxCount()) continue;

            existing.increment(stack.getCount());
            inventory.markDirty();
            return true;
        }

        for (int slot = startSlot; slot < endSlot; slot++) {
            if (!inventory.getStack(slot).isEmpty()) continue;

            inventory.setStack(slot, stack);
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
        return Registries.ITEM.get(new Identifier(ArdaPaths.MOD_ID, ModItems.PATH_REVEALER_ID));
    }

    /**
     * Sends the item-grant failure message to a player.
     *
     * @param player the player to notify
     * @param itemId the configured item identifier that failed
     */
    private void failGiveItem(ServerPlayerEntity player, String itemId) {
        player.sendMessage(Text.translatable("ardapaths.client.marker.give_item.failed", itemId), false);
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

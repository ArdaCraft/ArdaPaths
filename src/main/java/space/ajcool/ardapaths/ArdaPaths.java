package space.ajcool.ardapaths;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;
import space.ajcool.ardapaths.mc.blocks.ModBlocks;
import space.ajcool.ardapaths.mc.blocks.entities.ModBlockEntities;
import space.ajcool.ardapaths.mc.items.ModItemGroups;
import space.ajcool.ardapaths.mc.items.ModItems;
import space.ajcool.ardapaths.mc.particles.ModParticles;
import space.ajcool.ardapaths.mc.sounds.ModSounds;

public class ArdaPaths implements ModInitializer
{
    public static final String MOD_ID = "ardapaths";
    public static final Logger LOGGER = LoggerFactory.getLogger("ardapaths");

    public static final Identifier PATH_MARKER_UPDATE_PACKET = new Identifier(MOD_ID, "path_marker_update_packet");
    public static final Identifier PATH_PLAYER_TELEPORT_PACKET = new Identifier(MOD_ID, "path_player_teleport_packet");

    public static ArdaPathsConfig CONFIG;

    @Override
    public void onInitialize() {
        ArdaPathsConfig.INSTANCE.load();
        CONFIG = ArdaPathsConfig.INSTANCE.getConfig();

        ModBlocks.init();
        ModBlockEntities.init();
        ModItems.init();
        ModItemGroups.init();
        ModParticles.init();
        ModSounds.init();

        ServerPlayNetworking.registerGlobalReceiver(PATH_MARKER_UPDATE_PACKET, (server, player, handler, buf, responseSender) ->
        {
            var blockPos = buf.readBlockPos();
            var nbt = buf.readNbt();

            server.execute(() -> {
                var blockEntity = player.getWorld().getBlockEntity(blockPos);

                if (blockEntity instanceof PathMarkerBlockEntity pathMarkerBlockEntity)
                {
                    pathMarkerBlockEntity.readNbt(nbt);
                    pathMarkerBlockEntity.markUpdated();
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(PATH_PLAYER_TELEPORT_PACKET, (server, player, handler, buf, responseSender) ->
        {
            var x = buf.readDouble();
            var y = buf.readDouble();
            var z = buf.readDouble();

            server.execute(() -> player.requestTeleport(x, y, z));
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) ->
        {
            var blockEntity = world.getBlockEntity(hitResult.getBlockPos().offset(hitResult.getSide()));

            if ((blockEntity instanceof PathMarkerBlockEntity || player.getStackInHand(hand).isOf(ModBlocks.PATH_MARKER.asItem())) && !Permissions.check(player, "ardapaths.edit", false) && !player.hasPermissionLevel(4)) return ActionResult.FAIL;

            return ActionResult.PASS;
        });

        UseItemCallback.EVENT.register((player, world, hand) ->
        {
            var itemsStack = player.getStackInHand(hand);

            if (itemsStack.isOf(ModBlocks.PATH_MARKER.asItem()) && !Permissions.check(player, "ardapaths.edit", false) && !player.hasPermissionLevel(4)) return TypedActionResult.fail(itemsStack);

            return TypedActionResult.pass(itemsStack);
        });

        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) ->
        {
            if (blockEntity instanceof PathMarkerBlockEntity && !Permissions.check(player, "ardapaths.edit", false) && !player.hasPermissionLevel(4)) return false;

            return true;
        });
    }
}

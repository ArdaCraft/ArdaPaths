package space.ajcool.ardapaths;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.util.ActionResult;
import net.minecraft.util.TypedActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.ajcool.ardapaths.core.data.config.ServerConfigManager;
import space.ajcool.ardapaths.core.data.config.server.ServerConfig;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;
import space.ajcool.ardapaths.mc.blocks.ModBlocks;
import space.ajcool.ardapaths.mc.blocks.entities.ModBlockEntities;
import space.ajcool.ardapaths.mc.items.ModItemGroups;
import space.ajcool.ardapaths.mc.items.ModItems;
import space.ajcool.ardapaths.mc.particles.ModParticles;
import space.ajcool.ardapaths.mc.sounds.ModSounds;
import space.ajcool.ardapaths.core.networking.PacketRegistry;

public class ArdaPaths implements ModInitializer {
    public static final String MOD_ID = "ardapaths";
    public static final Logger LOGGER = LoggerFactory.getLogger("ardapaths");
    public static ServerConfigManager CONFIG_MANAGER;
    public static ServerConfig CONFIG;

    @Override
    public void onInitialize() {
        CONFIG_MANAGER = new ServerConfigManager("./config/arda-paths/server.json");
        CONFIG = CONFIG_MANAGER.getConfig();

        ModBlocks.init();
        ModBlockEntities.init();
        ModItems.init();
        ModItemGroups.init();
        ModParticles.init();
        ModSounds.init();
        PacketRegistry.init();

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
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

        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (blockEntity instanceof PathMarkerBlockEntity && !Permissions.check(player, "ardapaths.edit", false) && !player.hasPermissionLevel(4)) return false;

            return true;
        });
    }
}

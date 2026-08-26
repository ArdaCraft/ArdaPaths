package space.ajcool.ardapaths;

import lombok.extern.slf4j.Slf4j;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.minecraft.util.ActionResult;
import net.minecraft.util.TypedActionResult;
import space.ajcool.ardapaths.api.ArdaPathsApi;
import space.ajcool.ardapaths.api.ArdaPathsApiEntrypoint;
import space.ajcool.ardapaths.commands.ArdaPathsCommand;
import space.ajcool.ardapaths.core.Client;
import space.ajcool.ardapaths.core.PermissionHelper;
import space.ajcool.ardapaths.core.api.ArdaPathsApiImpl;
import space.ajcool.ardapaths.core.data.config.ServerConfigManager;
import space.ajcool.ardapaths.core.data.config.server.ServerConfig;
import space.ajcool.ardapaths.core.networking.PacketRegistry;
import space.ajcool.ardapaths.mc.blocks.ModBlocks;
import space.ajcool.ardapaths.mc.blocks.entities.ModBlockEntities;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;
import space.ajcool.ardapaths.mc.items.ModItemGroups;
import space.ajcool.ardapaths.mc.items.ModItems;
import space.ajcool.ardapaths.mc.particles.ModParticles;
import space.ajcool.ardapaths.mc.sounds.ModSounds;

/**
 * Main entry point for the ArdaPaths Fabric mod.
 * Initializes all mod features including blocks, items, particles, sounds, and networking.
 */
@Slf4j(topic = "ardapaths")
public class ArdaPaths implements ModInitializer {
    /**
     * The unique identifier for the ArdaPaths mod.
     */
    public static final String MOD_ID = "ardapaths";

    /**
     * The Fabric Permissions API permission string for editing paths and markers.
     */
    public static final String MOD_EDIT_PERMISSION = String.format("%s.edit", MOD_ID);

    /**
     * Manager for server-side configuration, handles loading and saving server.json.
     */
    public static ServerConfigManager CONFIG_MANAGER;

    /**
     * The current server-side path configuration, loaded from server.json.
     */
    public static ServerConfig CONFIG;

    /**
     * @return true if this code is running on the server
     */
    public static boolean amITheServer() {

        var serverEnv = FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER;

        if (!serverEnv) return Client.isInSinglePlayer();

        return true;
    }

    /**
     * Fabric mod initialization entry point.
     * Initializes all mod components including configs, registries, networking, and permission handlers.
     */
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
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> ArdaPathsCommand.register(dispatcher));

        ArdaPathsApiImpl.initialize();

        invokeApiEntrypoints();

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) ->
        {
            var blockEntity = world.getBlockEntity(hitResult.getBlockPos());

            if ((blockEntity instanceof PathMarkerBlockEntity || player.getStackInHand(hand).isOf(ModBlocks.PATH_MARKER.asItem())) && !PermissionHelper.hasEditPermission(player))
                return ActionResult.FAIL;

            return ActionResult.PASS;
        });

        UseItemCallback.EVENT.register((player, world, hand) ->
        {
            var itemsStack = player.getStackInHand(hand);

            if (itemsStack.isOf(ModBlocks.PATH_MARKER.asItem()) && !PermissionHelper.hasEditPermission(player))
                return TypedActionResult.fail(itemsStack);

            return TypedActionResult.pass(itemsStack);
        });

        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) ->
                !(blockEntity instanceof PathMarkerBlockEntity) || PermissionHelper.hasEditPermission(player));
    }

    /**
     * Queries Fabric for all mods that registered an {@code ardapaths:api} entrypoint and
     * calls {@link ArdaPathsApiEntrypoint#onApiReady(ArdaPathsApi)} on each of them.
     */
    private void invokeApiEntrypoints() {

        ArdaPathsApi api = ArdaPathsApiImpl.getInstance();

        for (EntrypointContainer<ArdaPathsApiEntrypoint> container :
                FabricLoader.getInstance().getEntrypointContainers(MOD_ID + ":api", ArdaPathsApiEntrypoint.class)) {

            String modId = container.getProvider().getMetadata().getId();
            try {

                log.info("[ArdaPaths] Invoking ardapaths:api entrypoint for mod '{}'", modId);
                container.getEntrypoint().onApiReady(api);

            } catch (Throwable throwable) {

                log.error("[ArdaPaths] Exception in ardapaths:api entrypoint of mod '{}': {}", modId, throwable.getMessage(), throwable);
            }
        }
    }
}

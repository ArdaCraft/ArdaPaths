package space.ajcool.ardapaths;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.sound.v1.FabricSoundInstance;
import net.fabricmc.fabric.api.event.client.ClientSpriteRegistryCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.GlowParticle;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Material;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.ajcool.ardapaths.block.PathMarkerBlock;
import space.ajcool.ardapaths.block.PathMarkerBlockEntity;
import space.ajcool.ardapaths.item.PathRevealerItem;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ArdaPaths implements ModInitializer
{
    public static final String ModID = "ardapaths";
    public static final Logger LOGGER = LoggerFactory.getLogger("ardapaths");

    public static final Block PATH_MARKER_BLOCK = new PathMarkerBlock(FabricBlockSettings.of(Material.AIR).noOcclusion().noCollission().strength(-1.0F, 3600000.8F).noLootTable());
    public static final Item PATH_MARKER_ITEM = new BlockItem(PATH_MARKER_BLOCK, new FabricItemSettings().group(CreativeModeTab.TAB_TOOLS));
    public static final Item PATH_REVEALER_ITEM = new PathRevealerItem(new FabricItemSettings().group(CreativeModeTab.TAB_TOOLS).maxCount(1).fireResistant().rarity(Rarity.EPIC));
    private static final CreativeModeTab PATH_ITEM_GROUP = FabricItemGroupBuilder.create(new ResourceLocation(ModID, "ardapaths"))
            .icon(() -> new ItemStack(PATH_REVEALER_ITEM))
            .appendItems((itemStacks) -> {
                itemStacks.add(PATH_MARKER_ITEM.getDefaultInstance());
                itemStacks.add(PATH_REVEALER_ITEM.getDefaultInstance());
            }).build();
    public static final SimpleParticleType PATH_PARTICLE_TYPE = FabricParticleTypes.simple(true);
    public static final ResourceLocation PATH_MARKER_UPDATE_PACKET = new ResourceLocation(ModID, "path_marker_update_packet");
    public static final ResourceLocation PATH_PLAYER_TELEPORT_PACKET = new ResourceLocation(ModID, "path_player_teleport_packet");
    public static final ResourceLocation TRAIL_SOUND_ID = new ResourceLocation(ModID, "trail_sound");
    public static final SoundEvent TRAIL_SOUND_EVENT = new SoundEvent(TRAIL_SOUND_ID);

    public static ArdaPathsConfig CONFIG;

    public static final BlockEntityType<PathMarkerBlockEntity> PATH_MARKER_BLOCK_ENTITY = Registry.register(
            Registry.BLOCK_ENTITY_TYPE,
            new ResourceLocation(ModID, "path_marker_block_entity"),
            FabricBlockEntityTypeBuilder.create(PathMarkerBlockEntity::new, PATH_MARKER_BLOCK).build()
    );

    @Override
    public void onInitialize() {
        ArdaPathsConfig.INSTANCE.load();

        CONFIG = ArdaPathsConfig.INSTANCE.getConfig();

        Registry.register(Registry.BLOCK, new ResourceLocation(ModID, "path_marker"), PATH_MARKER_BLOCK);
        Registry.register(Registry.ITEM, new ResourceLocation(ModID, "path_marker"), PATH_MARKER_ITEM);
        Registry.register(Registry.ITEM, new ResourceLocation(ModID, "path_revealer"), PATH_REVEALER_ITEM);
        Registry.register(Registry.PARTICLE_TYPE, new ResourceLocation(ModID, "path"), PATH_PARTICLE_TYPE);
        Registry.register(Registry.SOUND_EVENT, TRAIL_SOUND_ID, TRAIL_SOUND_EVENT);

        ServerPlayNetworking.registerGlobalReceiver(PATH_MARKER_UPDATE_PACKET, (server, player, handler, buf, responseSender) ->
        {
            var blockPos = buf.readBlockPos();
            var nbt = buf.readNbt();

            server.execute(() -> {
                var blockEntity = player.getLevel().getBlockEntity(blockPos);

                if (blockEntity instanceof PathMarkerBlockEntity pathMarkerBlockEntity)
                {
                    pathMarkerBlockEntity.load(nbt);
                    pathMarkerBlockEntity.markUpdated();
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(PATH_PLAYER_TELEPORT_PACKET, (server, player, handler, buf, responseSender) ->
        {
            var x = buf.readDouble();
            var y = buf.readDouble();
            var z = buf.readDouble();

            server.execute(() -> player.teleportTo(x, y, z));
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) ->
        {
            var blockEntity = world.getBlockEntity(hitResult.getBlockPos().relative(hitResult.getDirection()));

            if ((blockEntity instanceof PathMarkerBlockEntity || player.getItemInHand(hand).is(PATH_MARKER_ITEM)) && !Permissions.check(player, "ardapaths.edit", false) && !player.hasPermissions(4)) return InteractionResult.FAIL;

            return InteractionResult.PASS;
        });

        UseItemCallback.EVENT.register((player, world, hand) ->
        {
            var itemsStack = player.getItemInHand(hand);

            if (itemsStack.is(PATH_MARKER_ITEM) && !Permissions.check(player, "ardapaths.edit", false) && !player.hasPermissions(4)) return InteractionResultHolder.fail(itemsStack);

            return InteractionResultHolder.pass(itemsStack);
        });

        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) ->
        {
            if (blockEntity instanceof PathMarkerBlockEntity && !Permissions.check(player, "ardapaths.edit", false) && !player.hasPermissions(4)) return false;

            return true;
        });
    }
}

package space.ajcool.ardapaths;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.util.TypedActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.ajcool.ardapaths.block.PathMarkerBlock;
import space.ajcool.ardapaths.block.PathMarkerBlockEntity;
import space.ajcool.ardapaths.item.PathRevealerItem;

public class ArdaPaths implements ModInitializer
{
    public static final String ModID = "ardapaths";
    public static final Logger LOGGER = LoggerFactory.getLogger("ardapaths");

    public static final Block PATH_MARKER_BLOCK = new PathMarkerBlock(FabricBlockSettings.create().nonOpaque().collidable(false).dropsNothing().strength(-1.0f, 3600000.0f));
    public static final Item PATH_MARKER_ITEM = new BlockItem(PATH_MARKER_BLOCK, new FabricItemSettings());
    public static final Item PATH_REVEALER_ITEM = new PathRevealerItem(new FabricItemSettings().maxCount(1).fireproof().rarity(Rarity.EPIC));

    public static final RegistryKey<ItemGroup> PATH_ITEM_GROUP_KEY = RegistryKey.of(Registries.ITEM_GROUP.getKey(), Identifier.of(ModID, "item_group"));
    public static final ItemGroup PATH_ITEM_GROUP = FabricItemGroup.builder()
            .icon(PATH_REVEALER_ITEM::getDefaultStack)
            .displayName(Text.translatable("itemGroup.ardapaths.ardapaths"))
            .build();

    public static final DefaultParticleType PATH_PARTICLE_TYPE = FabricParticleTypes.simple(true);
    public static final Identifier PATH_MARKER_UPDATE_PACKET = new Identifier(ModID, "path_marker_update_packet");
    public static final Identifier PATH_PLAYER_TELEPORT_PACKET = new Identifier(ModID, "path_player_teleport_packet");
    public static final Identifier TRAIL_SOUND_ID = new Identifier(ModID, "trail_sound");
    public static final SoundEvent TRAIL_SOUND_EVENT = SoundEvent.of(TRAIL_SOUND_ID);

    public static ArdaPathsConfig CONFIG;

    public static final BlockEntityType<PathMarkerBlockEntity> PATH_MARKER_BLOCK_ENTITY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            new Identifier(ModID, "path_marker_block_entity"),
            FabricBlockEntityTypeBuilder.create(PathMarkerBlockEntity::new, PATH_MARKER_BLOCK).build()
    );

    @Override
    public void onInitialize() {
        ArdaPathsConfig.INSTANCE.load();

        CONFIG = ArdaPathsConfig.INSTANCE.getConfig();

        Registry.register(Registries.BLOCK, new Identifier(ModID, "path_marker"), PATH_MARKER_BLOCK);
        Registry.register(Registries.ITEM, new Identifier(ModID, "path_marker"), PATH_MARKER_ITEM);
        Registry.register(Registries.ITEM, new Identifier(ModID, "path_revealer"), PATH_REVEALER_ITEM);
        Registry.register(Registries.PARTICLE_TYPE, new Identifier(ModID, "path"), PATH_PARTICLE_TYPE);
        Registry.register(Registries.SOUND_EVENT, TRAIL_SOUND_ID, TRAIL_SOUND_EVENT);

        Registry.register(Registries.ITEM_GROUP, PATH_ITEM_GROUP_KEY, PATH_ITEM_GROUP);

        ItemGroupEvents.modifyEntriesEvent(PATH_ITEM_GROUP_KEY).register(itemGroup -> {
            itemGroup.add(PATH_MARKER_ITEM.getDefaultStack());
            itemGroup.add(PATH_REVEALER_ITEM.getDefaultStack());
        });

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

            if ((blockEntity instanceof PathMarkerBlockEntity || player.getStackInHand(hand).isOf(PATH_MARKER_ITEM)) && !Permissions.check(player, "ardapaths.edit", false) && !player.hasPermissionLevel(4)) return ActionResult.FAIL;

            return ActionResult.PASS;
        });

        UseItemCallback.EVENT.register((player, world, hand) ->
        {
            var itemsStack = player.getStackInHand(hand);

            if (itemsStack.isOf(PATH_MARKER_ITEM) && !Permissions.check(player, "ardapaths.edit", false) && !player.hasPermissionLevel(4)) return TypedActionResult.fail(itemsStack);

            return TypedActionResult.pass(itemsStack);
        });

        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) ->
        {
            if (blockEntity instanceof PathMarkerBlockEntity && !Permissions.check(player, "ardapaths.edit", false) && !player.hasPermissionLevel(4)) return false;

            return true;
        });
    }
}

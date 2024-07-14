package space.ajcool.ardapaths.block;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.ArdaPathsClient;

import java.util.HashMap;
import java.util.Map;

public class PathMarkerBlock extends BaseEntityBlock
{
    public static BlockPos selectedBlockPosition = null;

    public PathMarkerBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    public InteractionResult use(BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult) {
        if (!Permissions.check(player, "ardapaths.edit", false) && !player.hasPermissions(4)) return InteractionResult.CONSUME;

        BlockEntity selectedBlockEntity = level.getBlockEntity(blockPos);

        if (!player.getItemInHand(interactionHand).is(ArdaPaths.PATH_MARKER_ITEM)
                || !(selectedBlockEntity instanceof PathMarkerBlockEntity pathMarkerBlockEntity))
            return InteractionResult.PASS;

        if (!level.isClientSide()) return InteractionResult.CONSUME;

        // We are in a client context here.

        if (ArdaPathsClient.checkCtrlHeld())
        {
            ArdaPathsClient.openEditorScreen(pathMarkerBlockEntity);
            return InteractionResult.CONSUME;
        }

        if (selectedBlockPosition == null)
        {
            if (selectedBlockEntity instanceof PathMarkerBlockEntity)
            {
                selectedBlockPosition = blockPos;

                var message = Component.empty()
                        .append(Component.literal("ArdaPaths: ").withStyle(ChatFormatting.DARK_AQUA))
                        .append(Component.literal("Selected origin block.").withStyle(ChatFormatting.BLUE));

                player.sendSystemMessage(message);
            }
        }
        else
        {
            BlockEntity originBlockEntity = level.getBlockEntity(selectedBlockPosition);

            if (originBlockEntity instanceof PathMarkerBlockEntity originPathMarkerBlockEntity)
            {
                MutableComponent message;

                if (selectedBlockPosition.equals(blockPos))
                {
                    message = Component.empty()
                            .append(Component.literal("ArdaPaths: ").withStyle(ChatFormatting.DARK_AQUA))
                            .append(Component.literal("Target block removed.").withStyle(ChatFormatting.RED));

                    originPathMarkerBlockEntity.targetOffsets.remove(ArdaPathsClient.selectedTrailId());
                }
                else
                {
                    message = Component.empty()
                            .append(Component.literal("ArdaPaths: ").withStyle(ChatFormatting.DARK_AQUA))
                            .append(Component.literal("Target block set.").withStyle(ChatFormatting.GREEN));

                     originPathMarkerBlockEntity.targetOffsets.put(ArdaPathsClient.selectedTrailId(), blockPos.subtract(selectedBlockPosition));
                }

                var packetBuffer = PacketByteBufs.create();

                packetBuffer.writeBlockPos(originPathMarkerBlockEntity.getBlockPos());
                packetBuffer.writeNbt(originPathMarkerBlockEntity.saveWithoutMetadata());

                ClientPlayNetworking.send(ArdaPaths.PATH_MARKER_UPDATE_PACKET, packetBuffer);

                player.sendSystemMessage(message);

                ArdaPaths.LOGGER.info("Sending Update Packet");
            }

            selectedBlockPosition = null;
        }

        return InteractionResult.CONSUME;
    }

    public boolean propagatesSkylightDown(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos) {
        return true;
    }

    public RenderShape getRenderShape(BlockState blockState) {
        return RenderShape.INVISIBLE;
    }

    public float getShadeBrightness(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos) {
        return 1.0F;
    }

    public VoxelShape getShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
        return collisionContext.isHoldingItem(ArdaPaths.PATH_MARKER_ITEM) ? Shapes.block() : Shapes.empty();
    }


    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState)
    {
        return new PathMarkerBlockEntity(blockPos, blockState);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState blockState, BlockEntityType<T> blockEntityType)
    {
        return level.isClientSide ? createTickerHelper(blockEntityType, ArdaPaths.PATH_MARKER_BLOCK_ENTITY, PathMarkerBlockEntity::tick) : null;
    }
}
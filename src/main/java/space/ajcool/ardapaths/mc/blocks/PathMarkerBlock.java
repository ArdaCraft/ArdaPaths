package space.ajcool.ardapaths.mc.blocks;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.ArdaPathsClient;
import space.ajcool.ardapaths.mc.blocks.entities.ModBlockEntities;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;
import space.ajcool.ardapaths.mc.items.ModItems;

public class PathMarkerBlock extends BlockWithEntity
{
    public static BlockPos selectedBlockPosition = null;

    public PathMarkerBlock(AbstractBlock.Settings properties) {
        super(properties);
    }

    public ActionResult onUse(BlockState blockState, World level, BlockPos blockPos, PlayerEntity player, Hand interactionHand, BlockHitResult blockHitResult) {
        if (!Permissions.check(player, "ardapaths.edit", false) && !player.hasPermissionLevel(4)) return ActionResult.CONSUME;

        BlockEntity selectedBlockEntity = level.getBlockEntity(blockPos);

        if (!player.getStackInHand(interactionHand).isOf(ModItems.PATH_MARKER)
                || !(selectedBlockEntity instanceof PathMarkerBlockEntity pathMarkerBlockEntity))
            return ActionResult.PASS;

        if (!level.isClient()) return ActionResult.CONSUME;

        // We are in a client context here.

        if (ArdaPathsClient.checkCtrlHeld())
        {
            ArdaPathsClient.openEditorScreen(pathMarkerBlockEntity);
            return ActionResult.CONSUME;
        }

        if (selectedBlockPosition == null)
        {
            if (selectedBlockEntity instanceof PathMarkerBlockEntity)
            {
                selectedBlockPosition = blockPos;

                var message = Text.empty()
                        .append(Text.literal("ArdaPaths: ").formatted(Formatting.DARK_AQUA))
                        .append(Text.literal("Selected origin block.").formatted(Formatting.BLUE));

                player.sendMessage(message);
            }
        }
        else
        {
            BlockEntity originBlockEntity = level.getBlockEntity(selectedBlockPosition);

            if (originBlockEntity instanceof PathMarkerBlockEntity originPathMarkerBlockEntity)
            {
                MutableText message;

                if (selectedBlockPosition.equals(blockPos))
                {
                    message = Text.empty()
                            .append(Text.literal("ArdaPaths: ").formatted(Formatting.DARK_AQUA))
                            .append(Text.literal("Target block removed.").formatted(Formatting.RED));

                    originPathMarkerBlockEntity.targetOffsets.remove(ArdaPathsClient.selectedTrailId());
                }
                else
                {
                    message = Text.empty()
                            .append(Text.literal("ArdaPaths: ").formatted(Formatting.DARK_AQUA))
                            .append(Text.literal("Target block set.").formatted(Formatting.GREEN));

                     originPathMarkerBlockEntity.targetOffsets.put(ArdaPathsClient.selectedTrailId(), blockPos.subtract(selectedBlockPosition));
                }

                var packetBuffer = PacketByteBufs.create();

                packetBuffer.writeBlockPos(originPathMarkerBlockEntity.getPos());
                packetBuffer.writeNbt(originPathMarkerBlockEntity.createNbt());

                ClientPlayNetworking.send(ArdaPaths.PATH_MARKER_UPDATE_PACKET, packetBuffer);

                player.sendMessage(message);

                ArdaPaths.LOGGER.info("Sending Update Packet");
            }

            selectedBlockPosition = null;
        }

        return ActionResult.CONSUME;
    }

    public boolean isTransparent(BlockState blockState, BlockView blockGetter, BlockPos blockPos) {
        return true;
    }

    public BlockRenderType getRenderType(BlockState blockState) {
        return BlockRenderType.INVISIBLE;
    }

    public float getAmbientOcclusionLightLevel(BlockState blockState, BlockView blockGetter, BlockPos blockPos) {
        return 1.0F;
    }

    public VoxelShape getOutlineShape(BlockState blockState, BlockView blockGetter, BlockPos blockPos, ShapeContext collisionContext) {
        return collisionContext.isHolding(ModItems.PATH_MARKER) ? VoxelShapes.fullCube() : VoxelShapes.empty();
    }


    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos blockPos, BlockState blockState)
    {
        return new PathMarkerBlockEntity(blockPos, blockState);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World level, BlockState blockState, BlockEntityType<T> blockEntityType)
    {
        return level.isClient ? checkType(blockEntityType, ModBlockEntities.PATH_MARKER, PathMarkerBlockEntity::tick) : null;
    }
}
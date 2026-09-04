package space.ajcool.ardapaths.mc.blocks;

import com.mojang.serialization.MapCodec;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import space.ajcool.ardapaths.ArdaPathsClient;
import space.ajcool.ardapaths.core.Client;
import space.ajcool.ardapaths.core.PermissionHelper;
import space.ajcool.ardapaths.core.networking.PacketRegistry;
import space.ajcool.ardapaths.core.networking.packets.server.PathMarkerUpdatePacket;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;
import space.ajcool.ardapaths.mc.items.ModItems;
import space.ajcool.ardapaths.screens.Screens;

/**
 * Custom block for Path Markers.
 * Players place these blocks to define waypoints and trail segments in paths.
 * Supports Ctrl+click to open the marker editor and regular clicks to link markers.
 */
@Slf4j(topic = "ardapaths")
public class PathMarkerBlock extends BaseEntityBlock {

    /**
     * Codec used by vanilla block serialization.
     */
    public static final MapCodec<PathMarkerBlock> CODEC = simpleCodec(PathMarkerBlock::new);

    /**
     * The block position of the currently selected origin marker for linking paths,
     * or null if no marker is currently selected.
     */
    public static BlockPos selectedBlockPosition = null;

    /**
     * Constructs a PathMarkerBlock with the given Minecraft block properties.
     *
     * @param properties the block settings (non-opaque, no collision, indestructible, etc.)
     */
    public PathMarkerBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    /**
     * Gets the vanilla block codec for this block type.
     *
     * @return the path marker block codec
     */
    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    /**
     * Handles the player clicking on a Path Marker block.
     * Validates permissions and delegates to {@link #validateOnUse} if the player has edit permission.
     *
     * @param blockState     the state of the block
     * @param level          the world
     * @param blockPos       the position of the block
     * @param player         the player who clicked
     * @param blockHitResult the hit result
     * @return the action result (CONSUME if handled, PASS otherwise)
     */
    @Override
    protected @NotNull InteractionResult useWithoutItem(@NonNull BlockState blockState, Level level, @NonNull BlockPos blockPos, @NonNull Player player, @NonNull BlockHitResult blockHitResult) {
        BlockEntity selectedBlockEntity = level.getBlockEntity(blockPos);

        if (selectedBlockEntity == null) return InteractionResult.PASS;
        if (!player.isHolding(ModItems.PATH_MARKER) || !(selectedBlockEntity instanceof PathMarkerBlockEntity pathMarkerBlockEntity))
            return InteractionResult.PASS;
        if (!level.isClientSide()) return InteractionResult.CONSUME;

        if (PermissionHelper.hasEditPermission(player)) {
            this.validateOnUse(level, blockPos, pathMarkerBlockEntity, player);
        }

        return InteractionResult.CONSUME;
    }

    /**
     * Validates the player's interaction with the Path Marker block.
     * If Ctrl is held, opens the marker editor. Otherwise, links markers for path traversal.
     *
     * @param level                 the world
     * @param blockPos              the position of this block
     * @param pathMarkerBlockEntity the block entity
     * @param player                the player interacting
     */
    public void validateOnUse(Level level, BlockPos blockPos, PathMarkerBlockEntity pathMarkerBlockEntity, Player player) {
        Minecraft.getInstance().execute(() -> {
            if (Client.isCtrlDown()) {
                Screens.openEditorScreen(pathMarkerBlockEntity);
                return;
            }

            if (selectedBlockPosition == null) {
                selectedBlockPosition = blockPos;

                var message = Component.empty()
                        .append(Component.literal("ArdaPaths: ").withStyle(ChatFormatting.DARK_AQUA))
                        .append(Component.literal("Selected origin block.").withStyle(ChatFormatting.BLUE));

                player.sendSystemMessage(message);

            } else {

                BlockEntity blockEntity = level.getBlockEntity(selectedBlockPosition);

                if (blockEntity instanceof PathMarkerBlockEntity pathMarker) {
                    MutableComponent message;

                    if (selectedBlockPosition.equals(blockPos)) {
                        message = Component.empty()
                                .append(Component.literal("ArdaPaths: ").withStyle(ChatFormatting.DARK_AQUA))
                                .append(Component.literal("Target block removed.").withStyle(ChatFormatting.RED));

                        PathMarkerBlockEntity.ChapterNbtData data = pathMarker.getChapterData(ArdaPathsClient.CONFIG.getSelectedPathId(), ArdaPathsClient.CONFIG.getCurrentChapterId());
                        data.removeTarget();
                    } else {
                        message = Component.empty()
                                .append(Component.literal("ArdaPaths: ").withStyle(ChatFormatting.DARK_AQUA))
                                .append(Component.literal("Target block set.").withStyle(ChatFormatting.GREEN));

                        PathMarkerBlockEntity.ChapterNbtData data = pathMarker.getChapterData(ArdaPathsClient.CONFIG.getSelectedPathId(), ArdaPathsClient.CONFIG.getCurrentChapterId());
                        data.setTarget(blockPos.subtract(selectedBlockPosition));
                    }

                    PathMarkerUpdatePacket packet = new PathMarkerUpdatePacket(pathMarker.getBlockPos(), pathMarker.saveWithoutMetadata(level.registryAccess()));
                    PacketRegistry.PATH_MARKER_UPDATE.send(packet, null);
                    player.sendSystemMessage(message);
                    log.info("Sending Update Packet");
                }

                selectedBlockPosition = null;
            }
        });
    }

    @Override
    protected boolean propagatesSkylightDown(@NonNull BlockState blockState) {
        return true;
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NonNull BlockState blockState) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public float getShadeBrightness(@NonNull BlockState blockState, @NonNull BlockGetter blockGetter, @NonNull BlockPos blockPos) {
        return 1.0F;
    }

    @Override
    public @NotNull VoxelShape getShape(@NonNull BlockState blockState, @NonNull BlockGetter blockGetter, @NonNull BlockPos blockPos, CollisionContext collisionContext) {
        return collisionContext.isHoldingItem(ModItems.PATH_MARKER) ? Shapes.block() : Shapes.empty();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NonNull BlockPos blockPos, @NonNull BlockState blockState) {
        return new PathMarkerBlockEntity(blockPos, blockState);
    }
}

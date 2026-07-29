package space.ajcool.ardapaths.mc.blocks;

import lombok.extern.slf4j.Slf4j;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
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
import space.ajcool.ardapaths.ArdaPathsClient;
import space.ajcool.ardapaths.core.Client;
import space.ajcool.ardapaths.core.networking.PacketRegistry;
import space.ajcool.ardapaths.core.networking.packets.EmptyPacket;
import space.ajcool.ardapaths.core.networking.packets.server.PathMarkerUpdatePacket;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;
import space.ajcool.ardapaths.mc.items.ModItems;
import space.ajcool.ardapaths.screens.Screens;

/**
 * Custom block for Path Markers.
 * Players place these blocks to define waypoints and trail segments in paths.
 * Supports Ctrl+click to open the marker editor and regular clicks to link markers.
 */
@SuppressWarnings("deprecation")
@Slf4j(topic = "ardapaths")
public class PathMarkerBlock extends BlockWithEntity {
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
    public PathMarkerBlock(AbstractBlock.Settings properties) {
        super(properties);
    }

    /**
     * Handles the player clicking on a Path Marker block.
     * Validates permissions and delegates to {@link #validateOnUse} if the player has edit permission.
     *
     * @param blockState      the state of the block
     * @param level           the world
     * @param blockPos        the position of the block
     * @param player          the player who clicked
     * @param interactionHand the hand used
     * @param blockHitResult  the hit result
     * @return the action result (CONSUME if handled, PASS otherwise)
     */
    public ActionResult onUse(BlockState blockState, World level, BlockPos blockPos, PlayerEntity player, Hand interactionHand, BlockHitResult blockHitResult) {
        BlockEntity selectedBlockEntity = level.getBlockEntity(blockPos);

        if (selectedBlockEntity == null) return ActionResult.PASS;
        if (!player.isHolding(ModItems.PATH_MARKER) || !(selectedBlockEntity instanceof PathMarkerBlockEntity pathMarkerBlockEntity))
            return ActionResult.PASS;
        if (!level.isClient()) return ActionResult.CONSUME;

        PacketRegistry.PERMISSION_CHECK.send(new EmptyPacket(), response -> {
            if (response.hasPermission()) this.validateOnUse(level, blockPos, pathMarkerBlockEntity, player);
        });

        return ActionResult.CONSUME;
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
    public void validateOnUse(World level, BlockPos blockPos, PathMarkerBlockEntity pathMarkerBlockEntity, PlayerEntity player) {
        MinecraftClient.getInstance().execute(() -> {
            if (Client.isCtrlDown()) {
                Screens.openEditorScreen(pathMarkerBlockEntity);
                return;
            }

            if (selectedBlockPosition == null) {
                selectedBlockPosition = blockPos;

                var message = Text.empty()
                        .append(Text.literal("ArdaPaths: ").formatted(Formatting.DARK_AQUA))
                        .append(Text.literal("Selected origin block.").formatted(Formatting.BLUE));

                player.sendMessage(message);

            } else {

                BlockEntity blockEntity = level.getBlockEntity(selectedBlockPosition);

                if (blockEntity instanceof PathMarkerBlockEntity pathMarker) {
                    MutableText message;

                    if (selectedBlockPosition.equals(blockPos)) {
                        message = Text.empty()
                                .append(Text.literal("ArdaPaths: ").formatted(Formatting.DARK_AQUA))
                                .append(Text.literal("Target block removed.").formatted(Formatting.RED));

                        PathMarkerBlockEntity.ChapterNbtData data = pathMarker.getChapterData(ArdaPathsClient.CONFIG.getSelectedPathId(), ArdaPathsClient.CONFIG.getCurrentChapterId());
                        data.removeTarget();
                    } else {
                        message = Text.empty()
                                .append(Text.literal("ArdaPaths: ").formatted(Formatting.DARK_AQUA))
                                .append(Text.literal("Target block set.").formatted(Formatting.GREEN));

                        PathMarkerBlockEntity.ChapterNbtData data = pathMarker.getChapterData(ArdaPathsClient.CONFIG.getSelectedPathId(), ArdaPathsClient.CONFIG.getCurrentChapterId());
                        data.setTarget(blockPos.subtract(selectedBlockPosition));
                    }

                    PathMarkerUpdatePacket packet = new PathMarkerUpdatePacket(pathMarker.getPos(), pathMarker.createNbt());
                    PacketRegistry.PATH_MARKER_UPDATE.send(packet);
                    player.sendMessage(message);
                    log.info("Sending Update Packet");
                }

                selectedBlockPosition = null;
            }
        });
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
    public BlockEntity createBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new PathMarkerBlockEntity(blockPos, blockState);
    }
}

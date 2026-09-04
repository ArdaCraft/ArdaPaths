package space.ajcool.ardapaths.mc.items;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import space.ajcool.ardapaths.ArdaPathsClient;
import space.ajcool.ardapaths.core.data.config.shared.Color;
import space.ajcool.ardapaths.core.data.config.shared.PathData;

/**
 * Item tint source that colours the Pathfinder with the selected path's primary colour.
 */
public record SelectedPathTintSource() implements ItemTintSource {

    /**
     * Codec instance used by item model definitions to create this tint source.
     */
    public static final MapCodec<SelectedPathTintSource> MAP_CODEC = MapCodec.unit(SelectedPathTintSource::new);

    /**
     * Fallback tint used when no path is currently selected on the client.
     */
    private static final int DEFAULT_TINT = Color.fromRgb(100, 100, 100).asHex();

    /**
     * Calculates the Pathfinder tint from the active client configuration.
     *
     * @param itemStack the item stack being rendered
     * @param level     the client level rendering the stack, or null outside a level
     * @param entity    the entity holding or displaying the stack, or null in inventory rendering
     * @return the selected path colour, or the fallback tint when no path is selected
     */
    @Override
    public int calculate(@NonNull ItemStack itemStack, @Nullable ClientLevel level, @Nullable LivingEntity entity) {
        PathData selectedPath = ArdaPathsClient.CONFIG.getSelectedPath();
        if (selectedPath != null) {
            return selectedPath.getPrimaryColor().asHex();
        }

        return DEFAULT_TINT;
    }

    /**
     * Returns the codec registered for this tint source type.
     *
     * @return this tint source's codec
     */
    @Override
    public @NonNull MapCodec<? extends ItemTintSource> type() {
        return MAP_CODEC;
    }
}

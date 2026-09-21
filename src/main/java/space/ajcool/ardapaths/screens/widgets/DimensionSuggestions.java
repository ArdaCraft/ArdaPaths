package space.ajcool.ardapaths.screens.widgets;

import net.minecraft.client.Minecraft;
import space.ajcool.ardapaths.core.networking.PacketRegistry;
import space.ajcool.ardapaths.core.networking.packets.EmptyPacket;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Shared client-side dimension suggestion loading for resource-location inputs.
 */
public final class DimensionSuggestions {
    /**
     * Prevents construction of this static helper.
     */
    private DimensionSuggestions() {
    }

    /**
     * Builds dimension suggestions from the current client connection.
     *
     * @return ordered dimension identifiers with the overworld first
     */
    public static List<String> localOptions() {
        List<String> dimensions = Minecraft.getInstance().getConnection() == null
                ? new ArrayList<>(List.of("minecraft:overworld", "minecraft:the_nether", "minecraft:the_end"))
                : Minecraft.getInstance().getConnection().levels().stream()
                .map(key -> key.location().toString())
                .sorted()
                .toList();

        return SuggestionTextMatcher.orderDimensions(dimensions);
    }

    /**
     * Installs authoritative server dimension suggestions when the input is still mounted.
     *
     * @param input          input widget to update
     * @param stillMounted   callback that returns whether the input still belongs to the active screen
     */
    public static void requestServerDimensions(SuggestionInputWidget input, BooleanSupplier stillMounted) {
        PacketRegistry.DIMENSION_LIST.send(new EmptyPacket(), response -> Minecraft.getInstance().execute(() -> {
            if (!stillMounted.getAsBoolean() || response.dimensions().isEmpty()) {
                return;
            }
            input.setSuggestions(SuggestionTextMatcher.orderDimensions(response.dimensions()));
        }));
    }
}

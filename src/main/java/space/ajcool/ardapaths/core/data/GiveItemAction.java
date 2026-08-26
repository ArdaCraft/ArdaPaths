package space.ajcool.ardapaths.core.data;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * Utility for resolving authored marker give-item action values.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GiveItemAction {
    /**
     * Authored keyword that stores the held non-Pathfinder item back into inventory.
     */
    public static final String CLEAR_KEYWORD = "clear";

    /**
     * Checks whether a configured give-item value is the clear keyword.
     *
     * @param value the configured give-item text
     * @return true when the value requests clearing the non-Pathfinder hand
     */
    public static boolean isClear(String value) {
        return value != null && CLEAR_KEYWORD.equalsIgnoreCase(value.trim());
    }

    /**
     * Resolves a configured give-item value to a registered Minecraft item.
     *
     * @param value the configured give-item text
     * @return the matching item, or null when the value is blank, clear, malformed, or unknown
     */
    public static @Nullable Item resolveItem(String value) {
        if (value == null) return null;

        String trimmed = value.trim();
        if (trimmed.isEmpty() || isClear(trimmed)) return null;

        Identifier id = Identifier.tryParse(trimmed);
        if (id == null) return null;

        return Registries.ITEM.getOrEmpty(id).orElse(null);
    }
}

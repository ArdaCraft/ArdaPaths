package space.ajcool.ardapaths.core.conversions;

import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.ArdaPathsClient;
import space.ajcool.ardapaths.core.Fabric;
import space.ajcool.ardapaths.core.data.config.shared.PathData;
import space.ajcool.ardapaths.mc.NbtEncodeable;

import java.util.List;
import net.minecraft.nbt.CompoundTag;

/**
 * Handles migration of PathMarkerBlockEntity NBT data to newer formats.
 * Converts legacy per-marker NBT structure to the new multi-path structure.
 */
public class PathMarkerBlockEntityConverter {

    /**
     * Converts legacy NBT for a PathMarkerBlockEntity to the new format.
     * Migrates old flat NBT structure (proximityMessage, activationRange, targetOffset)
     * to the new nested structure (paths → pathId → chapterId → ChapterNbtData).
     * If the provided NBT compound already contains a "paths" compound,
     * no conversion is performed.
     * Legacy target offsets are mapped to paths by current config order because the
     * legacy format stored no path IDs; changing path order before migration can
     * therefore associate an old target with a different path.
     *
     * @param oldNbt the original NBT compound loaded from disk
     * @return the migrated NBT compound with the new structure
     */
    public static CompoundTag convertNbt(CompoundTag oldNbt) {
        if (NbtEncodeable.hasCompound(oldNbt, "paths")) {
            return oldNbt;
        }

        if (!hasAnyLegacyMarkerKey(oldNbt)) {
            return oldNbt;
        }

        List<PathData> paths = Fabric.isClient() ? ArdaPathsClient.CONFIG.getPaths() : ArdaPaths.CONFIG.getPaths();

        if (!hasLegacyMarkerDataForConfiguredPaths(oldNbt, paths.size())) {
            return oldNbt;
        }

        CompoundTag pathsCompound = new CompoundTag();

        String proximityMessage = NbtEncodeable.getStringOrEmpty(oldNbt, "proximityMessage");
        int activationRange = NbtEncodeable.getIntOrZero(oldNbt, "activationRange");

        int i = 0;

        for (PathData path : paths) {
            String legacyKey = "targetOffset-" + i;
            if (NbtEncodeable.hasCompound(oldNbt, legacyKey)) {
                CompoundTag dataCompound = new CompoundTag();
                dataCompound.put("target", NbtEncodeable.getCompound(oldNbt, legacyKey));
                NbtEncodeable.putStringIfNotEmpty(dataCompound, "proximity_message", proximityMessage);
                NbtEncodeable.putIntIfNonZero(dataCompound, "activation_range", activationRange);

                CompoundTag defaultChapter = new CompoundTag();
                defaultChapter.put("default", dataCompound);

                pathsCompound.put(path.getId(), defaultChapter);
            }
            i++;
        }

        oldNbt.remove("proximityMessage");
        oldNbt.remove("activationRange");

        for (i = 0; i < paths.size(); i++) {
            oldNbt.remove("targetOffset-" + i);
        }

        oldNbt.put("paths", pathsCompound);
        return oldNbt;
    }

    /**
     * Checks whether a marker tag has any key shaped like legacy marker data.
     *
     * @param oldNbt marker NBT to inspect
     * @return true when a configured-path-aware legacy check is needed
     */
    private static boolean hasAnyLegacyMarkerKey(CompoundTag oldNbt) {
        if (oldNbt.contains("proximityMessage") || oldNbt.contains("activationRange")) {
            return true;
        }

        for (String key : oldNbt.getAllKeys()) {
            if (key.startsWith("targetOffset-")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks whether a marker tag contains any key from the legacy flat marker format.
     *
     * @param oldNbt marker NBT to inspect
     * @param pathCount number of configured paths available for positional target keys
     * @return true when the tag should be migrated from the legacy format
     */
    private static boolean hasLegacyMarkerDataForConfiguredPaths(CompoundTag oldNbt, int pathCount) {
        if (oldNbt.contains("proximityMessage") || oldNbt.contains("activationRange")) {
            return true;
        }

        for (int i = 0; i < pathCount; i++) {
            if (NbtEncodeable.hasCompound(oldNbt, "targetOffset-" + i)) {
                return true;
            }
        }

        return false;
    }
}

package space.ajcool.ardapaths.core.conversions;

import net.minecraft.nbt.NbtCompound;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.core.data.config.shared.PathData;

import java.util.List;

public class PathMarkerBlockEntityConverter {

    /**
     * Converts legacy NBT for a PathMarkerBlockEntity to the new format.
     * If the provided NBT compound already contains a "paths" compound,
     * no conversion is performed.
     *
     * @param oldNbt The original NBT compound (loaded from disk).
     */
    public static NbtCompound convertNbt(NbtCompound oldNbt) {
        if (oldNbt.contains("paths", 10)) {
            return oldNbt;
        }

        NbtCompound pathsCompound = new NbtCompound();

        String proximityMessage = "";
        if (oldNbt.contains("proximityMessage", 8)) { // 8: string
            proximityMessage = oldNbt.getString("proximityMessage");
        }
        int activationRange = 0;
        if (oldNbt.contains("activationRange", 3)) { // 3: int
            activationRange = oldNbt.getInt("activationRange");
        }

        List<PathData> paths = ArdaPaths.CONFIG.getPaths();
        int i = 0;
        for (PathData path : paths) {
            String legacyKey = "targetOffset-" + i;
            if (oldNbt.contains(legacyKey, 10)) {
                NbtCompound dataCompound = new NbtCompound();
                dataCompound.put("target", oldNbt.getCompound(legacyKey));
                if (!proximityMessage.isEmpty()) {
                    dataCompound.putString("proximity_message", proximityMessage);
                }
                if (activationRange != 0) {
                    dataCompound.putInt("activation_range", activationRange);
                }

                pathsCompound.put(path.getId(), dataCompound);
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
}

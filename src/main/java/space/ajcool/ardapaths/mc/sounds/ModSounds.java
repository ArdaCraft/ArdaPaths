package space.ajcool.ardapaths.mc.sounds;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import space.ajcool.ardapaths.core.ModConstants;

/**
 * Registry for custom sound events in ArdaPaths.
 * Handles registration of sound events with the Minecraft registry.
 */
public class ModSounds {

    /**
     * The sound event played when traversing a trail.
     */
    public static final SoundEvent TRAIL = register("trail_sound");

    /**
     * Register a sound.
     *
     * @param id the sound's ID
     * @return the registered sound event
     */
    @SuppressWarnings("SameParameterValue")
    private static SoundEvent register(final String id) {
        final Identifier identifier = ModConstants.modId(id);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, identifier, SoundEvent.createVariableRangeEvent(identifier));
    }

    /**
     * Initializes the sound event registry by forcing class loading.
     * This method is called during mod initialization.
     */
    @SuppressWarnings("EmptyMethod")
    public static void init() {
    }
}
